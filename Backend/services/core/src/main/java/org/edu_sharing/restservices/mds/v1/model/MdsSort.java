package org.edu_sharing.restservices.mds.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.edu_sharing.metadataset.v2.MetadataSort;
import org.edu_sharing.metadataset.v2.MetadataSortColumn;

import java.util.ArrayList;
import java.util.List;

@Data
public class MdsSort {

	@JsonProperty(required = true)
	private String id;
	@JsonProperty("default")
	private MdsSortDefault defaultValue;
	@JsonProperty("defaultSearch")
	private MdsSortDefault defaultValueSearch;
	private List<MdsSortColumn> columns;

	@Data
	public static class MdsSortDefault {
		@JsonProperty(required = true)
		private String sortBy;
		@JsonProperty(required = true)
		private boolean sortAscending;
	}

	public MdsSort(){}
	public MdsSort(MetadataSort sort) {
		this.id=sort.getId();
		if(sort.getDefaultValue()!=null){
			this.defaultValue=new MdsSortDefault();
			this.defaultValue.setSortBy(sort.getDefaultValue().getSortBy());
			this.defaultValue.setSortAscending(sort.getDefaultValue().isSortAscending());
		}
		if(sort.getDefaultValueSearch()!=null){
			this.defaultValueSearch=new MdsSortDefault();
			this.defaultValueSearch.setSortBy(sort.getDefaultValueSearch().getSortBy());
			this.defaultValueSearch.setSortAscending(sort.getDefaultValueSearch().isSortAscending());
		}
		if(sort.getColumns()!=null){
			columns=new ArrayList<>();
			for(MetadataSortColumn column : sort.getColumns()){
				columns.add(new MdsSortColumn(column));
			}
		}
	}

}

