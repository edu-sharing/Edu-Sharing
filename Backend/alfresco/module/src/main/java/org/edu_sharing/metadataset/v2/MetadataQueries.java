package org.edu_sharing.metadataset.v2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MetadataQueries extends MetadataQueryBase implements Serializable {
	private boolean allowSearchWithoutCriteria;
	private List<MetadataQuery> queries;

	public boolean isAllowSearchWithoutCriteria() {
		return allowSearchWithoutCriteria;
	}
	public void setAllowSearchWithoutCriteria(boolean allowSearchWithoutCriteria) {
		this.allowSearchWithoutCriteria = allowSearchWithoutCriteria;
	}

	public List<MetadataQuery> getQueries() {
		return queries;
	}
	public void setQueries(List<MetadataQuery> queries) {
		this.queries = queries;
	}
	public void overrideWith(MetadataQueries queries2) {
		if(queries2==null)
			return;
		if(queries2.basequery!=null)
			setBasequery(queries2.basequery);
		for(MetadataQuery query: queries2.getQueries()){
			if(queries!=null) {
				int pos = queries.lastIndexOf(query);
				if (pos != -1) {
					queries.get(pos).overrideWith(query);
				} else {
					queries.add(query);
				}
			} else {
				queries = new ArrayList<>();
				queries.add(query);
			}
		}
	}
	public MetadataQuery findQuery(String queryId) {
		for(MetadataQuery query : queries){
			if(query.getId().equals(queryId)){
				return query;
			}
		}
		throw new IllegalArgumentException("Query id "+queryId+" not found");
	}
}
