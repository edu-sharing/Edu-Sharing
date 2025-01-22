package org.edu_sharing.restservices.shared;


import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.Map;

import lombok.Data;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.PreviewServlet;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.edu_sharing.service.model.NodeRef;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.model.GetPreviewResult;

@Data
public class Preview implements Serializable {

  @JsonProperty(required = true)
  private String url;
  @JsonProperty(required = true)
  private Integer width;
  @JsonProperty(required = true)
  private Integer height;
  @JsonProperty(required = true, value = "isIcon")
  private boolean isIcon;
  @JsonProperty("isGenerated")
  private boolean isGenerated;
  private String type;
  private String mimetype;
  @Schema(type = "string", format = "byte")
  private byte[] data;

  public Preview(){
	  
  }
  public Preview(NodeService nodeService, String storeProtocol, String storeIdentifier, String nodeId, String version, String type, Map<String, Object> nodeProps) {
    GetPreviewResult preview = nodeService.getPreview(storeProtocol, storeIdentifier, nodeId ,nodeProps, version);
    PreviewServlet.PreviewDetail detail = null;
    try {
      detail = PreviewServlet.getPreview(nodeService, storeProtocol, storeIdentifier, nodeId,nodeProps);
      if(detail != null) {
        setGenerated(!PreviewServlet.PreviewDetail.TYPE_USERDEFINED.equals(detail.getType()));
        setType(detail.getType());
      }
    } catch(Throwable ignored){
      // may fails for remote repos
    }
    setUrl(preview.getUrl());
    if(CCConstants.CCM_TYPE_IO.equals(type) || CCConstants.CCM_TYPE_IO.equals(CCConstants.getValidGlobalName(type))) {
      if(detail != null) {
        setIcon(detail.getType().equals(PreviewServlet.PreviewDetail.TYPE_DEFAULT));
      }
    } else {
      setIcon(!(nodeProps.containsKey(CCConstants.CCM_PROP_MAP_ICON) || nodeProps.containsKey(CCConstants.CM_ASSOC_THUMBNAILS)));
    }
  }

  public Preview(String storeProtocol, String storeIdentifier, String nodeId, NodeRef.Preview previewData) {
    setUrl(NodeServiceFactory.getLocalService().getPreviewUrl(
            storeProtocol,
            storeIdentifier,
            nodeId,
            null
    ));
    if(previewData.getIcon() != null) {
      setIcon(previewData.getIcon());
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
}
