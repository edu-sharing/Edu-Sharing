package org.edu_sharing.repository.server.tools.security;

import org.alfresco.repo.cache.SimpleCache;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.rpc.User;
import org.edu_sharing.restservices.ltiplatform.v13.model.LoginInitiationSessionObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import jakarta.servlet.http.HttpSession;

public class AllSessions {
	static Map<String,HttpSession> allSessions = null;

	static Map<String,HttpSession>  getInstance(){
		ReentrantReadWriteLock rw = new ReentrantReadWriteLock(); 
		WriteLock writeLock = rw.writeLock();
		
		if(allSessions == null){
			try{
				writeLock.lock();
				//check for null again to prevent the List is created once mor by a waiter on write lock
				if(allSessions == null){
					allSessions = Collections.synchronizedMap(new HashMap<>());
				}
			}finally{
				writeLock.unlock();
			}
		}		
		
		return allSessions;
		
	}
	
	
	
	public static HttpSession remove(String sessionId){
		return getInstance().remove(sessionId);
	}
	
	public static void put(String sessionId, HttpSession session){
		getInstance().put(sessionId,session);
	}
	
	public static int size(){
		return getInstance().size();
	}


	public static SimpleCache<String, LoginInitiationSessionObject> getUserLTISessions(){
		return (SimpleCache<String, LoginInitiationSessionObject>) AlfAppContextGate.getApplicationContext().getBean("eduSharingLtiSessionsCache");
	}
}
