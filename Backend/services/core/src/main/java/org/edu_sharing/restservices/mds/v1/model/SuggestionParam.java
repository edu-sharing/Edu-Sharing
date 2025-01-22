package org.edu_sharing.restservices.mds.v1.model;

import java.util.List;

import lombok.Data;
import org.edu_sharing.restservices.shared.MdsQueryCriteria;

@Data
public class SuggestionParam {
	private ValueParameters valueParameters;
	private List<MdsQueryCriteria> criteria;
}
