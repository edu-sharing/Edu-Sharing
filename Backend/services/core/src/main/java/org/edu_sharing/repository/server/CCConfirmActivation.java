/**
 *
 *  
 * 
 * 
 *	
 *
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 *
 */
package org.edu_sharing.repository.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;



public class CCConfirmActivation extends HttpServlet {

	static Logger log = Logger.getLogger(CCConfirmActivation.class);

	String repositoryFehler = "Repository Fehler. Bitte kontaktieren Sie den Administrator!";

	String accessDenied = "Der Zugang wurde verweigert. Der Zugang konnte nicht freigeschaltet werden.";

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse resp) throws ServletException, IOException {

		PrintWriter out = resp.getWriter();
		String appId = request.getParameter("appId");
		String appUserId = request.getParameter("appUserId");
		String mail = request.getParameter("mail");
		String key = request.getParameter("key");

		log.info("appId:" + appId + " appUserid" + appUserId + " mail:" + mail + " key:" + key);

		Map<String, ApplicationInfo> appInfos = ApplicationInfoList.getApplicationInfos();
		ApplicationInfo homeRepository = ApplicationInfoList.getHomeRepository();

		if (homeRepository == null) {
			out.print(repositoryFehler);
			log.error("Home Repository File not Found!!!");
			return;
		}

		ApplicationInfo remoteAppInfo = appInfos.get(appId);
		if (remoteAppInfo == null || remoteAppInfo.getTrustedclient() == null || !remoteAppInfo.getTrustedclient().equals("true")) {
			out.print(accessDenied);
			log.info("access denied!!!");
			return;
		}

		if (mail != null) {
			mail = mail.trim().toLowerCase();
		} else {
			out.print(accessDenied);
			log.info("access denied cause mail:" + mail);
			return;
		}

		String adminUn = homeRepository.getUsername();
		String password = homeRepository.getPassword();
		
		
		//determine the repository username in dependence of auth_by_app_username_prop in Application file 
		String repositoryUsername = appUserId;
		if(remoteAppInfo.getAuthByAppUsernameProp() != null && remoteAppInfo.getAuthByAppUsernameProp().equals(ApplicationInfo.AUTHBYAPP_USERNAME_PROP_MAIL)){
			repositoryUsername = mail;
		}
		
		
	

		try {

			AuthenticationTool authTool = RepoFactory.getAuthenticationToolInstance(homeRepository.getAppId());
			Map<String, String> authInfo = authTool.createNewSession(adminUn, password);
			MCAlfrescoBaseClient mcAlfrescoBaseClient = (MCAlfrescoBaseClient) RepoFactory.getInstance(null, authInfo);

			
			Map<String, Map<String, Object>> userResult = mcAlfrescoBaseClient.search("@cm\\:userName:" + repositoryUsername);
			
			if(userResult.size() > 0){
				if(userResult.size() > 1){
					out.print(repositoryFehler);
					log.error("UserId:" + repositoryUsername + " is used by more than one users. This is not allowed!!!");
					return;
				}else{
					 Map<String, Object> userProps = userResult.get(userResult.keySet().iterator().next());
					 String nodeId = (String)userProps.get(CCConstants.SYS_PROP_NODE_UID);
					 Map<String, Map<String, Object>> childMap = mcAlfrescoBaseClient.getChildrenByType(nodeId,
								CCConstants.CM_TYPE_PERSONACCESSELEMENT);

						for (Map.Entry<String, Map<String, Object>> entry : childMap.entrySet()) {
							Map<String, Object> childProps = entry.getValue();
							String tmpAppId = (String) childProps.get(CCConstants.CM_PROP_PERSONACCESSELEMENT_CCAPPID);
							String tmpappUserId = (String) childProps.get(CCConstants.CM_PROP_PERSONACCESSELEMENT_CCUSERID);
							String tmpActivateKey = (String) childProps.get(CCConstants.CM_PROP_PERSONACCESSELEMENT_CCACTIVATEKEY);

							log.info("activation request for user" + repositoryUsername + "found:" + " appId:" + tmpAppId + " appUserId:" + tmpappUserId
									+ " key:" + tmpActivateKey);

							if (tmpAppId.equals(appId) && tmpappUserId.equals(appUserId) && tmpActivateKey.equals(key)) {

								// create a new PropsToSafe Map. dont take the
								// one retrieved by getChildrenByType
								// cause there are properties that are not part
								// of the type definition
								Map<String, Object> propsToSafe = new HashMap<>();
								propsToSafe.put(CCConstants.CM_PROP_PERSONACCESSELEMENT_CCAPPID, tmpAppId);
								propsToSafe.put(CCConstants.CM_PROP_PERSONACCESSELEMENT_CCUSERID, tmpappUserId);
								propsToSafe.put(CCConstants.CM_PROP_PERSONACCESSELEMENT_CCACTIVATEKEY, tmpActivateKey);
								propsToSafe.put(CCConstants.CM_PROP_PERSONACCESSELEMENT_CCACCESS, new Boolean(true).toString());

								mcAlfrescoBaseClient.updateNode(entry.getKey(), propsToSafe);
								log.info("access for user:" + repositoryUsername + " appId:" + appId + " appUSerId:" + appUserId + " was activated!");
								out.print("Der Zugang für User " + appUserId + " und Applikation: " + remoteAppInfo.getAppCaption()
										+ " auf das Repository: " + homeRepository.getAppCaption() + " wurde freigeschaltet!");
								return;
							}
						}
				}
			}else{
				log.error("no user found with repository username: "+repositoryUsername);
			}
			
		} catch (Throwable e) {
			e.printStackTrace();
			out.print(e.getMessage());
		}

		log.info("access for user:" + mail + " appId:" + appId + " appUSerId:" + appUserId + " was denied!");
		out.print(this.accessDenied);
	}
}
