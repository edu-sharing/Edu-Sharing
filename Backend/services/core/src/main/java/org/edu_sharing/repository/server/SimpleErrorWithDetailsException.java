package org.edu_sharing.repository.server;

import org.edu_sharing.repository.server.ErrorFilter.ErrorFilterException;

public class SimpleErrorWithDetailsException extends ErrorFilterException {

    public SimpleErrorWithDetailsException(String details) {
        super(500, details);
    }

}
