package org.edu_sharing.restservices.mds.v1.model;

import lombok.Data;
import org.edu_sharing.metadataset.v2.MetadataTemplate;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
public class MdsView {
    private String id;
    private String caption;
    private String icon;
    private String html;
    private MetadataTemplate.REL_TYPE rel;
    private boolean hideIfEmpty;
    @JsonProperty("isExtended")
    private boolean isExtended;

    public MdsView() {
    }

    public MdsView(MetadataTemplate template) {
        this.id = template.getId();
        this.caption = template.getCaption();
        this.hideIfEmpty = template.getHideIfEmpty();
        this.isExtended = template.isExtended();
        this.icon = template.getIcon();
        this.html = template.getHtml();
        this.rel = template.getRel();
    }
}

