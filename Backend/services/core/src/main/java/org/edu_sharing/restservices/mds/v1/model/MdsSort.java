package org.edu_sharing.restservices.mds.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.edu_sharing.metadataset.v2.MetadataSort;
import org.edu_sharing.metadataset.v2.MetadataSortColumn;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "")
public class MdsSort {
	public class MdsSortDefault {
		private String sortBy;
		private boolean sortAscending;

		@JsonProperty(required = true)
		public String getSortBy() {
			return sortBy;
		}

		public void setSortBy(String sortBy) {
			this.sortBy = sortBy;
		}

		@JsonProperty(required = true)
		public boolean isSortAscending() {
			return sortAscending;
		}

		public void setSortAscending(boolean sortAscending) {
			this.sortAscending = sortAscending;
		}
	}
	private String id;
	@JsonProperty(value = "default")
	private MdsSortDefault defaultValue;
	@JsonProperty("defaultSearch")
	private MdsSortDefault defaultValueSearch;
	private List<MdsSortColumn> columns;

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

	@JsonProperty(required = true)
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	@JsonProperty
	public List<MdsSortColumn> getColumns() {
		return columns;
	}
	public void setColumns(List<MdsSortColumn> columns) {
		this.columns = columns;
	}
}

