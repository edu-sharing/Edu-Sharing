package org.edu_sharing.service.lifecycle;


import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.OwnableService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.service.search.CMISSearchHelper;
import org.edu_sharing.alfresco.workspace_administration.NodeServiceInterceptor;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.tools.VCardConverter;
import org.edu_sharing.repository.server.tools.cache.RepositoryCache;
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.authentication.ScopeUserHomeService;
import org.edu_sharing.service.authentication.ScopeUserHomeServiceFactory;
import org.edu_sharing.service.feedback.FeedbackServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.nodeservice.RecurseMode;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.edu_sharing.service.permission.PermissionServiceHelper;
import org.edu_sharing.service.rating.RatingServiceFactory;
import org.edu_sharing.service.stream.StreamServiceFactory;
import org.edu_sharing.service.tracking.TrackingServiceFactory;
import org.springframework.context.ApplicationContext;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;


/**
 * Konzept LöschJob -> status todelete
 *
 * Filter in personsearch (invite, workflow - non active)
 *
 * validate session only active,
 * webdav auth, share auth ....
 *
 * on create person set personstatus to 'active'
 *
 * job that sets active status for existing persons when edu-sharing property 'person_active_status' is set
 *
 * user_home remove with/without cc -> with: move cc content to cc_user space (hirachical date folders structure)
 *
 *
 * all files in shared content will be deleted except OER(cc) content:
 * student, external, teacher trainee's
 * instance owner becomes owner
 * Collections -> delete?
 *
 * all files in shared content stay and will be given to an Instanceowner
 * Teacher, Staff
 *
 * InviteHistory: delete or rename
 *
 * filtered in invite dialogs
 *
 * cause of owner (first/lastname) remove file from properties cache
 *
 *
 * --> no username use firstName and lastName
 * 		Problems: user with same name, and marriage
 */
public class PersonLifecycleService {

	private static final String DELETED_PERSONS_FOLDER = "DELETED_PERSONS";
	private static final String USERHOME_ALL = "USERHOME";
	private static final String USERHOME_FILES = "USERHOME_FILES";
	private static final String USERHOME_FOLDERS = "USERHOME_FOLDERS";
	private static final String USERHOME_FILES_CC = "USERHOME_CC_FILES";
	private static final String SHARED_FILES = "SHARED_FILES";
	private static final String SHARED_FILES_CC = "SHARED_CC_FILES";
	private static final List<QName> SKIP_ASPECTS_MOVE = Arrays.asList(
			QName.createQName(CCConstants.CCM_ASPECT_IO_CHILDOBJECT),
			QName.createQName(CCConstants.CCM_ASPECT_COLLECTION),
			QName.createQName(CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE)
	);
	private static final List<QName> SKIP_ASPECTS_OWNER = Arrays.asList(
			QName.createQName(CCConstants.CCM_ASPECT_IO_CHILDOBJECT),
			QName.createQName(CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE)
	);
	private static final List<QName> SKIP_TYPES = Collections.singletonList(
			QName.createQName(CCConstants.CCM_TYPE_TOOLPERMISSION)
	);
	public static final List<String> USER_GENERATED_TYPES = Arrays.asList(
			CCConstants.CCM_TYPE_IO,
			CCConstants.CCM_TYPE_MAP,
			CCConstants.CCM_TYPE_COLLECTION_PROPOSAL,
			CCConstants.CCM_TYPE_MATERIAL_FEEDBACK,
			CCConstants.CCM_TYPE_COMMENT,
			CCConstants.CCM_TYPE_RATING
	);
	public static final String DEFAULT_SCOPE = "default";
	public static String ROLE_STUDENT = "student";
	public static String ROLE_EXTERNAL = "external";
	public static String ROLE_TEACHER_TRAINEES = "teacher_trainees";
	public String[] ROLE_GROUP_REMOVE_SHARED = {ROLE_STUDENT,ROLE_EXTERNAL,ROLE_TEACHER_TRAINEES};

	public static String ROLE_TEACHER = "teacher";
	public static String ROLE_STAFF = "staff";
	public String[] ROLE_GROUP_KEEP_SHARED = {ROLE_TEACHER,ROLE_STAFF};


	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
	ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

	SearchService searchService = serviceRegistry.getSearchService();

	NodeService nodeService = serviceRegistry.getNodeService();

	PersonService personService = serviceRegistry.getPersonService();

	AuthorityService authorityService = serviceRegistry.getAuthorityService();

	PermissionService permissionService = serviceRegistry.getPermissionService();

	OwnableService ownableService = serviceRegistry.getOwnableService();

	TransactionService transactionService = serviceRegistry.getTransactionService();

	BehaviourFilter policyBehaviourFilter = (BehaviourFilter)applicationContext.getBean("policyBehaviourFilter");


	int maxItems = 20;

	public enum PersonStatus{
		active,
		blocked,
		todelete
	}

	Logger logger = Logger.getLogger(PersonLifecycleService.class);

	//public static String ROLE_

	/*
	private void deletePersons(int skipCount) {
		SearchParameters sp = new SearchParameters();
		sp.setQuery("TYPE:\"cm:person\" AND @cm\\:espersonstatus:" + PersonStatus.todelete.name());
		sp.setSkipCount(skipCount);
		sp.setMaxItems(maxItems);
		ResultSet rs = searchService.query(sp);
		for(NodeRef nodeRef : rs.getNodeRefs()) {
			deletePerson(nodeRef, options);
		}
		if(rs.hasMore()) {
			deletePersons(skipCount + maxItems);
		}
	}
    */

	public PersonReport deletePersons(List<String> usernames, PersonDeleteOptions options) {
		List<PersonDeleteResult> results=new ArrayList<>();
		for(String user : usernames) {
			NodeRef personNodeRef = personService.getPerson(user);
			results.add(deletePerson(personNodeRef,options));
		}
		PersonReport report=new PersonReport();
		report.options=options;
		report.results=results;
		return report;
	}

	private PersonDeleteResult deletePerson(NodeRef personNodeRef, PersonDeleteOptions options) {
		PersonDeleteResult result=new PersonDeleteResult();
		String status = (String)nodeService.getProperty(personNodeRef,
				QName.createQName(CCConstants.CM_PROP_PERSON_ESPERSONSTATUS));
		String role = (String)nodeService.getProperty(personNodeRef, QName.createQName(CCConstants.CM_PROP_PERSON_EDU_SCHOOL_PRIMARY_AFFILIATION));
		String userName = (String)nodeService.getProperty(personNodeRef, QName.createQName(CCConstants.CM_PROP_PERSON_USERNAME));
		result.authorityName=userName;

		// in case some data stays in place (e.g. comments, statistics, ...), we will identify this user with an unique delete timestamp
		String deletedUsername = generateDeletedUsername();
		result.deletedName=deletedUsername;
		if(PersonStatus.todelete.name().equals(status)) {
			if(hasAssigning(options)){
				if(options.receiver==null || options.receiver.isEmpty() || options.receiverGroup==null || options.receiverGroup.isEmpty()) {
					throw new IllegalArgumentException("Some options set to assign, but no user + org was specified for assigning");
				}
				if(personService.getPersonOrNull(options.receiver)==null){
					throw new IllegalArgumentException("The given receiver is not a valid user authority");
				}
			}
			//shared files are "mounted" in the user home, so always process them first!
			//handleSharedFiles(personNodeRef,options); -> is now done via foreignFiles

			result.homeFolder.put(DEFAULT_SCOPE,handleHomeHolder(personNodeRef, options, null, deletedUsername));
			List<NodeRef> filesToIgnore=result.homeFolder.get("default").
					getElements().stream().map((e)->new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,e.getId())).collect(Collectors.toList());
			result.homeFolder.put(CCConstants.CCM_VALUE_SCOPE_SAFE,handleHomeHolder(personNodeRef, options, CCConstants.CCM_VALUE_SCOPE_SAFE, deletedUsername));

			result.sharedFolders.put(DEFAULT_SCOPE,handleForeignFiles(personNodeRef,filesToIgnore,options,null, deletedUsername));
			result.sharedFolders.put(CCConstants.CCM_VALUE_SCOPE_SAFE,handleForeignFiles(personNodeRef,filesToIgnore,options,CCConstants.CCM_VALUE_SCOPE_SAFE, deletedUsername));
			result.collections=handleCollections(personNodeRef,options);

			handleStream(personNodeRef,options);
			if(options.comments.delete){
				result.comments=deleteAllOfType(personNodeRef, CCConstants.CCM_TYPE_COMMENT,null).size();
			}
			else{
				result.comments=assignUserToType(personNodeRef, deletedUsername, CCConstants.CCM_TYPE_COMMENT,null).size();
			}
			processRatings(result, userName, options.ratings.delete);
			processFeedback(result, userName, options.collectionFeedback.delete);

			handleStatistics(personNodeRef, deletedUsername,options);

			logger.info("deleting person");
			nodeService.addAspect(personNodeRef, ContentModel.ASPECT_TEMPORARY, null);
			personService.deletePerson(personNodeRef,true);
			return result;
		}
		else{
			throw new IllegalArgumentException("Person "+userName+" is not marked for deletion. Cancelling");
		}
	}

	public String generateDeletedUsername() {
		return CCConstants.AUTHORITY_DELETED_USER+"_"+System.currentTimeMillis();
	}

	public void processRatings(PersonDeleteResult result, String userName, boolean delete) {
		if(delete) {
			RatingServiceFactory.getLocalService().deleteUserData(userName);
		} else {
			RatingServiceFactory.getLocalService().changeUserData(userName, result.deletedName);
		}
	}
	public void processFeedback(PersonDeleteResult result, String userName, boolean delete) {
		if(delete) {
			FeedbackServiceFactory.getLocalService().deleteUserData(userName);
		} else {
			FeedbackServiceFactory.getLocalService().changeUserData(userName, result.deletedName);
		}
	}

	public List<NodeRef> getAllNodeRefs(String username, String type, String scope){
		Map<String, Object> filters=new HashMap<>();
		filters.put("cmis:createdBy",username);
		filters.put(CCConstants.CCM_PROP_EDUSCOPE_NAME,scope);
		return CMISSearchHelper.fetchNodesByTypeAndFilters(type,filters);
		/*
		SearchParameters params=new SearchParameters();
		params.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
		// will use the database
		params.setLanguage(SearchService.LANGUAGE_CMIS_ALFRESCO);
		params.setMaxItems(Integer.MAX_VALUE);
		String query="SELECT cmis:name FROM "+CCConstants.getValidLocalName(type)+" WHERE cmis:createdBy = '"+ username + "'";
		if(scope==null){
			scope=
		}
		params.setQuery(query);
		ResultSet result = searchService.query(params);
		logger.info(query+": "+result.getNodeRefs().size());
		return result.getNodeRefs();
		*/
	}

	private void handleStatistics(NodeRef personNodeRef, String deletedUsername, PersonDeleteOptions options) {
		String username = (String)nodeService.getProperty(personNodeRef,
				QName.createQName(CCConstants.CM_PROP_PERSON_USERNAME));
		try {
			if(options.statistics.delete) {
				TrackingServiceFactory.getTrackingService().deleteUserData(username);
				logger.info("removed tracking/statistics data");
			}else{
				TrackingServiceFactory.getTrackingService().reassignUserData(username,deletedUsername);
				logger.info("removed tracking/statistics data");
			}
		}catch(Throwable t){
			throw new RuntimeException(t);
		}
	}

	/**
	 * Deletes all nodes from the given person + type in the repository
	 */
	private List<NodeRef> deleteAllOfType(NodeRef personNodeRef,String type,String scope) {
		String username = (String)nodeService.getProperty(personNodeRef,
				QName.createQName(CCConstants.CM_PROP_PERSON_USERNAME));
		List<NodeRef> refs = getAllNodeRefs(username,type, scope);
		logger.info("Deleting all files of type "+type);
		deleteAllRefs(refs);
		return refs;
	}

	/**
	 * assigns the given user to all nodes from the given person + type in the repository
	 */
	public List<NodeRef> assignUserToType(NodeRef personNodeRef,String newUsername,String type,String scope) {
		String username = (String)nodeService.getProperty(personNodeRef,
				QName.createQName(CCConstants.CM_PROP_PERSON_USERNAME));
		List<NodeRef> refs = getAllNodeRefs(username,type, scope);
		logger.info("Reassigning deleted to all files of type "+type+" ("+refs.size()+")");
		return assignUserToNodes(newUsername, username, refs);
	}

	private List<NodeRef> assignUserToNodes(String newUsername, String username, List<NodeRef> refs) {
		RetryingTransactionHelper rth = transactionService.getRetryingTransactionHelper();
		refs.forEach((nodeRef)-> rth.doInTransaction((RetryingTransactionHelper.RetryingTransactionCallback<Void>) () -> {
			policyBehaviourFilter.disableBehaviour(nodeRef);
			ownableService.setOwner(nodeRef, newUsername);
			if (username.equals(nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CM_PROP_C_CREATOR)))) {
				nodeService.setProperty(nodeRef, QName.createQName(CCConstants.CM_PROP_C_CREATOR), newUsername);
			}
			if (username.equals(nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CM_PROP_C_MODIFIER)))) {
				nodeService.setProperty(nodeRef, QName.createQName(CCConstants.CM_PROP_C_MODIFIER), newUsername);
			}
			new RepositoryCache().remove(nodeRef.getId());
			policyBehaviourFilter.enableBehaviour(nodeRef);
			return null;
		}));
		return refs;
	}

	private void handleStream(NodeRef personNodeRef, PersonDeleteOptions options) {
		if(options.stream.delete) {
			String username = (String)nodeService.getProperty(personNodeRef,
					QName.createQName(CCConstants.CM_PROP_PERSON_USERNAME));
			StreamServiceFactory.getStreamService().deleteEntriesByAuthority(username);
		}
	}

	private boolean hasAssigning(PersonDeleteOptions options) {
		return options.homeFolder.privateFiles.equals(PersonDeleteOptions.DeleteMode.assign) ||
				options.homeFolder.ccFiles.equals(PersonDeleteOptions.DeleteMode.assign) ||
				options.sharedFolders.privateFiles.equals(PersonDeleteOptions.DeleteMode.assign) ||
				options.sharedFolders.ccFiles.equals(PersonDeleteOptions.DeleteMode.assign) ||
				options.collections.privateCollections.equals(PersonDeleteOptions.DeleteMode.assign) ||
				options.collections.publicCollections.equals(PersonDeleteOptions.DeleteMode.assign);
	}

	private List<ChildAssociationRef> getAllSharedFolders(NodeRef personNodeRef, String scope) {
		String username = (String)nodeService.getProperty(personNodeRef,
				QName.createQName(CCConstants.CM_PROP_PERSON_USERNAME));
		NodeRef homeFolder;
		if(scope == null) {
			homeFolder = getHomeFolder(personNodeRef);
		}else {
			ScopeUserHomeService scopeUserHomeService = ScopeUserHomeServiceFactory.getScopeUserHomeService();
			homeFolder = scopeUserHomeService.getUserHome(username, scope, false);
		}
		if(homeFolder != null) {
			List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(homeFolder);
			for(ChildAssociationRef childAssoc : childAssocs) {
				String mapType = (String)nodeService.getProperty(childAssoc.getChildRef(), QName.createQName(CCConstants.CCM_PROP_MAP_TYPE));
				if(CCConstants.CCM_VALUE_MAP_TYPE_EDUGROUP.equals(mapType)){
					return nodeService.getChildAssocs(childAssoc.getChildRef());
				}
			}
		}
		return null;
	}
	private PersonDeleteResult.CollectionCounts handleCollections(NodeRef personNodeRef, PersonDeleteOptions options) {
		if(options.collections.privateCollections.equals(PersonDeleteOptions.DeleteMode.none) &&
				options.collections.publicCollections.equals(PersonDeleteOptions.DeleteMode.none)){
			return null;
		}
		PersonDeleteResult.CollectionCounts counts=new PersonDeleteResult.CollectionCounts();
		String userName = (String)nodeService.getProperty(personNodeRef, QName.createQName(CCConstants.CM_PROP_PERSON_USERNAME));
		List<NodeRef> collectionsMaps = getAllNodeRefs(userName,CCConstants.CCM_TYPE_MAP,null).stream().
				filter((ref)->nodeService.hasAspect(ref,QName.createQName(CCConstants.CCM_ASPECT_COLLECTION))).
				collect(Collectors.toList());
		List<NodeRef> collectionsIos = getAllNodeRefs(userName,CCConstants.CCM_TYPE_IO,null).stream().
				filter((ref)->nodeService.hasAspect(ref,QName.createQName(CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE))).
				collect(Collectors.toList());
		counts.setCollections(convertToElements(collectionsMaps));
		counts.setRefs(convertToElements(collectionsIos));

		logger.info("Collections where "+userName+" is owner: "+collectionsMaps.size());
		logger.info("Collection Refs where "+userName+" is owner: "+collectionsIos.size());
		if(options.collections.privateCollections.equals(options.collections.publicCollections)){
			if(options.collections.privateCollections.equals(PersonDeleteOptions.DeleteMode.delete)) {
				// the maps may be not enough if the user contributed to foreign collections
				deleteAllRefs(collectionsIos);
				deleteAllRefs(collectionsMaps);
			}
			else if(options.collections.privateCollections.equals(PersonDeleteOptions.DeleteMode.assign)) {
				setOwnerAndPermissions(collectionsMaps, userName, options);
				//setOwnerAndPermissions(collectionsIos, userName, options);
				// io references should not support additional permissions (always inherit)
				collectionsIos.forEach((ref)->setOwner(ref,userName,options.receiver, options));
			}
			return counts;
		} else {
			List<NodeRef> collectionsIosPublic = filterPublic(collectionsIos);
			collectionsIos.removeAll(collectionsIosPublic);

			List<NodeRef> collectionsMapsPublic = filterPublic(collectionsMaps);
			collectionsMaps.removeAll(collectionsMapsPublic);

			// remove private/shared collections + ios
			if(options.collections.privateCollections.equals(PersonDeleteOptions.DeleteMode.delete)) {
				// the maps may be not enough if the user contributed to foreign collections
				deleteAllRefs(collectionsIos);
				deleteAllRefs(collectionsMaps);
			}
			else if(options.collections.privateCollections.equals(PersonDeleteOptions.DeleteMode.assign)) {
				setOwnerAndPermissions(collectionsMaps, userName, options);
				//setOwnerAndPermissions(collectionsIos, userName, options);
				// io references should not support additional permissions (always inherit)
				collectionsIos.forEach((ref)->setOwner(ref,userName,options.receiver, options));
			}

			// remove public collections + ios
			if(options.collections.publicCollections.equals(PersonDeleteOptions.DeleteMode.delete)) {
				// the maps may be not enough if the user contributed to foreign collections
				deleteAllRefs(collectionsIosPublic);
				deleteAllRefs(collectionsMapsPublic);
			}
			else if(options.collections.publicCollections.equals(PersonDeleteOptions.DeleteMode.assign)) {
				setOwnerAndPermissions(collectionsMapsPublic, userName, options);
				//setOwnerAndPermissions(collectionsIos, userName, options);
				// io references should not support additional permissions (always inherit)
				collectionsIosPublic.forEach((ref)->setOwner(ref,userName,options.receiver, options));
			}
			return counts;
		}
	}

	/**
	 * return only elements were GROUP_EVERYONE has Permission.Read
	 */
	private List<NodeRef> filterPublic(List<NodeRef> refs) {
		return refs.stream().filter(ref -> {
			try {
				return Arrays.stream(PermissionServiceFactory.getLocalService().getPermissions(ref.getId()).getAces()).anyMatch(
						ace -> ace.getAuthorityType().equals(CCConstants.PERM_AUTHORITY_TYPE_EVERYONE) &&
								ace.getAuthority().equals(CCConstants.AUTHORITY_GROUP_EVERYONE) && (
								ace.getPermission().equals(CCConstants.PERMISSION_READ) || ace.getPermission().equals(CCConstants.PERMISSION_CONSUMER
								)
						)
				);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}).collect(Collectors.toList());
	}

	/**
	 * All files that are from this user and not in the filesToIgnore list (which should be the home folder files)
	 * this can be files users contributed to other user homes he was invited to, or in shared folders
	 * @param personNodeRef
	 * @param filesToIgnore
	 * @param options
	 */
	private PersonDeleteResult.Counts handleForeignFiles(NodeRef personNodeRef, List<NodeRef> filesToIgnore, PersonDeleteOptions options, String scope, String deletedUsername) {
		String userName = (String)nodeService.getProperty(personNodeRef, QName.createQName(CCConstants.CM_PROP_PERSON_USERNAME));
		// getting all ios (files) and maps (folders) where this user is the creator, but not any which should be ignored

		List<NodeRef> refsIO = getAllNodeRefs(userName, CCConstants.CCM_TYPE_IO,scope).stream().
				filter((ref)->!filesToIgnore.contains(ref)).
				collect(Collectors.toList());
		List<NodeRef> refsMaps = getAllNodeRefs(userName, CCConstants.CCM_TYPE_MAP,scope).stream().
				filter((ref)->!filesToIgnore.contains(ref)).
				filter((ref)->!nodeService.hasAspect(ref, QName.createQName(CCConstants.CCM_ASPECT_COLLECTION))).
				collect(Collectors.toList());

		// split the files by their license type
		List<NodeRef> filesPrivate = refsIO.stream().filter((ref) -> !hasCCLicense(ref)).collect(Collectors.toList());
		List<NodeRef> filesCC = refsIO.stream().filter((ref) -> hasCCLicense(ref)).collect(Collectors.toList());
		PersonDeleteResult.Counts counts=new PersonDeleteResult.Counts(convertToElements(refsIO,refsMaps));
		// handle cc files
		if(options.sharedFolders.ccFiles.equals(PersonDeleteOptions.DeleteMode.assign) && filesCC.size()>0){
			// switching the owner
			setOwnerAndPermissions(filesCC, userName, options);
			if(options.sharedFolders.move) {
				moveNodes(filesCC, getOrCreateTargetFolder(personNodeRef, options, SHARED_FILES_CC, scope));
			}
		}else if(options.sharedFolders.ccFiles.equals(PersonDeleteOptions.DeleteMode.delete)){
			deleteAllRefs(filesCC);
		}
		// handle private files, that are not in the home dir
		if(options.sharedFolders.privateFiles.equals(PersonDeleteOptions.DeleteMode.assign) && filesPrivate.size()>0){
			setOwnerAndPermissions(filesPrivate, userName, options);
			if(options.sharedFolders.move) {
				moveNodes(filesPrivate, getOrCreateTargetFolder(personNodeRef, options, SHARED_FILES, scope));
			}
		}else if(options.sharedFolders.privateFiles.equals(PersonDeleteOptions.DeleteMode.delete)){
			deleteAllRefs(filesPrivate);
		}
		// assign the new user to all folders
		if(options.sharedFolders.folders.equals(PersonDeleteOptions.FolderDeleteMode.assign)) {
			setOwnerAndPermissions(refsMaps, userName, options);
		} else if (options.sharedFolders.folders.equals(PersonDeleteOptions.FolderDeleteMode.none)) {
			assignUserToNodes(deletedUsername, userName, refsMaps);
		}
		return counts;
	}

	public List<PersonDeleteResult.Element> convertToElements(Collection<NodeRef>... refs) {
		List<PersonDeleteResult.Element> result=new ArrayList<>();
		for (Collection<NodeRef> ref : refs) {
			result.addAll(ref.stream().map((r)->
					new PersonDeleteResult.Element(r.getId(),
							(String) nodeService.getProperty(r, QName.createQName(CCConstants.CM_NAME)),
							nodeService.getType(r).toString())
			).collect(Collectors.toList()));
		}
		return result;
	}
	/*
	private void handleSharedFiles(NodeRef personNodeRef, PersonDeleteOptions options) {
		if(options.sharedFolders.privateFiles.equals(PersonDeleteOptions.DeleteMode.none) &&
			options.sharedFolders.ccFiles.equals(PersonDeleteOptions.DeleteMode.none)){
			return;
		}
		String userName = (String)nodeService.getProperty(personNodeRef, QName.createQName(CCConstants.CM_PROP_PERSON_USERNAME));

		List<ChildAssociationRef> shared = getAllSharedFolders(personNodeRef, null);
		if(shared==null) {
			logger.info("User "+userName+" has no shared folders / orgs");

		}
		else{
			List<NodeRef> allFiles = new ArrayList<>();
			shared.forEach(
					(childAssociationRef -> {
						allFiles.addAll(NodeServiceFactory.getLocalService().getChildrenRecursive(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, childAssociationRef.getChildRef().getId(), null, RecurseMode.Folders));
					})
			);
			logger.info("Total shared files to check: "+allFiles.size());
			List<NodeRef> files = allFiles.stream().filter((ref) -> ownableService.getOwner(ref).equals(userName)).collect(Collectors.toList());
			logger.info("Shared files where "+userName+" is owner: "+files.size());

			List<NodeRef> filesPrivate = files.stream().filter((ref) -> !hasCCLicense(ref)).collect(Collectors.toList());
			List<NodeRef> filesCC = files.stream().filter((ref) -> hasCCLicense(ref)).collect(Collectors.toList());
			if(options.sharedFolders.privateFiles.equals(PersonDeleteOptions.DeleteMode.assign)){
				if(options.sharedFolders.move) {
					NodeRef target = getOrCreateTargetFolder(personNodeRef, options, SHARED_FILES, null);
					setOwnerAndPermissions(filesPrivate, userName, options);
					moveNodes(filesPrivate, target);
				}
				else{
					setOwnerAndPermissions(filesPrivate, userName, options);
				}
			}
			if(options.sharedFolders.ccFiles.equals(PersonDeleteOptions.DeleteMode.assign)){
				if(options.sharedFolders.move) {
					NodeRef target = getOrCreateTargetFolder(personNodeRef, options, SHARED_FILES_CC, null);
					setOwnerAndPermissions(filesCC, userName, options);
					moveNodes(filesCC, target);
				}
				else{
					setOwnerAndPermissions(filesCC, userName, options);
				}
			}
			if(options.sharedFolders.privateFiles.equals(PersonDeleteOptions.DeleteMode.delete)){
				deleteAllRefs(filesPrivate);
			}
			if(options.sharedFolders.ccFiles.equals(PersonDeleteOptions.DeleteMode.delete)){
				deleteAllRefs(filesCC);
			}
		}
	}
	 */

	private PersonDeleteResult.Counts handleHomeHolder(NodeRef personNodeRef, PersonDeleteOptions options, String scope, String deletedUsername) {
		NodeRef homeFolder;
		String userName = (String)nodeService.getProperty(personNodeRef, QName.createQName(CCConstants.CM_PROP_PERSON_USERNAME));
		if(scope==null){
			homeFolder = getHomeFolder(personNodeRef);
			if(homeFolder==null){
				logger.info("Person "+userName+" does not have a home folder, skipping it");
				return new PersonDeleteResult.Counts(new ArrayList<>());
			}
		}
		else{
			ScopeUserHomeService scopeUserHomeService = ScopeUserHomeServiceFactory.getScopeUserHomeService();
			homeFolder = scopeUserHomeService.getUserHome((String) nodeService.getProperty(personNodeRef, QName.createQName(CCConstants.CM_PROP_PERSON_USERNAME)), scope, false);
			if(homeFolder==null){
				logger.info("Person "+userName+" does not have a scope folder for "+scope+", skipping it");
				return new PersonDeleteResult.Counts(new ArrayList<>());
			}
		}
		// remove the dummy EDUGROUP folder first, it will lead to problems otherwise
		NodeRef child = NodeServiceFactory.getLocalService().getChild(homeFolder.getStoreRef(), homeFolder.getId(), CCConstants.CCM_TYPE_MAP, CCConstants.CCM_PROP_MAP_TYPE, CCConstants.CCM_VALUE_MAP_TYPE_EDUGROUP);
		if(child!=null){
			NodeServiceFactory.getLocalService().removeNode(child.getId(),null,false);
			logger.info("Deleting the EDUGROUP folder of "+userName);
		}
		List<NodeRef> childrens = NodeServiceFactory.getLocalService().getChildrenRecursive(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, homeFolder.getId(), null,RecurseMode.Folders);
		PersonDeleteResult.Counts counts = new PersonDeleteResult.Counts(convertToElements(childrens));
		if(options.homeFolder.folders.equals(PersonDeleteOptions.FolderDeleteMode.none)
				&& options.homeFolder.privateFiles.equals(PersonDeleteOptions.DeleteMode.none)
				&& options.homeFolder.ccFiles.equals(PersonDeleteOptions.DeleteMode.none)){
			logger.info("all modes are set to none for home folder, will keep everything in place");
			return counts;
		}
		else if(options.homeFolder.folders.equals(PersonDeleteOptions.FolderDeleteMode.assign)
				&& options.homeFolder.privateFiles.equals(PersonDeleteOptions.DeleteMode.assign)
				&& options.homeFolder.ccFiles.equals(PersonDeleteOptions.DeleteMode.assign)){
			if(options.homeFolder.keepFolderStructure){
				// @TODO: we set explicit permissions on all sub items because if a folder has inherit=false, then the org will not propagate
				childrens.add(homeFolder);
				setOwnerAndPermissions(childrens,userName,options);

				NodeRef deletedRef = getOrCreateTargetFolder(personNodeRef,options,USERHOME_ALL,scope);
				nodeService.moveNode(homeFolder,deletedRef,
						ContentModel.ASSOC_CONTAINS,
						QName.createQName(CCConstants.NAMESPACE_CCM, (String) nodeService.getProperty(homeFolder, QName.createQName(CCConstants.CM_NAME))));
				return counts;
			}
		}
		if(options.homeFolder.keepFolderStructure){
			throw new IllegalArgumentException("keepFolderStructure is only allowed when all modes are set to assign");
		}
		else{
			List<NodeRef> folders = childrens.stream().filter((ref)->nodeService.getType(ref).equals(QName.createQName(CCConstants.CCM_TYPE_MAP))).collect(Collectors.toList());
			List<NodeRef> privateFiles =  childrens.stream().filter((ref)->nodeService.getType(ref).equals(QName.createQName(CCConstants.CCM_TYPE_IO)) && !hasCCLicense(ref)).collect(Collectors.toList());
			List<NodeRef> ccFiles = childrens.stream().filter((ref)->nodeService.getType(ref).equals(QName.createQName(CCConstants.CCM_TYPE_IO)) && hasCCLicense(ref)).collect(Collectors.toList());

			logger.info("Managing private files for "+userName);
			// handle private files
			if(options.homeFolder.privateFiles.equals(PersonDeleteOptions.DeleteMode.assign) && privateFiles.size()>0){
				NodeRef privateTarget = getOrCreateTargetFolder(personNodeRef, options,USERHOME_FILES,scope);
				setOwnerAndPermissions(privateFiles,userName,options);
				moveNodes(privateFiles,privateTarget);
			}else if(options.homeFolder.privateFiles.equals(PersonDeleteOptions.DeleteMode.delete)){
				deleteAllRefs(privateFiles);
			}
			logger.info("Managing cc files for "+userName);
			// handle cc files
			if(options.homeFolder.ccFiles.equals(PersonDeleteOptions.DeleteMode.assign) && ccFiles.size()>0){
				NodeRef ccTarget = getOrCreateTargetFolder(personNodeRef, options,USERHOME_FILES_CC,scope);
				setOwnerAndPermissions(ccFiles,userName,options);
				moveNodes(ccFiles,ccTarget);
			}else if(options.homeFolder.ccFiles.equals(PersonDeleteOptions.DeleteMode.delete)){
				deleteAllRefs(ccFiles);
			}
			logger.info("Managing folders for "+userName);
			// handle folders
			if(folders.size()>0 && options.homeFolder.privateFiles.equals(PersonDeleteOptions.DeleteMode.assign)) {
				NodeRef foldersTarget = getOrCreateTargetFolder(personNodeRef, options, USERHOME_FOLDERS, scope);
				if (options.homeFolder.folders.equals(PersonDeleteOptions.FolderDeleteMode.assign)) {
					setOwnerAndPermissions(folders, userName, options);
				}
				moveNodes(folders, foldersTarget);
			}
			nodeService.setProperty(homeFolder, ContentModel.PROP_NAME, deletedUsername);
			nodeService.setProperty(homeFolder, ContentModel.PROP_TITLE, deletedUsername);
			folders.add(homeFolder);
			if(options.homeFolder.folders.equals(PersonDeleteOptions.FolderDeleteMode.none)) {
				// remove the authority name on all folders
				assignUserToNodes(deletedUsername, userName, folders);
			}
		}
		if(options.homeFolder.folders.equals(PersonDeleteOptions.FolderDeleteMode.delete)) {
			NodeServiceFactory.getLocalService().removeNode(homeFolder.getId(), null, false);
		}
		return counts;
	}

	private void moveNodes(List<NodeRef> refs, NodeRef targetRef) {
		refs.forEach((ref)-> {
			if (skipNode(ref,SKIP_ASPECTS_MOVE)) return;
			RetryingTransactionHelper rth = transactionService.getRetryingTransactionHelper();
			AtomicBoolean rename= new AtomicBoolean(false);
			rth.doInTransaction((RetryingTransactionHelper.RetryingTransactionCallback<Void>) () -> {
				policyBehaviourFilter.disableBehaviour(ref);
				try {
					nodeService.moveNode(ref, targetRef,
							ContentModel.ASSOC_CONTAINS,
							QName.createQName(CCConstants.NAMESPACE_CCM, (String) nodeService.getProperty(ref, QName.createQName(CCConstants.CM_NAME))));
				} catch (DuplicateChildNodeNameException e) {
					rename.set(true);
				}
				policyBehaviourFilter.enableBehaviour(ref);
				return null;
			});
			if(rename.get()){
				rth.doInTransaction((RetryingTransactionHelper.RetryingTransactionCallback<Void>) () -> {
					policyBehaviourFilter.disableBehaviour(ref);
					try {
						String newName = nodeService.getProperty(ref, QName.createQName(CCConstants.CM_NAME)) + "_" + ref.getId();
						nodeService.setProperty(ref, QName.createQName(CCConstants.CM_NAME), newName);
						nodeService.moveNode(ref, targetRef,
								ContentModel.ASSOC_CONTAINS,
								QName.createQName(CCConstants.NAMESPACE_CCM, newName));
					} catch (DuplicateChildNodeNameException e) {
						rename.set(true);
					}
					policyBehaviourFilter.enableBehaviour(ref);
					return null;
				});
			}
		});
	}

	private boolean skipNode(NodeRef ref,List<QName> skipAspects) {
		Set<QName> aspects = nodeService.getAspects(ref);
		List<QName> filtered = skipAspects.stream().filter((aspect) -> aspects.contains(aspect)).collect(Collectors.toList());
		if(filtered.size()>0){
			logger.info("will not move io since it contains a skip aspect "+filtered.get(0)+": "+ref);
			return true;
		}
		QName type = nodeService.getType(ref);
		filtered = SKIP_TYPES.stream().filter((t) -> t.equals(type)).collect(Collectors.toList());
		if(filtered.size()>0){
			logger.info("will not move io since it contains a skip type "+filtered.get(0)+": "+ref);
			return true;
		}
		return false;
	}

	/**
	 * create the new folder where all deleted data should remain
	 * this folder will be placed in the receiverGroup (org) shared folder
	 * @param personNodeRef
	 * @param options
	 * @return
	 */
	private NodeRef getOrCreateTargetFolder(NodeRef personNodeRef, PersonDeleteOptions options,String subfolder,String scope) {
		String oldScope=NodeServiceInterceptor.getEduSharingScope();
		try {
			NodeServiceInterceptor.setEduSharingScope(scope);
			String username = (String) nodeService.getProperty(personNodeRef, QName.createQName(CCConstants.CM_PROP_PERSON_USERNAME));
			NodeRef deleted = getDeletedFolderForOrg(options, scope);
			String userFolder = NodeServiceFactory.getLocalService().findNodeByName(deleted.getId(), username);
			if (userFolder == null) {
				Map<String, Object> props = new HashMap<>();
				props.put(CCConstants.CM_NAME, username);
				userFolder = NodeServiceFactory.getLocalService().createNodeBasic(deleted.getId(), CCConstants.CCM_TYPE_MAP, props);
			}
			if (subfolder == null)
				return new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, userFolder);

			Map<String, Object> props = new HashMap<>();
			props.put(CCConstants.CM_NAME, subfolder);
			return new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, NodeServiceFactory.getLocalService().createNodeBasic(userFolder, CCConstants.CCM_TYPE_MAP, props));
		}catch(Exception e){
			throw e;
		}finally {
			NodeServiceInterceptor.setEduSharingScope(oldScope);
		}
	}



	private NodeRef getDeletedFolderForOrg(PersonDeleteOptions options, String scope) {
		String orgName = options.receiverGroup;
		NodeRef orgRef = authorityService.getAuthorityNodeRef(scope==null ? orgName : orgName + "_" + scope);
		if(orgRef == null) {
			if(scope!=null) {
				orgRef = authorityService.getAuthorityNodeRef(
						ScopeUserHomeServiceFactory.getScopeUserHomeService().getOrCreateScopedEduGroup(orgName, scope).getGroupId()
				);
			}
			if(orgRef == null) {
				throw new IllegalArgumentException("Given org " + orgName + " (" + scope + ") could not be found");
			}
		}
		NodeRef orgHome = (NodeRef) nodeService.getProperty(orgRef,
				QName.createQName(CCConstants.CCM_PROP_EDUGROUP_EDU_HOMEDIR));

		String deletedHome= NodeServiceFactory.getLocalService().findNodeByName(orgHome.getId(),DELETED_PERSONS_FOLDER);
		if(deletedHome==null) {
			Map<String, Object> props=new HashMap<>();
			props.put(CCConstants.CM_NAME,DELETED_PERSONS_FOLDER);
			deletedHome=NodeServiceFactory.getLocalService().createNodeBasic(orgHome.getId(), CCConstants.CCM_TYPE_MAP, props);
			NodeRef deletedRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, deletedHome);
			permissionService.setInheritParentPermissions(deletedRef, false);
			permissionService.setPermission(deletedRef,getAdminGroup(options) , CCConstants.PERMISSION_COORDINATOR, true);
		}
		return new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,deletedHome);
	}
	private String getAdminGroup(PersonDeleteOptions options){
		return PermissionService.GROUP_PREFIX + org.edu_sharing.service.authority.AuthorityService.getGroupName(
				org.edu_sharing.alfresco.service.AuthorityService.ADMINISTRATORS_GROUP, options.receiverGroup
		);
	}
	private void setOwnerAndPermissions(List<NodeRef> children, String userName, PersonDeleteOptions options) {
		String adminGroup = getAdminGroup(options);
		RetryingTransactionHelper rth = transactionService.getRetryingTransactionHelper();
		rth.doInTransaction((RetryingTransactionHelper.RetryingTransactionCallback<Void>) () -> {
			children.forEach((ref) -> {
				if (skipNode(ref,SKIP_ASPECTS_OWNER)) return;
				setOwner(ref, userName, options.receiver, options);
				policyBehaviourFilter.disableBehaviour(ref);

				permissionService.setPermission(ref, adminGroup, CCConstants.PERMISSION_COORDINATOR, true);
				policyBehaviourFilter.enableBehaviour(ref);
				logger.debug("setOwnerAndPermissions for " + ref);
			});
			return null;
		});
	}

	public void deleteAllRefs(Collection<NodeRef> refs) {
		refs.forEach((ref)->{
			try {
				NodeServiceFactory.getLocalService().removeNode(ref.getId(),null,false);
			} catch (InvalidNodeRefException ignored){

			}
		});
	}


	private boolean hasCCLicense(NodeRef ref) {
		String license = NodeServiceHelper.getProperty(ref, CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY);
		return CCConstants.getAllCCLicenseKeys().contains(license);
	}

	private void setOwner(NodeRef nodeRef, String oldOwner, String newOwner, PersonDeleteOptions options) {
		RetryingTransactionHelper rth = transactionService.getRetryingTransactionHelper();
		rth.doInTransaction((RetryingTransactionHelper.RetryingTransactionCallback<Void>) () -> {
			policyBehaviourFilter.disableBehaviour(nodeRef);
			ownableService.setOwner(nodeRef, newOwner);
			if (oldOwner.equals(nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CM_PROP_C_CREATOR)))) {
				nodeService.setProperty(nodeRef, QName.createQName(CCConstants.CM_PROP_C_CREATOR), newOwner);
			}
			if (oldOwner.equals(nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CM_PROP_C_MODIFIER)))) {
				nodeService.setProperty(nodeRef, QName.createQName(CCConstants.CM_PROP_C_MODIFIER), newOwner);
			}
			updateMetadataCreator(nodeRef,oldOwner,options);
			policyBehaviourFilter.enableBehaviour(nodeRef);
			return null;
		});
		new RepositoryCache().remove(nodeRef.getId());
	}

	private void updateMetadataCreator(NodeRef nodeRef, String oldOwner, PersonDeleteOptions options) {
		if(!options.cleanupMetadata){
			return;
		}
		Serializable creator = nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_REPL_METADATACONTRIBUTER_CREATOR));
		ArrayList<String> creators;
		if(creator instanceof ArrayList){
			creators= (ArrayList<String>) creator;
		}else{
			creators = new ArrayList<>();
			creators.add((String)creator);
		}
		if(creators.size()>0) {
			// @TODO: Doing this for multiple calls may be time consuming, may make a cache
			NodeRef authority = authorityService.getAuthorityNodeRef(oldOwner);
			String firstName = (String) nodeService.getProperty(authority, QName.createQName(CCConstants.CM_PROP_PERSON_FIRSTNAME));
			String lastName = (String) nodeService.getProperty(authority, QName.createQName(CCConstants.CM_PROP_PERSON_LASTNAME));
			// filter all elements where current user is really attached
			creators= creators.stream().filter((c) -> {
				Map<String, Object> vcard = VCardConverter.
						getVCardMap(CCConstants.CCM_TYPE_IO, CCConstants.CCM_PROP_IO_REPL_METADATACONTRIBUTER_CREATOR, c);
				if (vcard != null && vcard.size() > 0) {
					if (Objects.equals(firstName, vcard.get(CCConstants.CCM_PROP_IO_REPL_METADATACONTRIBUTER_CREATOR+CCConstants.VCARD_GIVENNAME)) &&
							Objects.equals(lastName, vcard.get(CCConstants.CCM_PROP_IO_REPL_METADATACONTRIBUTER_CREATOR+CCConstants.VCARD_SURNAME))) {
						logger.info("vcard creator is same as old username, will clean up vcard entry");
						return false;
					}
				}
				return true;
			}).collect(Collectors.toCollection(ArrayList::new));
			if(creators.size()==0){
				nodeService.removeProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_REPL_METADATACONTRIBUTER_CREATOR));
			}else{
				nodeService.setProperty(nodeRef,QName.createQName(CCConstants.CCM_PROP_IO_REPL_METADATACONTRIBUTER_CREATOR), creators);
			}
		}

	}

	private void deleteNode(NodeRef nodeRef) {
		/**
		 * remove without archiving
		 */
		nodeService.addAspect(nodeRef, ContentModel.ASPECT_TEMPORARY, null);
		nodeService.deleteNode(nodeRef);
	}

	public void removeContributer(NodeRef nodeRef, String username){
		NodeRef personNodeRef = personService.getPerson(username);
		String firstName = (String)nodeService.getProperty(personNodeRef, QName.createQName(CCConstants.CM_PROP_PERSON_FIRSTNAME));
		String lastName = (String)nodeService.getProperty(personNodeRef, QName.createQName(CCConstants.CM_PROP_PERSON_LASTNAME));

		QName qnameAuthor = QName.createQName(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_AUTHOR);
		QName qnameMetadata = QName.createQName(CCConstants.CCM_PROP_IO_REPL_METADATACONTRIBUTER_CREATOR);
		List<String> contributerAuthor = (List<String>)nodeService.getProperty(nodeRef, qnameAuthor);
		List<String> contributerMetadata = (List<String>)nodeService.getProperty(nodeRef,qnameMetadata);
		contributerAuthor.removeIf(author -> contains(VCardConverter.vcardToMap(author), firstName, lastName));
		nodeService.setProperty(nodeRef, qnameAuthor, (ArrayList)contributerAuthor);

		contributerMetadata.removeIf(metadataContributer -> contains(VCardConverter.vcardToMap(metadataContributer), firstName, lastName));
		nodeService.setProperty(nodeRef, qnameMetadata, (ArrayList)contributerMetadata);

	}

	private boolean contains(ArrayList<Map<String, Object>> vcardList, String firstName, String lastName) {

		if(vcardList != null && vcardList.size() > 0) {
			Map<String,Object> vcard = vcardList.iterator().next();
			String vcFirstName = (String)vcard.get(CCConstants.VCARD_GIVENNAME);
			String vcLastName = (String)vcard.get(CCConstants.VCARD_SURNAME);

			return firstName.equals(vcFirstName) && lastName.equals(vcLastName);
		}

		return false;
	}

	public void deleteCollections(String userName) {

		SearchParameters sp = new SearchParameters();
		//sp.setQuery("ASPECT:\"ccm:collection\" AND @ccm\\:collectionlevel0:true AND OWNER:\""+userName+"\"");
		sp.setQuery("ASPECT:\"ccm:collection\" AND OWNER:\""+userName+"\"");
		sp.setSkipCount(0);
		sp.setMaxItems(-1);
		sp.addStore(MCAlfrescoAPIClient.storeRef);
		sp.setLanguage(SearchService.LANGUAGE_LUCENE);
		ResultSet rs = searchService.query(sp);
		for(NodeRef nodeRef : rs.getNodeRefs()) {
			String collection = (String)nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CM_NAME));
			logger.info("deleteing collection:" + collection);
			nodeService.addAspect(nodeRef, ContentModel.ASPECT_TEMPORARY, null);
			nodeService.deleteNode(nodeRef);
		}
	}

	private NodeRef getHomeFolder(NodeRef personNodeRef) {
		return (NodeRef)nodeService.getProperty(personNodeRef,
				QName.createQName(CCConstants.CM_PROP_PERSON_HOME_FOLDER));
	}

}
