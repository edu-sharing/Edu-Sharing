package org.edu_sharing.repository.server.tools.security;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import jakarta.servlet.http.HttpSession;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.cache.ShibbolethSessionsCache;
import org.edu_sharing.repository.server.tools.security.ShibbolethSessions.SessionInfo;
import org.springframework.context.ApplicationContext;

public class ShibbolethSessions {
	
	static Logger logger = Logger.getLogger(ShibbolethSessions.class);
	
	public static class SessionInfo{
		
		
		
		
		String alfrescoTicket;
		
		HttpSession tomcatSession;
		
		public SessionInfo(String alfrescoTicket,HttpSession tomcatSession) {
			this.alfrescoTicket = alfrescoTicket;
			this.tomcatSession = tomcatSession;
		}

		public String getAlfrescoTicket() {
			return alfrescoTicket;
		}

		public void setAlfrescoTicket(String alfrescoTicket) {
			this.alfrescoTicket = alfrescoTicket;
		}

		public jakarta.servlet.http.HttpSession getTomcatSession() {
			return tomcatSession;
		}

		public void setTomcatSession(jakarta.servlet.http.HttpSession tomcatSession) {
			this.tomcatSession = tomcatSession;
		}
		
	}
	
	private static Map<String,SessionInfo> shibSessionAlfTicketMap = null;
	
	
	
	
	private static Map<String,SessionInfo> getInstance(){
		ReentrantReadWriteLock rw = new ReentrantReadWriteLock(); //Collections.synchronizedMap(new HashMap<K, V>);
		
		
		WriteLock writeLock = rw.writeLock();
		
		
		if(shibSessionAlfTicketMap == null){
			try{
				writeLock.lock();
				//check for null again to prevent the Map is created once mor by a waiter on write lock
				if(shibSessionAlfTicketMap == null){
					shibSessionAlfTicketMap = Collections.synchronizedMap(new HashMap<>());
				}
				
			}finally{
				writeLock.unlock();
			}
		}
		
		
		return shibSessionAlfTicketMap;
	}
	
	
	
	
	public static SessionInfo get(String shibbolethSessionId){
		return getInstance().get(shibbolethSessionId);
	}
	
	public static void put(String shibbolethSessionId, SessionInfo sessionInfo){
		 getInstance().put(shibbolethSessionId, sessionInfo);
		 ShibbolethSessionsCache.put(shibbolethSessionId, sessionInfo.getAlfrescoTicket());
	}
	
	
	/**
	 * this Method should only be called by ShibbolethSessionsCacheListener
	 * so that it keeps in sync with ShibbolethSessionsCache
	 */
	public static void removeInternal(String shibbolethSessionId){
		getInstance().remove(shibbolethSessionId);
	}
	
	public static void removeByAlfTicket(String alfTicket){
		String shibbolethSessionId = getShibbolethSession(alfTicket);
		if(shibbolethSessionId != null){
			logger.debug("will remove data for shibbolethSessionId:"+shibbolethSessionId);
			
			removeInternal(shibbolethSessionId);
			ShibbolethSessionsCache.remove(shibbolethSessionId);
								
		}else{
			logger.debug("could not find any shibboleth sessionid for alfresco ticket:"+alfTicket+" maybe another cluster instance is responsible");
		}
	}
	
	public static int size(){
		return getInstance().size();
	}
	
	public static String getShibbolethSession(String alfTicket){
		Map<String,SessionInfo> instance = getInstance();
		//synchronization cause of http://docs.oracle.com/javase/7/docs/api/java/util/Collections.html#synchronizedMap(java.util.Map) 
		synchronized (instance) {
			Iterator<String> keyIter = instance.keySet().iterator();
			while(keyIter.hasNext()){
				String key = keyIter.next();
				SessionInfo value = instance.get(key);
				if(alfTicket.equals(value.alfrescoTicket)){
					return key;
				}
			}
		}
		
		return null;		
	}
	
	
	public static void killTomcatSession(String shibSessionId){
		
		SessionInfo sessionInfo = ShibbolethSessions.get(shibSessionId);
		if(sessionInfo != null){
			try{
				String scope = (String)sessionInfo.getTomcatSession().getAttribute(CCConstants.AUTH_SCOPE);
				if(scope != null){
					logger.debug("killing session for scope:"+ scope);
				}
				
	    		sessionInfo.getTomcatSession().invalidate();
	    	}catch(Throwable e){
	    		logger.error(e.getMessage(),e);
	    	}
			
		}else{
			logger.info("could not find session info for shibboleth session id:"+ shibSessionId +" maybe another cluster node is responsible");
		}
		
		
	}
	
	
	
	public static boolean containsKey(String shibSessionId){
		return getInstance().containsKey(shibSessionId);
	}
	
}
