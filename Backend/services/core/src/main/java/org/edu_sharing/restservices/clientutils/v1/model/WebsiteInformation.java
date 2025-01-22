package org.edu_sharing.restservices.clientutils.v1.model;

import lombok.Data;
import org.edu_sharing.restservices.shared.Node;

import java.util.List;

@Data
public class WebsiteInformation {
	private String title,page,description,license;
	private String[] keywords;
	private List<Node> duplicateNodes;

	public WebsiteInformation(){}
	public WebsiteInformation(org.edu_sharing.service.clientutils.WebsiteInformation info){
		if(info==null)
			return;
		this.title=info.getTitle();
		this.page=info.getPage();
		this.description=info.getDescription();
		this.keywords=info.getKeywords();
		this.duplicateNodes=info.getDuplicateNodes();
		if(info.getLicense()!=null)
			this.license=info.getLicense().getName()+" "+info.getLicense().getCcVersion();
	}
}
