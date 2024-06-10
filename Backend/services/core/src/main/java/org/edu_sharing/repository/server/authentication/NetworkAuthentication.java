package org.edu_sharing.repository.server.authentication;

import java.util.HashMap;
import java.util.Map;


/**
 * this is used to store federated AuthInfo in Session
 * 
 * @author rudi
 *
 */
public class NetworkAuthentication {
	
	Map<String,Authentication> authStore = new HashMap<>();
	
	
	public class Authentication{
		String user;
		String ticket;
		
		public Authentication(String user, String ticket) {
			this.user = user;
			this.ticket = ticket;
		}
		
		public String getTicket() {
			return ticket;
		}
		
		public String getUser() {
			return user;
		}
	}
	
	public void put(String appId, String user, String ticket){
		authStore.put(appId, new Authentication(user,ticket));
	}
	
	public void remove(String appId){
		authStore.remove(appId);
	}
	
	public String getTicket(String appId){
		Authentication auth = authStore.get(appId);
		if(auth != null) return auth.getTicket();
		return null;
	}
	
	public String getUser(String appId){
		Authentication auth = authStore.get(appId);
		if(auth != null) return auth.getUser();
		return null;
	}
	
	public Map<String, Authentication> getAuthStore() {
		return authStore;
	}
}
