package org.edu_sharing.service.admin;

import com.google.common.io.Files;
import jakarta.servlet.http.HttpSession;
import org.alfresco.repo.cache.SimpleCache;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.module.ModuleInstallState;
import org.alfresco.service.cmr.module.ModuleService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.AuthorityType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.exception.CCException;
import org.edu_sharing.repository.client.rpc.ACE;
import org.edu_sharing.repository.client.rpc.ACL;
import org.edu_sharing.repository.client.rpc.cache.CacheCluster;
import org.edu_sharing.repository.client.rpc.cache.CacheInfo;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.StringTool;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.importer.ExcelLOMImporter;
import org.edu_sharing.repository.server.importer.collections.CollectionImporter;
import org.edu_sharing.repository.server.jobs.quartz.*;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.repository.server.tools.*;
import org.edu_sharing.repository.server.tools.cache.CacheManagerFactory;
import org.edu_sharing.repository.server.tools.cache.EduGroupCache;
import org.edu_sharing.repository.server.tools.mailtemplates.MailTemplate;
import org.edu_sharing.repository.server.update.PrintWriterLogAppender;
import org.edu_sharing.repository.server.update.UpdaterService;
import org.edu_sharing.repository.tomcat.ClassHelper;
import org.edu_sharing.repository.tools.URLHelper;
import org.edu_sharing.repository.update.Protocol;
import org.edu_sharing.restservices.GroupDao;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.restservices.admin.v1.model.PluginStatus;
import org.edu_sharing.restservices.shared.Group;
import org.edu_sharing.service.admin.model.GlobalGroup;
import org.edu_sharing.service.admin.model.RepositoryConfig;
import org.edu_sharing.service.admin.model.ServerUpdateInfo;
import org.edu_sharing.service.admin.model.ToolPermission;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.editlock.EditLockServiceFactory;
import org.edu_sharing.service.foldertemplates.FolderTemplatesImpl;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.nodeservice.RecurseMode;
import org.edu_sharing.service.permission.PermissionService;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.edu_sharing.service.toolpermission.ToolPermissionService;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;
import org.edu_sharing.service.version.RepositoryVersionInfo;
import org.edu_sharing.service.version.VersionService;
import org.edu_sharing.spring.ApplicationContextFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StreamUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.Collator;
import java.util.*;
import java.util.stream.Collectors;

public class AdminServiceImpl implements AdminService {

    //cause standard properties class does not save the values sorted
    class SortedProperties extends Properties {

        public SortedProperties() {
            super();
        }

        public SortedProperties(Properties initWith) {
            for (Map.Entry entry : initWith.entrySet()) {
                this.setProperty((String) entry.getKey(), (String) entry.getValue());
            }
        }

        //for sorted xml storing
        @Override
        public Set<Object> keySet() {
            return new TreeSet<Object>(super.keySet());
        }

    }

    ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
    ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

    private static Logger logger = Logger.getLogger(AdminServiceImpl.class);


    @Override
    public Collection<String> getAllValuesFor(String property) throws Throwable {
        Set<String> result = new HashSet<String>();

        List<NodeRef> children = NodeServiceFactory.getLocalService().getChildrenRecursive(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,
                NodeServiceFactory.getLocalService().getCompanyHome(),
                Collections.singletonList(CCConstants.CCM_TYPE_IO),
                RecurseMode.Folders
        );

		/*HashMap<String, Object> importFolderProps = mcAlfrescoBaseClient.getChild(companyHomeId, CCConstants.CCM_TYPE_MAP, CCConstants.CM_NAME,
				OAIPMHLOMImporter.FOLDER_NAME_IMPORTED_OBJECTS);*/
        for (NodeRef childEntry : children) {
            Serializable value = NodeServiceHelper.getPropertyNative(childEntry, property);
            if (value == null) {
                continue;
            }
            if (value instanceof Collection) {
                result.addAll((Collection<? extends String>) value);
            } else {
                result.add(value.toString());
            }
        }

        return result;
    }

    @Override
    public RepositoryConfig getConfig() {
        return RepositoryConfigFactory.getConfig();
    }

    @Override
    public void setConfig(RepositoryConfig config) {
        RepositoryConfigFactory.setConfig(config);
    }

    @Override
    public Map<String, ToolPermission> getToolpermissions(String authority) throws Throwable {

        ToolPermissionService tpService = ToolPermissionServiceFactory.getInstance();
        PermissionService permissionService = PermissionServiceFactory.getLocalService();
        boolean isEveryone = CCConstants.AUTHORITY_GROUP_EVERYONE.equals(authority);
        if (!isEveryone && AuthorityServiceFactory.getLocalService().getMemberships(authority).contains(CCConstants.AUTHORITY_GROUP_ALFRESCO_ADMINISTRATORS)) {
            throw new IllegalArgumentException("Toolpermissions are not supported for members of " + CCConstants.AUTHORITY_GROUP_ALFRESCO_ADMINISTRATORS);
        }
        Map<String, ToolPermission> toolpermissions = new HashMap<>();
        // refresh the tp in this case, since may a new one is created meanwhile by the client
        for (String tp : tpService.getAllToolPermissions(true)) {
            String nodeId = tpService.getToolPermissionNodeId(tp, true);
            List<String> permissionsExplicit = permissionService.getExplicitPermissionsForAuthority(nodeId, authority);
            List<String> permissions = permissionService.getPermissionsForAuthority(nodeId, authority);
            ToolPermission status = new ToolPermission();
            Boolean managed = (Boolean) NodeServiceHelper.getPropertyNative(
                    new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId),
                    CCConstants.CCM_PROP_TOOLPERMISSION_SYSTEM_MANAGED
            );
            status.setSystemManaged(managed != null && managed);

            if (permissionsExplicit.contains(CCConstants.PERMISSION_DENY)) {
                status.setExplicit(ToolPermission.Status.DENIED);
            } else if (permissionsExplicit.contains(CCConstants.PERMISSION_READ)) {
                status.setExplicit(ToolPermission.Status.ALLOWED);
            }
            if (permissions.contains(CCConstants.PERMISSION_DENY)) {
                status.setEffective(ToolPermission.Status.DENIED);
                status.setEffectiveSource(getEffectiveSource(nodeId, authority, CCConstants.PERMISSION_DENY));
            } else if (permissions.contains(CCConstants.PERMISSION_READ)) {
                status.setEffective(ToolPermission.Status.ALLOWED);
                status.setEffectiveSource(getEffectiveSource(nodeId, authority, CCConstants.PERMISSION_READ));
            }
            toolpermissions.put(tp, status);
        }
        return toolpermissions;
    }

    private List<Group> getEffectiveSource(String nodeId, String authority, String permissionName) throws Exception {
        PermissionService permissionService = PermissionServiceFactory.getLocalService();
        List<Group> result = new ArrayList<>();
		// getMemberships can not be called for group everyone
		if(authority.equals(CCConstants.AUTHORITY_GROUP_EVERYONE)) {
			result.add(Group.getEveryone());
			return result;
		}
        for (String group : AuthorityServiceFactory.getLocalService().getMemberships(authority)) {
            List<String> permissionsExplicit = permissionService.getExplicitPermissionsForAuthority(nodeId, group);
            if (permissionsExplicit.contains(permissionName)) {
                if (group.equals(CCConstants.AUTHORITY_GROUP_EVERYONE)) {
                    result.add(Group.getEveryone());
                } else {
                    result.add(GroupDao.getGroup(RepositoryDao.getHomeRepository(), group).asGroup());
                }
            }
        }
        return result;

    }

    @Override
    public String addToolpermission(String name) throws Throwable {
        ToolPermissionService tpService = ToolPermissionServiceFactory.getInstance();
        HashMap<String, Object> props = new HashMap<>();
        props.put(CCConstants.CM_NAME, name);
        String nodeId = NodeServiceFactory.getLocalService().createNodeBasic(tpService.getEdu_SharingToolPermissionsFolder().getId(), CCConstants.CCM_TYPE_TOOLPERMISSION, props);
        PermissionServiceFactory.getLocalService().setPermissionInherit(nodeId, false);
        return nodeId;
    }

    @Override
    public void setToolpermissions(String authority, Map<String, ToolPermission.Status> toolpermissions) throws Throwable {
        ToolPermissionService tpService = ToolPermissionServiceFactory.getInstance();
        PermissionService permissionService = PermissionServiceFactory.getLocalService();
        if (!CCConstants.AUTHORITY_GROUP_EVERYONE.equals(authority) && AuthorityServiceFactory.getLocalService().getMemberships(authority).contains(CCConstants.AUTHORITY_GROUP_ALFRESCO_ADMINISTRATORS)) {
            throw new IllegalArgumentException("Toolpermissions are not supported for members of " + CCConstants.AUTHORITY_GROUP_ALFRESCO_ADMINISTRATORS);
        }
        for (String tp : tpService.getAllAvailableToolPermissions()) {
            ToolPermission.Status status = toolpermissions.get(tp);
            String nodeId = tpService.getToolPermissionNodeId(tp, true);
            ACL acl = permissionService.getPermissions(nodeId);
            boolean add = true;
            List<ACE> newAce = new ArrayList<>();
            for (ACE ace : acl.getAces()) {
                if (ace.getAuthority().equals(authority)) {
                    add = false;
                    if (status != null && status.equals(ToolPermission.Status.ALLOWED)) {
                        ace.setPermission(CCConstants.PERMISSION_READ);
                    } else if (status != null && status.equals(ToolPermission.Status.DENIED)) {
                        ace.setPermission(CCConstants.PERMISSION_DENY);
                    } else {
                        continue;
                    }
                }
                newAce.add(ace);
            }
            if (add) {
                ACE ace = new ACE();
                ace.setAuthority(authority);
                if (status != null && status.equals(ToolPermission.Status.ALLOWED)) {
                    ace.setPermission(CCConstants.PERMISSION_READ);
                } else if (status != null && status.equals(ToolPermission.Status.DENIED)) {
                    ace.setPermission(CCConstants.PERMISSION_DENY);
                } else {
                    ace = null;
                }
                if (ace != null)
                    newAce.add(ace);
            }
            permissionService.setPermissions(nodeId, newAce);
        }
    }

	@Override
	public void writePublisherToMDSXml(String vcardProps, String valueSpaceProp, String ignoreValues, String filePath, HashMap authInfo) throws Throwable {
		File file = new File(filePath);
		Result result = new StreamResult(file);
		writePublisherToMDSXml(result,vcardProps,valueSpaceProp,ignoreValues,authInfo);
	}
	
	public String getPublisherToMDSXml(List<String> vcardProps, String valueSpaceProp, String ignoreValues, HashMap authInfo) throws Throwable {
		StringWriter writer=new StringWriter();
		Result result = new StreamResult(writer);
		writePublisherToMDSXml(result,StringUtils.join(vcardProps,","),valueSpaceProp,ignoreValues,authInfo);
		return writer.toString();
	}
	@Override
	public List<JobInfo> getJobs() throws Throwable {
		return JobHandler.getInstance().getAllRunningJobs();
	}
	@Override
	public void cancelJob(String jobName, boolean force) throws Throwable {
		if(!JobHandler.getInstance().cancelJob(jobName, force)){
			throw new Exception("Job could not be canceled. Scheduler returned false");
		}
	}

    public void writePropertyToMDSXml(Result result, String property) throws Throwable {
        Collection<String> values = getAllValuesFor(property);

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document document = documentBuilder.newDocument();
        Element rootElement = document.createElement("valuespaces");
        document.appendChild(rootElement);

        Element valueSpace = document.createElement("valuespace");
        valueSpace.setAttribute("property", CCConstants.getValidLocalName(property));
        rootElement.appendChild(valueSpace);

        for (String val : values) {
            Element key = document.createElement("key");
            key.appendChild(document.createTextNode(val));
            valueSpace.appendChild(key);
        }

        Source source = new DOMSource(document);

        // Write the DOM document to the file
        Transformer xformer = TransformerFactory.newInstance().newTransformer();
        xformer.setOutputProperty(OutputKeys.INDENT, "yes");
        xformer.transform(source, result);
    }

    public void writePublisherToMDSXml(Result result, String vcardProps, String valueSpaceProp, String ignoreValues, HashMap authInfo) throws Throwable {

        List<String> ignoreValuesList = null;
        if (ignoreValues != null && !ignoreValues.trim().equals("")) {
            ignoreValuesList = Arrays.asList(ignoreValues.split(","));
        } else {
            ignoreValuesList = new ArrayList<String>();
        }

        ArrayList<String> allValues = new ArrayList<String>();
        String[] splittedVCardProp = vcardProps.split(",");
        for (String oneVCardProp : splittedVCardProp) {
            allValues.addAll(getAllValuesFor(oneVCardProp));
        }

        if (allValues != null && allValues.size() > 0) {

            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.newDocument();
            Element rootElement = document.createElement("valuespaces");
            document.appendChild(rootElement);

            Element valueSpace = document.createElement("valuespace");
            valueSpace.setAttribute("property", valueSpaceProp != null ? valueSpaceProp : splittedVCardProp[0]);
            rootElement.appendChild(valueSpace);

            ArrayList<String> toSort = new ArrayList<String>();
            for (String vcardString : allValues) {

                //multivalue
                String[] splitted = vcardString.split(StringTool.escape(CCConstants.MULTIVALUE_SEPARATOR));
                for (String splittedVCardString : splitted) {
                    if (valueSpaceProp == null) {
                        if (!toSort.contains(splittedVCardString))
                            toSort.add(splittedVCardString);
                    } else {
                        ArrayList<HashMap<String, Object>> vcardList = VCardConverter.vcardToHashMap(splittedVCardString);
                        if (vcardList != null && vcardList.size() > 0) {
                            Map<String, Object> map = vcardList.get(0);
                            String publisherString = (String) map.get(CCConstants.VCARD_T_FN);

                            if (publisherString != null && !publisherString.trim().equals("")) {

                                //allow only 80 chars cause suggest box makes Problems with longer values
                                publisherString = publisherString.trim();
                                if (publisherString.length() > 80) {
                                    publisherString = publisherString.substring(0, 80);
                                }

                                //remove "," at the end(appears since we deactivated the punctuation in lucene analyzer)
                                if (publisherString.endsWith(",")) {
                                    publisherString = publisherString.substring(0, publisherString.length() - 1);
                                }

                                publisherString = publisherString.trim();

                                if (!publisherString.equals("") && !toSort.contains(publisherString) && !ignoreValuesList.contains(publisherString.trim())) {
                                    toSort.add(publisherString);
                                }

                            }
                        }
                    }
                }
            }

            Comparator<String> comp = new Comparator<String>() {

                public int compare(String o1, String o2) {

                    Collator collator = Collator.getInstance(Locale.GERMAN);
                    return collator.compare(o1, o2);
                }
            };
            Collections.sort(toSort, comp);

            for (String sortedVal : toSort) {
                Element key = document.createElement("key");
                key.appendChild(document.createTextNode(sortedVal));
                valueSpace.appendChild(key);
            }

            Source source = new DOMSource(document);

            // Write the DOM document to the file
            Transformer xformer = TransformerFactory.newInstance().newTransformer();
            xformer.transform(source, result);

        }

    }

    @Override
    public void refreshApplicationInfo() {
        RepoFactory.refresh();
    }

    private String getAppPropertiesApplications() throws Exception {
        String appRegistryfileName = PropertiesHelper.Config.getPropertyFilePath("ccapp-registry.properties.xml");
        Properties propsAppRegistry = PropertiesHelper.getProperties(appRegistryfileName, PropertiesHelper.XML);
        return propsAppRegistry.getProperty("applicationfiles");
    }

    private void changeAppPropertiesApplications(String newValue, String comment) throws Exception {
        String appRegistryfileName = PropertiesHelper.Config.getPropertyFilePath("ccapp-registry.properties.xml");

        Properties propsAppRegistry = PropertiesHelper.getProperties(appRegistryfileName, PropertiesHelper.XML);

        String pathAppRegistry = PropertiesHelper.Config.getAbsolutePathForConfigFile(appRegistryfileName);

        //backup
        propsAppRegistry.storeToXML(new FileOutputStream(new File(pathAppRegistry + System.currentTimeMillis() + ".bak")), " backup of registry");

        propsAppRegistry.setProperty("applicationfiles", newValue);
        //overwrite
        propsAppRegistry.storeToXML(new FileOutputStream(new File(pathAppRegistry)), comment);

    }

    @Override
    public void removeApplication(ApplicationInfo info) throws Exception {
        String[] split = getAppPropertiesApplications().split(",");
        List<String> apps = new ArrayList<>(Arrays.asList(split));
        int pos = apps.indexOf(info.getAppFileName());
        if (pos == -1)
            throw new Exception("AppInfo file " + info.getAppFile() + " was not found in registry");
        apps.remove(pos);
        String result = org.apache.commons.lang3.StringUtils.join(apps, ",");
        changeAppPropertiesApplications(result, new Date() + " removed app: " + info.getAppFile());
        File appFile = new File(PropertiesHelper.Config.getAbsolutePathForConfigFile(
                PropertiesHelper.Config.getPropertyFilePath(info.getAppFileName())
        ));
        appFile.renameTo(new File(appFile.getAbsolutePath() + System.currentTimeMillis() + ".bak"));
        RepoFactory.refresh();
    }

    @Override
    public HashMap<String, String> addApplication(String appMetadataUrl) throws Exception {

        HttpQueryTool httpQuery = new HttpQueryTool();
        String httpQueryResult = httpQuery.query(appMetadataUrl);
        if (httpQueryResult == null) {
            throw new CCException(null, "something went wrong. got no result for metadata url: " + appMetadataUrl);
        }

        InputStream is = new ByteArrayInputStream(httpQueryResult.getBytes("UTF-8"));
        return addApplicationFromStream(is);
    }

    @Override
    public HashMap<String, String> addApplicationFromStream(InputStream is) throws Exception {

        Properties props = new SortedProperties();
        props.loadFromXML(is);
        String appId = props.getProperty(ApplicationInfo.KEY_APPID);

        if (appId == null || appId.trim().equals("")) {
            throw new Exception("no appId found");
        }

        return storeProperties(appId, props);
    }

    public HashMap<String, String> addApplication(Map<String, String> properties) throws Exception {
        if (properties == null) {
            throw new Exception("no properties provided");
        }

        if (!properties.containsKey(ApplicationInfo.KEY_APPID)) {
            throw new Exception("no appid provided");
        }

        boolean fieldKnown = false;
        String unknownField = null;
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            for (Field f : ApplicationInfo.class.getFields()) {
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers())
                        && f.getName().startsWith("KEY_")) {
                    if (entry.getKey().equals(f.get(null))) {
                        fieldKnown = true;
                        unknownField = entry.getKey();
                    }
                }
            }
        }

        if (!fieldKnown) {
            throw new Exception("unknown field:" + unknownField);
        }

        String appId = properties.get(ApplicationInfo.KEY_APPID);

        Properties props = new Properties();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            if (entry.getValue() != null) {
                props.put(entry.getKey(), entry.getValue());
            }
        }

        return storeProperties(appId, props);
    }

    private HashMap<String, String> storeProperties(String appId, Properties props) throws Exception {
        String fileNamePart = appId.replaceAll("[/:]", "");
        String filename = "app-" + fileNamePart + ".properties.xml";

        //check if appID already exists
        if (ApplicationInfoList.getApplicationInfos().keySet().contains(appId)) {
            throw new Exception("appId is already in registry");
        }

        //check for mandatory Property type
        String type = props.getProperty(ApplicationInfo.KEY_TYPE);
        if (type == null || type.trim().equals("")) {
            throw new Exception("missing type");
        }

        if (type.equals(ApplicationInfo.TYPE_RENDERSERVICE)) {
            String contentUrl = props.getProperty("contenturl");
            if (contentUrl == null || contentUrl.trim().equals("")) {
                throw new Exception("a renderservice must have an contenturl");
            }

        }

        File appFile = new File(PropertiesHelper.Config.getAbsolutePathForConfigFile(PropertiesHelper.Config.getPropertyFilePath(filename)));
        if (!appFile.exists()) {
            props.storeToXML(new FileOutputStream(appFile), "");
        } else {
			throw new Exception("File "+appFile.getPath() + " already exists");
        }


		String existingFileList = getAppPropertiesApplications();

		if(existingFileList == null) {
			try {
				appFile.delete();
			} catch(Throwable t){
				logger.warn("Could not rollback app file " + appFile.getName(), t);
			}
			throw new Exception("AppList is currently empty. Please try again later");
		}

		String newProperty=existingFileList+","+filename;
        changeAppPropertiesApplications(newProperty, new Date() + " added file:" + filename);


        if (type.equals(ApplicationInfo.TYPE_RENDERSERVICE)) {

            String contentUrl = props.getProperty("contenturl");
            //String previewUrl = props.getProperty("previewurl");

            //store that in the homeApplication.properties.xml cause every repository has it's own renderservice
            //and we don't want to config an renderservice of an remote repository

            String homeAppFileName = PropertiesHelper.Config.getPropertyFilePath(AdminServiceFactory.HOME_APPLICATION_PROPERTIES);
            Properties homeAppProps = PropertiesHelper.getProperties(homeAppFileName, PropertiesHelper.XML);
            homeAppProps = new SortedProperties(homeAppProps);

            String homeAppPath = PropertiesHelper.Config.getAbsolutePathForConfigFile(homeAppFileName);

            //backup
            homeAppProps.storeToXML(new FileOutputStream(new File(homeAppPath + System.currentTimeMillis() + ".bak")), " backup of homeApplication.properties.xml");

            homeAppProps.setProperty(ApplicationInfo.KEY_CONTENTURL, contentUrl);
            //homeAppProps.setProperty(ApplicationInfo.KEY_PREVIEWURL, previewUrl);

            //overwrite
            homeAppProps.storeToXML(new FileOutputStream(new File(homeAppPath)), " added contenturl and preview url");
        }

        RepoFactory.refresh();

        HashMap<String, String> result = new HashMap<String, String>();
        for (Object key : props.keySet()) {
            result.put((String) key, props.getProperty((String) key));
        }
        return result;
    }

    @Override
    public String getPropertyToMDSXml(List<String> properties) throws Throwable {
        properties.replaceAll(NameSpaceTool::transformToLongQName);
        StringWriter writer = new StringWriter();
        Result result = new StreamResult(writer);
        writePropertyToMDSXml(result, properties.get(0));
        return writer.toString();
        // return getPublisherToMDSXml(properties,null, null, getAuthInfo());
    }

    @Override
    public List<ServerUpdateInfo> getServerUpdateInfos() {
        UpdaterService updaterService = ApplicationContextFactory.getApplicationContext().getBean(UpdaterService.class);
        return updaterService.getUpdateInfo().stream()
                .map((u) -> new ServerUpdateInfo(u.getId(), u.getDescription(), u.getOrder(), u.isAuto(), u.isTestable()))
                .peek((r) -> {
                    try {
                        Protocol protocol = ApplicationContextFactory.getApplicationContext().getBean(Protocol.class);
                        HashMap<String, Object> entry = protocol.getSysUpdateEntry(r.getId());
                        String date = (String) entry.get(CCConstants.CCM_PROP_SYSUPDATE_DATE);
                        r.setExecutedAt(Long.parseLong(date));
                    } catch (Throwable ignored) {

                    }
                }).collect(Collectors.toList());
    }

    @Override
    public String runUpdate(String updateId, boolean execute) throws Exception {
        UpdaterService updaterService = ApplicationContextFactory.getApplicationContext().getBean(UpdaterService.class);
        StringWriter result = new StringWriter();
        PrintWriter out = new PrintWriter(result);

        org.apache.logging.log4j.Logger logger = LogManager.getLogger();
        PrintWriterLogAppender appender = PrintWriterLogAppender.createAppender("PrintWriterLogAppender", out);
        appender.start();

        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        ThreadContext.put("logThreadId", Long.toString(Thread.currentThread().getId()));
        AbstractConfiguration config = (AbstractConfiguration) context.getConfiguration();

        config.addAppender(appender);

        LoggerConfig loggerConfig = config.getLoggerConfig(logger.getName());
        loggerConfig.addAppender(appender, null, null);
        context.updateLoggers();

        try {
            if (execute) {
                updaterService.runUpdate(updateId);
            } else {
                updaterService.testUpdate(updateId);
            }
            return result.toString();
        } finally {
            // Appender aus der Konfiguration entfernen (optional)
            appender.stop();
            config.removeAppender(appender.getName());
            loggerConfig.removeAppender(appender.getName());
            context.updateLoggers();
            ThreadContext.remove("logThreadId");
        }
    }

    @Override
    public void refreshEduGroupCache(boolean keepExisting) {
        if (keepExisting) {
            EduGroupCache.refreshByKeepExisting();
        } else {
            EduGroupCache.refresh();
        }
    }

    @Override
    public void testMail(String receiver, String template) {
        try {
            HashMap<String, String> dummy = new HashMap<>();
            dummy.put("link", URLHelper.getNgComponentsUrl(true) + "admin");
            dummy.put("link.static", URLHelper.getNgComponentsUrl(false) + "admin");
            MailTemplate.sendMail(receiver, template, dummy);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CacheInfo getCacheInfo(String name) {
        return CacheManagerFactory.getCacheInfo(name);
    }

    @Override
    public Map<Serializable, Serializable> getCacheEntries(String beanName) {
        Map<Serializable, Serializable> result = new HashMap<>();
        SimpleCache simpleCache = (SimpleCache) AlfAppContextGate.getApplicationContext().getBean(beanName);
        simpleCache.getKeys().forEach(k -> result.put((Serializable) k, (Serializable) simpleCache.get((Serializable) k)));
        return result;
    }

    @Override
    public void removeCacheEntry(Integer index, String beanName) {
        SimpleCache simpleCache = (SimpleCache) AlfAppContextGate.getApplicationContext().getBean(beanName);
        int idx = 0;
        Serializable keyToRemove = null;
        for (Object key : simpleCache.getKeys()) {
            if (idx == index) {
                keyToRemove = (Serializable) key;
            }
        }

        if (keyToRemove != null) {
            simpleCache.remove(keyToRemove);
        }
    }

    @Override
    public void clearCache(String beanName) {
        SimpleCache simpleCache = (SimpleCache) AlfAppContextGate.getApplicationContext().getBean(beanName);
        simpleCache.clear();
    }

    @Override
    public List<String> getCatalinaOut() throws IOException {
        List<String> result = new ArrayList<>();
        String path = System.getProperty("catalina.base");
        path += "/logs/catalina.out";
        int n_lines = 1000;
        ReversedLinesFileReader object = new ReversedLinesFileReader(new File(path));
        for (int i = 0; i < n_lines; i++) {
            String line = object.readLine();
            if (line == null)
                break;
            result.add(line);
        }
        return result;
    }

    @Override
    public CacheCluster getCacheCluster() {
        return CacheManagerFactory.getCacheCluster();
    }

    @Override
    public List<CacheCluster> getCacheClusters() {
        // TODO Auto-generated method stub
        return SystemStatistic.getAllRepoStates();
    }

    /*
     * Returns the number of active sessions from tomcat
     */
    @Override
    public int getActiveSessions() throws Exception {
        // https://stackoverflow.com/questions/4069444/getting-a-list-of-active-sessions-in-tomcat-using-java
        String context = Context.getCurrentInstance().getRequest().getSession().getServletContext().getContextPath();
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName objectName = new ObjectName("Catalina:type=Manager,context=" + context + ",host=localhost");
        Object activeSessions = mBeanServer.getAttribute(objectName, "activeSessions");
        System.out.println(activeSessions);
        return (Integer) activeSessions;
    }

    @Override
    public void applyTemplate(String template, String group, String folderId) throws Throwable {
        FolderTemplatesImpl ft = new FolderTemplatesImpl(new MCAlfrescoAPIClient());
        ft.setTemplate(template, group, folderId);
        List<String> slist = ft.getMessage();
        String error = slist.toString();
        if (slist.size() > 0 && !error.isEmpty())
            throw new Exception(error);
    }

    @Override
    public Collection<NodeRef> getActiveNodeLocks() {
        return EditLockServiceFactory.getEditLockService().getActiveLocks();
    }

    @Override
    public List<GlobalGroup> getGlobalGroups() throws Throwable {

        ArrayList<GlobalGroup> result = new ArrayList<GlobalGroup>();

        MCAlfrescoBaseClient mcAlfrescoBaseClient = new MCAlfrescoAPIClient();
        HashMap<String, HashMap<String, Object>> raw = mcAlfrescoBaseClient.search("TYPE:cm\\:authorityContainer AND @ccm\\:scopetype:\"global\"");

        for (Map.Entry<String, HashMap<String, Object>> entry : raw.entrySet()) {
            GlobalGroup group = new GlobalGroup();
            group.setName((String) entry.getValue().get(CCConstants.CM_PROP_AUTHORITY_AUTHORITYNAME));
            group.setDisplayName((String) entry.getValue().get(CCConstants.CM_PROP_AUTHORITY_AUTHORITYDISPLAYNAME));
            group.setNodeId((String) entry.getValue().get(CCConstants.SYS_PROP_NODE_UID));
            group.setAuthorityType(AuthorityType.getAuthorityType(group.getName()).name());
            group.setScope((String) entry.getValue().get(CCConstants.CCM_PROP_SCOPE_TYPE));
            group.setGroupType((String) entry.getValue().get(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE));
            result.add(group);
        }
        return result;
    }

    private HashMap<String, String> getAuthInfo() {
        return new AuthenticationToolAPI().getAuthentication(Context.getCurrentInstance().getRequest().getSession());
    }

    @Override
    public List<Class> getImporterClasses() throws Exception {
        Class[] importerBaseClass = new Class[]{
                org.edu_sharing.repository.server.jobs.quartz.ImporterJob.class,
                org.edu_sharing.repository.server.jobs.quartz.OAIXMLValidatorJob.class,
                org.edu_sharing.repository.server.jobs.quartz.ImporterJobSAX.class
        };
        return Arrays.asList(importerBaseClass);
    }

    /**
     * Import excel data and return the number of rows processed
     *
     * @param parent
     * @param csv
     * @param addToCollection
     * @return
     * @throws Exception
     */
    @Override
    public int importExcel(String parent, InputStream csv, Boolean addToCollection) throws Exception {
        return new ExcelLOMImporter(parent, csv, addToCollection).getRowCount();
    }

    @Override
    public String importOaiXml(InputStream xml, String recordHandlerClassName, String binaryHandlerClassName) throws Exception {
        HashMap<String, Object> paramsMap = new HashMap<>();
        if (recordHandlerClassName != null && !recordHandlerClassName.trim().equals("")) {
            paramsMap.put(OAIConst.PARAM_RECORDHANDLER, recordHandlerClassName);
        }
        if (binaryHandlerClassName != null && !binaryHandlerClassName.trim().equals("")) {
            paramsMap.put(OAIConst.PARAM_BINARYHANDLER, binaryHandlerClassName);
        }
        paramsMap.put(OAIConst.PARAM_USERNAME, AuthenticationUtil.getFullyAuthenticatedUser());
        paramsMap.put(OAIConst.PARAM_XMLDATA, StreamUtils.copyToByteArray(xml));
        paramsMap.put(OAIConst.PARAM_FORCE_UPDATE, true);
        return new ImporterJob().start(JobHandler.createJobDataMap(paramsMap));
    }

    @Override
    public void updateConfigFile(String filename, PropertiesHelper.Config.PathPrefix pathPrefix, String content) throws Throwable {
        filename = mapConfigFile(filename, pathPrefix);
        File file = new File(PropertiesHelper.Config.getAbsolutePathForConfigFile(filename));
        try {
            Files.copy(file, new File(file.getAbsolutePath() + System.currentTimeMillis() + ".bak"));
        } catch (FileNotFoundException e) {
            logger.info(e.getMessage());
            // not exists yet
        }
        FileUtils.write(file, content);
    }

    @Override
    public String getConfigFile(String filename, PropertiesHelper.Config.PathPrefix pathPrefix) throws Throwable {
        // use new File() to remove any folder like access
        filename = mapConfigFile(new File(filename).getName(), pathPrefix);
        File file = new File(PropertiesHelper.Config.getAbsolutePathForConfigFile(filename));
        return FileUtils.readFileToString(file);
    }

    private String mapConfigFile(String filename, PropertiesHelper.Config.PathPrefix pathPrefix) {
        return LightbendConfigLoader.getConfigFileLocation(filename, pathPrefix);
    }

    @Override
    public void importOai(String set, String fileUrl, String oaiBaseUrl, String metadataSetId, String metadataPrefix, String importerJobClassName, String importerClassName, String recordHandlerClassName, String binaryHandlerClassName, String persistentHandlerClassName, String oaiIds, boolean forceUpdate, String from, String until, String periodInDays) throws Exception {
        //new JobExecuter().start(ImporterJob.class, authInfo, setsParam.toArray(new String[setsParam.size()]));

        HashMap<String, Object> paramsMap = new HashMap<String, Object>();
        List<String> sets = new ArrayList(Arrays.asList(set.split(",")));
        if (fileUrl != null && !fileUrl.isEmpty() && !(fileUrl.startsWith("http://") || fileUrl.startsWith("https://")))
            throw new Exception("file url " + fileUrl + " is not a valid url");
        if (fileUrl != null && !fileUrl.isEmpty())
            sets.add(fileUrl);
        paramsMap.put(JobHandler.AUTH_INFO_KEY, getAuthInfo());
        paramsMap.put("sets", sets);
        paramsMap.put(OAIConst.PARAM_FORCE_UPDATE, forceUpdate);
        if (oaiBaseUrl != null && !oaiBaseUrl.trim().equals("")) {
            paramsMap.put(OAIConst.PARAM_OAI_BASE_URL, oaiBaseUrl);
        }
        if (metadataSetId != null && !metadataSetId.trim().equals("")) {
            paramsMap.put(OAIConst.PARAM_METADATASET_ID, metadataSetId);
        }

        //metadataPrefix
        if (metadataPrefix != null && !metadataPrefix.trim().equals("")) {
            paramsMap.put(OAIConst.PARAM_OAI_METADATA_PREFIX, metadataPrefix);
        }

        if (importerClassName != null && !importerClassName.trim().equals("")) {
            paramsMap.put(OAIConst.PARAM_IMPORTERCLASS, importerClassName);
        }

        if (recordHandlerClassName != null && !recordHandlerClassName.trim().equals("")) {
            paramsMap.put(OAIConst.PARAM_RECORDHANDLER, recordHandlerClassName);
        }
        if (binaryHandlerClassName != null && !binaryHandlerClassName.trim().equals("")) {
            paramsMap.put(OAIConst.PARAM_BINARYHANDLER, binaryHandlerClassName);
        }
        if (persistentHandlerClassName != null && !persistentHandlerClassName.trim().equals("")) {
            paramsMap.put(OAIConst.PARAM_PERSISTENTHANDLER, persistentHandlerClassName);
        }
        if (oaiIds != null && !oaiIds.trim().isEmpty()) {
            paramsMap.put(OAIConst.PARAM_OAI_IDS, oaiIds);
        }
        if (from != null && !from.trim().equals("")
                && until != null && !until.trim().equals("")) {
            paramsMap.put(OAIConst.PARAM_FROM, from);
            paramsMap.put(OAIConst.PARAM_UNTIL, until);
        } else if (periodInDays != null) {
            paramsMap.put(OAIConst.PARAM_PERIOD_IN_DAYS, periodInDays);
        }


        paramsMap.put(OAIConst.PARAM_USERNAME, AuthenticationUtil.getFullyAuthenticatedUser());

        Class importerClass = null;
        for (Class c : AdminServiceFactory.getInstance().getImporterClasses()) {
            if (c.getName().equals(importerJobClassName)) {
                importerClass = c;
                break;
            }
        }

        if (importerClass == null) {
            throw new Exception("no Importer Jobclass found for " + importerJobClassName);
        }

        ImmediateJobListener jobListener = JobHandler.getInstance().startJob(importerClass, paramsMap);
        if (jobListener.isVetoed()) {
            throw new Exception("job was vetoed by " + jobListener.getVetoBy());
        }
    }

    @Override
    public ImmediateJobListener startJob(String jobClass, HashMap<String, Object> params) throws Exception {

        if (params == null) {
            params = new HashMap<>();
        }
        if (!AuthenticationUtil.isRunAsUserTheSystemUser()) {
            params.put(OAIConst.PARAM_USERNAME, getAuthInfo().get(CCConstants.AUTH_USERNAME));
            params.put(JobHandler.AUTH_INFO_KEY, getAuthInfo());
        }

        Class job = Class.forName(jobClass);
        ImmediateJobListener jobListener = JobHandler.getInstance().startJob(job, params);
        if (jobListener != null && jobListener.isVetoed()) {
            throw new Exception("job was vetoed by " + jobListener.getVetoBy());
        }
        return jobListener;

    }

	@Override
	public Object startJobSync(String jobClass, HashMap<String,Object> params) throws Throwable {
		ImmediateJobListener listener = startJob(jobClass, params);
		while(true) {
			if(listener.wasExecuted()) {
				Optional<JobInfo> result = getJobs().stream().filter(job -> job.getStatus().equals(JobInfo.Status.Finished) && job.getJobClass().getName().equals(jobClass)).max((a, b) -> Long.compare(a.getFinishTime(), b.getFinishTime()));
				if(!result.isPresent()) {
					throw new IllegalStateException("Job status not found");
				}
				return result.get().getJobDataMap().get(JobHandler.KEY_RESULT_DATA);
			}
			if(listener.isVetoed()) {
				throw new Exception("job was vetoed by " + listener.getVetoBy());
			}
			Thread.sleep(1000);
		}
	}

    @Override
    public void startCacheRefreshingJob(String folderId, boolean sticky) throws Exception {
        HashMap<String, Object> paramsMap = new HashMap<String, Object>();
        paramsMap.put("rootFolderId", folderId);
        paramsMap.put("sticky", sticky + "");
        paramsMap.put(JobHandler.AUTH_INFO_KEY, getAuthInfo());
        ImmediateJobListener jobListener = JobHandler.getInstance().startJob(org.edu_sharing.repository.server.jobs.quartz.RefreshCacheJob.class, paramsMap);

        if (jobListener.isVetoed()) {
            throw new Exception("job was vetoed by " + jobListener.getVetoBy());
        }
    }

    @Override
    public void removeDeletedImports(String oaiBaseUrl, String cataloges, String oaiMetadataPrefix) throws Exception {
        HashMap<String, Object> paramsMap = new HashMap<String, Object>();
        paramsMap.put(JobHandler.AUTH_INFO_KEY, getAuthInfo());
        paramsMap.put(OAIConst.PARAM_OAI_BASE_URL, oaiBaseUrl);
        paramsMap.put(OAIConst.PARAM_OAI_SETS, cataloges);
        paramsMap.put(OAIConst.PARAM_OAI_METADATA_PREFIX, oaiMetadataPrefix);
        paramsMap.put(RemoveDeletedImportsFromSetJob.PARAM_TESTMODE, "false");

        ImmediateJobListener jobListener = JobHandler.getInstance().startJob(org.edu_sharing.repository.server.jobs.quartz.RemoveDeletedImportsFromSetJob.class, paramsMap);

        if (jobListener.isVetoed()) {
            throw new Exception("job was vetoed by " + jobListener.getVetoBy());
        }
    }

    @Override
    public Properties getPropertiesXML(String xmlFile) throws Exception {
        Properties homeAppProps = PropertiesHelper.getProperties(PropertiesHelper.Config.getPropertyFilePath(xmlFile), PropertiesHelper.XML);
        return homeAppProps;
    }

    @Override
    public void updatePropertiesXML(String xmlFile, Map<String, String> properties) throws Exception {
        File appFile = new File(PropertiesHelper.Config.getAbsolutePathForConfigFile(
                PropertiesHelper.Config.getPropertyFilePath(xmlFile)
        ));
        Files.copy(appFile, new File(appFile.getAbsolutePath() + "_" + System.currentTimeMillis() + ".bak"));
        for (String key : properties.keySet()) {
            PropertiesHelper.setProperty(key, properties.get(key), appFile.getAbsolutePath(), PropertiesHelper.XML);
        }
        RepoFactory.refresh();
    }

    public void exportLom(String filterQuery, String targetDir, boolean subobjectHandler) throws Exception {
        HashMap<String, Object> paramsMap = new HashMap<String, Object>();
        paramsMap.put(ExporterJob.PARAM_LUCENE_FILTER, filterQuery);
        paramsMap.put(ExporterJob.PARAM_OUTPUT_DIR, targetDir);
        paramsMap.put(ExporterJob.PARAM_WITH_SUBOBJECTS, new Boolean(subobjectHandler).toString());
        paramsMap.put(JobHandler.AUTH_INFO_KEY, getAuthInfo());
        ImmediateJobListener jobListener = JobHandler.getInstance().startJob(ExporterJob.class, paramsMap);
        if (jobListener.isVetoed()) {
            throw new Exception("job was vetoed by " + jobListener.getVetoBy());
        }
    }

    @Override
    public int importCollections(String parent, InputStream is) throws Throwable {
        return new CollectionImporter().importFile(parent, is);
    }

    @Override
    public String uploadTemp(String name, InputStream is) throws Exception {
        String TMP_DIR = System.getProperty("java.io.tmpdir");
        File upload = new File(TMP_DIR, new File(name).getName());
        if (upload.exists()) {
            throw new IllegalArgumentException("file " + upload.getAbsolutePath() + " already exists");
        }
        FileUtils.copyInputStreamToFile(is, upload);
        return upload.getAbsolutePath();
    }

    @Override
    public List<JobDescription> getJobDescriptions(boolean fetchAbstractJobs) {

        List<JobDescription> result = new ArrayList<>();

        List<Class> jobClasses = ClassHelper.getSubclasses(AbstractJob.class);

        for (Class clazz : jobClasses) {
            if (!fetchAbstractJobs) {
                if (Modifier.isAbstract(clazz.getModifiers())) {
                    continue;
                }
            }
            JobDescription desc = new JobDescription();
            desc.setName(clazz.getName());
            if (clazz.isAnnotationPresent(org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription.class)) {
                org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription annotationDesc = (org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription) clazz.getAnnotation(org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription.class);
                desc.setDescription(annotationDesc.description());
                desc.setTags(annotationDesc.tags());
            }
            desc.setParams(Arrays.stream(clazz.getDeclaredFields()).filter(
                    (f) -> f.isAnnotationPresent(JobFieldDescription.class)
            ).map((f) -> {
                JobDescription.JobFieldDescription fieldDesc = new JobDescription.JobFieldDescription();
                fieldDesc.setName(f.getName());
                boolean isArray = f.getType().isAssignableFrom(Collection.class) || f.getType().equals(List.class);
				Class<?> type;
				if(isArray) {
					Type subtype = ((ParameterizedType) f.getGenericType()).getActualTypeArguments()[0];
					if (subtype instanceof ParameterizedType) {
						subtype = ((ParameterizedType) subtype).getActualTypeArguments()[0];
					}
					 type = (Class<?>) subtype;
				} else {
					type = f.getType();
				}
                fieldDesc.setType(type);
                fieldDesc.setIsArray(isArray);
                fieldDesc.setDescription(f.getAnnotation(JobFieldDescription.class).description());
                fieldDesc.setSampleValue(f.getAnnotation(JobFieldDescription.class).sampleValue());
                fieldDesc.setFile(f.getAnnotation(JobFieldDescription.class).file());
                if (type.isEnum()) {
                    fieldDesc.setValues(Arrays.stream(type.getDeclaredFields()).
                            filter((v) -> !v.getName().startsWith("$")).
                            map((v) -> {
                                JobDescription.JobFieldDescription value = new JobDescription.JobFieldDescription();
                                value.setName(v.getName());
                                if (v.isAnnotationPresent(JobFieldDescription.class)) {
                                    value.setDescription(v.getAnnotation(JobFieldDescription.class).description());
                                }
                                return value;
                            }).collect(Collectors.toList()));
                }
                return fieldDesc;
            }).collect(Collectors.toList()));
            result.add(desc);
        }
        return result;
    }

    @Override
    public void switchAuthentication(String authorityName) {
        HttpSession session = Context.getCurrentInstance().getRequest().getSession(true);
        //session.setMaxInactiveInterval(30);
        AuthenticationToolAPI authTool = new AuthenticationToolAPI();
        String ticket = authTool.setUser(authorityName);
        authTool.storeAuthInfoInSession(
                authorityName,
                ticket,
                CCConstants.AUTH_TYPE_OAUTH,
                session);
    }

    @Override
    public Object getLightbendConfig() {
        return LightbendConfigLoader.get().root().unwrapped();
    }

    @Override
    public Collection<PluginStatus> getPlugins() {
        ModuleService moduleService = (ModuleService) applicationContext.getBean("moduleService");
        return moduleService.getAllModules().stream().map(m ->
                new PluginStatus(m.getTitle(), m.getModuleVersionNumber().toString(), m.getInstallState().equals(ModuleInstallState.INSTALLED))
        ).collect(Collectors.toList());
    }

    @Override
    public Map<String, RepositoryVersionInfo> getVersions() {
        try {
            Map<String, RepositoryVersionInfo> result = new HashMap<>(VersionService.getRepositoryVersionInfo());
            try {
                RepositoryVersionInfo renderingVersionInfo = new RepositoryVersionInfo();
                String renderserviceVersion = VersionService.getRenderserviceVersion();
                if(StringUtils.isNotBlank(renderserviceVersion)) {
                    renderingVersionInfo.version = new RepositoryVersionInfo.Version();
                    String[] versionInfos = renderserviceVersion.split("\\.");
                    renderingVersionInfo.version.major = versionInfos[0];
                    renderingVersionInfo.version.minor = versionInfos[1];
                    renderingVersionInfo.version.patch = versionInfos[2];
                    renderingVersionInfo.version.full = renderserviceVersion;
                    renderingVersionInfo.repository="service-rendering-service";
                    result.put(renderingVersionInfo.repository,renderingVersionInfo);
                }
            } catch (Exception e) {
                logger.warn(e.getMessage(), e);
            }
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
