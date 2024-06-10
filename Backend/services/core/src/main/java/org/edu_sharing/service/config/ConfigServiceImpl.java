package org.edu_sharing.service.config;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import lombok.RequiredArgsConstructor;
import org.alfresco.repo.cache.SimpleCache;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.rpc.ACE;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.PropertiesHelper;
import org.edu_sharing.repository.server.tools.UserEnvironmentTool;
import org.edu_sharing.restservices.shared.Authority;
import org.edu_sharing.service.NotAnAdminException;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.alfresco.service.config.model.Config;
import org.edu_sharing.alfresco.service.config.model.Context;
import org.edu_sharing.alfresco.service.config.model.KeyValuePair;
import org.edu_sharing.alfresco.service.config.model.Language;
import org.edu_sharing.alfresco.service.config.model.Values;
import org.edu_sharing.alfresco.service.config.model.Variables;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.permission.PermissionService;
import org.edu_sharing.spring.scope.refresh.RefreshScopeRefreshedEvent;
import org.json.JSONObject;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class ConfigServiceImpl implements ConfigService, ApplicationListener<RefreshScopeRefreshedEvent> {
	private static Logger logger=Logger.getLogger(ConfigServiceImpl.class);
	private static String CACHE_KEY = "CLIENT_CONFIG";
	// we use a non-serializable Config as value because this is a local cache and not distributed
	private static SimpleCache<String, Config> configCache = (SimpleCache) AlfAppContextGate.getApplicationContext().getBean("eduSharingConfigCache");

	private static final Unmarshaller jaxbUnmarshaller;

	private final NodeService nodeService;
	private final PermissionService permissionService;

	private static final ObjectReader jacksonReader;

	static{
		Unmarshaller jaxbUnmarshaller1;
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(Config.class);
			jaxbUnmarshaller1 = jaxbContext.createUnmarshaller();
		} catch (JAXBException e) {
			jaxbUnmarshaller1 = null;
			logger.error(e.getMessage(),e);
		}
		jaxbUnmarshaller = jaxbUnmarshaller1;
		jacksonReader = new ObjectMapper().reader(Config.class);
	}

	/*
	XPathFactory pfactory = XPathFactory.newInstance();
	XPath xpath = pfactory.newXPath();
	private Document doc;
	private DocumentBuilder builder;
	private JSONObject json;
	*/

	/*
	public JSONObject getAsJson(String contextId) throws Exception {
		JSONObject copy = new JSONObject(json, JSONObject.getNames(json));
		copy.getJSONObject("config").remove("contexts");
		System.out.println(copy.toString());

		NodeList nodeMetadataSet = (NodeList) xpath.evaluate("/config/contexts/context", doc, XPathConstants.NODESET);
		for(int i=0;i<nodeMetadataSet.getLength();i++) {
			Node node=nodeMetadataSet.item(i);
			NodeList childs=node.getChildNodes();
			Node config=null;
			for(int j=0;j<childs.getLength();j++) {
				String name=childs.item(j).getNodeName();
				if(name.equals("id")){
					
				}
				if(name.equals("config")) {
					config=childs.item(j);
				}
			}
			if(config!=null) {
				overrideJson(copy,config);
			}
		}
		
		return copy;
	}
	*/
	@Override
	public Config getConfig() throws Exception {
	    if(!"true".equalsIgnoreCase(ApplicationInfoList.getHomeRepository().getDevmode()) && configCache.getKeys().contains(CACHE_KEY)) {
			return configCache.get(CACHE_KEY);
			//return new Gson().fromJson(configCache.get(CACHE_KEY), Config.class);
			//return SerializationUtils.clone((Config) configCache.get(CACHE_KEY));
		}
		InputStream is = getConfigInputStream();
		Config config;
		synchronized (jaxbUnmarshaller) {
            config = (Config) jaxbUnmarshaller.unmarshal(is);
        }
        is.close();
		configCache.put(CACHE_KEY,config);
		return config;
	}

	private InputStream getConfigInputStream() throws IOException {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		String file = LightbendConfigLoader.getConfigFileLocation(PropertiesHelper.Config.CONFIG_FILENAME, PropertiesHelper.Config.PathPrefix.DEFAULTS);
		InputStream is = classLoader.getResourceAsStream(file);
		if(is==null)
			throw new IOException(file + " missing");
		return is;
	}

	@Override
	public Context getContext(String domain) throws Exception {
		Config config=getConfig();
		if(config.contexts!=null && config.contexts.context!=null) {
			for (Context context : config.contexts.context) {
				if (context.domain == null)
					continue;
				for (String cd : context.domain) {
					if (cd.equals(domain)) {
						return context;
					}
				}
			}
		}
		return null;
	}
	@Override
	public Config getConfigByDomain(String domain) throws Exception {
		Context context=getContext(domain);
		if(context == null) {
			throw new IllegalArgumentException("Context with domain "+ domain +" does not exists");
		}
		return getConfigByContext( context);

	}

	@Override
	public Config getConfigByContext(Context context) throws Exception {
		if(!"true".equalsIgnoreCase(ApplicationInfoList.getHomeRepository().getDevmode()) && configCache.getKeys().contains(CACHE_KEY + "_" + context)) {
			return configCache.get(CACHE_KEY + "_" + context);
		}
		Config config=getConfig().deepCopy();
		overrideValues(config.values, context.values);
		if(context.language!=null)
			config.language = overrideLanguage(config.language, context.language);
		if(context.variables!=null)
			config.variables = overrideVariables(config.variables, context.variables);
		configCache.put(CACHE_KEY + "_" + context, config);
		return config;
	}

	@Override
	public DynamicConfig setDynamicValue(String key, boolean readPublic, JSONObject object) throws Throwable {
		if(!AuthorityServiceFactory.getLocalService().isGlobalAdmin()){
			throw new NotAnAdminException();
		}
		String folder = new UserEnvironmentTool().getEdu_SharingConfigFolder();
		String nodeId;
		try {
			NodeRef child = nodeService.getChild(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,folder,CCConstants.CCM_TYPE_CONFIGOBJECT, CCConstants.CM_NAME, key);
			nodeId = child.getId();
		}catch(Throwable t){
			// does not exists -> we will create a new node
			Map<String, String[]> props=new HashMap<>();
			props.put(CCConstants.CM_NAME,new String[]{key});
			nodeId=nodeService.createNode(folder,CCConstants.CCM_TYPE_CONFIGOBJECT,props);
		}

		Map<String, Object> props=new HashMap<>();
		props.put(CCConstants.CCM_PROP_CONFIGOBJECT_VALUE,object.toString());
		nodeService.updateNodeNative(nodeId,props);
		DynamicConfig result=new DynamicConfig();
		result.setNodeId(nodeId);
		result.setValue(object.toString());
		List<ACE> aces = new ArrayList<>();
		if(readPublic){
			ACE ace=new ACE();
			ace.setAuthority(CCConstants.AUTHORITY_GROUP_EVERYONE);
			ace.setAuthorityType(Authority.Type.EVERYONE.name());
			ace.setPermission(CCConstants.PERMISSION_CONSUMER);
			aces.add(ace);
		}
		permissionService.setPermissions(nodeId,aces,false);
		return result;
	}

	@Override
	public DynamicConfig getDynamicValue(String key) throws Throwable {
		String folder = AuthenticationUtil.runAsSystem(()-> {
			try {
				return new UserEnvironmentTool().getEdu_SharingConfigFolder();
			} catch (Throwable throwable) {
				return null;
			}
		});
		NodeRef child = nodeService.getChild(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,folder,CCConstants.CCM_TYPE_CONFIGOBJECT, CCConstants.CM_NAME, key);
		if(child==null)
			throw new IllegalArgumentException(key);
		String value=NodeServiceHelper.getProperty(child,CCConstants.CCM_PROP_CONFIGOBJECT_VALUE);
		DynamicConfig result=new DynamicConfig();
		result.setNodeId(child.getId());
		result.setValue(value);
		return result;
	}

	private Variables overrideVariables(Variables values, Variables override) {
		if(values==null)
			return override;
		if(override==null)
			return values;
		overrideList(values.variable,override.variable);	
		return values;
	}
	private void overrideList(List<KeyValuePair> list, List<KeyValuePair> override) {
		for(KeyValuePair obj : override) {
			if(list.contains(obj)) {
				list.remove(obj);
			}
			list.add(obj);
		}
	}
	private List<Language> overrideLanguage(List<Language> values, List<Language> override) {
		if(values==null)
			return override;
		if(override==null)
			return values;
		for(Language language : override) {
			for(Language language2 : values) {
				if(language.language.equals(language2.language)) {
					overrideList(language2.string,language.string);
				}
			}
		}
		return values;
	}
	private void overrideValues(Values values, Values override) throws IllegalArgumentException, IllegalAccessException {
		Class<?> c = override.getClass();
		Field[] fields = c.getDeclaredFields();
		for(Field field : fields) {
			if(field.get(override)!=null)
				field.set(values, field.get(override));
		}
		
	}

	private void refresh() {
		configCache.remove(CACHE_KEY);
		try {
			getConfig();
		} catch (Exception e) {
			logger.error("error refreshing client config: "+e.getMessage(),e);
		}
	}

	@Override
	public void onApplicationEvent(RefreshScopeRefreshedEvent event) {
		refresh();
	}


	/**
	 * override the default json with all the json from the xml
	 * @param copy
	 * @param config
	 * @throws TransformerException 
	 * @throws JSONException 
	 */
	/*
	private void overrideJson(JSONObject copy, Node config) throws Exception {
		JSONObject jsonOverride = XML.toJSONObject(nodeToString(config));
		String[] names=JSONObject.getNames(jsonOverride);
		for(String name : names) {
			System.out.println(name);
			copy.put(name, jsonOverride.get(name));
		}
	}
	
	// https://stackoverflow.com/questions/6534182/java-geting-all-content-of-a-xml-node-as-string
	private static String nodeToString(Node node) throws TransformerException {
		StringWriter sw = new StringWriter();
	    Transformer t = TransformerFactory.newInstance().newTransformer();
	    //t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
	    t.transform(new DOMSource(node), new StreamResult(sw));
		 
		return sw.toString();
	}
	*/
}
