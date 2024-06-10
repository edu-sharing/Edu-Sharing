package org.edu_sharing.restservices.shared;


import io.swagger.v3.oas.annotations.media.Schema;
;

import java.io.Serializable;
import java.util.Map;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.PreviewServlet;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.edu_sharing.service.model.NodeRef;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.model.GetPreviewResult;


@Schema(description = "")
public class Preview implements Serializable {
  
  private String url = null;
  private Integer width = null;
  private Integer height = null;
  private boolean isIcon;
  private boolean isGenerated;
  private String type;
  private String mimetype;
  private byte[] data;

  public Preview(){
	  
  }
  public Preview(NodeService nodeService, String storeProtocol, String storeIdentifier, String nodeId, String version, String type, Map<String, Object> nodeProps) {
    GetPreviewResult preview = nodeService.getPreview(storeProtocol, storeIdentifier, nodeId ,nodeProps, version);
    PreviewServlet.PreviewDetail detail = null;
    try {
      detail = PreviewServlet.getPreview(nodeService, storeProtocol, storeIdentifier, nodeId,nodeProps);
      if(detail != null) {
        setIsGenerated(!PreviewServlet.PreviewDetail.TYPE_USERDEFINED.equals(detail.getType()));
        setType(detail.getType());
      }
    } catch(Throwable ignored){
      // may fails for remote repos
    }
    setUrl(preview.getUrl());
    if(CCConstants.CCM_TYPE_IO.equals(type) || CCConstants.CCM_TYPE_IO.equals(CCConstants.getValidGlobalName(type))) {
      if(detail != null) {
        setIsIcon(detail.getType().equals(PreviewServlet.PreviewDetail.TYPE_DEFAULT));
      }
    } else {
      setIsIcon(!(nodeProps.containsKey(CCConstants.CCM_PROP_MAP_ICON) || nodeProps.containsKey(CCConstants.CM_ASSOC_THUMBNAILS)));
    }
    // these values do not match up properly
    //setIsIcon(preview.isIcon());

    //if(repositoryType.equals(ApplicationInfo.REPOSITORY_TYPE_ALFRESCO) || repositoryType.equals(ApplicationInfo.REPOSITORY_TYPE_LOCAL)){
	/*  }
	  else{
		  setUrl((String)nodeProps.get(CCConstants.CM_ASSOC_THUMBNAILS));
		  setIsIcon(false);
	  }
	  */
  }

  public Preview(String storeProtocol, String storeIdentifier, String nodeId, NodeRef.Preview previewData) {
    setUrl(NodeServiceFactory.getLocalService().getPreviewUrl(
            storeProtocol,
            storeIdentifier,
            nodeId,
            null
    ));
    if(previewData.getIcon() != null) {
      setIsIcon(previewData.getIcon());
    } else {
      Logger.getLogger(Preview.class).warn("no preview icon info in elastic index for node " + nodeId);
    }
    if(previewData.getType() != null) {
      setType(previewData.getType());
    } else {
      Logger.getLogger(Preview.class).warn("no preview type info in elastic index for node " + nodeId);
    }
    setMimetype(previewData.getMimetype());
    setData(previewData.getData());
  }

  /**
   **/
  @Schema(required = true, description = "")
  @JsonProperty("url")
  public String getUrl() {
    return url;
  }
  public void setUrl(String url) {
    this.url = url;
  }

  
  /**
   **/
  @Schema(required = true, description = "")
  @JsonProperty("width")
  public Integer getWidth() {
    return width;
  }
  public void setWidth(Integer width) {
    this.width = width;
  }

  
  /**
   **/
  @Schema(required = true, description = "")
  @JsonProperty("height")
  public Integer getHeight() {
    return height;
  }
  public void setHeight(Integer height) {
    this.height = height;
  }

  

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class Preview {\n");
    
    sb.append("  url: ").append(url).append("\n");
    sb.append("  width: ").append(width).append("\n");
    sb.append("  height: ").append(height).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
  /**
   **/
  @Schema(required = true, description = "")
  @JsonProperty("isIcon")
  public boolean isIcon() {
    return isIcon;
  }
  public void setIsIcon(boolean isIcon) {
	this.isIcon=isIcon;
}

  @JsonProperty("isGenerated")
  public boolean isGenerated() {
    return isGenerated;
  }

  public void setIsGenerated(boolean generated) {
    isGenerated = generated;
  }

  @JsonProperty
  public void setType(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }

  @JsonProperty
  public void setMimetype(String mimetype) {
      this.mimetype = mimetype;
  }

  public String getMimetype() {
        return mimetype;
    }

  @JsonProperty
  public void setData(byte[] data) {
    this.data = data;
  }

  @Schema(type = "string", format = "byte")
  public byte[] getData() {
    return data;
  }
}
