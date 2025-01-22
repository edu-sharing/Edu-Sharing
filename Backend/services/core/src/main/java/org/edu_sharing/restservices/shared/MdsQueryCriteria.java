package org.edu_sharing.restservices.shared;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MdsQueryCriteria {
	@JsonProperty(required = true)
	private String property;

	@JsonProperty(required = true)
	private List<String> values;


	public static List<MdsQueryCriteria> fromMap(Map<String, String[]> criterias) {
		List<MdsQueryCriteria>  criterasConverted =new ArrayList<>();
		for(Entry<String, String[]> set : criterias.entrySet()){
			MdsQueryCriteria criteria = new MdsQueryCriteria();
			criteria.setProperty(set.getKey());
			criteria.setValues(Arrays.asList(set.getValue()));
			criterasConverted.add(criteria);
		}
		return criterasConverted;
	}

}
