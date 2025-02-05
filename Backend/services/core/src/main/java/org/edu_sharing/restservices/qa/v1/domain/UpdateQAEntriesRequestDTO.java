package org.edu_sharing.restservices.qa.v1.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.edu_sharing.service.qa.domain.QAEntry;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateQAEntriesRequestDTO {
    List<QAEntry> qaEntries;
}
