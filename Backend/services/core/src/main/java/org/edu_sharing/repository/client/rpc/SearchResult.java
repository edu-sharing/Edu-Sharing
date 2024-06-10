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
package org.edu_sharing.repository.client.rpc;

import org.edu_sharing.metadataset.v2.SearchCriterias;

import java.util.HashMap;
import java.util.Map;

public class SearchResult extends Result<Map<String, Map<String, Object>>>{
	
	Map<String,Map<String,Integer>> countedProps = null;
	
	private SearchCriterias searchCriterias = null;
	
	public SearchResult(){
		
	}
	
	public Map<String, Map<String, Object>> getData() {
		return data;
	}
	public void setData(Map<String, Map<String, Object>> data) {
		this.data = data;
	}
	
	/**
	 * Map<property, Map<value, count>>
	 * @return
	 */
	public Map<String, Map<String, Integer>> getCountedProps() {
		return countedProps;
	}
	public void setCountedProps(Map<String, Map<String, Integer>> countedProps) {
		this.countedProps = countedProps;
	}
	
	public SearchCriterias getSearchCriterias() {
		return searchCriterias;
	}
	/**
	 * 
	 * @param searchCriterias
	 */
	public void setSearchCriterias(SearchCriterias searchCriterias) {
		this.searchCriterias = searchCriterias;
	}
	
}
