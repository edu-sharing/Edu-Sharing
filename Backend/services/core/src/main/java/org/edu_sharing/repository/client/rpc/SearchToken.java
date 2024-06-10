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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SearchToken implements Serializable {
	//private String query = "";
	private int startIDX = 0;
	private int nrOfResults = 0;
	private String repositoryId = null;
	
	private boolean stickyResult = false;
	/**
	 * list of props to count
	 */
	private ArrayList<String> countProps = null;
	
	/**
	 * global facet mincount
	 * @todo make it configurable for every single facet
	 */
	protected int countPropsMinCount = 5;
	
	/**
	 * <property,subStringMaxIdx>
	 */
	private Map<String,Integer> countPropsSubString = null;
	
	
	private SearchCriterias searchCriterias = null;
	
	
	private String sort = null;
	boolean sortAscending = false;
		
	//is an requirement from gwt to have an default constructor for serializable objects
	public SearchToken(){
		
	}
	
	public int getStartIDX() {
		return startIDX;
	}
	public void setStartIDX(int startIDX) {
		this.startIDX = startIDX;
	}
	public int getNrOfResults() {
		return nrOfResults;
	}
	/**
	 * set to -1 if you want to see all results
	 * @param nrOfResults
	 */
	public void setNrOfResults(int nrOfResults) {
		this.nrOfResults = nrOfResults;
	}
	
	public String getRepositoryId() {
		return repositoryId;
	}
	public void setRepositoryId(String repositoryId) {
		this.repositoryId = repositoryId;
	}
	
	/**
	 * set this to get counted Props back
	 * @param countProps
	 */
	public void setCountProps(ArrayList<String> countProps) {
		this.countProps = countProps;
	}
	
	public ArrayList<String> getCountProps() {
		return countProps;
	}
	
	public Map<String, Integer> getCountPropsSubString() {
		return countPropsSubString;
	}
	
	/**
	 * set this to count substring, only in combination with setCountProps usable
	 * 
	 * for example countPropsSubString is necessary for counting categories:
	 * if we got the subject "german" with the id "120" we want to count every entity with subject 120 but also 
	 * the entities with subcategories like 12001 "Literatur" or 12006 "Grammatik" so we count everything that starts with 120.
	 * The Integer for the category prop would be 3
	 * 
	 * @param countPropsSubString
	 */
	public void setCountPropsSubString(Map<String, Integer> countPropsSubString) {
		this.countPropsSubString = countPropsSubString;
	}
	
	public void setCountPropsMinCount(int countPropsMinCount) {
		this.countPropsMinCount = countPropsMinCount;
	}
	
	public int getCountPropsMinCount() {
		return countPropsMinCount;
	}

	/**
	 * @return the searchCriterias
	 */
	public SearchCriterias getSearchCriterias() {
		return searchCriterias;
	}
	/**
	 * @param searchCriterias the searchCriterias to set
	 */
	public void setSearchCriterias(SearchCriterias searchCriterias) {
		this.searchCriterias = searchCriterias;
	}
	public void setSort(String sort) {
		this.sort = sort;
	}
	
	public String getSort() {
		return sort;
	}

	public void setSortAscending(boolean sortAscending) {
		this.sortAscending = sortAscending;
	}
	
	public boolean isSortAscending() {
		return sortAscending;
	}
	
}
