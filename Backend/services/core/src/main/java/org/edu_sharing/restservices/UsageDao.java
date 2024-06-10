package org.edu_sharing.restservices;

import java.io.StringReader;
import java.util.*;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.authentication.ContextManagementFilter;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.restservices.shared.Filter;
import org.edu_sharing.restservices.usage.v1.model.CreateUsage;
import org.edu_sharing.restservices.usage.v1.model.Usages;
import org.edu_sharing.restservices.usage.v1.model.Usages.Usage;
import org.edu_sharing.service.collection.CollectionServiceFactory;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.permission.PermissionService;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.edu_sharing.service.usage.Usage2Service;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.ValidationEvent;
import jakarta.xml.bind.ValidationEventHandler;

public class UsageDao {
	Logger logger = Logger.getLogger(UsageDao.class);

	private final PermissionService permissionService;
	RepositoryDao repoDao;

	MCAlfrescoBaseClient baseClient;

	public UsageDao(RepositoryDao repoDao) {

		this.repoDao = repoDao;
		this.baseClient = repoDao.getBaseClient();
		this.permissionService = PermissionServiceFactory.getPermissionService(repoDao.getId());
	}

	public List<Usage> getUsages(String appId) throws DAOException {

		try {
			List<Usage> result = new ArrayList<>();

			for (org.edu_sharing.service.usage.Usage usage : new Usage2Service().getUsages(appId)) {
				Usage usageResult = convertUsage(usage, Usage.class);
				result.add(usageResult);
			}

			return result;
		} catch (Throwable e) {
			throw DAOException.mapping(e);
		}
	}

	private <T extends Usage> T convertUsage(org.edu_sharing.service.usage.Usage usage, Class<T> cls) throws Exception {
		T usageResult = cls.newInstance();
		usageResult.setAppId(usage.getLmsId());

		try {
			usageResult.setAppType(ApplicationInfoList.getRepositoryInfoById(usage.getLmsId()).getType());
			usageResult.setAppSubtype(ApplicationInfoList.getRepositoryInfoById(usage.getLmsId()).getSubtype());
		} catch (Throwable t) {

		}

		// filter out usage info cause of security reasons
		String currentUser = baseClient.getAuthenticationInfo().get(CCConstants.AUTH_USERNAME);
		if (baseClient.isAdmin(currentUser) || currentUser.equals(usage.getAppUser())) {
			usageResult.setAppUser(usage.getAppUser());
			usageResult.setAppUserMail(usage.getAppUserMail());
		}

		usageResult.setType(usage.getType().name());
		usageResult.setCourseId(usage.getCourseId());
		usageResult.setDistinctPersons(usage.getDistinctPersons());
		usageResult.setGuid(usage.getGuid());
		usageResult.setNodeId(usage.getNodeId());
		usageResult.setParentNodeId(usage.getParentNodeId());
		usageResult.setResourceId(usage.getResourceId());
		usageResult.setUsageCounter(usage.getUsageCounter());
		usageResult.setUsageVersion(usage.getUsageVersion());
		usageResult.setCreated(usage.getCreated());
		usageResult.setModified(usage.getModified());
		String xmlParams = usage.getUsageXmlParams();
		
		if(xmlParams != null) {
			try {
				JAXBContext jaxbContext = JAXBContext.newInstance(Usage.Parameters.class);
	
				Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
				jaxbUnmarshaller.setEventHandler(new ValidationEventHandler() {
					@Override
					public boolean handleEvent(ValidationEvent event) {
						// ignore all errors, try to parse what is possible
						return true;
					}
				});
				usageResult.setUsageXmlParams(
						(Usage.Parameters) jaxbUnmarshaller.unmarshal(new StringReader(usage.getUsageXmlParams())));
			} catch (Throwable t) {
				logger.warn("Error converting usage xml " + usage.getUsageXmlParams(), t);
			}
			
			
			usageResult.setUsageXmlParamsRaw(org.json.XML.toJSONObject(xmlParams).toString());
		}
		return usageResult;
	}

	public List<Usage> getUsagesByCourse(String appId, String courseId) throws DAOException {
		try {
			List<Usage> result = new ArrayList<>();
			for (org.edu_sharing.service.usage.Usage usage : new Usage2Service().getUsagesByCourse(appId, courseId)) {
				Usage usageResult = convertUsage(usage, Usage.class);
				result.add(usageResult);
			}
			return result;
		} catch (Throwable e) {
			throw DAOException.mapping(e);
		}

	}

	public void deleteUsage(String nodeId, String usageId) throws DAOException {
		try {
			org.edu_sharing.service.usage.Usage usage = AuthenticationUtil.runAsSystem(() -> {
				for (org.edu_sharing.service.usage.Usage u : new Usage2Service().getUsageByParentNodeId(null, null,
						nodeId)) {
					if (u.getNodeId().equals(usageId)) {
						return u;
					}
				}
				return null;
			});
			if(usage == null) {
				throw new DAOMissingException(new IllegalArgumentException(usageId + " is not an usage of " + nodeId));
			}
			boolean permission = (ContextManagementFilter.accessTool.get() != null) ? true : permissionService.hasPermission(StoreRef.PROTOCOL_WORKSPACE,
					StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId,
					CCConstants.PERMISSION_CHANGEPERMISSIONS);
			if(ContextManagementFilter.accessTool != null
					&& ContextManagementFilter.accessTool.get() != null){
				if(ContextManagementFilter.accessTool.get().getApplicationInfo().getAppId().equals(usage.getLmsId())) {
					permission = true;
					logger.info("Delete usage allowed for app id " + usage.getLmsId());
				} else {
					throw new SecurityException("The current authenticated app id is not allowed to delete this usage");
				}
			}
			if (!permission) {
				throw new SecurityException("Can not modify usages on node " + nodeId);
			}
			AuthenticationUtil.runAsSystem(() -> {
				if (new Usage2Service().deleteUsage(null, null, usage.getLmsId(), usage.getCourseId(), nodeId,
						usage.getResourceId())) {
					return null;
				} else {
					throw new RuntimeException("Error deleting usage " + usage.getNodeId());
				}
			});
		} catch (Throwable t) {
			// unmarshall exception
			if(t instanceof DAOException) {
				throw t;
			} else if(t.getCause() != null) {
				throw DAOException.mapping(t.getCause().getCause());
			} else {
				throw DAOException.mapping(t);
			}
		}
	}

	public List<Usage> getUsagesByNode(String nodeId) throws DAOException {
		try {

			List<Usage> result = new ArrayList<>();
			for (org.edu_sharing.service.usage.Usage usage : new Usage2Service().getUsageByParentNodeId(null, null,
					nodeId)) {
				Usage usageResult = convertUsage(usage, Usage.class);
				result.add(usageResult);
			}
			// remove usages from internal collections, since they're returned in getUsagesByNodeCollection
			/*
			Set<Usages.CollectionUsage> collections = getUsagesByNodeCollection(nodeId);
			result = result.stream().filter((r) ->
				collections.stream().filter((c) -> c.getNodeId().equals(r.getNodeId())).count()==0
			).collect(Collectors.toList());
			*/
			return result;

		} catch (Throwable e) {
			throw DAOException.mapping(e);
		}
	}

	public Collection<Usages.CollectionUsage> getUsagesByNodeCollection(String nodeId) throws DAOException {
		try {
			//Collection<Usages.CollectionUsage> collections = new HashSet<>();
			Collection<Usages.CollectionUsage> collections = new ArrayList<>();
			for (org.edu_sharing.service.usage.Usage usage : new Usage2Service().getUsageByParentNodeId(null, null,
					nodeId)) {
				if (usage.getCourseId() == null)
					continue;
				try {
					Usages.CollectionUsage collectionUsage = convertUsage(usage, Usages.CollectionUsage.class);
					collectionUsage
							.setCollection(CollectionDao.getCollection(repoDao, usage.getCourseId()).asNode());
					collectionUsage.setCollectionUsageType(Usages.CollectionUsageType.ACTIVE);
					collections.add(collectionUsage);
				} catch (Throwable t) {
				}
			}
			CollectionServiceFactory.getLocalService().getCollectionProposals(nodeId, CCConstants.PROPOSAL_STATUS.PENDING).forEach((ref) -> {
				Usages.CollectionUsage usage = new Usages.CollectionUsage();
				try {
					usage.setCollection(CollectionDao.getCollection(repoDao, ref.getId()).asNode());
					usage.setCollectionUsageType(Usages.CollectionUsageType.PROPOSAL_PENDING);
					collections.add(usage);
				} catch (DAOException e) {
					logger.warn("Could not fetch collection: " + e.getMessage(), e);
				}
			});
			CollectionServiceFactory.getLocalService().getCollectionProposals(nodeId, CCConstants.PROPOSAL_STATUS.DECLINED).forEach((ref) -> {
				Usages.CollectionUsage usage = new Usages.CollectionUsage();
				try {
					usage.setCollection(CollectionDao.getCollection(repoDao, ref.getId()).asNode());
					usage.setCollectionUsageType(Usages.CollectionUsageType.PROPOSAL_DECLINED);
					collections.add(usage);
				} catch (DAOException e) {
					logger.warn("Could not fetch collection: " + e.getMessage(), e);
				}
			});
			return collections;
		} catch (Throwable t) {
			throw DAOException.mapping(t);
		}
	}

	public List<Usages.NodeUsage> getUsages(String repositoryId, String nodeId, Long from, Long to) throws Exception {

		
		RunAsWork<List<Usages.NodeUsage>> runAs = new RunAsWork<List<Usages.NodeUsage>>() {
			@Override
			public List<Usages.NodeUsage> doWork() throws Exception {
				RepositoryDao rd = RepositoryDao.getHomeRepository();
				
				List<Usages.NodeUsage> result = new ArrayList<Usages.NodeUsage>();
				for (org.edu_sharing.service.usage.Usage usage : new Usage2Service().getUsages(repositoryId, nodeId, from,
						to)) {
					try {
						Usages.NodeUsage usageRest = convertUsage(usage, Usages.NodeUsage.class);
						usageRest.setNode(NodeDao.getNode(rd, usageRest.getParentNodeId(),Filter.createShowAllFilter()).asNode());
						result.add(usageRest);
					} catch (Throwable t) {
					}
				}
				return result;
			}
		};
		return AuthenticationUtil.runAsSystem(runAs);
		
	}

	public Usages.Usage setUsage(String repository, CreateUsage usage) throws Exception {

		if(ContextManagementFilter.accessTool == null
				|| ContextManagementFilter.accessTool.get() == null){
			throw new DAOSecurityException(new Exception("app signature required to use this endpoint."));
		}
		if(AuthenticationUtil.getFullyAuthenticatedUser() == null || AuthorityServiceFactory.getLocalService().isGuest()){
			throw new DAOSecurityException(new Exception("authenticated user required to use this endpoint."));
		}

		Usage2Service us = new Usage2Service();
		org.edu_sharing.service.usage.Usage usageResult = us.setUsage(
				repository,
				AuthenticationUtil.getFullyAuthenticatedUser(),
				usage.appId,
				usage.courseId,
				usage.nodeId,
				(String)AuthorityServiceFactory.getLocalService().getUserInfo(AuthenticationUtil.getFullyAuthenticatedUser()).get(CCConstants.PROP_USER_EMAIL),
				null,
				null,
				-1 ,
				usage.nodeVersion,
				usage.resourceId,
				null
		);
		return convertUsage(usageResult, Usages.Usage.class);
	}
}
