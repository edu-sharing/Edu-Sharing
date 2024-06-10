package org.edu_sharing.repository.client.rpc;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class SchoolContextValues implements Serializable{

	Boolean resultBasedOnCombination = false;
	
	Map<String,String> federalState = new HashMap<>();
	Map<String,String> typeOfSchool = new HashMap<>();
	Map<String,String> schoolSubject = new HashMap<>();
	Map<String,String> ageGroup = new HashMap<>();
	Map<String,String> topic = new HashMap<>();
	
	public SchoolContextValues() {
	}
	
	/**
	 * <NodeRef,Caption>
	 * @return
	 */
	public Map<String, String> getFederalState() {
		return federalState;
	}

	/**
	 * <NodeRef,Caption>
	 * @return
	 */
	public Map<String, String> getTypeOfSchool() {
		return typeOfSchool;
	}

	/**
	 * <NodeRef,Caption>
	 * @return
	 */
	public Map<String, String> getSchoolSubject() {
		return schoolSubject;
	}

	/**
	 * <NodeRef,Caption>
	 * @return
	 */
	public Map<String, String> getAgeGroup() {
		return ageGroup;
	}

	/**
	 * <NodeRef,Caption>
	 * @return
	 */
	public Map<String, String> getTopic() {
		return topic;
	}
	
	public Boolean getResultBasedOnCombination() {
		return resultBasedOnCombination;
	}
	
	public void setResultBasedOnCombination(Boolean resultBasedOnCombination) {
		this.resultBasedOnCombination = resultBasedOnCombination;
	}
	
}
