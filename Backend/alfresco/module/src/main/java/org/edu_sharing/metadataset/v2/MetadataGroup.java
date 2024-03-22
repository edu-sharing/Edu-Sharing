package org.edu_sharing.metadataset.v2;

import java.io.Serializable;
import java.util.List;

public class MetadataGroup implements Serializable {
	public enum Rendering{
		legacy,
		angular
	};
	private String id;
	private Rendering rendering = Rendering.angular;
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public List<String> getViews() {
		return views;
	}
	public void setViews(List<String> views) {
		this.views = views;
	}

	public Rendering getRendering() {
		return rendering;
	}

	public void setRendering(Rendering rendering) {
		this.rendering = rendering;
	}

	private List<String> views;
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof MetadataGroup){
			return ((MetadataGroup)obj).id.equals(id);
		}
		return super.equals(obj);
	}
}
