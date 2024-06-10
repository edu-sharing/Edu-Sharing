package org.edu_sharing.alfresco.policy;

import com.typesafe.config.Config;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.ContentServicePolicies.OnContentUpdatePolicy;
import org.alfresco.repo.copy.CopyServicePolicies;
import org.alfresco.repo.node.NodeServicePolicies.OnCreateNodePolicy;
import org.alfresco.repo.node.NodeServicePolicies.OnUpdatePropertiesPolicy;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.version.VersionModel;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.lock.LockService;
import org.alfresco.service.cmr.lock.LockStatus;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.thumbnail.ThumbnailService;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.cmr.version.VersionType;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.alfresco.service.search.CMISSearchHelper;
import org.edu_sharing.metadataset.v2.MetadataReader;
import org.edu_sharing.metadataset.v2.MetadataWidget;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.forms.VCardTool;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.cache.RepositoryCache;
import org.json.JSONObject;
import org.quartz.Scheduler;
import org.springframework.security.crypto.codec.Base64;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author rudi
 *
 *important only for IO's
 *
 * onCreateNode:
 * - content -> IO
 * - folder -> Map
 * - LOM metadata:
 * * - title
 * * - technical location
 * * - contributer
 *
 * onContentUpdate:
 *
 * - if IO
 * - set LOM technical size
 * - create preview only if new content
 * - fetch resourcinfo action
 * - Create version history and version
 * * - only if create_version value is true (default = true)
 */
public class NodeCustomizationPolicies implements OnContentUpdatePolicy, OnCreateNodePolicy, OnUpdatePropertiesPolicy, CopyServicePolicies.OnCopyCompletePolicy {
	/**
	 * Thread local holding the current context id as defined in the client.config.xml
	 */
	static ThreadLocal<String> eduSharingContext = new ThreadLocal<String>();

	/* Some safe properties they're not necessary in the mds, but the client is allowed to define */
	public static final String[] SAFE_PROPS = new String[]{
			CCConstants.CM_NAME,
			CCConstants.CM_PROP_TITLE,
			CCConstants.CM_PROP_DESCRIPTION,
			CCConstants.LOM_PROP_GENERAL_TITLE,
			CCConstants.LOM_PROP_TECHNICAL_FORMAT,
			CCConstants.CCM_PROP_IO_WWWURL,
			CCConstants.CCM_PROP_IO_CREATE_VERSION,
			CCConstants.CCM_PROP_IO_VERSION_COMMENT,
			CCConstants.CCM_PROP_CCRESSOURCETYPE,
			CCConstants.CCM_PROP_CCRESSOURCESUBTYPE,
			CCConstants.CCM_PROP_CCRESSOURCEVERSION,
			CCConstants.CCM_PROP_WF_INSTRUCTIONS,
			CCConstants.CCM_PROP_WF_PROTOCOL,
			CCConstants.CCM_PROP_WF_RECEIVER,
			CCConstants.CCM_PROP_WF_STATUS,
			CCConstants.CCM_PROP_IO_IMPORT_BLOCKED,
			CCConstants.CCM_PROP_MAP_COLLECTIONREMOTEID,
			CCConstants.CM_PROP_METADATASET_EDU_METADATASET,
			CCConstants.CM_PROP_METADATASET_EDU_FORCEMETADATASET,
			CCConstants.CCM_PROP_EDITOR_TYPE,
			CCConstants.CCM_PROP_TOOL_OBJECT_TOOLINSTANCEREF,
			CCConstants.CCM_PROP_SAVED_SEARCH_REPOSITORY,
			CCConstants.CCM_PROP_SAVED_SEARCH_MDS,
			CCConstants.CCM_PROP_SAVED_SEARCH_QUERY,
			CCConstants.CCM_PROP_SAVED_SEARCH_PARAMETERS,
			CCConstants.CCM_PROP_AUTHOR_FREETEXT,
			CCConstants.CCM_PROP_CHILDOBJECT_ORDER,
			CCConstants.CCM_PROP_LINKTYPE,
			CCConstants.CCM_PROP_TOOL_INSTANCE_KEY,
			CCConstants.CCM_PROP_TOOL_INSTANCE_SECRET,
			CCConstants.CCM_PROP_SERVICE_NODE_NAME,
			CCConstants.CCM_PROP_SERVICE_NODE_DESCRIPTION,
			CCConstants.CCM_PROP_SERVICE_NODE_TYPE,
			CCConstants.CCM_PROP_SERVICE_NODE_DATA,
			CCConstants.CCM_PROP_IO_REF_VIDEO_VTT,
			CCConstants.CCM_PROP_MAP_REF_TARGET,
			CCConstants.CCM_PROP_RESTRICTED_ACCESS,
			CCConstants.CCM_PROP_RESTRICTED_ACCESS_PERMISSIONS,
	};
	/**
	 * These are the properties that will be copied to all io_reference nodes inside collections
	 * if the original node gets changed
	 */
	private static final Collection<String> IO_REFERENCE_COPY_PROPERTIES = new ArrayList<>(
			Arrays.asList(
					CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY,
					CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_LOCALE,
					CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_VERSION,
					CCConstants.CCM_PROP_IO_COMMONLICENSE_QUESTIONSALLOWED,
					CCConstants.CCM_PROP_IO_LICENSE,
					CCConstants.CCM_PROP_IO_LICENSE_DESCRIPTION,
					CCConstants.CCM_PROP_IO_LICENSE_FROM,
					CCConstants.CCM_PROP_IO_LICENSE_PROFILE_URL,
					CCConstants.CCM_PROP_IO_LICENSE_SOURCE_URL,
					CCConstants.CCM_PROP_IO_LICENSE_TITLE_OF_WORK,
					CCConstants.CCM_PROP_IO_LICENSE_TO,
					CCConstants.CCM_PROP_IO_LICENSE_VALID,

					// fix for 4.2, override changed content resource props
					CCConstants.CCM_PROP_CCRESSOURCETYPE,
					CCConstants.CCM_PROP_CCRESSOURCESUBTYPE,
					CCConstants.CCM_PROP_CCRESSOURCEVERSION,

					// fix for 4.2, override all relevant metadata when changed on original
					CCConstants.LOM_PROP_GENERAL_TITLE,
					CCConstants.LOM_PROP_GENERAL_KEYWORD,
					CCConstants.LOM_PROP_GENERAL_DESCRIPTION,
					CCConstants.LOM_PROP_EDUCATIONAL_LEARNINGRESOURCETYPE
			)
	);

	public static final String[] LICENSE_PROPS = new String[]{
			CCConstants.LOM_PROP_RIGHTS_RIGHTS_DESCRIPTION,
			CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY,
			CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_LOCALE,
			CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_VERSION,
			CCConstants.CCM_PROP_IO_LICENSE_TITLE_OF_WORK,
			CCConstants.CCM_PROP_IO_LICENSE_SOURCE_URL,
			CCConstants.CCM_PROP_IO_LICENSE_PROFILE_URL,
			CCConstants.CCM_PROP_IO_COMMONLICENSE_QUESTIONSALLOWED
	};

	static{
		// add all contributor array maps (e.g. author) to be copied to collection refs
		IO_REFERENCE_COPY_PROPERTIES.addAll(CCConstants.getLifecycleContributerPropsMap().values());
		IO_REFERENCE_COPY_PROPERTIES.addAll(CCConstants.getMetadataContributerPropsMap().values());
	}


	static Logger logger = Logger.getLogger(NodeCustomizationPolicies.class);

	ActionService actionService;

	NodeService nodeService;

	VersionService versionService;

	PersonService personService;

	PolicyComponent policyComponent;

	ContentService contentService;

	PermissionService permissionService;

	LockService lockService;

	ThumbnailService thumbnailService;

	BehaviourFilter policyBehaviourFilter;

	TransactionService transactionService;

	private SearchService searchService;

	Scheduler scheduler;

	/**
	 * The current context, or the default value @EDUCONTEXT_DEFAULT
	 * @return
	 */
	public static String getEduSharingContext(){
		if(eduSharingContext.get()==null){
			return CCConstants.EDUCONTEXT_DEFAULT;
		}
		return eduSharingContext.get();
	}
	public static void setEduSharingContext(String context) {
		eduSharingContext.set(context);
	}

	public void init() {

		policyComponent.bindClassBehaviour(OnCreateNodePolicy.QNAME, ContentModel.TYPE_CONTENT, new JavaBehaviour(this, "onCreateNode"));
		policyComponent.bindClassBehaviour(OnCreateNodePolicy.QNAME, ContentModel.TYPE_FOLDER, new JavaBehaviour(this, "onCreateNode"));
		policyComponent.bindClassBehaviour(OnCreateNodePolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_IO), new JavaBehaviour(this, "onCreateNode"));
		policyComponent.bindClassBehaviour(OnCreateNodePolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_MAP), new JavaBehaviour(this, "onCreateNode"));

		policyComponent.bindClassBehaviour(OnContentUpdatePolicy.QNAME, ContentModel.TYPE_CONTENT, new JavaBehaviour(this, "onContentUpdate"));
		policyComponent.bindClassBehaviour(OnContentUpdatePolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_IO), new JavaBehaviour(this, "onContentUpdate"));

		// update the cache for changed previews for folders or collections
		policyComponent.bindClassBehaviour(OnContentUpdatePolicy.QNAME, CCConstants.CCM_PROP_MAP_ICON, new JavaBehaviour(this, "onContentUpdate"));
		policyComponent.bindClassBehaviour(OnContentUpdatePolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_MAP), new JavaBehaviour(this, "onContentUpdate"));

		//for async changed properties refresh node in cache
		policyComponent.bindClassBehaviour(OnUpdatePropertiesPolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_IO), new JavaBehaviour(this, "onUpdateProperties"));
		policyComponent.bindClassBehaviour(OnUpdatePropertiesPolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_MAP), new JavaBehaviour(this, "onUpdateProperties"));

		// for educontext copy complete
		for(String type : CCConstants.EDUCONTEXT_TYPES) {
			this.policyComponent.bindClassBehaviour(QName.createQName(NamespaceService.ALFRESCO_URI, "onCopyComplete"),
					QName.createQName(type), new JavaBehaviour(this, "onCopyComplete"));
		}
	}

	@Override
	public void onCopyComplete(QName classRef,
							   NodeRef sourceNodeRef,
							   NodeRef targetNodeRef,
							   boolean copyToNewNode,
							   Map<NodeRef,NodeRef> copyMap) {
		// add/override the previous node context
		copyMap.forEach((source,dest)-> {
			addCurrentContext(dest);
		});
	}

	@Override
	public void onContentUpdate(NodeRef nodeRef, boolean newContent) {

		logger.debug("nodeRef:" +  nodeRef.getId());

		if(QName.createQName(CCConstants.CCM_TYPE_IO).equals(nodeService.getType(nodeRef))){


			ContentReader reader = contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);

			LockStatus lockStatus = lockService.getLockStatus(nodeRef);
			long contentSize =  0l;
			if ((reader!=null) && (reader.getContentData()!=null)) contentSize = reader.getContentData().getSize();
			String mimetype = null;
			if (reader!=null) mimetype = reader.getMimetype();
			logger.debug(" reader.getContentData().getSize():"+ contentSize +" newContent:"+newContent+" LockStatus:"+lockStatus+" mimetype:"+mimetype);

			if(reader != null){
				nodeService.setProperty(nodeRef, QName.createQName(CCConstants.LOM_PROP_TECHNICAL_SIZE), reader.getContentData().getSize());
			}
			// only override / sync the technical format for non imported objects, because otherwise
			// the technical format come's via import and might be wrongly replaced
			if(contentSize > 0 && mimetype != null &&
					nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_REPLICATIONSOURCE)) == null &&
					!nodeService.hasAspect(nodeRef,QName.createQName(CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE))){
				nodeService.setProperty(nodeRef, QName.createQName(CCConstants.LOM_PROP_TECHNICAL_FORMAT), mimetype);
			}
			logger.debug("will do the resourceinfo. noderef:"+nodeRef);
			Action resourceInfoAction = actionService.createAction(CCConstants.ACTION_NAME_RESOURCEINFO);
			actionService.executeAction(resourceInfoAction, nodeRef, true, false);

			logger.debug("lockStatus:"+lockStatus);
			if(newContent
					&& (LockStatus.NO_LOCK.equals(lockStatus) || LockStatus.LOCK_EXPIRED.equals(lockStatus))
					&& (reader!=null) && (reader.getContentData()!=null) && reader.getContentData().getSize() > 0){

				new ThumbnailHandling().thumbnailHandling(nodeRef);
			}
			if(verifyMimetypeEnabled()) {
				if(newContent &&
						!nodeService.getProperty(nodeRef,ContentModel.PROP_NODE_UUID)
								.equals(nodeService.getProperty(nodeRef,QName.createQName(CCConstants.CCM_PROP_IO_ORIGINAL))))
				{
					logger.info("will not verifyMimetypeEnabled for copy");
				}else {
					String filename = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);
					verifyMimetype(reader, filename, getMimetypeAllowList(), LightbendConfigLoader.get().getBoolean("security.fileManagement.mimetypeVerification.allowUnknownMimetypes"));
				}
			}
			Long sizeLimit = LightbendConfigLoader.get().hasPath("security.fileManagement.limits.fileSize") ? LightbendConfigLoader.get().getLong("security.fileManagement.limits.fileSize") : null;
			if(sizeLimit != null) {
				if (!(newContent &&
						!nodeService.getProperty(nodeRef, ContentModel.PROP_NODE_UUID)
								.equals(nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_ORIGINAL))))) {
					verifyFileLimit(reader, sizeLimit);
				}
			}
			Action extractMetadataAction = actionService.createAction("extract-metadata");
			//dont do async cause it conflicts with preview creation when webdav is used
			actionService.executeAction(extractMetadataAction, nodeRef, true, false);

			Boolean createVersion = (Boolean)nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_CREATE_VERSION));
			//only create version when content is there, cause else in webdav for one file it goes twice here
			//when metadata changes the servlet does version creation
			if (createVersion && (reader!=null) && (reader.getContentData()!=null) && reader.getContentData().getSize() > 0) {
				if(versionService.getVersionHistory(nodeRef) == null) {
					Map<String, Serializable> transFormedProps = transformQNameKeyToString(nodeService.getProperties(nodeRef));

					//see https://issues.alfresco.com/jira/browse/ALF-12815
					//alfresco-4.0.d fix version should start with 1.0 not with 0.1
					transFormedProps.put(VersionModel.PROP_VERSION_TYPE, VersionType.MAJOR);
					versionService.createVersion(nodeRef,transFormedProps);
				}else{

					//prevent version will be created on revert
					if(!this.policyBehaviourFilter.isEnabled(nodeRef, ContentModel.ASPECT_VERSIONABLE)) {
						logger.debug(ContentModel.ASPECT_VERSIONABLE +" is not enabled on " + nodeRef );
					}else{
						versionService.createVersion(nodeRef,transformQNameKeyToString(nodeService.getProperties(nodeRef)));
					}

				}
			}
			// if may just the content gets updated, the refs still need to get a new modified date
			AuthenticationUtil.runAsSystem(()-> {
				//ResultSet result = fetchCollectionReferences(nodeRef);
				List<NodeRef> result = fetchCollectionReferencesByCmis(nodeRef);
				for (NodeRef ref : result) {
					transactionService.getRetryingTransactionHelper().doInTransaction(()-> {
						policyBehaviourFilter.disableBehaviour(ref, ContentModel.ASPECT_AUDITABLE);
						nodeService.setProperty(ref, QName.createQName(CCConstants.CM_PROP_C_MODIFIED), new Date());
						policyBehaviourFilter.disableBehaviour(ref, ContentModel.ASPECT_AUDITABLE);
						return null;
					});
				}
				return null;
			});
		}
		new RepositoryCache().remove(nodeRef.getId());
	}

	private void verifyFileLimit(ContentReader reader, Long sizeLimit) throws NodeFileSizeExceededException {
		if(reader.getSize() > sizeLimit) {
			throw new NodeFileSizeExceededException(sizeLimit, reader.getSize());
		}
	}

	private static Map<String, List<String>> getMimetypeAllowList() {
		Map<String, List<String>> allowList = LightbendConfigLoader.get().
				getConfig("security.fileManagement.mimetypeVerification.list").entrySet().stream().collect(
						Collectors.toMap(e -> StringUtils.strip(e.getKey(), "\""), e -> (List<String>) e.getValue().unwrapped(),
								(a, b) -> b, HashMap::new)
				);
		return allowList;
	}

	private static boolean verifyMimetypeEnabled() {
		return LightbendConfigLoader.get().getBoolean("security.fileManagement.mimetypeVerification.enabled");
	}

	static void verifyMimetype(ContentReader reader, String filename, Map<String, List<String>> allowList, boolean allowUnknownMimetypes) throws NodeMimetypeUnknownValidationException {
		// String reportedMimeType = reader.getMimetype();
		try {
			TikaConfig config = TikaConfig.getDefaultConfig();
			Detector detector = config.getDetector();
			TikaInputStream stream = TikaInputStream.get(reader.getContentInputStream());
			Metadata metadata = new Metadata();
			MediaType mediaType = detector.detect(stream, metadata);
			if(mediaType.equals(MediaType.OCTET_STREAM) && !allowUnknownMimetypes) {
				throw new NodeMimetypeUnknownValidationException();
			}
			String detectedMimeType = mediaType.getType() + "/" + mediaType.getSubtype();
			if(!allowList.containsKey(detectedMimeType)) {
				logger.warn("Mimetype not allowed: " + detectedMimeType);
				throw new NodeMimetypeValidationException(detectedMimeType);
			}
			List<String> fileExtensions = allowList.get(detectedMimeType);
			String extension = FilenameUtils.getExtension(filename);
			if(!(fileExtensions == null || fileExtensions.contains("*") || fileExtensions.stream().anyMatch(e -> e.equalsIgnoreCase(extension)))) {
				logger.warn("Found no allowed file extension for given mimetype: " + detectedMimeType);
				throw new NodeFileExtensionValidationException(fileExtensions, extension);
			}
		} catch (IOException e) {
			logger.warn("Tika mime type detection failed", e);
		}
	}

	@Override
	public void onCreateNode(ChildAssociationRef childAssocRef) {

		NodeRef eduNodeRef = childAssocRef.getChildRef();
		QName type = nodeService.getType(eduNodeRef);

		/**
		 * it seams it is not possible to get an order to policy execution
		 * so sometimes NodeCustomization.onCreateNode leads to ScopePolicies.beforeUpdateNode
		 * which leads to ScopeNodeWrongScopeException: trying to modify unscoped node from within a scope"
		 *
		 * disable policies for this node to prevent that beforeUpdateNode 
		 * checks the scope which will be there after update
		 */
		policyBehaviourFilter.disableBehaviour(eduNodeRef);

		logger.debug("nodeRef:" +  eduNodeRef.getId());

		try{

			/**
			 * set content types to io type
			 */
			if(ContentModel.TYPE_CONTENT.equals(type)){
				logger.debug("its a content node will transform to IO");
				// type
				QName ioType = QName.createQName(CCConstants.CCM_TYPE_IO);
				nodeService.setType(eduNodeRef,ioType);
				type = ioType;
			}

			/**
			 * generate metadata
			 */
			if(QName.createQName(CCConstants.CCM_TYPE_IO).equals(type)){
				logger.debug("will generate lom metadata");
				// props
				Map<QName, Serializable> props = nodeService.getProperties(eduNodeRef);
				String name = (String) props.get(ContentModel.PROP_NAME);

				//sometimes when this method is called the prop is already set so check if null i.e. lom importer
				if(nodeService.getProperty(eduNodeRef, QName.createQName(CCConstants.LOM_PROP_GENERAL_TITLE)) == null){
					// removed on 2017-04-20
					//nodeService.setProperty(eduNodeRef, QName.createQName(CCConstants.LOM_PROP_GENERAL_TITLE), name);
				}

				//for collections and solr set originalid (will be overwritten by collectionservice if a reference io is created)
				// the id will be written on copy, so may it already exists -> then keep it
				if(nodeService.getProperty(eduNodeRef,QName.createQName(CCConstants.CCM_PROP_IO_ORIGINAL))==null)
					nodeService.setProperty(eduNodeRef, QName.createQName(CCConstants.CCM_PROP_IO_ORIGINAL),eduNodeRef.getId());

				NodeRef personRef = personService.getPersonOrNull((String) props.get(ContentModel.PROP_CREATOR));
				String uid = null;
				String givenName = null;
				String surename = null;
				String email = null;
				if(personRef!=null) {
					Map<QName, Serializable> userInfo = nodeService.getProperties(personRef);

					uid = (String) userInfo.get(QName.createQName(CCConstants.PROP_USER_ESUID));
					givenName = (String) userInfo.get(ContentModel.PROP_FIRSTNAME);
					surename = (String) userInfo.get(ContentModel.PROP_LASTNAME);
					email = (String) userInfo.get(ContentModel.PROP_EMAIL);

					if (surename == null || surename.isEmpty()) {
						surename = (String) userInfo.get(ContentModel.PROP_USERNAME);
					}
				}
				String replicationSourceId = (String)nodeService.getProperty(eduNodeRef, QName.createQName(CCConstants.CCM_PROP_IO_REPLICATIONSOURCEID));

				/**
				 * only do this with local created objects. contributer info is delivered by importer
				 */
				if(replicationSourceId == null || replicationSourceId.trim().equals("")){
					Map<String, String> vcardMap = new HashMap<>();
					if(personRef!=null) {
						vcardMap.put(CCConstants.VCARD_URN_UID, uid);
						vcardMap.put(CCConstants.VCARD_GIVENNAME, givenName);
						vcardMap.put(CCConstants.VCARD_SURNAME, surename);
						vcardMap.put(CCConstants.VCARD_EMAIL, email);
					}
					String vcardString = VCardTool.hashMap2VCard(vcardMap);

					//sometimes when this method is called the prop is already set so check if null i.e. lom importer
					if(nodeService.getProperty(eduNodeRef,  QName.createQName(CCConstants.CCM_PROP_IO_REPL_METADATACONTRIBUTER_CREATOR)) == null){
						nodeService.setProperty(eduNodeRef, QName.createQName(CCConstants.CCM_PROP_IO_REPL_METADATACONTRIBUTER_CREATOR), vcardString);
					}
					if(nodeService.getProperty(eduNodeRef,  QName.createQName(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_AUTHOR)) == null){
						// Changed for 4.0: DESREPO-897 do not autofill author
						// nodeService.setProperty(eduNodeRef, QName.createQName(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_AUTHOR), vcardString);
					}

					String techLocValue = CCConstants.CCREP_PROTOCOL + ApplicationInfoList.getHomeRepository().getAppId() + "/" + eduNodeRef.getId();
					if(nodeService.getProperty(eduNodeRef,  QName.createQName(CCConstants.LOM_PROP_TECHNICAL_LOCATION)) == null){
						nodeService.setProperty(eduNodeRef, QName.createQName(CCConstants.LOM_PROP_TECHNICAL_LOCATION), techLocValue);
					}
				}

				// inherit the mds from the parent folder
				Serializable mdsForceSer = nodeService.getProperty(childAssocRef.getParentRef(), QName.createQName(CCConstants.CM_PROP_METADATASET_EDU_FORCEMETADATASET));
				boolean mdsForce=(mdsForceSer == null) ? false : (boolean)mdsForceSer;
				if(mdsForce){
					String mdsName=(String)nodeService.getProperty(childAssocRef.getParentRef(), QName.createQName(CCConstants.CM_PROP_METADATASET_EDU_METADATASET));
					nodeService.setProperty(eduNodeRef, QName.createQName(CCConstants.CM_PROP_METADATASET_EDU_METADATASET),mdsName);
				}

			}

			addCurrentContext(eduNodeRef);

			if(ContentModel.TYPE_FOLDER.equals(type)){
				// type
				logger.debug("its a folder node will transform to map");
				nodeService.setType(eduNodeRef, QName.createQName(CCConstants.CCM_TYPE_MAP));
			}
		}finally{
			policyBehaviourFilter.enableBehaviour(eduNodeRef);
		}
	}

	void addCurrentContext(NodeRef eduNodeRef) {
		try{
			policyBehaviourFilter.disableBehaviour(eduNodeRef);
			QName type = nodeService.getType(eduNodeRef);
			// do not add the context for references / they will copy it from their original
			if(CCConstants.EDUCONTEXT_TYPES.contains(type.toString()) && !nodeService.hasAspect(eduNodeRef, QName.createQName(CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE))){
				String context = getEduSharingContext();
				nodeService.setProperty(
						eduNodeRef,
						QName.createQName(CCConstants.CCM_PROP_EDUCONTEXT_NAME),
						context);
			}
		}finally{
			policyBehaviourFilter.enableBehaviour(eduNodeRef);
		}
	}

	@Override
	public void onUpdateProperties(NodeRef nodeRef, Map<QName, Serializable> before, Map<QName, Serializable> after) {

		//System.out.println("********** onUpdateProperties node("+nodeRef.getId()+")");

		// make the title like the name(when webdav rename is done), @TODO mybe just show the name in the gui
		String nameBefore = (String)before.get(ContentModel.PROP_NAME);
		String nameAfter =  (String)after.get(ContentModel.PROP_NAME);

		QName type = nodeService.getType(nodeRef);

		logger.debug("nodeRef:" + nodeRef +" nodeRef:" +  nodeRef.getId());


		if(type.equals(QName.createQName(CCConstants.CCM_TYPE_IO))){
			if(nameAfter != null && !nameAfter.equals(nameBefore)){
				// removed on 2017-04-20
				//nodeService.setProperty(nodeRef, QName.createQName(CCConstants.LOM_PROP_GENERAL_TITLE), nameAfter);

				if(verifyMimetypeEnabled() && nodeService.exists(nodeRef)) {
					if(nameBefore == null && !nodeService.getProperty(nodeRef,ContentModel.PROP_NODE_UUID)
							.equals(nodeService.getProperty(nodeRef,QName.createQName(CCConstants.CCM_PROP_IO_ORIGINAL)))){
						logger.info("will not verifyMimetypeEnabled for copy");
					}else{
						ContentReader reader = contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);
						if(reader != null && reader.exists()) {
							try {
								verifyMimetype(reader, nameAfter, getMimetypeAllowList(), LightbendConfigLoader.get().getBoolean("security.fileManagement.mimetypeVerification.allowUnknownMimetypes"));
							}catch(NodeMimetypeValidationException ignored) {
								// we ignore this since the node is now already uploaded. we only want to throw the
								// @NodeFileExtensionValidationException
							}
						}
					}
				}
			}
			// refresh all collection io's metadata
			// run as admin to refresh all, see ESPUB-633
			AuthenticationUtil.runAsSystem(()-> {
				List<NodeRef> result = fetchCollectionReferencesByCmis(nodeRef);
				for (NodeRef ref : result) {
					syncCollectionRefProps(nodeRef,ref, before, after,true, nodeService);
				}
				return null;
			});
		}

		// refresh Titel for Maps changed in webdav
		if(type.equals(QName.createQName(CCConstants.CCM_TYPE_MAP))){
			/**
			 * only do this when it's not a collection to keep special signs in title,
			 * we have to check for property instead of aspect here,
			 * cause aspect ccm:collection would not be present onCreate
			 */
			String collectionType = (String)after.get(QName.createQName(CCConstants.CCM_PROP_MAP_COLLECTIONTYPE));
			logger.debug("collectionType:" +collectionType);
			if(collectionType == null){
				if(nameAfter != null && !nameAfter.equals(nameBefore)){
					try{
						policyBehaviourFilter.disableBehaviour(nodeRef);
						nodeService.setProperty(nodeRef, ContentModel.PROP_TITLE, nameAfter);
					}finally{
						policyBehaviourFilter.enableBehaviour(nodeRef);
					}
				}
			}
		}

		// for async prozessed properties like exif: remove from cache
		new RepositoryCache().remove(nodeRef.getId());

		if(
				!nodeService.hasAspect(nodeService.getPrimaryParent(nodeRef).getParentRef(),
						QName.createQName(CCConstants.CCM_ASPECT_COLLECTION))
		) {
			// URL link update
			String beforeURL = null;
			String afterURL = null;
			for (QName qName : before.keySet())
				if ("wwwurl".equals(qName.getLocalName())) beforeURL = "" + before.get(qName);
			for (QName qName : after.keySet())
				if ("wwwurl".equals(qName.getLocalName())) afterURL = "" + after.get(qName);
			if ((afterURL != null) && (!afterURL.equals(beforeURL))) {

				logger.info("---> UPDATE/CREATE THUMBNAIL FOR LINK(" + afterURL + ") ON NODE(" + nodeRef.getId() + ")");

				String linktype = (String) after.get(QName.createQName(CCConstants.CCM_PROP_LINKTYPE));
				if (linktype != null && linktype.equals(CCConstants.CCM_VALUE_LINK_LINKTYPE_USER_GENERATED)) {
					generateWebsitePreview(nodeRef, afterURL);
				}
			}
		}

	}

	public static void syncCollectionRefProps(NodeRef nodeRef, NodeRef ref, Map<QName, Serializable> before, Map<QName, Serializable> after, boolean checkRefPropsForCustomization, NodeService nodeService) throws Exception {
		Map<QName, Serializable> ioColRefProperties = nodeService.getProperties(ref);
		// security check: make sure we have an object which really matches the solr query
		if (!nodeService.hasAspect(ref, QName.createQName(CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE)) || !ioColRefProperties.get(QName.createQName(CCConstants.CCM_PROP_IO_ORIGINAL)).equals(nodeRef.getId())) {
			logger.warn("CMIS query for node " + nodeRef.getId() + " returned node " + ref.getId() + ", but it's metadata do not match");
			return;
		}
		Set<String> props = new HashSet<>(Arrays.asList(SAFE_PROPS));
		props.addAll(Arrays.asList(LICENSE_PROPS));
		props.addAll(MetadataReader.getWidgetsByNode(ref,"de_DE").stream().
				map(MetadataWidget::getId).map(CCConstants::getValidGlobalName).
				collect(Collectors.toList()));

		Set<QName> propSet = Stream.concat(before.keySet().stream(), after.keySet().stream()).collect(Collectors.toSet());

		for(QName entry : propSet){
			if(entry.getLocalName().startsWith("lifecyclecontributer")
					|| entry.getLocalName().startsWith("metadatacontributer")){
				props.add(entry.toString());
			}
		}

		for (QName prop : propSet) {
			// the prop is contained in the mds of the node or a SAFE_PROP, than check if it still the original one -> replace it on the ref
			if (props.contains(prop.toString())) {
				if(checkRefPropsForCustomization){
					if(propertyEquals(before.get(prop), ioColRefProperties.get(prop))){
						ioColRefProperties.put(prop, after.get(prop));
					}
				}else{
					ioColRefProperties.put(prop, after.get(prop));
				}
			}
		}
		try {
			nodeService.setProperties(ref, ioColRefProperties);
		}catch(DuplicateChildNodeNameException e){
			logger.error(e.getMessage() +" try to rename");
			String originalName = (String)ioColRefProperties.get(ContentModel.PROP_NAME);
			for(int i = 2; i < 42;i++){
				ioColRefProperties.put(ContentModel.PROP_NAME, renameNode(originalName,i));
				try{
					nodeService.setProperties(ref, ioColRefProperties);
					break;
				}catch (DuplicateChildNodeNameException ex){
					logger.debug(e.getMessage()+" - will rename " +originalName +" "+i);
				}
			}
		}

		new RepositoryCache().remove(ref.getId());
	}

	public static String renameNode(String oldName,int number){
		String[] split=oldName.split("\\.");
		int i=split.length-2;
		i=Math.max(0, i);
		split[i]+=" - "+number;
		return String.join(".",split);
	}

	private static String propertyToString(Object p){
		if(p == null){
			return "";
		} else if(p instanceof MLText){
			if(((MLText) p).getDefaultValue() == null){
				return "";
			}
			return ((MLText) p).getDefaultValue();
		} else if(p instanceof String) {
			return (String)p;
		}
		return p.toString();
	}
	private static boolean isEmpty(Serializable p){
		if(p==null) {
			return true;
		}else if(p instanceof List){
			return ((List) p).isEmpty() || ((List) p).size()==1 && propertyToString(((List) p).get(0)).isEmpty();
		}else{
			String s=propertyToString(p);
			return s==null || s.isEmpty();
		}
	}
	private static boolean propertyEquals(Serializable p1, Serializable p2) {
		if(p1 instanceof MLText){
			p1 = ((MLText) p1).getDefaultValue();
		}
		if(p2 instanceof MLText){
			p2 = ((MLText) p2).getDefaultValue();
		}
		if(Objects.equals(p1,p2)) {
			return true;
		}
		if(isEmpty(p1) && isEmpty(p2)){
			return true;
		}
		if(p1 instanceof List && p2 instanceof List && ((List) p1).size()==((List) p2).size()){
			for(int i=0;i<((List) p1).size();i++){
				if(!Objects.equals(propertyToString(((List) p1).get(0)),propertyToString(((List) p2).get(0)))) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	public ResultSet fetchCollectionReferences(NodeRef nodeRef) {
		String query = "ASPECT:\"" + CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE + "\" AND @ccm\\:original:\"" + nodeRef.getId() + "\"";
		return searchService.query(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, SearchService.LANGUAGE_LUCENE, query);
	}

	public static List<NodeRef> fetchCollectionReferencesByCmis(NodeRef nodeRef){
		Map<String,Object> map = new HashMap<>();
		map.put(CCConstants.CCM_PROP_IO_ORIGINAL,nodeRef.getId());

		List<String> aspects = new ArrayList<>();
		aspects.add(CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE);
		logger.debug("cmis helper start");
		List<org.alfresco.service.cmr.repository.NodeRef> nodes = CMISSearchHelper.fetchNodesByTypeAndFilters(CCConstants.CCM_TYPE_IO,map,aspects,null,100000, null);
		logger.debug("cmis helper finished");
		return nodes;
	}


	@Deprecated
	private void writeBase64Image(NodeRef nodeRef, String previewImageBase64) {
		if (previewImageBase64!=null) {

			logger.info("---> GOT PREVIEW IMAGE BASE64: "+previewImageBase64.substring(21, 256)+" ...");
			final ContentWriter contentWriter = contentService.getWriter(nodeRef, QName.createQName("{http://www.campuscontent.de/model/1.0}userdefined_preview"), true);
			contentWriter.addListener(new ContentStreamListener() {
				@Override
				public void contentStreamClosed() throws ContentIOException {
					logger.info("Content Stream of preview Image was closed");
					logger.info(" ContentData size:" + contentWriter.getContentData().getSize());
					logger.info(" ContentData URL:" + contentWriter.getContentData().getContentUrl());
					logger.info(" ContentData MimeTyp:" + contentWriter.getContentData().getMimetype());
					logger.info(" ContentData ToString:" + contentWriter.getContentData().toString());
				}
			});
			contentWriter.setMimetype("image/png");
			byte[] imageData = Base64.decode(previewImageBase64.getBytes());
			if (imageData.length==0) logger.warn("LENGTH OF IMAGE BYTE DATA IS 0 !! ");
			try {
				ByteArrayInputStream is = new ByteArrayInputStream(imageData);
				contentWriter.putContent(is);
			} catch (Exception e) {
				logger.error("EXCEPTION:");
				e.printStackTrace();
			}
			logger.info("---> OK IMAGE WRITTEN");

		} else {
			logger.warn("---> NO PREVIEW IMAGE");
		}
	}
	private void writeImage(NodeRef nodeRef, byte[] previewImage) {
		if (previewImage!=null) {
			final ContentWriter contentWriter = contentService.getWriter(nodeRef, QName.createQName("{http://www.campuscontent.de/model/1.0}userdefined_preview"), true);
			contentWriter.addListener(new ContentStreamListener() {
				@Override
				public void contentStreamClosed() throws ContentIOException {
					logger.debug("Content Stream of preview Image was closed");
					logger.debug(" ContentData size:" + contentWriter.getContentData().getSize());
					logger.debug(" ContentData URL:" + contentWriter.getContentData().getContentUrl());
					logger.debug(" ContentData MimeTyp:" + contentWriter.getContentData().getMimetype());
					logger.debug(" ContentData ToString:" + contentWriter.getContentData().toString());
				}
			});
			contentWriter.setMimetype("image/png");
			try {
				ByteArrayInputStream is = new ByteArrayInputStream(previewImage);
				contentWriter.putContent(is);
			} catch (Exception e) {
				logger.error("Could not write preview image", e);
			}

		}
	}

	public  void generateWebsitePreview(NodeRef nodeRef, String url) {
		if(nodeRef == null || url == null) {
			return;
		}
		String previewImageBase64 = getPreviewFromURL(url);
		if(previewImageBase64 != null) {
			writeBase64Image(nodeRef, previewImageBase64);
		} else {
			byte[] previewImage = getPreviewFromURLPlaywright(url);
			if(previewImage == null) {
				previewImage = getPreviewFromURLSplash(url);
			}
			if(previewImage != null){
				writeImage(nodeRef, previewImage);
			}
		}
	}

	/**
	 * edu-sharing for setting version props
	 * @param props
	 * @return
	 */
	Map<String,Serializable> transformQNameKeyToString(Map<QName, Serializable> props){
		Map<String,Serializable> result = new HashMap<>();
		for(Map.Entry<QName,Serializable> entry : props.entrySet()){
			result.put(entry.getKey().toString(), entry.getValue());
		}
		return result;
	}

	public void setActionService(ActionService actionService) {
		this.actionService = actionService;
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}
	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}
	public void setVersionService(VersionService versionService) {
		this.versionService = versionService;
	}

	public void setPolicyComponent(PolicyComponent policyComponent) {
		this.policyComponent = policyComponent;
	}

	public void setPersonService(PersonService personService) {
		this.personService = personService;
	}

	public void setContentService(ContentService contentService) {
		this.contentService = contentService;
	}

	public void setPermissionService(PermissionService permissionService) {
		this.permissionService = permissionService;
	}

	public void setLockService(LockService lockService) {
		this.lockService = lockService;
	}

	public void setThumbnailService(ThumbnailService thumbnailService) {
		this.thumbnailService = thumbnailService;
	}

	public void setTransactionService(TransactionService transactionService) {
		this.transactionService = transactionService;
	}

	/*
	 * handle NULL when not possible or deactivated
	 */
	@Deprecated
	public static String getPreviewFromURL(String httpURL) {

		String websitePreviewRenderService = "";
		try {
			websitePreviewRenderService = ApplicationInfoList.getHomeRepository().getWebsitepreviewrenderservice();
		} catch (Exception e) {
			logger.debug(CCConstants.REPOSITORY_FILE_HOME+": Was not able to find or unvalid value '"+ApplicationInfo.WEBSITEPREVIEWRENDERSERVICE+"' - set at least to empty string or 'false' to deactivate",e);
		}
		if ((websitePreviewRenderService==null) || (websitePreviewRenderService.trim().length()==0) || (!websitePreviewRenderService.trim().startsWith("http"))) {
			logger.debug("No preview Image of Link - websitepreviewrenderservice on "+CCConstants.REPOSITORY_FILE_HOME+" is deactivated -> Use Splash");
			return null;
		} else {
			logger.info("OK got websitepreviewrenderservice ...");
		}

		// setting the scale factor from the 1024 default width
		String scale = "0.25";

		// basic result setup
		String result = null;
		if (httpURL==null) return null;

		// try to get a preview image from local nodeJS server running
		// the following service: https://github.com/rootzoll/web-screenshot
		// --> IF NOT AVAILABLE WILL JUST WARN
		try {
			final String localServiceUrl = websitePreviewRenderService+"/?url="+java.net.URLEncoder.encode(httpURL, "ISO-8859-1")+"&scale="+scale+"&base64=1";
			HttpClient client = new HttpClient();
			GetMethod method = new GetMethod(localServiceUrl);
			int statusCode = client.executeMethod(method);
			if (statusCode == HttpStatus.SC_OK) {
				result = new String(method.getResponseBody());
				logger.info("OK on on Preview Image Service");
			} else {
				logger.error("HTTP Error "+statusCode+" on Preview Image Service: "+localServiceUrl);
			}
		}
		catch (java.net.ConnectException ce) {
			logger.warn("!WARN! No Preview Image Service running at '"+websitePreviewRenderService+"' ...");
		}
		catch (Exception e) {
			logger.error("EXCEPTION on Preview Image Service: "+e.getMessage(), e);
		}

		return result;
	}

	private static byte[] getPreviewFromURLSplash(String httpURL) {
		Config splash = LightbendConfigLoader.get().getConfig("repository.communication.splash");
		if(splash != null && splash.hasPath("url")) {
			try {
				final StringBuilder url = new StringBuilder(splash.getString("url") + "?url=" + java.net.URLEncoder.encode(httpURL, "ISO-8859-1"));
				splash.entrySet().stream().filter((e) -> !"url".equals(e.getKey())).forEach((e) -> {
					url.append("&").append(e.getKey()).append("=").append(e.getValue().unwrapped().toString());
				});
				HttpClient client = new HttpClient();
				GetMethod method = new GetMethod(url.toString());
				int timeout = (int) ((splash.getDouble("wait") + splash.getDouble("timeout")) * 1000);
				client.getHttpConnectionManager().getParams().setConnectionTimeout(timeout);
				client.getHttpConnectionManager().getParams().setSoTimeout(timeout);
				int statusCode = client.executeMethod(method);
				if (statusCode == HttpStatus.SC_OK) {
					return method.getResponseBody();
				} else {
					logger.warn("Splash returned non-okay error code " + statusCode + ", " + url.toString());
				}
			}catch(Exception e) {
				logger.warn("Calling Splash service failed: " + e.getMessage(), e);
			}
		}
		return null;
	}
	private static byte[] getPreviewFromURLPlaywright(String httpURL) {
		Config playwright = LightbendConfigLoader.get().getConfig("repository.communication.playwright");
		if(playwright != null && playwright.hasPath("url")) {
			try {
				final StringBuilder url = new StringBuilder(playwright.getString("url") + "/v1/website?url=" + java.net.URLEncoder.encode(httpURL, "ISO-8859-1"));
				playwright.entrySet().stream().filter((e) -> !"url".equals(e.getKey())).forEach((e) -> {
					try {
						url.append("&").append(e.getKey()).append("=").append(java.net.URLEncoder.encode(e.getValue().unwrapped().toString(), "ISO-8859-1"));
					}catch(UnsupportedEncodingException ignored) {}
				});
				HttpClient client = new HttpClient();
				GetMethod method = new GetMethod(url.toString());
				int timeout = (int) (playwright.getDouble("timeout") * 1000);
				client.getHttpConnectionManager().getParams().setConnectionTimeout(timeout);
				client.getHttpConnectionManager().getParams().setSoTimeout(timeout);
				int statusCode = client.executeMethod(method);
				if (statusCode == HttpStatus.SC_OK) {
					JSONObject result = new JSONObject(method.getResponseBodyAsString());
					return Base64.decode(result.getString("preview").getBytes(StandardCharsets.UTF_8));
				} else {
					logger.warn("Playwright returned non-okay error code " + statusCode + ", " + url.toString());
				}
			}catch(Exception e) {
				logger.warn("Calling Playwright service failed: " + e.getMessage(), e);
			}
		}
		return null;
	}

	public void setPolicyBehaviourFilter(BehaviourFilter policyBehaviourFilter) {
		this.policyBehaviourFilter = policyBehaviourFilter;
	}

	public void setScheduler(Scheduler scheduler) {
		this.scheduler = scheduler;
	}
}
