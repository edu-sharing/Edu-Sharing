package org.edu_sharing.service.statistic;

import java.util.ArrayList;
import java.util.List;

public class Filter {
	
	List<FilterEntry> entries = new ArrayList<>();
	
	
	public List<FilterEntry> getEntries() {
		return entries;
	}
	
	public void setEntries(List<FilterEntry> entries) {
		this.entries = entries;
	}
}
