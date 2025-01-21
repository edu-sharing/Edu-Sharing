package org.edu_sharing.restservices.login.v1.model;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.edu_sharing.repository.server.authentication.LoginHelper;
import org.edu_sharing.repository.server.authentication.RemoteAuthDescription;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.restservices.DAOException;
import org.edu_sharing.restservices.NodeDao;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.restservices.ltiplatform.v13.LTIPlatformConstants;
import org.edu_sharing.restservices.ltiplatform.v13.model.LoginInitiationSessionObject;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.lti13.LTIConstants;
import org.edu_sharing.service.lti13.model.LTISessionObject;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;

import com.fasterxml.jackson.annotation.JsonProperty;


@Schema(description = "")
public class Login  {

  private Map<String, RemoteAuthDescription> remoteAuthentications;
  private boolean isValidLogin;
  private String currentScope;
  private String userHome;
  private int sessionTimeout;
  private boolean isGuest;
  private List<String> toolPermissions;
  private boolean isAdmin;
  private String statusCode;
  private String authorityName;
  private LTISession ltiSession;
  
  
  public final static String STATUS_CODE_OK ="OK";
  public final static String STATUS_CODE_GUEST ="GUEST";
  public final static String STATUS_CODE_INVALID_CREDENTIALS ="INVALID_CREDENTIALS";
  public final static String STATUS_CODE_PREVIOUS_SESSION_REQUIRED = "PREVIOUS_SESSION_REQUIRED";
  public final static String STATUS_CODE_PREVIOUS_USER_WRONG = "PREVIOUS_USER_WRONG";
  public final static String STATUS_CODE_INVALID_SCOPE = "INVALID_SCOPE";
  public final static String STATUS_CODE_PASSWORD_EXPIRED = "PASSWORD_EXPIRED";
  public final static String STATUS_CODE_PERSON_BLOCKED = "PERSON_BLOCKED";

  public class LTISession{
      @JsonProperty("acceptMultiple")
      boolean acceptMultiple;

      @JsonProperty("deeplinkReturnUrl")
      String deeplinkReturnUrl;

      @JsonProperty("acceptTypes")
      List<String> acceptTypes = new ArrayList<>();

      @JsonProperty("acceptPresentationDocumentTargets")
      List<String> acceptPresentationDocumentTargets = new ArrayList<>();

      @JsonProperty("canConfirm")
      boolean canConfirm;

      @JsonProperty("title")
      String title;

      @JsonProperty("text")
      String text;

      /**
       * custom property:
       * when the context of ltideeplink message is an edu-sharing nodeId.
       * we resolve the node to find out if it was created for a ltitool with ltitool_customcontent_option
       * and prevent a platform representing the same app as the tool: embedding it's own nodes
       */
      @JsonProperty("customContentNode")
      Node customContentNode;
  }


  public Login(){

  }


public Login(boolean isValidLogin, String scope, HttpSession session) {
	  this(isValidLogin,scope,null,session, (isValidLogin) ? STATUS_CODE_OK : STATUS_CODE_INVALID_CREDENTIALS );
  }
  
  public Login(boolean isValidLogin, String scope, String userHome, HttpSession session, String statusCode) {
	
	org.edu_sharing.service.authority.AuthorityService service=AuthorityServiceFactory.getAuthorityService(ApplicationInfoList.getHomeRepository().getAppId());
	
	if(isValidLogin){
		try{
			this.toolPermissions=ToolPermissionServiceFactory.getInstance().getAllAvailableToolPermissions();
		}catch(Throwable t){
			// not logged in
		}
	}
	this.statusCode = service.isGuest() ? STATUS_CODE_GUEST : statusCode;
	this.authorityName=service.isGuest() ? null : AuthenticationUtil.getFullyAuthenticatedUser();
	this.isAdmin=service.isGlobalAdmin();
	this.isValidLogin = isValidLogin;
	this.currentScope = scope;
	if(isValidLogin && scope==null && !service.isGuest())
	    this.remoteAuthentications = LoginHelper.getRemoteAuthsForSession();
	this.userHome = userHome;
	this.isGuest = service.isGuest();
	this.sessionTimeout = session.getMaxInactiveInterval();

      LTISessionObject ltiSessionObject = (LTISessionObject)session.getAttribute(LTISessionObject.class.getName());
      if(ltiSessionObject != null){
          LTISession ltiSession = new LTISession();
          if(ltiSessionObject.getDeepLinkingSettings() != null) {
              if(ltiSessionObject.getDeepLinkingSettings().containsKey(LTIConstants.DEEP_LINK_ACCEPT_MULTIPLE)) {
                  ltiSession.acceptMultiple = (Boolean) ltiSessionObject.getDeepLinkingSettings().get(LTIConstants.DEEP_LINK_ACCEPT_MULTIPLE);
              }
              if(ltiSessionObject.getDeepLinkingSettings().containsKey(LTIConstants.DEEP_LINK_DOCUMENT_TARGETS)){
                  ltiSession.acceptPresentationDocumentTargets = (List<String>)ltiSessionObject.getDeepLinkingSettings().get(LTIConstants.DEEP_LINK_DOCUMENT_TARGETS);
              }
              if(ltiSessionObject.getDeepLinkingSettings().containsKey(LTIConstants.DEEP_LINK_ACCEPT_TYPES)){
                  ltiSession.acceptTypes = (List<String>)ltiSessionObject.getDeepLinkingSettings().get(LTIConstants.DEEP_LINK_ACCEPT_TYPES);
              }
              if(ltiSessionObject.getDeepLinkingSettings().containsKey(LTIConstants.DEEP_LINK_CAN_CONFIRM)) {
                  ltiSession.canConfirm = (Boolean) ltiSessionObject.getDeepLinkingSettings().get(LTIConstants.DEEP_LINK_CAN_CONFIRM);
              }
              if(ltiSessionObject.getDeepLinkingSettings().containsKey(LTIConstants.DEEP_LINK_RETURN_URL)) {
                  ltiSession.deeplinkReturnUrl = (String) ltiSessionObject.getDeepLinkingSettings().get(LTIConstants.DEEP_LINK_RETURN_URL);
              }
              if(ltiSessionObject.getDeepLinkingSettings().containsKey(LTIConstants.DEEP_LINK_TEXT)) {
                  ltiSession.text = (String) ltiSessionObject.getDeepLinkingSettings().get(LTIConstants.DEEP_LINK_TEXT);
              }
              if(ltiSessionObject.getDeepLinkingSettings().containsKey(LTIConstants.DEEP_LINK_TITLE)) {
                  ltiSession.title = (String) ltiSessionObject.getDeepLinkingSettings().get(LTIConstants.DEEP_LINK_TITLE);
              }
          }

          if(ltiSessionObject.getContextId() != null
                  && NodeServiceFactory.getLocalService().exists("workspace","SpacesStore",ltiSessionObject.getContextId())){
              try {
                  Node node = NodeDao.getNode(RepositoryDao.getHomeRepository(), ltiSessionObject.getContextId()).asNode();
                  if(node.getAspects().contains("ccm:ltitool_node")){
                      String toolUrl = node.getProperties().get("ccm:ltitool_url")[0];
                      ApplicationInfo applicationInfo = ApplicationInfoList
                              .getApplicationInfos().values().stream()
                              .filter(a -> toolUrl.equals(a.getLtitoolUrl()))
                              .findFirst().get();
                      if(applicationInfo != null && applicationInfo.hasLtiToolCustomContentOption()) {
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
  	@JsonProperty("authorityName")
	public String getAuthorityName() {
		return authorityName;
	}
	
	public void setAuthorityName(String authorityName) {
		this.authorityName = authorityName;
	}

@Schema(required = true, description = "")
  @JsonProperty("isGuest")
  public boolean isGuest() {
		return isGuest;
	}

	public void setGuest(boolean isGuest) {
		this.isGuest = isGuest;
	}
  @Schema(required = true, description = "")
  @JsonProperty("sessionTimeout")
  public int getSessionTimeout() {
	return sessionTimeout;
}

public void setSessionTimeout(int sessionTimeout) {
	this.sessionTimeout = sessionTimeout;
}
/**
   **/
  @JsonProperty("currentScope")
  public String getCurrentScope() {
    return currentScope;
  }
  public void setCurrentScope(String currentScope) {
    this.currentScope = currentScope;
  }
  @Schema(required = true, description = "")
  @JsonProperty("isValidLogin")
  public boolean isValidLogin() {
    return isValidLogin;
  }
  public void setIsValidLogin(boolean isValidLogin) {
    this.isValidLogin = isValidLogin;
  }
  @Schema(required = true, description = "")
  @JsonProperty("isAdmin")
  public boolean isAdmin() {
    return isAdmin;
  }
  public void setIsAdmin(boolean isAdmin) {
    this.isAdmin = isAdmin;
  }
  @Schema(required = false, description = "")
  @JsonProperty("userHome")
  public String getUserHome() {
	return userHome;
}
  @JsonProperty("toolPermissions")
  public List<String> getToolPermissions() {
	return toolPermissions;
  }
  @JsonProperty
  public Map<String, RemoteAuthDescription> getRemoteAuthentications() {
      return remoteAuthentications;
  }

  @JsonProperty("statusCode")
  public String getStatusCode() {
	return statusCode;
  }

    @JsonProperty
    public LTISession getLtiSession() {
        return ltiSession;
    }

    public void setToolPermissions(List<String> toolPermissions) {
	this.toolPermissions = toolPermissions;
}

@Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class Login {\n");
    
    sb.append("  isValidLogin: ").append(isValidLogin).append("\n");
    sb.append("  isAdmin: ").append(isAdmin).append("\n");
    sb.append("  currentScope: ").append(currentScope).append("\n");
    sb.append("  userHome: ").append(userHome).append("\n");
    sb.append("  statusCode: ").append(statusCode).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
