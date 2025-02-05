package org.edu_sharing.restservices;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.springframework.stereotype.Component;

@Component
public class IllegalArgumentExceptionMapper implements ExceptionMapper<IllegalArgumentException> {

    @Override
    public Response toResponse(IllegalArgumentException e) {

        return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponse(e)).build();
    }
}
