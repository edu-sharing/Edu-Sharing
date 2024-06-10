package org.edu_sharing.service.nodeservice;

import java.util.Map;

import org.edu_sharing.service.search.SearchServiceOCartImpl;

public class NodeServiceOCAImpl extends NodeServiceAdapter{



	
	public NodeServiceOCAImpl(String appId) {
		super(appId);
	}

	@Override
	public Map<String, Object> getProperties(String storeProtocol, String storeId, String nodeId) throws Throwable {

		SearchServiceOCartImpl searchservice = new SearchServiceOCartImpl (this.appId);
		return searchservice.getProperties(nodeId);

   }
	
}
