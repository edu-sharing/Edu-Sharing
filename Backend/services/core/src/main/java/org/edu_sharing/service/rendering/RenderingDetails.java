package org.edu_sharing.service.rendering;

import lombok.*;
import org.edu_sharing.repository.server.rendering.RenderingException;
import org.edu_sharing.restservices.NodeDao;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class RenderingDetails {
    @NonNull
    String details;
    @NonNull
    RenderingServiceData renderingServiceData;
    RenderingException exception;
}
