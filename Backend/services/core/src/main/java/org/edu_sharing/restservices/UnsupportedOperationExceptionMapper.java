package org.edu_sharing.restservices;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.springframework.stereotype.Component;

@Component
public class UnsupportedOperationExceptionMapper implements ExceptionMapper<UnsupportedOperationException> {

    @Override
    public Response toResponse(UnsupportedOperationException e) {
        return Response.status(Response.Status.NOT_IMPLEMENTED).entity(new ErrorResponse(e)).build();
    }
}
