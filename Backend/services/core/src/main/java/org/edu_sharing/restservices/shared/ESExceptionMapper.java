package org.edu_sharing.restservices.shared;

import com.fasterxml.jackson.databind.JsonMappingException;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.log4j.Level;
import org.edu_sharing.restservices.DAOException;
import org.edu_sharing.restservices.DAOValidationException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ESExceptionMapper implements jakarta.ws.rs.ext.ExceptionMapper<JsonMappingException> {
    @Override
    public Response toResponse(JsonMappingException throwable) {
        log.debug(throwable.getMessage(), throwable);
        return ErrorResponse.createResponse(throwable, Response.Status.BAD_REQUEST);
    }
}
