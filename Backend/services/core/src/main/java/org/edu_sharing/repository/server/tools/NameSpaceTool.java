package org.edu_sharing.repository.server.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;



public class NameSpaceTool<V> {

	static Logger logger = Logger.getLogger(NameSpaceTool.class);
	
	
	/**
	 * replaces the short ke form i.e cm:name 
	 * with the long version {http://www.alfresco.org/model/content/1.0}name
	 * 
	 * @param properties
	 * @return
	 */
	public Map<String,V> transformKeysToLongQname(Map<String,V>  properties){
				
		Map<String, V> newProperties;
		try {
			newProperties = properties.getClass().newInstance();
			for(Map.Entry<String, V> entry : properties.entrySet()){
				if(entry.getKey().matches(CCConstants.NAMESPACE_PREFIX_REGEX_LONG)){
					newProperties.put(entry.getKey(),entry.getValue());
				}else if(entry.getKey().matches(CCConstants.NAMESPACE_PREFIX_REGEX_SHORT)){
					String longQName =CCConstants.getValidGlobalName(entry.getKey()); 
					if(longQName != null){
						newProperties.put(longQName,entry.getValue());
					}else{
						logger.debug("can not transform to long QName: "+longQName);
					}
				}else{
					logger.debug("unknown property:"+ entry.getKey());
				}
			}
			return newProperties;
			
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static List<String> transFormToShortQName(List<String> qnames){
		
	
		List<String> list = new ArrayList<>();
		for(String qname : qnames){
			if(qname.matches(CCConstants.NAMESPACE_PREFIX_REGEX_SHORT)){
				list.add(qname);
			}else if(qname.matches(CCConstants.NAMESPACE_PREFIX_REGEX_LONG)){
				String shortQName = CCConstants.getValidLocalName(qname);
				if(shortQName != null){
					list.add(shortQName);
				}else{
					logger.debug("can not transform to shart QName:" + qname);
				}
			}
		}
		return list;
		
		
	}
	
	public Map<String,V> transformKeysToShortQname(Map<String,V>  properties){
		
		Map<String, V> newProperties;
		try {
			newProperties = properties.getClass().newInstance();
			for(Map.Entry<String, V> entry : properties.entrySet()){
				if(entry.getKey().matches(CCConstants.NAMESPACE_PREFIX_REGEX_SHORT)){
					newProperties.put(entry.getKey(), entry.getValue());
				}else if(entry.getKey().matches(CCConstants.NAMESPACE_PREFIX_REGEX_LONG)){
					
					String shortQName = CCConstants.getValidLocalName(entry.getKey());
					if(shortQName != null){
						newProperties.put(shortQName, entry.getValue());
					}else{
						logger.debug("can not transform to shart QName:" + entry.getKey());
					}
				}else{
					logger.debug("unknown property:"+ entry.getKey());
				}
			}
			return newProperties;
			
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	public static String transformToLongQName(String shortQName){
		if(shortQName.matches(CCConstants.NAMESPACE_PREFIX_REGEX_LONG)){
			return shortQName;
		}else{
			return CCConstants.getValidGlobalName(shortQName);
		}
	}
	
	public static String transformToShortQName(String longQName){
		if(longQName.matches(CCConstants.NAMESPACE_PREFIX_REGEX_SHORT)){
			return longQName;
		}else{
			return CCConstants.getValidLocalName(longQName);
		}
	}
}
