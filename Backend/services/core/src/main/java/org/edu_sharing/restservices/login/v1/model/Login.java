package org.edu_sharing.restservices.login.v1.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

import lombok.Data;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.edu_sharing.repository.server.authentication.LoginHelper;
import org.edu_sharing.repository.server.authentication.RemoteAuthDescription;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.restservices.DAOException;
import org.edu_sharing.restservices.NodeDao;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.lti13.LTIConstants;
import org.edu_sharing.service.lti13.model.LTISessionObject;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;

import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class Login {

    private Map<String, RemoteAuthDescription> remoteAuthentications;
    @JsonProperty(required = true, value = "isValidLogin")
    private boolean isValidLogin;
    private String currentScope;
    private String userHome;
    @JsonProperty(required = true)
    private int sessionTimeout;
    @JsonProperty(required = true, value = "isGuest")
    private boolean isGuest;
    private List<String> toolPermissions;
    @JsonProperty(required = true,  value = "isAdmin")
    private boolean isAdmin;
    private String statusCode;
    private String authorityName;
    private LTISession ltiSession;


    public final static String STATUS_CODE_OK = "OK";
    public final static String STATUS_CODE_GUEST = "GUEST";
    public final static String STATUS_CODE_INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public final static String STATUS_CODE_PREVIOUS_SESSION_REQUIRED = "PREVIOUS_SESSION_REQUIRED";
    public final static String STATUS_CODE_PREVIOUS_USER_WRONG = "PREVIOUS_USER_WRONG";
    public final static String STATUS_CODE_INVALID_SCOPE = "INVALID_SCOPE";
    public final static String STATUS_CODE_PASSWORD_EXPIRED = "PASSWORD_EXPIRED";
    public final static String STATUS_CODE_PERSON_BLOCKED = "PERSON_BLOCKED";

    @Data
    public static class LTISession {
        private boolean acceptMultiple;
        private String deeplinkReturnUrl;
        private List<String> acceptTypes = new ArrayList<>();
        private List<String> acceptPresentationDocumentTargets = new ArrayList<>();
        private boolean canConfirm;
        private String title;
        private String text;

        /**
         * custom property:
         * when the context of ltideeplink message is an edu-sharing nodeId.
         * we resolve the node to find out if it was created for a ltitool with ltitool_customcontent_option
         * and prevent a platform representing the same app as the tool: embedding it's own nodes
         */
        private Node customContentNode;
    }


    public Login() {

    }


    public Login(boolean isValidLogin, String scope, HttpSession session) {
        this(isValidLogin, scope, null, session, (isValidLogin) ? STATUS_CODE_OK : STATUS_CODE_INVALID_CREDENTIALS);
    }

    public Login(boolean isValidLogin, String scope, String userHome, HttpSession session, String statusCode) {

        org.edu_sharing.service.authority.AuthorityService service = AuthorityServiceFactory.getAuthorityService(ApplicationInfoList.getHomeRepository().getAppId());

        if (isValidLogin) {
            try {
                this.toolPermissions = ToolPermissionServiceFactory.getInstance().getAllAvailableToolPermissions();
            } catch (Throwable t) {
                // not logged in
            }
        }
        this.statusCode = service.isGuest() ? STATUS_CODE_GUEST : statusCode;
        this.authorityName = service.isGuest() ? null : AuthenticationUtil.getFullyAuthenticatedUser();
        this.isAdmin = service.isGlobalAdmin();
        this.isValidLogin = isValidLogin;
        this.currentScope = scope;
        if (isValidLogin && scope == null && !service.isGuest())
            this.remoteAuthentications = LoginHelper.getRemoteAuthsForSession();
        this.userHome = userHome;
        this.isGuest = service.isGuest();
        this.sessionTimeout = session.getMaxInactiveInterval();

        LTISessionObject ltiSessionObject = (LTISessionObject) session.getAttribute(LTISessionObject.class.getName());
        if (ltiSessionObject != null) {
            LTISession ltiSession = new LTISession();
            if (ltiSessionObject.getDeepLinkingSettings() != null) {
                if (ltiSessionObject.getDeepLinkingSettings().containsKey(LTIConstants.DEEP_LINK_ACCEPT_MULTIPLE)) {
                    ltiSession.acceptMultiple = (Boolean) ltiSessionObject.getDeepLinkingSettings().get(LTIConstants.DEEP_LINK_ACCEPT_MULTIPLE);
                }
                if (ltiSessionObject.getDeepLinkingSettings().containsKey(LTIConstants.DEEP_LINK_DOCUMENT_TARGETS)) {
                    ltiSession.acceptPresentationDocumentTargets = (List<String>) ltiSessionObject.getDeepLinkingSettings().get(LTIConstants.DEEP_LINK_DOCUMENT_TARGETS);
                }
                if (ltiSessionObject.getDeepLinkingSettings().containsKey(LTIConstants.DEEP_LINK_ACCEPT_TYPES)) {
                    ltiSession.acceptTypes = (List<String>) ltiSessionObject.getDeepLinkingSettings().get(LTIConstants.DEEP_LINK_ACCEPT_TYPES);
                }
                if (ltiSessionObject.getDeepLinkingSettings().containsKey(LTIConstants.DEEP_LINK_CAN_CONFIRM)) {
                    ltiSession.canConfirm = (Boolean) ltiSessionObject.getDeepLinkingSettings().get(LTIConstants.DEEP_LINK_CAN_CONFIRM);
                }
                if (ltiSessionObject.getDeepLinkingSettings().containsKey(LTIConstants.DEEP_LINK_RETURN_URL)) {
                    ltiSession.deeplinkReturnUrl = (String) ltiSessionObject.getDeepLinkingSettings().get(LTIConstants.DEEP_LINK_RETURN_URL);
                }
                if (ltiSessionObject.getDeepLinkingSettings().containsKey(LTIConstants.DEEP_LINK_TEXT)) {
                    ltiSession.text = (String) ltiSessionObject.getDeepLinkingSettings().get(LTIConstants.DEEP_LINK_TEXT);
                }
                if (ltiSessionObject.getDeepLinkingSettings().containsKey(LTIConstants.DEEP_LINK_TITLE)) {
                    ltiSession.title = (String) ltiSessionObject.getDeepLinkingSettings().get(LTIConstants.DEEP_LINK_TITLE);
                }
            }

            if (ltiSessionObject.getContextId() != null
                    && NodeServiceFactory.getLocalService().exists("workspace", "SpacesStore", ltiSessionObject.getContextId())) {
                try {
                    Node node = NodeDao.getNode(RepositoryDao.getHomeRepository(), ltiSessionObject.getContextId()).asNode();
                    if (node.getAspects().contains("ccm:ltitool_node")) {
                        String toolUrl = node.getProperties().get("ccm:ltitool_url")[0];
                        ApplicationInfo applicationInfo = ApplicationInfoList
                                .getApplicationInfos().values().stream()
                                .filter(a -> toolUrl.equals(a.getLtitoolUrl()))
                                .findFirst().get();
                        if (applicationInfo.hasLtiToolCustomContentOption()) {
                            ltiSession.customContentNode = node;
                        }
                    }
                } catch (DAOException e) {
                    throw new RuntimeException(e);
                }
            }

            this.ltiSession = ltiSession;
        }
    }
}
