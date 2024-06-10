package org.edu_sharing.service.share;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.namespace.QName;
import org.apache.axis.utils.StringUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.validator.EmailValidator;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.policy.ScopeNodeWrongScopeException;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.rpc.Share;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.DateTool;
import org.edu_sharing.repository.server.tools.I18nServer;
import org.edu_sharing.repository.server.tools.KeyTool;
import org.edu_sharing.repository.server.tools.Mail;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.alfresco.service.toolpermission.ToolPermissionException;
import org.edu_sharing.service.permission.PermissionService;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;
import org.springframework.context.ApplicationContext;

public class ShareServiceImpl implements ShareService {
	
	public static final QName SHARE_TYPE = QName.createQName(CCConstants.CCM_TYPE_SHARE);
	public static final QName SHARE_ASSOC = QName.createQName(CCConstants.CCM_ASSOC_ASSIGNED_SHARES);
	
	public static final QName SHARES_ASPECT = QName.createQName(CCConstants.CCM_ASPECT_SHARES);
	
	public static final QName SHARE_PROP_EXPIRYDATE = QName.createQName(CCConstants.CCM_PROP_SHARE_EXPIRYDATE);
	public static final QName SHARE_PROP_PASSWORD = QName.createQName(CCConstants.CCM_PROP_SHARE_PASSWORD);
	public static final QName SHARE_PROP_SHARE_MAIL = QName.createQName(CCConstants.CCM_PROP_SHARE_MAIL);
	public static final QName SHARE_PROP_SHARE_TOKEN = QName.createQName(CCConstants.CCM_PROP_SHARE_TOKEN);
	public static final QName SHARE_PROP_DOWNLOAD_COUNTER = QName.createQName(CCConstants.CCM_PROP_SHARE_DOWNLOAD_COUNTER);
	
	public static String I18n_MailSubject = "dialog_share_mailsubject_file";
	public static String I18n_MailSubjectLink = "dialog_share_mailsubject_link";
	public static String I18n_MailText = "dialog_share_mailtext_file";
	public static String I18n_MailTextLink = "dialog_share_mailtext_link";

	private final ServiceRegistry serviceRegistry;
	private final PermissionService permissionService;

	
	Logger logger = Logger.getLogger(ShareServiceImpl.class);
	
	public ShareServiceImpl(PermissionService permissionService) {
		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();

		this.serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
		this.permissionService = permissionService;
	}
	
	public Share createShare(String nodeId, long expiryDate, String password) throws EMailValidationException, EMailSendFailedException, ExpiryDateValidationException, NodeDoesNotExsistException, PermissionFailedException{
		return getShare(nodeId, createShare(nodeId,new String[]{EMAIL_TYPE_LINK},expiryDate,password,null));
	}
	
	@Override
	public String createShare(String nodeId, String[] emails, long expiryDate, String password, String emailMessageLocale) throws EMailValidationException, EMailSendFailedException, ExpiryDateValidationException,
			NodeDoesNotExsistException, PermissionFailedException {
		if(!ToolPermissionServiceFactory.getInstance().hasToolPermission(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_LINK)) {
			throw new ToolPermissionException(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_LINK);
		}
		NodeService nodeService = serviceRegistry.getNodeService();
		AuthenticationService authService = serviceRegistry.getAuthenticationService();
		//email validation
		EmailValidator mailValidator = EmailValidator.getInstance();
		for(String email : emails){
			if(!EMAIL_TYPE_LINK.equals(email) && !mailValidator.isValid(email)){
				throw new EMailValidationException("EMail: " + email + " is invalid!");
			}
		}
		
		//expiry date validation
		if(expiryDate != EXPIRY_DATE_UNLIMITED && new Date(expiryDate).before(new Date(System.currentTimeMillis()))){
			throw new ExpiryDateValidationException("Expiry Date "+ new Date(expiryDate) +  " is to old!");
		}
		
		//parent validation
		NodeRef io = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,nodeId);
		if(!nodeService.exists(io)){
			throw new NodeDoesNotExsistException();
		}
		throwIfScopedNode(io);
		
		//license/permission validation
		String globalLicense = null;
		ArrayList<String> globalLicenses = (ArrayList<String>)nodeService.getProperty(io, QName.createQName(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY));
		if(globalLicenses != null){
			globalLicense = globalLicenses.get(0);
		}
		
		boolean isLink = false;
		String wwwUrl = (String)nodeService.getProperty(io, QName.createQName(CCConstants.CCM_PROP_IO_WWWURL));
		if(wwwUrl != null){
			isLink = true;
		}
		
		NodeRef personNodeRef = (NodeRef)serviceRegistry.getPersonService().getPerson(authService.getCurrentUserName());
		Map<QName, Serializable> personProps = serviceRegistry.getNodeService().getProperties(personNodeRef);
		String nameInvitor = (String)personProps.get(QName.createQName(CCConstants.CM_PROP_PERSON_FIRSTNAME))+" "+(String)personProps.get(QName.createQName(CCConstants.CM_PROP_PERSON_LASTNAME));


		//shares is no mandatory aspect so:
		if(!nodeService.hasAspect(io, SHARES_ASPECT)){
			nodeService.addAspect(io, SHARES_ASPECT, null);
		}

		permissionService.addUserToSharedList(authService.getCurrentUserName(), io);
		ArrayList<String> phUsers = (ArrayList<String>) nodeService.getProperty(io, QName.createQName(CCConstants.CCM_PROP_PH_USERS));

		String token=null;
		for(String email : emails){
			
			Map<QName, Serializable> props = new HashMap<>();
			
			props.put(SHARE_PROP_EXPIRYDATE, expiryDate);
			props.put(SHARE_PROP_SHARE_MAIL, email);
			props.put(SHARE_PROP_PASSWORD, encryptPassword(password));
			
			token = new KeyTool().getKey();
			
			props.put(SHARE_PROP_SHARE_TOKEN, token);
			props.put(SHARE_PROP_DOWNLOAD_COUNTER, 0);

			ChildAssociationRef createdChild = nodeService.createNode(io, SHARE_ASSOC, QName.createQName(email), SHARE_TYPE, props);
			
			if(!EMAIL_TYPE_LINK.equals(email)){
				Mail mail = new Mail();
				String subject = (isLink)?  I18nServer.getTranslationDefaultResourcebundle(I18n_MailSubjectLink, emailMessageLocale) : I18nServer.getTranslationDefaultResourcebundle(I18n_MailSubject, emailMessageLocale);
				
				subject = subject.replace("{user}",nameInvitor);
				
				String message = (isLink) ? I18nServer.getTranslationDefaultResourcebundle(I18n_MailTextLink, emailMessageLocale) : I18nServer.getTranslationDefaultResourcebundle(I18n_MailText, emailMessageLocale);
				
				message = message.replace("{link}", URLTool.getShareServletUrl(io, token));
				message = message.replace("{user}", nameInvitor);
				
				String expiryDateString = null;
				if(expiryDate == -1){
					expiryDateString = I18nServer.getTranslationDefaultResourcebundle("dialog_share_expiry_unlimited", emailMessageLocale);
				}else{
					expiryDateString = new DateTool().formatDate(expiryDate,DateFormat.LONG,null);
				}
				
				message = message.replace("{expiry}", expiryDateString);
				try{
					mail.sendMail(email, subject, message);
				}catch(Exception e){
					
					//rollback
					nodeService.deleteNode(createdChild.getChildRef());
					logger.error(e.getMessage(), e);
					throw new EMailSendFailedException(e.getMessage());
				}
			}
		}
		return token;
	}

	public static String encryptPassword(String password) {
		if(password==null || password.isEmpty())
			return null;
		return DigestUtils.shaHex(password);
	}

	private void throwIfScopedNode(NodeRef io) {
		if(serviceRegistry.getNodeService().getProperty(io, QName.createQName(CCConstants.CCM_PROP_SCOPE_TYPE))!=null){
			throw new ScopeNodeWrongScopeException("Not allowed to share a scoped node!");
		}		
	}
	@Override
	public void removeShare(String nodeId, String shareNodeId){
		serviceRegistry.getNodeService().deleteNode(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,shareNodeId));
		permissionService.cleanUpSharedList(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId));
	}

	@Override
	public void updateShare(String nodeId, String email, long expiryDate) throws EMailValidationException, ExpiryDateValidationException,
			NodeDoesNotExsistException, PermissionFailedException {
	}

	public void updateDownloadCount(Share share){
		throwIfScopedNode(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,share.getIoNodeId()));
		serviceRegistry.getNodeService().setProperty(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, share.getNodeId()), SHARE_PROP_DOWNLOAD_COUNTER,share.getDownloadCount());
	}
	@Override
	public void updateShare(Share share) {
		NodeRef shareNodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, share.getNodeId());
		Map<QName,Serializable> props = new HashMap<>();
		props.put(SHARE_PROP_SHARE_TOKEN, share.getToken());
		props.put(SHARE_PROP_EXPIRYDATE, share.getExpiryDate());

		// reset password only if it is given and empty otherwise update the password if given or use the stored one if not provided
		if(share.getPassword()==null){
			props.put(SHARE_PROP_PASSWORD, serviceRegistry.getNodeService().getProperty(shareNodeRef,SHARE_PROP_PASSWORD));
		} else if(!StringUtils.isEmpty(share.getPassword())) {
			props.put(SHARE_PROP_PASSWORD, encryptPassword(share.getPassword()));
		}

		props.put(SHARE_PROP_SHARE_MAIL, share.getEmail());
		props.put(SHARE_PROP_DOWNLOAD_COUNTER, share.getDownloadCount());
		throwIfScopedNode(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,share.getIoNodeId()));
		serviceRegistry.getNodeService().setProperties(shareNodeRef, props);
	}
	
	@Override
	public Share[] getShares(String nodeId) {
		
		Set<QName> qnameSet = new HashSet<QName>();
		qnameSet.add(SHARE_TYPE);
		List<ChildAssociationRef> childNodeRefs = serviceRegistry.getNodeService().getChildAssocs(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,nodeId),qnameSet);
		
		List<Share> result = new ArrayList<>();
		for(ChildAssociationRef childRef : childNodeRefs){
			Map<QName,Serializable> props =  serviceRegistry.getNodeService().getProperties(childRef.getChildRef());
			Share share = getNodeShareObject(nodeId,childRef.getChildRef());
			result.add(share);
		}
		return result.toArray(new Share[result.size()]);
	}
	
	
	public Share getShare(String nodeId, String token){
		List<ChildAssociationRef> childAssocRefs = serviceRegistry.getNodeService().getChildAssocsByPropertyValue(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,nodeId), SHARE_PROP_SHARE_TOKEN,token);
		if(childAssocRefs.size() == 1){
			NodeRef shareNodeRef = childAssocRefs.get(0).getChildRef();
			Share share = getNodeShareObject(nodeId, shareNodeRef);
			return share;
			
		}else{
			logger.error("invalid number of share objects for io:"+childAssocRefs.size()+" "+nodeId);
			return null;
		}
	}
	@Override
	public boolean isNodeAccessibleViaShare(NodeRef sharedNode, String accessNodeId){
		boolean isChild=false;
		for(ChildAssociationRef ref : serviceRegistry.getNodeService().getChildAssocs(sharedNode)){
			if(ref.getChildRef().getId().equals(accessNodeId)){
				isChild=true;
				break;
			}
		}
		return isChild || sharedNode.getId().equals(accessNodeId);
	}
	private Share getNodeShareObject(String nodeId, NodeRef shareNodeRef) {
		Map<String, Object> props;
		Map<QName, Serializable> propsNative;
		try {
			props = NodeServiceFactory.getLocalService().getProperties(shareNodeRef.getStoreRef().getProtocol(),
					shareNodeRef.getStoreRef().getIdentifier(),
					shareNodeRef.getId());
			propsNative = serviceRegistry.getNodeService().getProperties(shareNodeRef);
		}catch(Throwable t){
			throw new RuntimeException(t);
		}
		Share share = new Share();
		share.setProperties(props);
		share.setIoNodeId(nodeId);
		share.setNodeId(shareNodeRef.getId());
		share.setEmail((String)propsNative.get(SHARE_PROP_SHARE_MAIL));
		share.setPassword((String)propsNative.get(SHARE_PROP_PASSWORD));
		share.setExpiryDate((Long)propsNative.get(SHARE_PROP_EXPIRYDATE));
		share.setDownloadCount((Integer)propsNative.get(SHARE_PROP_DOWNLOAD_COUNTER));
		share.setInvitedAt((Date)propsNative.get(QName.createQName(CCConstants.CM_PROP_C_CREATED)));
		share.setToken((String)propsNative.get(SHARE_PROP_SHARE_TOKEN));

		return share;
	}
}
