package org.edu_sharing.repository.server.importer.collections;

import org.apache.log4j.Logger;
import org.apache.poi.util.IOUtils;
import org.apache.tika.Tika;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.importer.collections.xmlclasses.Collections;
import org.edu_sharing.repository.server.importer.collections.xmlclasses.Collections.Collection.Property;
import org.edu_sharing.restservices.CollectionDao;
import org.edu_sharing.service.collection.Collection;
import org.edu_sharing.service.collection.CollectionService;
import org.edu_sharing.service.collection.CollectionServiceFactory;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class CollectionImporter {	
	private static Logger logger = Logger.getLogger(CollectionImporter.class);

	private int importCount;

	private ZipInputStream zip;

	private ByteArrayOutputStream buffer;
	/**
	 * Import file (either a single xml or a zip file including exactly one xml file (any name) and any number of images to refer to)
	 * @param parent
	 * @param is
	 * @return
	 * @throws Throwable
	 */
	public int importFile(String parent,InputStream is) throws Throwable {
		importCount=0;
	    zip=null;
        buffer=new ByteArrayOutputStream();
        IOUtils.copy(is,buffer);
        String mimetype=new Tika().detect(buffer.toByteArray());
        if(mimetype.equals("application/xml")) {
        	readXML(parent,new ByteArrayInputStream(buffer.toByteArray()));
        }
        else if(mimetype.equals("application/zip")) {
        	InputStream xml;
        	int pos=0;
        	while(true) {
	            xml=findFile("*.xml",false,pos);
	            if(xml==null && pos==0) {
	            	throw new IllegalArgumentException("Zip file contained no *.xml file");            
	            }
	            if(xml==null)
	            	break;
            	readXML(parent,xml);
	            pos++;
        	}
        }
        else {
        	throw new IllegalArgumentException("Only application/xml or application/zip files are allowed");            
        }
        return importCount;
	}
	private InputStream findFile(String name,boolean caseSensitive, int pos) throws IOException {
        zip = new ZipInputStream(new ByteArrayInputStream(buffer.toByteArray()));
		ZipEntry entry;
		int count=0;
		if(!caseSensitive) {
			name=name.toLowerCase();
		}
		while ((entry = zip.getNextEntry())!=null) {
			String entryName=entry.getName();
			if(!caseSensitive) {
				entryName=entryName.toLowerCase();
			}
		    if (name.startsWith("*") && entryName.endsWith(name.substring(1))) {
		    	if(count++==pos)
		    		break;
		    }
			if (entryName.equals(name)) {
		    	if(count++==pos)
		    		break;
			}
		}
		if(entry==null)
			return null;
		ByteArrayOutputStream out=new ByteArrayOutputStream();
		IOUtils.copy(zip, out);
		return new ByteArrayInputStream(out.toByteArray());
		}
    private void readXML(String parent, InputStream is) throws Throwable {
    	JAXBContext jc = JAXBContext.newInstance(Collections.class);
        Unmarshaller unmarshaller = jc.createUnmarshaller();
    	Collections collections = (Collections) unmarshaller.unmarshal(is);
        // process all collections
        for (Collections.Collection collection : collections.getCollection()) {
        		createCollection(parent, collection);
        }
	}

	// created a collection and returns the nodeID
    private String createCollection(String parentId, Collections.Collection collection) throws Throwable {
    	String collectionID = null;
		try {
			CollectionService collectionService=CollectionServiceFactory.getLocalService();
			NodeService nodeService=NodeServiceFactory.getLocalService();
			
			// set main attributes and create collection
			Collection collectionObj = new Collection();
			collectionObj.setTitle(collection.getTitle()!=null ? collection.getTitle().trim() : "");
			collectionObj.setDescription(collection.getDescription()!=null ? collection.getDescription().trim() : "");
			collectionObj.setColor(collection.getColor()!=null ? collection.getColor() : CCConstants.COLLECTION_COLOR_DEFAULT);
			collectionObj.setType(collection.getType()!=null ? collection.getType() : CCConstants.COLLECTIONTYPE_DEFAULT);
			collectionObj.setScope(collection.getScope()!=null ? collection.getScope() : CollectionDao.Scope.EDU_ALL.name());
			collectionID = collectionService.createAndSetScope(parentId, collectionObj).getNodeId();
			
			importCount++;
			
			// set custom collection properties
			if(collection.getProperty()!=null) {
				Map<String,String[]> properties=new HashMap<>();
				for(Property property : collection.getProperty()) {
					properties.put(property.getKey(),property.getValue().toArray(new String[0]));
				}
				nodeService.updateNode(collectionID,NodeServiceHelper.transformShortToLongProperties(properties));    			
			}
			if(collection.getImage()!=null) {
				InputStream is=null;
				
				try {
					if(zip!=null) {
	    				is=findFile(collection.getImage(),true,0);
	    			}
	    			if(is==null) {
		    			URL url = new URL(collection.getImage());   
		    			URLConnection connection = url.openConnection();
				        connection.setDoOutput(true);
				        is = connection.getInputStream();
	    			}
				
					collectionService.writePreviewImage(collectionID, is, "image");
	    			is.close();
				}catch(Throwable t) {
					t.printStackTrace();
				}
			}
		}catch(Throwable t) {
    		throw new Throwable("Failed while importing collection "+collection.getTitle()+": "+t.getMessage(),t);
    	}
		// create sub collections recursively
		Collections subCollections = collection.getCollections();
		if ((subCollections!=null) && (subCollections.getCollection().size()>0)) {
	        for (Collections.Collection subCollection : subCollections.getCollection()) {
	        	createCollection(collectionID, subCollection);
	        }		
		}
		return collectionID;

    }

    
		
}
