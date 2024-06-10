package org.edu_sharing.repository.server.jobs.quartz;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.LogFactory;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationTool;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.NodeTool;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class NodeDistributionJob extends AbstractJob {

	private static final String JOB_PATH = "path";
	private static final String JOB_PATTERN = "pattern";
	private static final String SEPARATOR = "/";
	
	public NodeDistributionJob() {
		this.logger = LogFactory.getLog(NodeDistributionJob.class);
	}
	
	@Override
	public Class[] getJobClasses() {

		Class[] result = Arrays.copyOf(allJobs, allJobs.length + 1);
	    result[result.length - 1] = NodeDistributionJob.class;
		return result;
	}

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {

		try {

			// init
			
			ApplicationInfo homeRep = ApplicationInfoList.getHomeRepository();
			AuthenticationTool authTool = RepoFactory.getAuthenticationToolInstance(homeRep.getAppId());
			Map<String, String> authInfo = authTool.createNewSession(homeRep.getUsername(), homeRep.getPassword());
			MCAlfrescoBaseClient client = (MCAlfrescoBaseClient) RepoFactory.getInstance(homeRep.getAppId(), authInfo);

			JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
				
			String[] patterns = jobDataMap.getString(JOB_PATTERN).split(SEPARATOR); 
			
			DateFormat[] formatter = new DateFormat[patterns.length];
			for (int i = 0, c = patterns.length; i<c; ++i) {
			
				formatter[i] = new SimpleDateFormat(patterns[i]);
			}
			
			// request node
				
			Map<String, Map<String, Object>> search =
					client.search(
							"PATH:\"" + jobDataMap.getString(JOB_PATH) + "\"", 
							CCConstants.CM_TYPE_FOLDER);
			
			if (search.size() != 1) {
				throw new IllegalArgumentException("The path must reference a unique node.");
			}

			String rootId = search.keySet().iterator().next();
			
			// request io's
			
			Map<String, Map<String, Object>> children =
					client.getChildrenByType(rootId, CCConstants.CCM_TYPE_IO);
			
			Calendar calendar = Calendar.getInstance();
			Map<String, String> cache = new HashMap<>();
			
			for (Entry<String, Map<String, Object>> child : children.entrySet()) {

				String childId = child.getKey();
				Map<String, Object> childProps = child.getValue();
				
				Date created = new Date(Long.parseLong((String) childProps.get(CCConstants.CM_PROP_C_CREATED)));
			
				String[] items = new String[formatter.length];
				StringBuilder path = new StringBuilder();
				
				for (int i = 0, c = formatter.length; i < c; ++i) {
					items[i] = formatter[i].format(created);
					
					if (i > 0) {
						path.append(SEPARATOR);
					}
					path.append(items[i]);
				}
				
				String key = path.toString();
				
				String nodeId = cache.get(key);
				
				if (nodeId == null) {
					
					cache.put(key, nodeId = NodeTool.createOrGetNodeByName(rootId, items));
				}
				
				String childName = childProps.get(CCConstants.CM_NAME).toString();
								
				if (client.getChild(nodeId, CCConstants.CCM_TYPE_IO, CCConstants.CM_NAME, childName) == null) {
					
					client.moveNode(nodeId, CCConstants.CM_ASSOC_FOLDER_CONTAINS, childId);
					
				} else {
					
					logger.warn("Node (" + childId +") can't move to (" + nodeId + ") due to name collision");

				}					
			}
			
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
			e.printStackTrace();
		}

	}
}
