package org.edu_sharing.restservices.qa.v1.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateQANodeRequestDTO {
    private String usedText;
    private List<CreateQAEntryRequestDTO> entries;
}
