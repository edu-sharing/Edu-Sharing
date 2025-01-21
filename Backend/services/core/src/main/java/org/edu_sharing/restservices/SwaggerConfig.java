package org.edu_sharing.restservices;


import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration

@SecurityScheme(type = SecuritySchemeType.HTTP, name = "basicAuth", scheme = "basic")
@SecurityScheme(type = SecuritySchemeType.APIKEY, in = SecuritySchemeIn.COOKIE, name = "cookieAuth", paramName = "JSESSIONID")
@OpenAPIDefinition(
        security = {
                @SecurityRequirement(name = "cookieAuth"),
                @SecurityRequirement(name = "basicAuth")
        })
public class SwaggerConfig {
}
