package org.edu_sharing.service.qa.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QAEntry {
    String question;
    String answer;
    boolean reviewed;
    boolean edited;
    LocalDateTime lastReviewed;
    String reviewedBy;
}
