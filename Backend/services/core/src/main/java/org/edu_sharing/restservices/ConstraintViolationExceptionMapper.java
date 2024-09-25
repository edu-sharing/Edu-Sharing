package org.edu_sharing.restservices;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import lombok.Value;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.service.util.ViolationUtils;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Value
    private static class InvalidatedParams implements Serializable {
        String attribute;
        String cause;
    }

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        Set<ConstraintViolation<?>> errors = exception.getConstraintViolations();

        ErrorResponse errorResponse = new ErrorResponse(exception);
        Map<String, Serializable> collect = errors.stream()
                .collect(Collectors.groupingBy(ViolationUtils::getViolationKind,
                        Collectors.mapping(this::getViolationParams,
                                Collectors.collectingAndThen(Collectors.toList(), x->(Serializable)x))));

        errorResponse.setDetails(collect);

                        //groupingBy(this::getViolationIssue, HashMap::new, this::getViolationParams)));

        return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
    }

    private InvalidatedParams getViolationParams(ConstraintViolation<?> violation){
        return new InvalidatedParams(violation.getPropertyPath().toString(), ViolationUtils.getViolationMessage(violation));
    }

}
