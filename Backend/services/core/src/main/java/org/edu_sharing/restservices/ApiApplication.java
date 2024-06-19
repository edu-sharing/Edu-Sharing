package org.edu_sharing.restservices;

import com.fasterxml.jackson.jakarta.rs.json.JacksonXmlBindJsonProvider;
import io.swagger.v3.jaxrs2.SwaggerSerializers;
import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//@ApplicationPath(value = "/rest")
public class ApiApplication extends ResourceConfig {

    public static final int MAJOR = 1;
    public static final int MINOR = 1;

    public static final Set<Class<?>> SERVICES;
    public static final Set<Class<?>> exceptionMapper;

    static {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(ApiService.class));
        Set<BeanDefinition> annotated = scanner.findCandidateComponents("org.edu_sharing.restservices");
        SERVICES = annotated.stream().map(BeanDefinition::getBeanClassName).map(x-> {
            try {
                return Class.forName(x);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toSet());

        ClassPathScanningCandidateComponentProvider exceptionScanner = new ClassPathScanningCandidateComponentProvider(false);
        exceptionScanner.addIncludeFilter(new org.springframework.core.type.filter.AssignableTypeFilter(ExceptionMapper.class));
        Set<BeanDefinition> exceptionMapperCandidates = exceptionScanner.findCandidateComponents("org.edu_sharing");
        exceptionMapper = exceptionMapperCandidates.stream().map(BeanDefinition::getBeanClassName).map(x-> {
            try {
                return Class.forName(x);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toSet());
    }

    public ApiApplication() {

        // multi-part feature

        this.register(MultiPartFeature.class);

        // custom services

        this.registerClasses(SERVICES);
        this.registerClasses(exceptionMapper);
        this.packages(getClass().getPackage().getName());

        // swagger service
        /**
         * @TODO: ApiListingResource
         */
        //this.register(ApiListingResource.class, SwaggerSerializers.class);
        this.register(SwaggerSerializers.class);
        //this.packages("io.swagger.jaxrs.listing");
        this.packages("io.swagger.v3.jaxrs2.integration.resources");
        // required in order to catch invalid json data exception
        this.register(JacksonXmlBindJsonProvider.class);

        //final BeanConfig beanConfig = new BeanConfig();

        OpenAPI oas = new OpenAPI();
        Info info = new Info();
        info.setTitle("edu-sharing Repository REST API");
        info.setDescription("The public restful API of the edu-sharing repository.");
        info.setVersion(MAJOR + "." + MINOR);
        oas.info(info);
        String url = "/" + ApplicationInfoList.getHomeRepository().getWebappname() + "/rest";
        //+ getClass().getAnnotation(ApplicationPath.class).value();
        oas.servers(Collections.singletonList(new Server().url(url)));

        SwaggerConfiguration oasConfig = new SwaggerConfiguration()
                .openAPI(oas)
                .prettyPrint(true)
                .resourcePackages(Stream.of(getClass().getPackage().getName()).collect(Collectors.toSet()));

        /**
         * @TODO
         */
        //beanConfig.setScan(true);


        try {
            new JaxrsOpenApiContextBuilder()
                    /**
                     * @TODO
                     */
                    //.servletConfig(servletConfig)
                    .application(this)
                    .openApiConfiguration(oasConfig)
                    .buildContext(true);
        } catch (OpenApiConfigurationException e) {
            throw new RuntimeException(e.getMessage(), e);
        }


    }

}
