package org.edu_sharing.metadataset.v2.tools;

import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.collections.IteratorUtils;
import org.edu_sharing.metadataset.v2.*;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.metadata.ValueTool;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.restservices.mds.v1.model.MdsWidget;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public class MetadataHelper {

	public static MetadataSet getLocalDefaultMetadataset() throws Exception{
		return MetadataReader.getMetadataset(ApplicationInfoList.getHomeRepository(),CCConstants.metadatasetdefault_id,getLocale());
	}
	public static MetadataSet getMetadataset(ApplicationInfo appId, String mdsSet) throws Exception{
		return MetadataReader.getMetadataset(appId, mdsSet,getLocale());
	}
	public static MetadataSet getMetadataset(NodeRef node) throws Exception{
		return getMetadataset(node, getLocale());
	}
	public static MetadataSet getMetadataset(NodeRef node, String locale) throws Exception{
		String mdsSet = NodeServiceHelper.getProperty(node, CCConstants.CM_PROP_METADATASET_EDU_METADATASET);
		if(mdsSet==null || mdsSet.isEmpty())
			mdsSet=CCConstants.metadatasetdefault_id;

		return MetadataReader.getMetadataset(ApplicationInfoList.getHomeRepository(), mdsSet, locale);
	}
	public static MetadataSet getMetadataset(org.edu_sharing.restservices.shared.NodeRef node) throws Exception{
		if(node.isHomeRepo()) {
			return getMetadataset(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, node.getId()));
		} else {
			return MetadataReader.getMetadataset(ApplicationInfoList.getRepositoryInfoById(node.getRepo()), CCConstants.metadatasetdefault_id, getLocale());
		}
	}
	public static Collection<MetadataWidget> getWidgetsByNode(NodeRef node, boolean onlyPrimaryWidgets) throws Exception{
		MetadataSet metadata = getMetadataset(node);
		return metadata.getWidgetsByNode(
				NodeServiceFactory.getLocalService().getType(node.getId()),
				Arrays.asList(NodeServiceHelper.getAspects(node)),
				onlyPrimaryWidgets
		);
	}

		private static String getLocale() {
		String locale="default";
		try{
			locale = new AuthenticationToolAPI().getCurrentLocale();
		}catch(Throwable t){}
		return locale;
	}
	public static String getTranslation(String key) throws Exception {
		return getTranslation(ApplicationInfoList.getHomeRepository(),key,null);
	}
	public static String getTranslation(ApplicationInfo appId,String key,String fallback) throws Exception {
		return MetadataReader.getTranslation(getMetadataset(appId,CCConstants.metadatasetdefault_id).getI18n(),key,fallback,getLocale());
	}

	public static String[] getDisplayNames(MetadataSet mds, String key, Serializable value){
		try{
			if(mds == null){
				return null;
			}
			MetadataWidget widget = mds.findWidget(key);
			if(widget != null) {
				Map<String, MetadataKey> map = widget.getValuesAsMap();
				if (!map.isEmpty()) {
					String[] keys = ValueTool.getMultivalue((String) value);
					String[] values = new String[keys.length];
					for (int i = 0; i < keys.length; i++)
						values[i] = map.containsKey(keys[i]) ? map.get(keys[i]).getCaption() : keys[i];
					return values;
				}
			}
		} catch (Exception e){};
		return null;
	}

    /** resolves this widget's condition
     * only works for condition type TOOLPERMISSION
     * @return
     */
    public static boolean checkConditionTrue(MetadataCondition condition) {
        if(condition==null)
            return true;
        if(MetadataCondition.CONDITION_TYPE.TOOLPERMISSION.equals(condition.getType())){
            boolean result= ToolPermissionServiceFactory.getInstance().hasToolPermission(condition.getValue());
            return result!=condition.isNegate();
        }
        //logger.info("skipping condition type "+condition.getType()+" for widget "+getId()+" since it's not supported in backend");
        return true;
    }

	/**
	 * attach any available translations/display names for keys of a given property set and attach them with the postfix DISPLAYNAME in this set
	 * The current locale will be used
	 */
	public static void addVirtualDisplaynameProperties(MetadataSet mds, Map<String, Object> props) {
		for(MetadataWidget widget: mds.getWidgets()) {
			Map<String, MetadataKey> values = widget.getValuesAsMap();
			String id = CCConstants.getValidGlobalName(widget.getId());
			if(values!=null && values.size() > 0 && props.containsKey(id)) {
				Object prop = props.get(CCConstants.getValidGlobalName(widget.getId()));
				if(prop instanceof String) {
					prop = Arrays.asList(ValueTool.getMultivalue((String) prop));
				}
				if(prop instanceof Iterable) {
					List<MetadataKey> keys = new ArrayList<>();
					((Iterable<?>) prop).forEach(
							p -> keys.add(values.get((String)p))
					);
					props.put(id + CCConstants.DISPLAYNAME_SUFFIX,
									keys.stream()
									.map(metadataKey -> metadataKey == null ? "" : metadataKey.getCaption())
									.collect(Collectors.toList())
					);
				}
			}
		}
	}
}
