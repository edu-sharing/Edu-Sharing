package org.edu_sharing.restservices.qa.v1.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateQAEntryRequestDTO {
    private String question;
    private String answer;
}
