package org.edu_sharing.webservices.alfresco.extension;

import java.net.URL;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.webservices.authentication.Authentication;
import org.edu_sharing.webservices.authentication.AuthenticationResult;
import org.edu_sharing.webservices.util.AuthenticationDetails;
import org.edu_sharing.webservices.util.AuthenticationUtils;
import org.edu_sharing.webservices.util.EduWebServiceFactory;

public class InstanceUsage {

	NativeAlfrescoWrapper  naw;
	
	
	long size = 0;
	
	//to customize
	String nodeId = "f5ea9f81-01ee-4beb-bc3b-061a5cc72b6f";
	String baseUrl = "http://localhost:8084/edu-sharing";
	
	String user = "admin";
	
	String pw = "edualf2012";
	
	public static void main(String[] args) {
		
		try {
			
		new InstanceUsage().start();
		
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void start() throws Exception{
		
		Authentication auth = EduWebServiceFactory.getAuthenticationServiceByEndpointAddress(baseUrl + "/services/authentication");
		AuthenticationResult authResult =  auth.authenticate(user,pw);
		AuthenticationUtils.setAuthenticationDetails(new AuthenticationDetails(authResult.getUsername(), authResult.getTicket(), authResult.getSessionid()));
		naw = new NativeAlfrescoWrapperServiceLocator(AuthenticationUtils.getEngineConfiguration()).getNativeAlfrescoWrapper(new URL(baseUrl + "/services/NativeAlfrescoWrapper?wsdl"));
		
		count(nodeId);
		
		System.out.println("size is:" + size + " MB:" + ((size /1000)/1000) );
	}
	
	private void count(String parentId) throws RemoteException {
		System.out.println("Instance:" + naw.getProperties(parentId).get(CCConstants.CM_NAME));
		Map<String,Object> nodes = naw.getChildren(parentId, null);
		for(Map.Entry<String, Object> entry : nodes.entrySet()) {
			Map<String, Object> nodeProps = (HashMap<>)entry.getValue();
			
			String type = (String)nodeProps.get(CCConstants.NODETYPE);
			System.out.println("nodeType:" + type + " " + nodeProps.get(CCConstants.CM_NAME) + " size:"+size + " MB:" + ((size /1000)/1000) +" Date:" + new Date());
			if(CCConstants.CCM_TYPE_IO.equals(type)){
				String sizeStr = (String)nodeProps.get(CCConstants.LOM_PROP_TECHNICAL_SIZE);
				try {
					 size += Long.parseLong(sizeStr);
				}catch(NumberFormatException e) {
					
				}
			}else if(CCConstants.CCM_TYPE_MAP.equals(type) 
					|| CCConstants.CM_TYPE_FOLDER.equals(type)) {
				count(entry.getKey());
			}else {
				System.out.println(entry.getKey() + " is not a folder or io");
			}
			
		}
		
	}
	
}
