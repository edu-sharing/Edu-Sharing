package org.edu_sharing.alfresco.monitoring;

import java.util.*;

import javax.management.*;

import org.apache.catalina.Container;
import org.apache.catalina.Engine;
import org.apache.catalina.Manager;
import org.apache.catalina.Service;
import org.apache.catalina.Session;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardHost;
import org.apache.tomcat.util.modeler.Registry;

public class TomcatUtil {


    /**
     * The application prefix.
     */
    private static final String APP_PREFIX = "/";
    /**
     * The root application.
     */
    private static final String ROOT_NAME = "ROOT";
    /**
     * The root id.
     */
    private static final String ROOT_ID = "";


    public static org.apache.catalina.Session[] getTomcatSessions(Service service, String host, String webApp) {
        org.apache.catalina.Session[] result = null;
        StandardContext context = getContext(service, host, webApp);
        result = getSessions(context);
        return result;
    }

    private static StandardContext getContext(Service service, String host, String webApp) {
        StandardContext result = null;
        StandardHost hostContainer = getHost(service, host);
        if (hostContainer != null) {

            result = (StandardContext) hostContainer.findChild(webApp);
        } else {
            System.out.println("No host {0} found" + new Object[]{host});
        }
        return result;
    }

    private static Container[] getContexts(Service service) {
        List<Container> result = new ArrayList<>();
        Container root = service.getContainer();
        if (root != null) {
            Container[] hosts = root.findChildren();
            if (hosts != null) {
                for (Container host : hosts) {
                    Container[] items = host.findChildren();
                    result.addAll(Arrays.asList(items));
                }
            }
        }
        return result.toArray(new Container[result.size()]);
    }

    private static StandardHost getHost(Service service, String host) {
        StandardHost result = null;
        Engine engineService = (Engine) service.getContainer();
        if (engineService != null) {
            result = (StandardHost) engineService.findChild(host);
        }
        return result;
    }

    private static org.apache.catalina.Session[] getSessions(StandardContext context) {
        org.apache.catalina.Session[] result = null;
        if (context != null) {
            Manager manager = context.getManager();
            if (manager != null) {
                try {
                    result = manager.findSessions();
                } catch (Exception ex) {
                    //LOGGER.log(Level.SEVERE, "Error by reading the list of sessions", ex);
                }
            } else {
                // LOGGER.log(Level.SEVERE, "No mananager found for web application");
            }
        } else {
            //LOGGER.log(Level.SEVERE, "No context found for web application");
        }
        return result;
    }


    public Map<Application, Integer> getSessionCount(String host) throws Exception {

        Service service = getService();

        Map<Application, Integer> result = new HashMap<>();

        List<Application> apps = getApplications(service);
        for (Application app : apps) {
            Session[] sessions = getTomcatSessions(service, host, "/" + app.getName());

            result.put(app, sessions.length);
        }


        return result;
    }

    public Session[] getSessions(String host, String webapp) throws Exception {
        Service service = getService();
        List<Application> apps = getApplications(service);
        Application app = apps.stream().filter(a -> a.getName().equals(webapp)).findFirst().get();
        return getTomcatSessions(service, host, "/" + app.getName());
    }

    private Service getService() throws MalformedObjectNameException, ReflectionException, AttributeNotFoundException, InstanceNotFoundException, MBeanException {
        MBeanServer mBeanServer = Registry.getRegistry(null, null).getMBeanServer();
        org.apache.catalina.Server server = (org.apache.catalina.Server) mBeanServer.getAttribute(new ObjectName("Catalina", "type", "Server"), "managedResource");

        return server.findService("Catalina");
    }

    public static List<Application> getApplications(Service service) {
        Container[] contexts = getContexts(service);
        List<Application> result = new ArrayList<>();
        if (contexts != null) {
            for (Container tmp : contexts) {
                Application app = createApplication(tmp);
                if (app != null) {
                    result.add(app);
                }
            }
        }
        return result;
    }

    private static Application createApplication(Container context) {
        Application result = null;
        if (context != null) {
            result = new Application();
            result.setId(getTomcatApplicationId(context));
            Container parent = context.getParent();
            if (parent != null) {
                result.setHost(parent.getName());
            }
            // The name string (suitable for use by humans)
            result.setName(getApplicationName(context.getName()));
        }
        return result;
    }


    private static String getTomcatApplicationId(Container container) {
        String result = null;
        if (container != null) {
            result = getApplicationName(container.getName());
        }
        return result;
    }

    private static String getApplicationName(String name) {
        String result = name;
        if (name == null || name.isEmpty()) {
            result = ROOT_NAME;
        } else {
            if (name.startsWith(APP_PREFIX)) {
                result = name.substring(APP_PREFIX.length());
            }
        }
        return result;
    }
}
