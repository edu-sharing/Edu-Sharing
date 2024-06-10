package org.alfresco.solr;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.util.OpenBitSet;
import org.apache.solr.core.SolrCore;
import org.apache.solr.search.SolrIndexReader;
import org.apache.solr.search.SolrIndexSearcher;

public class ESSlaveAlfrescoSolrEventListener extends AlfrescoSolrEventListener {

	public ESSlaveAlfrescoSolrEventListener(SolrCore core) {
		super(core);
	}
	
	@Override
	public LinkedList<CacheMatch> buildCacheUpdateOperations(boolean hasNew, CacheSection[] before,
			CacheSection[] after, IndexReader[] afterIndexReaders) {
		try{
			return super.buildCacheUpdateOperations(hasNew, before, after, afterIndexReaders);
		}catch(IllegalStateException e){
			System.out.println("edu-sharing fix");
			System.out.println(e.getMessage());
			return new LinkedList<CacheMatch>();
		}
	}
	
	 /*
     * (non-Javadoc)
     * @see org.apache.solr.core.SolrEventListener#newSearcher(org.apache.solr.search.SolrIndexSearcher,
     * org.apache.solr.search.SolrIndexSearcher)
     */
    @Override
    public void newSearcher(SolrIndexSearcher newSearcher, SolrIndexSearcher currentSearcher)
    {
        SolrIndexReader newReader = newSearcher.getReader();
        log.info("Max " + newReader.maxDoc());
        log.info("Docs " + newReader.numDocs());
        log.info("Deleted " + newReader.numDeletedDocs());

        long startTime = System.nanoTime();

        CacheEntry[] indexedByDocId = new CacheEntry[newReader.maxDoc()];
        HashSet<String> globalReaders = new HashSet<String>();
        OpenBitSet allLeafDocs = new OpenBitSet();
        long[] aclIdByDocId = new long[newReader.maxDoc()];
        long[] txByDocId = new long[newReader.maxDoc()];
        long[] aclTxByDocId = new long[newReader.maxDoc()];
        for (int i = 0; i < aclIdByDocId.length; i++)
        {
            aclIdByDocId[i] = -1;
            txByDocId[i] = -1;
            aclTxByDocId[i] = -1;
        }
      
        OpenBitSet deleted = new OpenBitSet();
        OwnerIdManager ownerIdManager = new OwnerIdManager();

        Map<Long, CacheEntry> unmatchedByDBID = new HashMap<>();

        if (currentSearcher != null)
        {
            CacheEntry[] oldIndexedByDocId = (CacheEntry[]) currentSearcher.cacheLookup(ALFRESCO_CACHE, KEY_DBID_LEAF_PATH_BY_DOC_ID);
            long[] oldAclIdByDocId = (long[]) currentSearcher.cacheLookup(ALFRESCO_CACHE, KEY_ACL_ID_BY_DOC_ID);
            long[] oldTxIdByDocId = (long[]) currentSearcher.cacheLookup(ALFRESCO_CACHE, KEY_TX_ID_BY_DOC_ID);
            long[] oldAclTxIdByDocId = (long[]) currentSearcher.cacheLookup(ALFRESCO_CACHE, KEY_ACL_TX_ID_BY_DOC_ID);
            OpenBitSet oldAllLeafDocs = (OpenBitSet) currentSearcher.cacheLookup(ALFRESCO_CACHE, KEY_ALL_LEAF_DOCS);
            OwnerIdManager oldOwnerIdManager = (OwnerIdManager) currentSearcher.cacheLookup(ALFRESCO_CACHE, KEY_OWNER_ID_MANAGER);
            ownerIdManager.addAll(oldOwnerIdManager);

            ConcurrentMap<Long, Long> addedLeaves = (ConcurrentMap<Long, Long>) currentSearcher.cacheLookup(ALFRESCO_CACHE, KEY_ADDED_LEAVES);
            ConcurrentMap<Long, Long> addedAux = (ConcurrentMap<Long, Long>) currentSearcher.cacheLookup(ALFRESCO_CACHE, KEY_ADDED_AUX);
            ConcurrentMap<Long, Long> addedAcl = (ConcurrentMap<Long, Long>) currentSearcher.cacheLookup(ALFRESCO_CACHE, KEY_ADDED_ACL);
            ConcurrentMap<Long, Long> addedTx = (ConcurrentMap<Long, Long>) currentSearcher.cacheLookup(ALFRESCO_CACHE, KEY_ADDED_TX);
            ConcurrentMap<Long, Long> addedAclTx = (ConcurrentMap<Long, Long>) currentSearcher.cacheLookup(ALFRESCO_CACHE, KEY_ADDED_ACL_TX);
            ConcurrentMap<Long, Long> updatedLeaves = (ConcurrentMap<Long, Long>) currentSearcher.cacheLookup(ALFRESCO_CACHE, KEY_UPDATED_LEAVES);
            ConcurrentMap<Long, Long> updatedAux = (ConcurrentMap<Long, Long>) currentSearcher.cacheLookup(ALFRESCO_CACHE, KEY_UPDATED_AUX);
            ConcurrentMap<Long, Long> updatedAcl = (ConcurrentMap<Long, Long>) currentSearcher.cacheLookup(ALFRESCO_CACHE, KEY_UPDATED_ACL);
            ConcurrentMap<Long, Long> updatedTx = (ConcurrentMap<Long, Long>) currentSearcher.cacheLookup(ALFRESCO_CACHE, KEY_UPDATED_TX);
            ConcurrentMap<Long, Long> updatedAclTx = (ConcurrentMap<Long, Long>) currentSearcher.cacheLookup(ALFRESCO_CACHE, KEY_UPDATED_ACL_TX);
            ConcurrentMap<Long, Long> deletedLeaves = (ConcurrentMap<Long, Long>) currentSearcher.cacheLookup(ALFRESCO_CACHE, KEY_DELETED_LEAVES);
            ConcurrentMap<Long, Long> deletedAux = (ConcurrentMap<Long, Long>) currentSearcher.cacheLookup(ALFRESCO_CACHE, KEY_DELETED_AUX);
            ConcurrentMap<Long, Long> deletedAcl = (ConcurrentMap<Long, Long>) currentSearcher.cacheLookup(ALFRESCO_CACHE, KEY_DELETED_ACL);
            ConcurrentMap<Long, Long> deletedTx = (ConcurrentMap<Long, Long>) currentSearcher.cacheLookup(ALFRESCO_CACHE, KEY_DELETED_TX);
            ConcurrentMap<Long, Long> deletedAclTx = (ConcurrentMap<Long, Long>) currentSearcher.cacheLookup(ALFRESCO_CACHE, KEY_DELETED_ACL_TX);
            AtomicBoolean deleteAll = (AtomicBoolean) currentSearcher.cacheLookup(ALFRESCO_CACHE, KEY_DELETE_ALL);
            AtomicBoolean checkCache = (AtomicBoolean) currentSearcher.cacheLookup(ALFRESCO_CACHE, KEY_CHECK_CACHE);

            if (checkCache == null)
            {
                checkCache = new AtomicBoolean(false);
            }

            boolean hasNew = (addedLeaves.size()
                    + addedAux.size() + addedAcl.size() + addedTx.size() + addedAclTx.size() + updatedLeaves.size() + updatedAux.size() + updatedAcl.size() + updatedTx.size() + updatedAclTx
                    .size()) > 0;

            if (newReader.maxDoc() == 0)
            {
                // nothing to do
            }
            else if ((oldIndexedByDocId == null) || (oldAclIdByDocId == null) || (oldTxIdByDocId == null) || (oldAclTxIdByDocId == null) || (oldAllLeafDocs == null) || (oldOwnerIdManager == null))
            {
                log.warn("Recover from missing cache");
                buildCacheForReader(indexedByDocId, allLeafDocs, aclIdByDocId, txByDocId, aclTxByDocId, newReader, 0, newReader.maxDoc(), unmatchedByDBID, ownerIdManager);

            }
            else if (deleteAll.get())
            {
                buildCacheForReader(indexedByDocId, allLeafDocs, aclIdByDocId, txByDocId, aclTxByDocId, newReader, 0, newReader.maxDoc(), unmatchedByDBID, ownerIdManager);
            }
            else
            {
            	System.out.println("edusharing fix do from the beginning");
            	buildCacheForReader(indexedByDocId, allLeafDocs, aclIdByDocId, txByDocId, aclTxByDocId, newReader, 0, newReader.maxDoc(), unmatchedByDBID, ownerIdManager);
            }

        }
        else
        {
            buildCacheForReader(indexedByDocId, allLeafDocs, aclIdByDocId, txByDocId, aclTxByDocId, newReader, 0, newReader.maxDoc(), unmatchedByDBID, ownerIdManager);
        }

        long endTime = System.nanoTime();
        log.info("Core cache rebuilt in " + ((endTime - startTime) / (1.0e9)));
        startTime = System.nanoTime();

        CacheEntry[] indexedOderedByAclIdThenDoc = new CacheEntry[(int) allLeafDocs.cardinality()];
        CacheEntry[] indexedOderedByOwnerIdThenDoc = new CacheEntry[(int) allLeafDocs.cardinality()];

        int doc = -1;
        int pos = 0;
        while ((doc = allLeafDocs.nextSetBit(doc + 1)) != -1)
        {
            CacheEntry entry = indexedByDocId[doc];
            indexedOderedByAclIdThenDoc[pos] = entry;
            indexedOderedByOwnerIdThenDoc[pos] = entry;
            pos++;
        }

        Arrays.sort(indexedOderedByAclIdThenDoc, new Comparator<CacheEntry>()
        {

            @Override
            public int compare(CacheEntry o1, CacheEntry o2)
            {
                if (o2 == null)
                {
                    if (o1 == null)
                    {
                        return 0;
                    }

                    else
                    {
                        return -1; // nulls at the end
                    }
                }
                else
                {
                    if (o1 == null)
                    {
                        return 1;
                    }
                    else
                    {
                        long diff = o1.getAclid() - o2.getAclid();
                        if (diff == 0L)
                        {
                            return o1.getLeaf() - o2.getLeaf();
                        }
                        else
                        {
                            return (diff > 0L) ? 1 : -1;
                        }
                    }
                }

            }
        });

        // build lookups

        Map<Long, AclLookUp> alcLookUp = new HashMap<>();

        AclLookUp currentAclLookUp = null;
        for (int i = 0; i < indexedOderedByAclIdThenDoc.length; i++)
        {
            CacheEntry entry = indexedOderedByAclIdThenDoc[i];
            if (entry != null)
            {
                if (currentAclLookUp == null)
                {
                    currentAclLookUp = new AclLookUp(entry.getAclid(), i);
                }
                else
                {
                    if (currentAclLookUp.aclid == entry.aclid)
                    {
                        // carry on
                    }
                    else
                    {
                        // acl id has changed - new set
                        currentAclLookUp.setEnd(i);
                        AclLookUp next = new AclLookUp(entry.getAclid(), i);
                        alcLookUp.put(Long.valueOf(currentAclLookUp.aclid), currentAclLookUp);
                        currentAclLookUp = next;
                    }
                }
            }
            else
            {
                // found first null we are done
                if (currentAclLookUp != null)
                {
                    currentAclLookUp.setEnd(i);
                    alcLookUp.put(Long.valueOf(currentAclLookUp.aclid), currentAclLookUp);
                }
                break;
            }
        }
        if (currentAclLookUp != null)
        {
            currentAclLookUp.setEnd(indexedOderedByAclIdThenDoc.length);
            alcLookUp.put(Long.valueOf(currentAclLookUp.aclid), currentAclLookUp);
        }

        Arrays.sort(indexedOderedByOwnerIdThenDoc, new Comparator<CacheEntry>()
        {

            @Override
            public int compare(CacheEntry o1, CacheEntry o2)
            {
                if (o2 == null)
                {
                    if (o1 == null)
                    {
                        return 0;
                    }

                    else
                    {
                        return -1; // nulls at the end
                    }
                }
                else
                {
                    if (o1 == null)
                    {
                        return 1;
                    }
                    else
                    {
                        int diff = o1.getOwner() - o2.getOwner();
                        if (diff == 0)
                        {
                            return o1.getLeaf() - o2.getLeaf();
                        }
                        else
                        {
                            return diff;
                        }
                    }
                }

            }
        });

        // build lookups

        Map<String, OwnerLookUp> ownerLookUp = new HashMap<>();

        OwnerLookUp currentOwnerLookUp = null;
        for (int i = 0; i < indexedOderedByOwnerIdThenDoc.length; i++)
        {
            CacheEntry entry = indexedOderedByOwnerIdThenDoc[i];
            if (entry != null)
            {
                if (currentOwnerLookUp == null)
                {
                    currentOwnerLookUp = new OwnerLookUp(entry.getOwner(), i);
                }
                else
                {
                    if (currentOwnerLookUp.owner == entry.owner)
                    {
                        // carry on
                    }
                    else
                    {
                        // acl id has changed - new set
                        currentOwnerLookUp.setEnd(i);
                        OwnerLookUp next = new OwnerLookUp(entry.getOwner(), i);
                        try
                        {
                            ownerLookUp.put(ownerIdManager.get(currentOwnerLookUp.owner), currentOwnerLookUp);
                        }
                        catch (IndexOutOfBoundsException e)
                        {
                            log.warn("  " + ownerIdManager);
                            log.warn("  looking for " + currentOwnerLookUp.owner);
                            throw e;
                        }
                        currentOwnerLookUp = next;
                    }
                }
            }
            else
            {
                // found first null we are done
                if (currentOwnerLookUp != null)
                {
                    currentOwnerLookUp.setEnd(i);
                    try
                    {
                        ownerLookUp.put(ownerIdManager.get(currentOwnerLookUp.owner), currentOwnerLookUp);
                    }
                    catch (IndexOutOfBoundsException e)
                    {
                        log.warn("  " + ownerIdManager);
                        log.warn("  looking for " + currentOwnerLookUp.owner);
                        throw e;
                    }
                }
                break;
            }
        }
        if (currentOwnerLookUp != null)
        {
            currentOwnerLookUp.setEnd(indexedOderedByOwnerIdThenDoc.length);
            try
            {
                ownerLookUp.put(ownerIdManager.get(currentOwnerLookUp.owner), currentOwnerLookUp);
            }
            catch (IndexOutOfBoundsException e)
            {
                log.warn("  " + ownerIdManager);
                log.warn("  looking for " + currentOwnerLookUp.owner);
                throw e;
            }
        }

        endTime = System.nanoTime();
        log.info("Derived caches rebuilt in " + ((endTime - startTime) / (1.0e9)));

        newSearcher.cacheInsert(ALFRESCO_CACHE, KEY_DBID_LEAF_PATH_BY_DOC_ID, indexedByDocId);
        newSearcher.cacheInsert(ALFRESCO_CACHE, KEY_ACL_ID_BY_DOC_ID, aclIdByDocId);
        newSearcher.cacheInsert(ALFRESCO_CACHE, KEY_TX_ID_BY_DOC_ID, txByDocId);
        newSearcher.cacheInsert(ALFRESCO_CACHE, KEY_ACL_TX_ID_BY_DOC_ID, aclTxByDocId);

        globalReaders.add("ROLE_OWNER");
        globalReaders.add("ROLE_ADMINISTRATOR");
        newSearcher.cacheInsert(ALFRESCO_CACHE, KEY_GLOBAL_READERS, globalReaders);

        newSearcher.cacheInsert(ALFRESCO_CACHE, KEY_ALL_LEAF_DOCS, allLeafDocs);

        newSearcher.cacheInsert(ALFRESCO_CACHE, KEY_ACL_LOOKUP, alcLookUp);
        newSearcher.cacheInsert(ALFRESCO_CACHE, KEY_DBID_LEAF_PATH_BY_ACL_ID_THEN_LEAF, indexedOderedByAclIdThenDoc);

        newSearcher.cacheInsert(ALFRESCO_CACHE, KEY_OWNER_LOOKUP, ownerLookUp);
        newSearcher.cacheInsert(ALFRESCO_CACHE, KEY_DBID_LEAF_PATH_BY_OWNER_ID_THEN_LEAF, indexedOderedByOwnerIdThenDoc);
        
        newSearcher.cacheInsert(ALFRESCO_CACHE, KEY_OWNER_ID_MANAGER, ownerIdManager);

    }
}
