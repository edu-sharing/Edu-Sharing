package org.edu_sharing.swagger;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import io.swagger.v3.jaxrs2.Reader;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

// we need to implement this in the alf context because the class loader of swagger is located in alfresco
public class SwaggerReader extends Reader {

    @Override
    protected Operation parseMethod(Class<?> cls, Method method, List<Parameter> globalParameters, Produces methodProduces, Produces classProduces, Consumes methodConsumes, Consumes classConsumes, List<SecurityRequirement> classSecurityRequirements, Optional<ExternalDocumentation> classExternalDocs, Set<String> classTags, List<Server> classServers, boolean isSubresource, RequestBody parentRequestBody, ApiResponses parentResponses, JsonView jsonViewAnnotation, ApiResponse[] classResponses, AnnotatedMethod annotatedMethod) {
        Operation parsedOperation = super.parseMethod(cls, method, globalParameters, methodProduces, classProduces, methodConsumes, classConsumes, classSecurityRequirements, classExternalDocs, classTags, classServers, isSubresource, parentRequestBody, parentResponses, jsonViewAnnotation, classResponses, annotatedMethod);

        // cls is not the declaring class of the method
        Class<?> clazz = method.getDeclaringClass();
        SecurityRequirements[] classSecurityRequirements2 = clazz.getAnnotationsByType(SecurityRequirements.class);
        SecurityRequirements[] methodSecurityRequirements = method.getAnnotationsByType(SecurityRequirements.class);
        if (hasEmptySecurityDefinitions(methodSecurityRequirements) || hasEmptySecurityDefinitions(classSecurityRequirements2)) {
            parsedOperation.setSecurity(new ArrayList<>(0));
        }

        return parsedOperation;
    }

    private static boolean hasEmptySecurityDefinitions(SecurityRequirements[] securityRequirements) {
        return securityRequirements.length == 1 && (securityRequirements[0].value() == null || securityRequirements[0].value().length == 0);
    }
}
