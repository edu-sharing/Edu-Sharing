package org.edu_sharing.service.handleservicedoi;

import lombok.Getter;

@Getter
public class DOIServiceMissingAttributeException extends DOIServiceException{

    String property;
    String schemaField;
    public DOIServiceMissingAttributeException(String property, String schemaField) {
        super("missing datacity schema field: " + schemaField);
        this.schemaField = schemaField;
        this.property = property;
    }

}
