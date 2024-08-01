package org.edu_sharing.service.permission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HandleParam {
    public HandleMode handleService;
    public HandleMode doiService;
}
