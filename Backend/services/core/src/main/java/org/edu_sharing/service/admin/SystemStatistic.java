package org.edu_sharing.service.admin;


import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.Buffer;
import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.edu_sharing.repository.client.rpc.cache.CacheCluster;

public class SystemStatistic {
	
	static Buffer bufferRepoState = new CircularFifoBuffer(1000);
	
	
	public static void addRepoState(CacheCluster cacheCluster) {
		bufferRepoState.add(cacheCluster);
	}
	
	public static List<CacheCluster> getAllRepoStates(){
		
		List<CacheCluster> cacheCluster = new ArrayList<>();
		
		Iterator iter = bufferRepoState.iterator();
		while(iter.hasNext()) {
			cacheCluster.add((CacheCluster)iter.next());
		}
		return cacheCluster;
	}
	
	
	public static List<CacheCluster> getRepoStates(Date since){
		
		List<CacheCluster> cacheCluster = new ArrayList<>();
		
		Iterator iter = bufferRepoState.iterator();
		while(iter.hasNext()) {
			CacheCluster cc = (CacheCluster)iter.next();
			if(cc.getTimeStamp().getTime() > since.getTime()) {
				cacheCluster.add(cc);
			}
		}
		
		return cacheCluster;
	}
	
	
	
	
	
	

}
