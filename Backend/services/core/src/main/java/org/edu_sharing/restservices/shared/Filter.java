package org.edu_sharing.restservices.shared;

import java.util.ArrayList;
import java.util.List;

public class Filter {
	
	public static final String ALL="-all-";
	
	List<String> properties = new ArrayList<>();
	
	public Filter() {
	}
	
	public static Filter createShowAllFilter(){
		List<String> list=new ArrayList<>();
		list.add(ALL);
		return new Filter(list);
	}
	public Filter(List<String> propertyFilter) {
		if(propertyFilter != null){
			setProperties(propertyFilter);
		}
	}
	
	public List<String> getProperties() {
		return properties;
	}
	
	public void setProperties(List<String> properties) {		
		this.properties = properties;
	}

}
