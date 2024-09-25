/**
 *
 */
package org.edu_sharing.repository.server;

import jakarta.servlet.ServletContextEvent;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.chemistry.opencmis.server.impl.CmisRepositoryContextListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.alfresco.policy.OnUpdatePersonPropertiesPolicy;
import org.edu_sharing.alfresco.service.guest.GuestService;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.metadataset.v2.MetadataReader;
import org.edu_sharing.repository.server.jobs.quartz.JobHandler;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.InitHelper;
import org.edu_sharing.repository.server.tools.UserEnvironmentTool;
import org.edu_sharing.repository.server.tracking.TrackingService;
import org.edu_sharing.repository.server.tracking.TrackingService.TrackingBufferFactory;
import org.edu_sharing.repository.server.tracking.buffer.FileRingBuffer;
import org.edu_sharing.repository.server.tracking.buffer.MemoryRingBuffer;
import org.edu_sharing.repository.server.tracking.buffer.TrackingBuffer;
import org.edu_sharing.repository.update.KeyGenerator;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;
import org.edu_sharing.spring.context.EduSharingContextInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.ContextLoaderListener;

import java.io.File;

public class MCAlfrescoManager extends ContextLoaderListener {

    Log logger = LogFactory.getLog(MCAlfrescoManager.class);
    private ApplicationContext applicationContext;
    private ServiceRegistry serviceRegistry;

    // -- startup ---

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        try {
            applicationContext = AlfAppContextGate.getApplicationContext();
            serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);


            //generate security keys if not there
            new KeyGenerator().execute(false);
            ApplicationInfoList.refresh();

            logger.info("load ApplicationInfos");
            ApplicationInfo appInfo = ApplicationInfoList.getHomeRepository();

            logger.info("load Metadatasets");
            MetadataReader.refresh();


            //do update this class checks if it is already done
            AuthenticationToolAPI authTool = new AuthenticationToolAPI();
            authTool.createNewSession(appInfo.getUsername(), appInfo.getPassword());

            super.setContextInitializers(new EduSharingContextInitializer());
            super.contextInitialized(servletContextEvent);


            //init the system folders so that are created with a admin
            UserEnvironmentTool uet = new UserEnvironmentTool(appInfo.getUsername());
            uet.createAllSystemFolders();

            // init the esuid for admin
            createESUIDAdmin();

            InitHelper.initProxyUser();
            try {
                InitHelper.initGroups();
            } catch (Throwable t) {
                logger.error("init of config groups failed: " + t.getMessage(), t);
            }
            try {
                // create all guests
                GuestService guestService = applicationContext.getBean(GuestService.class);
                guestService.init();
            } catch (Throwable t) {
                logger.error("init of config guests failed: " + t.getMessage(), t);
            }
            try {
                InitHelper.initPersons();
            } catch (Throwable t) {
                logger.error("init of config persons failed: " + t.getMessage(), t);
            }


            //init ToolPermisssions
            ToolPermissionServiceFactory.getInstance().init();


            if (appInfo.getTrackingBufferSize() > 0) {

                int size = appInfo.getTrackingBufferSize();
                logger.info("startup TrackingBuffer (max=" + size + ")");

                File directory = (File) servletContextEvent.getServletContext().getAttribute("jakarta.servlet.context.tempdir");

                final TrackingBuffer trackingBuffer =
                        (directory != null
                                ? new FileRingBuffer(directory, size)
                                : new MemoryRingBuffer(size));

                TrackingService.registerBuffer(() -> trackingBuffer);
            } else {
                logger.warn("no tracking!");
            }

            logger.info("startup JobHandler");
            JobHandler.getInstance();

            //test setting cmis factory to use cmis in edu-sharing
            //ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
            Object factory = applicationContext.getBean("CMISServiceFactory");
            servletContextEvent.getServletContext().setAttribute(CmisRepositoryContextListener.SERVICES_FACTORY, factory);

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void createESUIDAdmin() {
        NodeRef admin = serviceRegistry.getPersonService().getPersonOrNull(ApplicationInfoList.getHomeRepository().getUsername());
        if (admin == null) {
            logger.warn("Creating an esuid for admin failed. Check that the username property in your homeapp is correct: " + ApplicationInfoList.getHomeRepository().getUsername());
        } else {
            if (OnUpdatePersonPropertiesPolicy.createESUIDIfNotExists(serviceRegistry.getNodeService(), admin)) {
                logger.info("Successfully created an esuid for the admin user");
            }
        }
    }

    // -- shutdown ---
    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
        try {
            logger.info("shutdown JobHandler");
            JobHandler.getInstance().shutDown();
            super.contextDestroyed(arg0);

            TrackingBufferFactory trackingBufferFactory = TrackingService.unregisterBuffer();
            if (trackingBufferFactory != null) {

                logger.info("shutdown Tracking");
                TrackingBuffer trackingBuffer = trackingBufferFactory.getTrackingBuffer();

                if (!trackingBuffer.isEmpty()) {
                    logger.warn("TrackingBuffer is not empty!");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
