package org.edu_sharing.restservices.shared;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
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
}
