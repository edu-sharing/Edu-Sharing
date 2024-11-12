package org.edu_sharing.repository.server.oai;

import io.gdcc.xoai.dataprovider.DataProvider;
import io.gdcc.xoai.model.oaipmh.OAIPMH;
import io.gdcc.xoai.xml.XmlWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.ContentType;
import org.edu_sharing.spring.ApplicationContextFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;


@Slf4j
public class OaiServlet extends HttpServlet {


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {

            ApplicationContext applicationContext = ApplicationContextFactory.getApplicationContext();
            OaiSettings settings = applicationContext.getBean(OaiSettings.class);
            if (!settings.isEnabled()) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            DataProvider dataProvider = applicationContext.getBean(DataProvider.class);
            OAIPMH response = dataProvider.handle(req.getParameterMap());
            String responseXML = responseToXML(response);

            try {
                OaiPostProcessor postProcessor = applicationContext.getBean(OaiPostProcessor.class);
                responseXML = postProcessor.postProcessResponse(req.getParameterMap(), responseXML);
            } catch (NoSuchBeanDefinitionException ignored) { }

            resp.setHeader("Content-Type", ContentType.APPLICATION_XML.getMimeType());
            resp.getOutputStream().write(responseXML.getBytes());

        } catch (Throwable t) {
            log.warn(t.getMessage(), t);
            resp.sendError(500, t.getMessage());
        }
    }

    private String responseToXML(OAIPMH response) throws XMLStreamException, IOException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            try (XmlWriter writer = new XmlWriter(os)) {
                response.write(writer);
            }
            return os.toString();
        }
    }

}
