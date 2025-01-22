package org.edu_sharing.restservices.admin.v1.model;

;

import java.util.List;

import lombok.Data;
import org.edu_sharing.restservices.shared.Node;

@Data
public class AdminStatistics  {
  private int activeSessions,numberOfPreviews;
  private long maxMemory,allocatedMemory,previewCacheSize;
  private List<Node> activeLocks;
}
