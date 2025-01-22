package org.edu_sharing.restservices.config.v1.model;

import java.util.Map;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Variables {
	private Map<String,String> global;
	private Map<String,String> current;
}
