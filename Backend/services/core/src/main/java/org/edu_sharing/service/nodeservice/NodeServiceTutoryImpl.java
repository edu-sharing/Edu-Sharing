package org.edu_sharing.service.nodeservice;

import java.util.HashMap;
import java.util.Map;

import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.search.SearchServiceTutoryImpl;
import org.json.JSONObject;

public class NodeServiceTutoryImpl extends NodeServiceAdapter {
	
	
	
	public NodeServiceTutoryImpl(String appId) {
		super(appId);
	}

	@Override
	public Map<String, Object> getProperties(String storeProtocol, String storeId, String nodeId) throws Throwable {
		
		String httpResult = SearchServiceTutoryImpl.getHttpResult("https://www.tutory.de/api/v1/worksheet/" + nodeId);
		
		JSONObject worksheet = new JSONObject(httpResult);
		Map<String, Object> properties = SearchServiceTutoryImpl.getProperties(worksheet);
		properties.put(CCConstants.REPOSITORY_ID, this.appId);
		
		return properties;
	}
	
	
	
}
