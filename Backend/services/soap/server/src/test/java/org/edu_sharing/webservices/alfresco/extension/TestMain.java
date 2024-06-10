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
package org.edu_sharing.webservices.alfresco.extension;

import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.validator.routines.InetAddressValidator;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.webservices.authentication.Authentication;
import org.edu_sharing.webservices.authentication.AuthenticationResult;
import org.edu_sharing.webservices.util.AuthenticationDetails;
import org.edu_sharing.webservices.util.AuthenticationUtils;
import org.edu_sharing.webservices.util.EduWebServiceFactory;




public class TestMain {
	
public static void main(String[] args){
		
		try{

		Authentication auth = EduWebServiceFactory.getAuthenticationServiceByEndpointAddress("http://localhost/edu-sharing/services/authentication");
		
		AuthenticationResult authResult =  auth.authenticate("admin", "admin");
		
		AuthenticationUtils.setAuthenticationDetails(new AuthenticationDetails(authResult.getUsername(), authResult.getTicket(), authResult.getSessionid()));
		
		final NativeAlfrescoWrapper  naw = new NativeAlfrescoWrapperServiceLocator(AuthenticationUtils.getEngineConfiguration()).getNativeAlfrescoWrapper(new URL("https://localhost/edu-sharing/services/NativeAlfrescoWrapper?wsdl"));
		
		int counterDatabase = 0;
		int counterSolr = 0;
		Map< naw.getChildren("8fbbf231-8bfe-4495-b374-6fe5463a1171", CCConstants.CCM_TYPE_MAP);
		for(Map.Entry<String,Object> entry : nrFolders.entrySet()) {
			Map<String,Object> nrFolder = (Map<String,Object>)entry.getValue();
			System.out.println("nrfolder:" + nrFolder.get(CCConstants.CM_NAME));
			Map< naw.getChildren(entry.getKey(), CCConstants.CCM_TYPE_IO);
			for(Map.Entry<String, Object> ioEntry : ios.entrySet()) {
				counterDatabase++;
				Map<String,Object> ioProps = (Map<String,Object>)ioEntry.getValue();
				String replicationSourceId = (String)ioProps.get(CCConstants.CCM_PROP_IO_REPLICATIONSOURCEID);
				if(replicationSourceId == null) {
					System.out.println("No Replication Source Id for:" + ioEntry.getKey());
				}else {
					SearchResult sr = naw.searchSolr("@ccm\\:replicationsourceid:" + replicationSourceId.trim(), 0, 1, new String[] {CCConstants.LOM_PROP_GENERAL_KEYWORD}, 1, 1);
					if(sr.getData() != null && sr.getData().length > 0) {
						if(!ioEntry.getKey().equals(sr.getData()[0].getNodeId())){
							System.out.println("search delivered wrong nodeid for " + ioEntry.getKey() + " " + sr.getData()[0].getNodeId() + " replicationsourceid:" + replicationSourceId);
						}else {
							counterSolr++;
						}
					}else {
						System.out.println("Node not found in solr: "+ ioEntry.getKey() + " replicationSourceId:" + replicationSourceId);
					}
				}
			}
			if(counterDatabase % 100 == 0) {
				System.out.println("counterDatabase:" + counterDatabase+ " counterSolr" + counterSolr);
			}
		}
		

		if(true) {
			return;
		}
		
		testThread();
		
		
		naw.deleteUser(new String[] {"testuser"});
		if(true) return;
		
		String root = naw.getRepositoryRoot();
		Map<String,Map<String,Object>> children = (Map<String,Map<String,Object>>) naw.getChildren(root,null);
		for(Map.Entry<String, Map<String, Object>>  entry : children.entrySet()) {
			
			String name = (String)entry.getValue().get(CCConstants.CM_NAME);
			if(name.equals("Company Home")) {
				Map<String,Map<String,Object>> chchildren = (Map<String,Map<String,Object>>) naw.getChildren(entry.getKey(),null);
				for(Map.Entry<String, Map<String, Object>>  chentry : chchildren.entrySet()) {
					String chName = (String)chentry.getValue().get(CCConstants.CM_NAME);
					if(chName.equals("User Homes")) {
						Map<String,Map<String,Object>> uhchildren = (Map<String,Map<String,Object>>) naw.getChildren(chentry.getKey(),null);
						System.out.println(uhchildren.size());
						
						int i = 0;
						for(Map.Entry<String, Map<String, Object>>  uhentry : uhchildren.entrySet()) {
							String uhName = (String)uhentry.getValue().get(CCConstants.CM_NAME);
							System.out.println(uhName);
							if(InetAddressValidator.getInstance().isValid(uhName)){
								System.out.println("deleting " + uhName + " " + uhentry.getKey() +" count:" +i  + " from: "+uhentry.getKey());
								naw.removeNode(uhentry.getKey(), null);
								
								i++;
								if(i == 15000) {
									break;
								}
								
							}
							
						}
						
						
					}
					
				}
			}
			
		}
		
		if(true) return;
				
		
		Map<String,Object> test = naw.getProperties("defedcc4-fcf6-4182-b575-f137739c22aa");
		
		for(Map.Entry<String,Object> entry : test.entrySet()){
			System.out.println("prop:"+entry.getKey()+" value:"+entry.getValue()); 
		}
		
		
		Map<String,Boolean> hasPermissionsResult = naw.hasPermissions("admin", new String[]{"Read"}, "ef84a120-2e3f-4dcb-9b43-67f7505bc576");
		
		System.out.println("HAS PERMISSIONSRESULT");
		
		for(Map.Entry<String,Boolean> entry : hasPermissionsResult.entrySet()){
			System.out.println("Permission:"+entry.getKey()+" value:"+entry.getValue()); 
		}
		
		
		HashMap result = naw.getProperties("ef84a120-2e3f-4dcb-9b43-67f7505bc576");
		for(Object key :result.keySet()){
			System.out.println("key:"+key+" val:"+result.get(key));
		}
		
		System.out.println("GET CHILDREN!!!!");
		result = naw.getChildren("ef84a120-2e3f-4dcb-9b43-67f7505bc576", null);
		for(Object key :result.keySet()){
			System.out.println("key:"+key+" val:"+result.get(key));
		}
		
		
		long currentTimeMillis = System.currentTimeMillis();
		System.out.println("START GETTING NODIDS");
		String[] nodeIds = naw.searchNodeIds("workspace://SpacesStore", "@cclom\\:title:*", "Read");
		
		long timeMillisDiff = System.currentTimeMillis() - currentTimeMillis;
		System.out.println("END GETTING GETTING NODIDS in " + (timeMillisDiff / 1000) +" sec for node results:"+nodeIds.length);
		
		currentTimeMillis = System.currentTimeMillis();
		System.out.println("START GETTING PERMISSIONS" + new Date(currentTimeMillis));
		
		
		
		RepositoryNode[] srs = naw.searchNodes("workspace://SpacesStore", "@cclom\\:title:*", "Write",new String[]{CCConstants.LOM_PROP_GENERAL_TITLE});
		System.out.println("size search result: "+srs.length);
		for(RepositoryNode sr : srs){
			System.out.println("READ RECHT auf:"+sr.getNodeId());
			if(sr.getProperties() != null){
				for(KeyValue keyVal : sr.getProperties()){
					System.out.println("  prop:"+keyVal.getKey()+" val:"+keyVal.getValue());
				}
			}
		}
		
		timeMillisDiff = System.currentTimeMillis() - currentTimeMillis;
		System.out.println("END GETTING PERMISSIONS in " + (timeMillisDiff / 1000) +" sec for node results:"+srs.length);
		}catch(Exception e){
			e.printStackTrace();
		}
		
		
		
	}

	public static void testThread(){
		int maxThreads = 5;
		
		List<Runnable> list = new ArrayList<>();
		for(int i = 0; i < maxThreads; i++) {
			
			final int fi = i;
			
			Runnable r = new Runnable(){
				@Override
				public void run() {
					try {
						Authentication tauth = EduWebServiceFactory.getAuthenticationServiceByEndpointAddress("http://localhost:8080/edu-sharing/services/authentication");
						AuthenticationResult authResult =  tauth.authenticate("admin", "admin");
						
						AuthenticationUtils.setAuthenticationDetails(new AuthenticationDetails(authResult.getUsername(), authResult.getTicket(), authResult.getSessionid()));
						
						final NativeAlfrescoWrapper  naw = new NativeAlfrescoWrapperServiceLocator(AuthenticationUtils.getEngineConfiguration()).getNativeAlfrescoWrapper(new URL("http://localhost:8080/edu-sharing/services/NativeAlfrescoWrapper?wsdl"));
					
						ArrayList<UserDetails> l = new ArrayList<>();
						
						for(int i = 0; i < 10; i++) {
							UserDetails ud = new UserDetails();
							ud.setEmail("dd"+i+"@dd.de");
							ud.setFirstName("ddd" +i);
							ud.setLastName("ln"+i);
							ud.setUserName("dtest");
							ud.setPassword("test"+i);
							l.add(ud);
							
						}
						
						System.out.println("thread nr: " + fi);
						
				
						naw.setUserDetails(l.toArray(new UserDetails[0]));
					}catch(Exception e) {
						e.printStackTrace();
					}
				}
			};
			list.add(r);
		}
		
		for(Runnable r : list) {
			new Thread(r).start();
		}
	}
}
