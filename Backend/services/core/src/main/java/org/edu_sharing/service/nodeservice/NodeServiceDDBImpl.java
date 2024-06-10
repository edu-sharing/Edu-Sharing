package org.edu_sharing.service.nodeservice;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.forms.VCardTool;
import org.edu_sharing.repository.client.tools.metadata.ValueTool;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.search.SearchServiceDDBImpl;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class NodeServiceDDBImpl extends NodeServiceAdapterCached{
	Logger logger = Logger.getLogger(NodeServiceDDBImpl.class);

	private final String APIKey;
	private final XPathFactory pfactory = XPathFactory.newInstance();
	private final XPath xpath = pfactory.newXPath();

	private final Map<String, String> LICENSE_MAPPINGS = new HashMap<>(){{
		put("http://creativecommons.org/publicdomain/zero/1.0/", CCConstants.COMMON_LICENSE_CC_ZERO);
		put("http://creativecommons.org/licenses/by/3.0/", CCConstants.COMMON_LICENSE_CC_BY);
		put("http://creativecommons.org/licenses/by-sa/4.0/", CCConstants.COMMON_LICENSE_CC_BY_SA);
		put("http://creativecommons.org/licenses/by-nc-nd/4.0/", CCConstants.COMMON_LICENSE_CC_BY_NC_ND);
		put("http://rightsstatements.org/vocab/InC/1.0/", CCConstants.LICENSE_COPYRIGHT_LICENSE);
		put("http://creativecommons.org/publicdomain/mark/1.0/", CCConstants.LICENSE_PDM);
	}};
	public NodeServiceDDBImpl(String appId) {
		super(appId);
		ApplicationInfo appInfo = ApplicationInfoList.getRepositoryInfoById(appId);
		this.appId = appId;
		this.APIKey = appInfo.getApiKey();
	}

	@Override
	public Map<String, Object> getProperties(String storeProtocol, String storeId, String nodeId) throws Throwable {
		Map<String, Object> props = super.getProperties(storeProtocol, storeId, nodeId);
		if(props != null) {
			return props;
		}

		Map<String,Object> properties = new HashMap<>();
		properties.put(CCConstants.SYS_PROP_NODE_UID,nodeId);
		String url = "https://www.deutsche-digitale-bibliothek.de/item/"+nodeId;
		properties.put(CCConstants.LOM_PROP_TECHNICAL_LOCATION, url);
		properties.put(CCConstants.CCM_PROP_IO_WWWURL, url);
		properties.put(CCConstants.CCM_PROP_IO_REPLICATIONSOURCE, "ddb");
		try{
			// fetch binary info
			String all = SearchServiceDDBImpl.httpGet(SearchServiceDDBImpl.DDB_API+"/items/"+nodeId+"?oauth_consumer_key=" + URLEncoder.encode(this.APIKey, "UTF-8"), new HashMap<>() {{
				put("Accept", "application/xml");
				put("Content-Type", "application/xml");
			}});
			DocumentBuilderFactory factory =
					DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new ByteArrayInputStream(all.getBytes()));
			properties.put(
					CCConstants.CM_NAME,
					getXPathSingleValue(doc, "/cortex/edm/RDF/ProvidedCHO/title")
			);
			properties.put(
					CCConstants.LOM_PROP_GENERAL_TITLE, properties.get(CCConstants.CM_NAME)
			);
			properties.put(
					CCConstants.CM_PROP_C_CREATOR,
					getXPathSingleValue(doc, "/cortex/edm/RDF/ProvidedCHO/creator")
			);
			properties.put(
					CCConstants.CM_PROP_C_MODIFIED,getLastModified(doc));
			properties.put(
					CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_PUBLISHER,
					// fix after DESP-738
					ValueTool.toMultivalue(getXPathArray(doc, Collections.singleton("/cortex/indexing-profile/facet[@name='provider_fct']")).stream().map(
							VCardTool::nameToVCard
					).toArray(String[]::new))
			);
			properties.put(
					CCConstants.LOM_PROP_GENERAL_DESCRIPTION,
					StringUtils.join(getXPathArray(doc, Collections.singleton("/cortex/edm/RDF/ProvidedCHO/description")), "\n")
			);
			properties.put(
					CCConstants.LOM_PROP_GENERAL_KEYWORD,
					// fix after DESP-738
					ValueTool.toMultivalue(
							getXPathArray(doc, Arrays.asList(
									"/cortex/indexing-profile/facet[@name='keywords_fct']/value",
									"/cortex/indexing-profile/facet[@name='topic_fct']/value"
							)).stream().distinct().toArray(String[]::new)
					)
			);
			properties.put(CCConstants.CCM_PROP_IO_THUMBNAILURL,
					getThumbnail(doc)
			);
			properties.put(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY,
					getLicense(doc)
			);
		} catch (Throwable t) {
		}

		updateCache(properties);
		return properties;
	}

	private Date getLastModified(Document doc) throws XPathExpressionException, ParseException {
		String date = getXPathSingleValue(doc, "/cortex/indexing-profile/facet[@name='last_update']");
		if (date != null) {
			return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(date);
		}
		return null;
	}
	private String getThumbnail(Document doc) throws XPathExpressionException {
		Node node = (org.w3c.dom.Node) xpath.evaluate("cortex/preview/thumbnail", doc, XPathConstants.NODE);
		return "";
	}

	private Object getLicense(Document doc) throws XPathExpressionException {
		try {
			Node rights = (Node) xpath.evaluate("cortex/edm/RDF/Aggregation/rights", doc, XPathConstants.NODE);
			String licenseUri = rights.getAttributes().getNamedItem("ns2:resource").getNodeValue();
			return LICENSE_MAPPINGS.get(licenseUri);
		}catch(Throwable t) {
			return null;
		}
	}

	private String getXPathSingleValue(Document doc, String path) throws XPathExpressionException {
		return (String) xpath.evaluate(path, doc, XPathConstants.STRING);
	}
	private List<String> getXPathArray(Document doc, Collection<String> path) throws XPathExpressionException {
		return path.stream().map(
				p -> {
					try {
						NodeList nodeList = (NodeList) xpath.evaluate(p, doc, XPathConstants.NODESET);
						List<String> result = new ArrayList<>(nodeList.getLength());
						for (int index = 0; index < nodeList.getLength(); index++) {
							result.add(nodeList.item(index).getTextContent());
						}
						return result;
					} catch (XPathExpressionException e) {
						throw new RuntimeException(e);
					}
				}
		).collect(ArrayList::new, List::addAll, List::addAll);
	}

	private String getLanguageString(JSONObject meta, String field) throws JSONException {
		try {
			return meta.getJSONArray(field).getJSONObject(0).getString("$");
		}catch (Throwable t1) {
			try {
				return meta.getJSONObject(field).getString("$");
			} catch (Throwable t2) {
				return meta.getString(field);
			}
		}
	}
	@Override
	public String getType(String nodeId) {
		return CCConstants.CCM_TYPE_IO;
	}

	@Override
	public InputStream getContent(String nodeId) throws Throwable {
		return null;
	}
}
