package org.edu_sharing.spring.security.openid;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.policy.NodeCustomizationPolicies;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.spring.ApplicationContextFactory;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

import java.io.IOException;

public class SSORegistrationsDispatcherServlet extends HttpServlet {

    Logger logger = Logger.getLogger(SSORegistrationsDispatcherServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ClientRegistrationRepository clientRegistrationRepository = (ClientRegistrationRepository)
                ApplicationContextFactory.getApplicationContext().getBean("clientRegistrationRepository");

        String registrationId = NodeCustomizationPolicies.getEduSharingContext();
        if(StringUtils.isEmpty(registrationId) || registrationId.equals(CCConstants.EDUCONTEXT_DEFAULT)){
            registrationId = SecurityConfigurationOpenIdConnect.DEFAULT_REGISTRATION_ID;
        }

        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId(registrationId);
        if(clientRegistration == null){
            String message = "Client registration not found:" + registrationId +". check lightbend context config.";
            logger.warn(message);

            InMemoryClientRegistrationRepository cr = ((InMemoryClientRegistrationRepository)clientRegistrationRepository);
            if(cr.iterator() != null && cr.iterator().hasNext()){
                clientRegistration = cr.iterator().next();
                logger.warn("using fallback: "+clientRegistration.getRegistrationId());
            }else throw new ServletException(message);
        }

        String redirectPath = "/edu-sharing/oauth2/authorization/" + clientRegistration.getRegistrationId();
        resp.sendRedirect(redirectPath);
    }
}
