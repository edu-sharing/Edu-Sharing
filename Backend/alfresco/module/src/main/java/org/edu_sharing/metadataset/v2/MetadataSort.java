package org.edu_sharing.metadataset.v2;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

public class MetadataSort implements Serializable {
	public static class MetadataSortDefault implements Serializable {
		private String sortBy;
		private boolean sortAscending;

		public String getSortBy() {
			return sortBy;
		}

		public void setSortBy(String sortBy) {
			this.sortBy = sortBy;
		}

		public boolean isSortAscending() {
			return sortAscending;
		}

		public void setSortAscending(boolean sortAscending) {
			this.sortAscending = sortAscending;
		}
	}
	private String id;
	private String mode;
	@JsonPropertyDescription("Default sort state (for both with and without a search string)")
	@Getter
	@Setter
	private MetadataSortDefault defaultValue=new MetadataSortDefault();
	@JsonPropertyDescription("Default sort state only when searching (when unset, defaultValue is used)")
	@Getter
	@Setter
	private MetadataSortDefault defaultValueSearch=null;
	private List<MetadataSortColumn> columns;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public List<MetadataSortColumn> getColumns() {
		return columns;
	}
	public void setColumns(List<MetadataSortColumn> columns) {
		this.columns = columns;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof MetadataSort){
			return ((MetadataSort)obj).id.equals(id);
		}
		return super.equals(obj);
	}
}
