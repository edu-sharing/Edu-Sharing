package org.edu_sharing.service.usage;

import java.util.Map;

public interface UsageDAO {
	
	Map<String, Map<String, Object>> getUsages(String nodeId);

	Map<String, Object> getUsageOnNodeOrParents(String lmsId, String courseId, String objectNodeId, String resourceId) throws Exception;

	Map<String, Object> getUsage(String lmsId, String courseId, String objectNodeId, String resourceId) throws Exception;
	
	Map<String, Object> getUsage(String usageId) throws Exception;
	
	Map<String, Map<String, Object>> getUsagesByCourse(String lmsId, String courseId) throws Exception;
	
	Map<String, Map<String, Object>> getUsagesByAppId(String appId) throws Exception;
	
	Map<String, Map<String, Object>> getUsages(String repositoryId,
                                               String nodeId,
                                               Long from,
                                               Long to) throws Exception;
	
	String createUsage(String parentId, Map<String, Object> properties);
	
	void updateUsage(String usageNodeId, Map<String, Object> properties);
	
	
	void removeUsage(String appId, String courseId, String parentNodeId, String resourceId) throws Exception;
	
	boolean removeUsages(String appId, String courseId) throws Exception;
	
}
