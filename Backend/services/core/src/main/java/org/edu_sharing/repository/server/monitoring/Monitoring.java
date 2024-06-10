package org.edu_sharing.repository.server.monitoring;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.log4j.Logger;
import org.apache.tomcat.util.modeler.Registry;
import org.edu_sharing.alfresco.monitoring.Application;
import org.edu_sharing.alfresco.monitoring.MonitoringDao;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.springframework.context.ApplicationContext;

public class Monitoring extends HttpServlet {

    private transient BasicDataSource dataSource = null;

    private static Logger logger = Logger.getLogger(Monitoring.class);

    @Override
    public void init() throws ServletException {
        ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
        dataSource = (BasicDataSource) applicationContext.getBean("defaultDataSource");

        super.init();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        try {
            if (!new MCAlfrescoAPIClient().isAdmin()) {
                resp.getOutputStream().println("access denied");
                return;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        doIt(resp.getOutputStream());
    }


    private void doIt(ServletOutputStream outputStream) {
        try {
            MonitoringDao dao = new MonitoringDao();


            info(outputStream, "**********DATABASE****************");
            for (Map.Entry<String, String> entry : dao.getDataBasePoolInfo().entrySet()) {
                info(outputStream, entry.getKey() + ":" + entry.getValue());

            }

            info(outputStream, "");

            info(outputStream, "**********THREADS****************");

            for (String mbean : dao.getMBeans()) {

                info(outputStream, "");


                info(outputStream, "MBEAN:" + mbean);


                Map<String, String> attributes = dao.getMBeanAttributes(mbean, MonitoringDao.mbeanAttributes);
                for (Map.Entry<String, String> entry : attributes.entrySet()) {
                    info(outputStream, entry.getKey() + ":" + entry.getValue());

                }
            }

            info(outputStream, "");


            info(outputStream, "**********SessionCount****************");

            Map<Application, Integer> map = dao.getSessionCount("localhost");
            for (Map.Entry<Application, Integer> entry : map.entrySet()) {
                info(outputStream, entry.getKey().getName() + " " + entry.getValue());
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }


    protected ObjectName objName = null;

    public void mbeans() throws Exception {

        MBeanServer mBeanServer = Registry.getRegistry(null, null).getMBeanServer();
        String onStr = "*:type=ThreadPool,*";
        ObjectName objectName = new ObjectName(onStr);
        Set<ObjectInstance> set = mBeanServer.queryMBeans(objectName, null);
        Iterator<ObjectInstance> iterator = set.iterator();
        while (iterator.hasNext()) {
            ObjectInstance oi = iterator.next();
            System.out.println(oi.getClassName());

            objName = oi.getObjectName();
            String name = objName.getKeyProperty("name");
            System.out.println("name:" + name);

            MBeanInfo mbeanInfo = mBeanServer.getMBeanInfo(objName);


            for (MBeanAttributeInfo info : mbeanInfo.getAttributes()) {
                try {
                    System.out.println("mbean attribute: " + info.getName() + ": " + mBeanServer.getAttribute(objName, info.getName()));
                } catch (javax.management.ReflectionException e) {
                }
            }

        }


    }


    private void info(ServletOutputStream output, String text) {
        logger.info(text);
        try {
            output.println(text);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

}
