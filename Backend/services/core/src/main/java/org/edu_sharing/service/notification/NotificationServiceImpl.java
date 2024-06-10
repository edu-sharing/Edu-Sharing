package org.edu_sharing.service.notification;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.alfresco.workspace_administration.NodeServiceInterceptor;
import org.edu_sharing.metadataset.v2.MetadataWidget;
import org.edu_sharing.repository.client.rpc.EduGroup;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.I18nAngular;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.Mail;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.repository.server.tools.mailtemplates.MailTemplate;
import org.edu_sharing.rest.notification.data.StatusDTO;
import org.edu_sharing.rest.notification.event.NotificationEventDTO;
import org.edu_sharing.restservices.mds.v1.model.MdsValue;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.authority.AuthorityServiceHelper;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.rating.RatingDetails;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import jakarta.servlet.ServletContext;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class NotificationServiceImpl implements NotificationService {

    private final Logger logger = Logger.getLogger(NotificationServiceImpl.class);

    private final ApplicationInfo appInfo;


    public NotificationServiceImpl(String appId) {
        appInfo = (appId == null) ? ApplicationInfoList.getHomeRepository() : ApplicationInfoList.getRepositoryInfoById(appId);
    }

    @Override
    public void notifyNodeIssue(String nodeId, String reason, String nodeType, List<String> aspects, Map<String, Object> properties, String userEmail, String userComment) throws Throwable {
        logger.info(String.format("send notifyNodeIssue: nodeId: %s, reason: %s, userComment: %s", nodeId, reason, userComment));
        String currentLocale = new AuthenticationToolAPI().getCurrentLocale();
        String subject=MailTemplate.getSubject("nodeIssue", currentLocale);
        String content=MailTemplate.getContent("nodeIssue", currentLocale, true);
        Map<String, String> replace = new HashMap<>();
        replace.put("reporterEmail", userEmail.trim());
        replace.put("userComment", userComment);
        replace.put("reason", reason);
        replace.put("name", (String) properties.get(CCConstants.CM_NAME));
        replace.put("id", nodeId);
        replace.put("link", URLTool.getNgRenderNodeUrl(nodeId, null, true));
        replace.put("link.static", URLTool.getNgRenderNodeUrl(nodeId, null, false));
        MailTemplate.applyNodePropertiesToMap("node.", properties, replace);

		if(Context.getCurrentInstance() != null && Context.getCurrentInstance().getRequest() != null) {
			// add request headers to evaluate in template (i.e. user-agent)
			replace.putAll(
					Collections.list(Context.getCurrentInstance().getRequest().getHeaderNames()).stream().collect(
							HashMap::new,
							(m, entry) -> m.put("request." + entry, Context.getCurrentInstance().getRequest().getHeader(entry)),
							HashMap::putAll
					)
			);
		}
        try {
            Map<String, Object> userProps = NodeServiceHelper.getProperties(AuthorityServiceFactory.getLocalService().getAuthorityNodeRef(AuthenticationUtil.getFullyAuthenticatedUser()));
            MailTemplate.applyNodePropertiesToMap("user.", userProps, replace);
            SearchResult<EduGroup> orgList = SearchServiceFactory.getLocalService().getAllOrganizations(true);
            if(!orgList.getData().isEmpty()) {
                Map<String, Object> orgProps = NodeServiceHelper.getProperties(AuthorityServiceFactory.getLocalService().getAuthorityNodeRef(orgList.getData().get(0).getGroupname()));
                MailTemplate.applyNodePropertiesToMap("user.organization.", orgProps, replace);
            }
            List<String> mzList = SearchServiceFactory.getLocalService().getAllMediacenters();
            if(!mzList.isEmpty()) {
                Map<String, Object> mzProps = NodeServiceHelper.getProperties(AuthorityServiceFactory.getLocalService().getAuthorityNodeRef(mzList.get(0)));
                MailTemplate.applyNodePropertiesToMap("user.mediacenter.", mzProps, replace);
            }
        } catch (Throwable ignored) {

        }
        Mail mail=new Mail();
        List<String> receivers = null;
        if(mail.getConfig().hasPath("report.receivers")) {
            receivers = mail.getConfig().getStringList("report.receivers");
        } else if (mail.getConfig().getString("report.receiver") != null){
            receivers = Collections.singletonList(mail.getConfig().getString("report.receiver"));
            logger.info("report.receiver is deprecated. Prefer using the report.receivers field instead");
        }
        if(receivers==null || receivers.isEmpty()) {
            throw new IllegalArgumentException("no mail.report.receivers registered in ccmail.properties");
        }
        ServletContext context = Context.getCurrentInstance().getRequest().getSession().getServletContext();
        for (String receiver : receivers) {
            mail.sendMailHtml(
                    context,
                    null,
                    userEmail,
                    receiver,
                    subject, content, replace);
        }
    }

    @Override
    public void notifyWorkflowChanged(String nodeId, String nodeType, List<String> aspects, Map<String, Object> nodeProperties, String receiver, String comment, String status) {
        logger.info(String.format("send notifyWorkflowChanged: nodeId: %s, nodePropertiesList: %s, comment: %s, status: %s", nodeId, nodeProperties, comment, status));

        MailTemplate.UserMail receiverMail = MailTemplate.getUserMailData(receiver);
        EmailValidator mailValidator = EmailValidator.getInstance(true, true);
        if (mailValidator.isValid(receiverMail.getEmail())) {
            try {
                MailTemplate.UserMail sender = MailTemplate.getUserMailData(new AuthenticationToolAPI().getCurrentUser());
                Map<String, String> replace = new HashMap<>();
                sender.applyToMap("assigner.", replace);
                replace.put("comment", comment);
                MailTemplate.addContentLinks(ApplicationInfoList.getHomeRepository(), nodeId, replace, "link");
                replace.put("status", I18nAngular.getTranslationAngular("common", "WORKFLOW." + status));
                receiverMail.applyToMap("", replace);
                MailTemplate.applyNodePropertiesToMap("nodeId.", nodeProperties, replace);

                String template = "invited_workflow";
                MailTemplate.sendMail(sender.getFullName(), sender.getEmail(), receiver, template, replace);
            } catch (Throwable t) {
                logger.warn("Mail send failed", t);
            }
        }
    }

    @Override
    public void notifyPersonStatusChanged(String receiver, String firstname, String lastName, String oldStatus, String newStatus) {
        Map<String, String> replace = new HashMap<>();
        replace.put("firstName", firstname);
        replace.put("lastName", lastName);
        replace.put("oldStatus", I18nAngular.getTranslationAngular("permissions", "PERMISSIONS.USER_STATUS." + oldStatus));
        replace.put("newStatus", I18nAngular.getTranslationAngular("permissions", "PERMISSIONS.USER_STATUS." + newStatus));
        try {
            String template = "userStatusChanged";
            MailTemplate.sendMail(receiver, template, replace);
        } catch (Exception e) {
            logger.warn("Can not send status notify mail to user: " + e.getMessage(), e);
        }
    }

    @Override
    public void notifyPermissionChanged(String senderAuthority, String receiverAuthority, String nodeId, String nodeType, List<String> aspects, Map<String, Object> props, String[] permissions, String mailText) throws Throwable {
        MailTemplate.UserMail sender = MailTemplate.getUserMailData(senderAuthority);
        MailTemplate.UserMail receiver = MailTemplate.getUserMailData(receiverAuthority);
        EmailValidator mailValidator = EmailValidator.getInstance(true, true);

        if (!mailValidator.isValid(receiver.getEmail())) {
            logger.info("username/receiverAuthority: " + receiverAuthority + " has no valid emailaddress:" + receiver.getEmail());
            return;
        }

        // if the receiver is the creator itself, skip it (because it is automatically added)
        String nodeCreator = (String) props.get(CCConstants.CM_PROP_C_CREATOR);
        if (receiverAuthority.equals(nodeCreator)) {
            return;
        }

        // used for sending copy to user
        String internalNodeType = (String) props.get(CCConstants.NODETYPE);

        String name = internalNodeType.equals(CCConstants.CCM_TYPE_IO)
                ? (String) props.get(CCConstants.LOM_PROP_GENERAL_TITLE)
                : (String) props.get(CCConstants.CM_PROP_C_TITLE);

        if (StringUtils.isBlank(name)) {
            name = (String) props.get(CCConstants.CM_NAME);
        }

        String permText = Arrays.stream(permissions)
                .filter(perm -> !(CCConstants.CCM_VALUE_SCOPE_SAFE.equals(NodeServiceInterceptor.getEduSharingScope()) && Objects.equals(CCConstants.PERMISSION_CC_PUBLISH, perm)))
                .map(I18nAngular::getPermissionDescription)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining("\n"));

        Map<String, String> replace = new HashMap<>();
        receiver.applyToMap("", replace);
        sender.applyToMap("inviter.", replace);
        MailTemplate.applyNodePropertiesToMap("nodeId.", props, replace);
        replace.put("name", name.trim());
        replace.put("message", mailText.replace("\n", "<br />").trim());
        replace.put("permissions", permText.trim());
        MailTemplate.addContentLinks(appInfo, nodeId, replace, "link");

        String template = "invited";
        if (CCConstants.CCM_VALUE_SCOPE_SAFE.equals(NodeServiceInterceptor.getEduSharingScope())) {
            template = "invited_safe";
        } else if (internalNodeType.equals(CCConstants.CCM_TYPE_MAP) && aspects.contains(CCConstants.CCM_ASPECT_COLLECTION)) {
            template = "invited_collection";
        }

        MailTemplate.sendMail(sender.getFullName(), sender.getEmail(), receiver.getEmail(), template, replace);
    }

    public void notifyMetadataSetSuggestion(MdsValue mdsValue, MetadataWidget widgetDefinition, List<String> nodeId, List<String> nodeType, List<List<String>> aspects, List<Map<String, Object>> nodePropertiesList) throws Throwable {
        String currentUser = AuthenticationUtil.getFullyAuthenticatedUser();
        Map<String, String> replace = new HashMap<>();
        if (currentUser != null) {
            NodeRef userRef = AuthorityServiceHelper.getAuthorityNodeRef(currentUser);
            if (userRef != null) {
                replace.put("firstName", NodeServiceHelper.getProperty(userRef, CCConstants.CM_PROP_PERSON_FIRSTNAME));
                replace.put("lastName", NodeServiceHelper.getProperty(userRef, CCConstants.CM_PROP_PERSON_LASTNAME));
            }
        }
        replace.put("widgetId", widgetDefinition.getId());
        replace.put("widgetCaption", widgetDefinition.getCaption());
        replace.put("caption", mdsValue.getCaption());
        replace.put("id", mdsValue.getId());
        replace.put("parentId", mdsValue.getParent());
        replace.put("parentCaption", mdsValue.getParent() == null ? null : widgetDefinition.getValuesAsMap().get(mdsValue.getParent()).getCaption());
        if(!nodeId.isEmpty()) {
            MailTemplate.applyNodePropertiesToMap("nodeId.", nodePropertiesList.get(0), replace);
            MailTemplate.addContentLinks(ApplicationInfoList.getHomeRepository(), nodeId.get(0), replace, "link");
        }
        String[] receivers = widgetDefinition.getSuggestionReceiver().split(",");
        for (String receiver : receivers) {
            MailTemplate.sendMail(receiver, "mdsValuespaceSuggestion", replace);
        }
    }

    @Override
    public void notifyComment(String node, String comment, String commentReference, String nodeType, List<String> aspects, Map<String, Object> nodeProperties, Status status) {

    }

    @Override
    public void notifyAddCollection(String collectionId, String refNodeId, String collectionType, List<String> collectionAspects, Map<String, Object> collectionProperties, String nodeType, List<String> nodeAspects, Map<String, Object> nodeProperties, Status status) {

    }
    @Override
    public void notifyProposeForCollection(String collectionId, String refNodeId, String collectionType, List<String> collectionAspects, Map<String, Object> collectionProperties, String nodeType, List<String> nodeAspects, Map<String, Object> nodeProperties, Status status) {

    }

    @Override
    public void notifyRatingChanged(String nodeId, String nodeType, List<String> aspects, Map<String, Object> nodeProps, Double rating, RatingDetails accumulatedRatings, Status removed) {

    }

    @Override
    public Page<NotificationEventDTO> getNotifications(String receiverId, List<StatusDTO> status, Pageable pageable) {
        return null;
    }

    @Override
    public NotificationEventDTO setNotificationStatusByNotificationId(String id, StatusDTO status) {
        return null;
    }

    @Override
    public void setNotificationStatusByReceiverId(String receiverId, List<StatusDTO> oldStatusList, StatusDTO newStatus) throws IOException {
    }

    @Override
    public void deleteNotification(String id) {

    }

}
