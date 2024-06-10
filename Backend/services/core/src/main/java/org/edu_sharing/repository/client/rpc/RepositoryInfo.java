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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author rudolph
 */
public class RepositoryInfo implements Serializable {

	/**
	 * repositoryId, repository Properties
	 */
	Map<String, Map<String,String>> repInfoMap;

	/**
	 * Help URLs
	 */
	String helpUrlCC = null;
	String helpUrlES = null;
	String helpUrlCustom = null;
	
	String helpUrlShare = null; 
	Boolean fuzzyUserSearch = false;
	
	
	public RepositoryInfo() {
	}

	/**
	 * @return the repInfoMap
	 */
	public Map<String, Map<String, String>> getRepInfoMap() {
		return repInfoMap;
	}

	/**
	 * @param repInfoMap the repInfoMap to set
	 */
	public void setRepInfoMap(Map<String, Map<String, String>> repInfoMap) {
		this.repInfoMap = repInfoMap;
	}

	public String getHelpUrlCC() {
		return helpUrlCC;
	}

	public void setHelpUrlCC(String helpUrlCC) {
		this.helpUrlCC = helpUrlCC;
	}

	public String getHelpUrlES() {
		return helpUrlES;
	}

	public void setHelpUrlES(String helpUrlES) {
		this.helpUrlES = helpUrlES;
	}

	public String getHelpUrlCustom() {
		return helpUrlCustom;
	}

	public void setHelpUrlCustom(String helpUrlCustom) {
		this.helpUrlCustom = helpUrlCustom;
	}
	
	public void setHelpUrlShare(String helpUrlShare) {
		this.helpUrlShare = helpUrlShare;
	}
	
	public String getHelpUrlShare() {
		return helpUrlShare;
	}

	public boolean isFuzzyUserSearch() {
		return this.fuzzyUserSearch;
	}

	public void setFuzzyUserSearch(boolean fuzzyUserSearch) {
		this.fuzzyUserSearch = fuzzyUserSearch;
	}
}
