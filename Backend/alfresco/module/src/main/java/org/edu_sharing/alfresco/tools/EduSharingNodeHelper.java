package org.edu_sharing.alfresco.tools;

import com.google.common.base.CharMatcher;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class EduSharingNodeHelper {
    public static ServiceRegistry serviceRegistry = (ServiceRegistry)AlfAppContextGate.getApplicationContext().getBean(ServiceRegistry.SERVICE_REGISTRY);
	public static NodeService nodeService = serviceRegistry.getNodeService();
	public static NodeService nodeServiceAlfresco = (NodeService) AlfAppContextGate.getApplicationContext().getBean("alfrescoDefaultDbNodeService");


	private final static List<String> typesToIgnore = List.of(
			CCConstants.CCM_TYPE_SHARE,
			CCConstants.CCM_TYPE_USAGE,
			CCConstants.CCM_TYPE_COMMENT,
			CCConstants.CCM_TYPE_COLLECTION_FEEDBACK,
			CCConstants.CCM_TYPE_MATERIAL_FEEDBACK,
			CCConstants.CCM_TYPE_COLLECTION_PROPOSAL,
			CCConstants.CM_TYPE_THUMBNAIL);

	/**
	 * Helper function to filter nodes
	 *
	 * @param node   node to inspect
	 * @param filter a list of filtered properties on which the node should be filtered out
	 * @return true if the node should be filtered out
	 */
	public static boolean shouldFilter(NodeRef node, List<String> filter) {

    	try {
	        // filter nodes for link inivitation and usages
	        if (filter == null) {
				filter = new ArrayList<>();
			}else {
				filter = new ArrayList<>(filter);
			}

	        if (filter.contains("special")) {
	            // special mode, we do not filter anything
	            return false;
	        }

	        String type = nodeServiceAlfresco.getType(node).toString();
	        if (typesToIgnore.contains(type)) {
	            return true;
	        }
	        // filter the metadata template file
			boolean isDirectory = typeIsDirectory(type);
			if(isDirectory) {
				String mapType = (String) nodeServiceAlfresco.getProperty(node, QName.createQName(CCConstants.CCM_PROP_MAP_TYPE));
				if (CCConstants.CCM_VALUE_MAP_TYPE_FAVORITE.equals(mapType)) {
					return true;
				}

				if (CCConstants.CCM_VALUE_MAP_TYPE_EDUGROUP.equals(mapType)) {
					if (!filter.contains("edugroup")) {
						return true;
					}
				}
				//@TODO change this in 4.2
				if(CCConstants.CCM_VALUE_MAP_TYPE_USERSAVEDSEARCH.equals(mapType)) {
					if(!filter.contains("savedsearch")) {
						return true;
					}
				}
			} else {
				if (nodeServiceAlfresco.hasAspect(node, QName.createQName(CCConstants.CCM_ASSOC_METADATA_PRESETTING_TEMPLATE))) {
					return true;
				}
				String name = (String) nodeServiceAlfresco.getProperty(node, QName.createQName(CCConstants.CM_NAME));
				if ((".DS_Store".equals(name) || "._.DS_Store".equals(name))) {
					return true;
				}
			}
	        //prevent later code filters only cause of filter is set
			filter.remove("edugroup");

	        //prevent later code filters only cause of filter is set
			filter.remove("savedsearch");
	        

	        if(filter.isEmpty()) {
				return false;
			}

	        boolean shouldFilter = true;
	        for(String f : filter) {
	            if(f.equals("folders") && isDirectory){
	                shouldFilter=false;
	                break;
	            }
	            if(f.equals("files") && !isDirectory){
	                shouldFilter=false;
	                break;
	            }
	            if(f.startsWith("mime:")){
	                throw new IllegalArgumentException("Filtering by mime: is currently not supported");
	            }
	        }
	        return shouldFilter;
    	}catch(Throwable e) {
    		log.debug(e.getMessage(), e);
    		return false;
    	}
    }
    public static boolean typeIsDirectory(String type) {
        return type.equals(CCConstants.CM_TYPE_FOLDER) || type.equals(CCConstants.CCM_TYPE_MAP);
    }
    
    public static String cleanupCmName(String cmNameReadableName){
		// replace chars that can lead to an
		// org.alfresco.repo.node.integrity.IntegrityException
		cmNameReadableName = cmNameReadableName.replaceAll(
			ApplicationInfoList.getHomeRepository().getValidatorRegexCMName(), "_");

		//replace ending dot with nothing
		//cmNameReadableName = cmNameReadableName.replaceAll("\\.$", "");
		cmNameReadableName = cmNameReadableName.replaceAll("[.]*$", "").trim();
        cmNameReadableName = CharMatcher.javaIsoControl().removeFrom(cmNameReadableName);
        cmNameReadableName = cmNameReadableName.replaceAll("[^\\x00-\\x7FÄäÖöÜüß]", "");

		return cmNameReadableName;
	}

	public static String makeUniqueName(String name) {
		return name+"_"+ DigestUtils.sha1Hex(System.currentTimeMillis()+""+ RandomUtils.nextLong());
	}
}
