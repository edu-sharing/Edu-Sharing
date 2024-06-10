package org.edu_sharing.restservices.suggestions.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NodeSuggestionResponseDTO {
    private String nodeId;
    private Map<String, List<SuggestionResponseDTO>> suggestions;
}
