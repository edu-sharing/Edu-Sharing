/**
 *
 *  
 * 
 * 
 *	
 *
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 *
 */
package org.edu_sharing.repository.client.tools.metadata;

import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.repository.client.tools.CCConstants;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


public class ValueTool {

	public static String[] getMultivalue(String mvValue){
		String[] result = null;
		if (mvValue.contains(CCConstants.MULTIVALUE_SEPARATOR)) {
			result = StringUtils.splitByWholeSeparator(mvValue, CCConstants.MULTIVALUE_SEPARATOR);
		} else {
			result = new String[]{mvValue};
		}
		return result;
	}
	public static String toMultivalue(String[] multivalue){
		return StringUtils.join(multivalue,CCConstants.MULTIVALUE_SEPARATOR);
	}
	public static Map<String, Object> getMultivalue(Map<String, Object> data){
		for(Map.Entry<String, Object> entry : data.entrySet()) {
			if(entry.getValue() instanceof String) {
				List<String> list = Arrays.asList(StringUtils.splitByWholeSeparator((String) entry.getValue(), CCConstants.MULTIVALUE_SEPARATOR));
				if(list.size() > 1) {
					data.put(entry.getKey(), list);
				}
			}
		}
		return data;
	}
	public static Map<String, Object> toMultivalue(Map<String, Object> data){
		for(Map.Entry<String, Object> entry : data.entrySet()) {
			if(entry.getValue() instanceof Iterable) {
				if(((Iterable<?>) entry.getValue()).iterator().hasNext() && ((Iterable<?>) entry.getValue()).iterator().next() instanceof String) {
					data.put(entry.getKey(), StringUtils.join((Iterable<?>) entry.getValue(), CCConstants.MULTIVALUE_SEPARATOR));
				}
			}
		}
		return data;
	}
}
