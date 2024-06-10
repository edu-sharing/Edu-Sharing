package org.edu_sharing.restservices;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.springframework.stereotype.Component;

@Component
public class DaoExceptionMapper implements ExceptionMapper<DAOException> {
    @Override
    public Response toResponse(DAOException daoException) {
        return ErrorResponse.createResponse(daoException);
    }
}
