package org.edu_sharing.service.nodeservice;

import java.util.Map;

import org.edu_sharing.service.search.SearchServiceMemuchoImpl;

public class NodeServiceMemuchoImpl extends NodeServiceAdapter{




	public NodeServiceMemuchoImpl(String appId) {
		super(appId);
	}

	@Override
	public Map<String, Object> getProperties(String storeProtocol, String storeId, String nodeId) throws Throwable {

		SearchServiceMemuchoImpl searchservice = new SearchServiceMemuchoImpl(this.appId);
		return null;//searchservice.getProperties(nodeId);

	}

}
