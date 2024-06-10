package org.edu_sharing.service.nodeservice;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.search.SearchServiceBrockhausImpl;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class NodeServiceBrockhausImpl extends NodeServiceAdapterCached{

	private String repositoryId;
	private Logger logger= Logger.getLogger(NodeServiceBrockhausImpl.class);
	private String apiKey;

	public NodeServiceBrockhausImpl(String appId) {
		super(appId);
		ApplicationInfo appInfo = ApplicationInfoList.getRepositoryInfoById(appId);
		this.repositoryId = appInfo.getAppId();		
		this.apiKey = appInfo.getApiKey();
	}

	@Override
	public Map<String, Object> getProperties(String storeProtocol, String storeId, String nodeId) throws Throwable {
		Map<String, Object> props = super.getProperties(storeProtocol, storeId, nodeId);
		if (props == null) {
			props = new HashMap<>();
		}
		String url=SearchServiceBrockhausImpl.buildUrl(apiKey,nodeId);
		props.put(CCConstants.CONTENTURL,url);
		props.put(CCConstants.CCM_PROP_IO_WWWURL,url);

		return props;
	}
	@Override
	public Map<String, Object> getPropertiesDynamic(String storeProtocol, String storeId, String nodeId) throws Throwable {
		return getProperties(storeProtocol, storeId, nodeId);
	}

	@Override
	public InputStream getContent(String nodeId) throws Throwable{
		return null;
	}
}
