package org.edu_sharing.restservices.suggestions.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;
import org.edu_sharing.service.suggestion.SuggestionStatus;
import org.edu_sharing.service.suggestion.SuggestionType;

import java.io.Serializable;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateSuggestionRequestDTO {
   private String propertyId;
   private Object value;

   private String description;
   private double confidence = 0;
}
