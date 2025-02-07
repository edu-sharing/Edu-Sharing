package org.edu_sharing.metadataset.v2;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.cache.SimpleCache;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.metadataset.v2.MetadataCondition.CONDITION_TYPE;
import org.edu_sharing.metadataset.v2.valuespace_reader.ValuespaceReader;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.PropertiesHelper;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class MetadataReader {

    public static final String GENDER_SEPARATOR = "*";
    public static final String SUGGESTION_SOURCE_SEARCH = "Search";
    public static final String SUGGESTION_SOURCE_MDS = "Mds";
    public static final String SUGGESTION_SOURCE_SQL = "Sql";
    private static final String DEFAULT_QUERY_SYNTAX = MetadataReader.QUERY_SYNTAX_LUCENE;
    public static final String QUERY_SYNTAX_LUCENE = "lucene";
    public static final String QUERY_SYNTAX_DSL = "dsl";
    public static final String NONE = "none";
    @SuppressWarnings("unchecked")
    private static final SimpleCache<String, MetadataSet> mdsCache = (SimpleCache<String, MetadataSet>) AlfAppContextGate.getApplicationContext().getBean("eduSharingMdsCache");
    XPathFactory pfactory = XPathFactory.newInstance();
    XPath xpath = pfactory.newXPath();
    private final Document doc;
    private final DocumentBuilder builder;
    private String i18nPath;
    private final String locale;

    public static void main(String[] args) throws Exception {
        MetadataSet mds = MetadataReader.getMetadataset(ApplicationInfoList.getHomeRepository(), CCConstants.metadatasetdefault_id, "de_DE");
        System.out.print(mds);
    }

    public static String getPath() {
        return PropertiesHelper.Config.PATH_CONFIG + PropertiesHelper.Config.PathPrefix.DEFAULTS_METADATASETS + "/";
    }

    public static Collection<MetadataWidget> getWidgetsByNode(NodeRef node, String locale) throws Exception {
        ApplicationContext alfApplicationContext = AlfAppContextGate.getApplicationContext();
        ServiceRegistry serviceRegistry = (ServiceRegistry) alfApplicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
        Object mdsSet = serviceRegistry.getNodeService().getProperty(node, QName.createQName(CCConstants.CM_PROP_METADATASET_EDU_METADATASET));
        if (mdsSet == null || mdsSet.toString().isEmpty()) {
            mdsSet = CCConstants.metadatasetdefault_id;
        }
        MetadataSet metadata = MetadataReader.getMetadataset(ApplicationInfoList.getHomeRepository(), (String) mdsSet, locale);
        return metadata.getWidgetsByNode(serviceRegistry.getNodeService().getType(node).toString(),
                serviceRegistry.getNodeService().getAspects(node).stream().map(QName::toString).collect(Collectors.toList()),
                true);
    }

    public static MetadataSet getMetadataset(ApplicationInfo appId, String mdsSet, String locale) throws Exception {
        return getMetadataset(appId, mdsSet, locale, true);
    }

    public static MetadataSet getMetadataset(ApplicationInfo appId, String mdsSet, String locale, boolean checkConfigurationState) throws Exception {
        MetadataReader reader;
        MetadataSet mds;
        String mdsNameDefault = "mds";
        if (appId.getMetadatsets() != null) {
            mdsNameDefault = appId.getMetadatsets()[0];
            if (mdsNameDefault.toLowerCase().endsWith(".xml")) {
                mdsNameDefault = mdsNameDefault.substring(0, mdsNameDefault.length() - 4);
            }
        }

        String mdsName = mdsNameDefault;
        if (!mdsSet.equals("-default-") && !mdsSet.equals(CCConstants.metadatasetdefault_id)) {
            if (checkConfigurationState) {

                if (ApplicationInfoList.getApplicationInfos().values().stream().map(ApplicationInfo::getMetadatsets).
                        anyMatch((a) -> Arrays.asList(a).contains(mdsSet))) {
                    mdsName = mdsSet;
                    if (mdsName.toLowerCase().endsWith(".xml")) {
                        mdsName = mdsName.substring(0, mdsName.length() - 4);
                    }
                } else {
                    throw new IllegalArgumentException("Invalid mds set " + mdsSet + ", was not found in any app");
                }
            } else {
                mdsName = mdsSet;
            }
        }
        try {
            String id = appId.getAppId() + "_" + mdsName + "_" + locale;
            if (mdsCache.getKeys().contains(id) && !"true".equalsIgnoreCase(ApplicationInfoList.getHomeRepository().getDevmode())) {
                return mdsCache.get(id);
            }

            reader = new MetadataReader(mdsNameDefault + ".xml", locale);
            mds = reader.getMetadatasetForFile(mdsNameDefault);
            mds.setRepositoryId(appId.getAppId());
            if (mds.getInherit() != null && !mds.getInherit().isEmpty()) {
                String inheritName = mds.getInherit() + ".xml";
                reader = new MetadataReader(inheritName, locale);
                MetadataSet mdsInherit = reader.getMetadatasetForFile(inheritName);
                try {
                    reader = new MetadataReader(mds.getInherit() + "_override.xml", locale);
                    MetadataSet mdsOverride = reader.getMetadatasetForFile(inheritName);
                    mdsInherit.overrideWith(mdsOverride);
                } catch (IOException ignored) {
                }
                mdsInherit.overrideWith(mds);
                mds = mdsInherit;
            }
            if (!mdsName.equals(mdsNameDefault)) {
                reader = new MetadataReader(mdsName + ".xml", locale);
                MetadataSet mdsOverride = reader.getMetadatasetForFile(mdsName);
                if (mdsOverride.getInherit() != null && !mdsOverride.getInherit().isEmpty()) {
                    log.info("Mds " + mdsName + " is going to inherit data from mds " + mdsOverride.getInherit());
                    if (mdsOverride.getInherit().equals(mdsName)) {
                        throw new RuntimeException("Detected cyclic dependency in your mds inherition. Please check your mds " + mdsName);
                    }
                    mds = SerializationUtils.clone(getMetadataset(appId, mdsOverride.getInherit(), locale, false));
                    mds.overrideWith(mdsOverride);
                } else {
                    // fallback for backward compatibility: use the default mds for inherition
                    log.info("Mds {} is going to inherit data from the default mds (fallback)", mdsName);
                    mds.overrideWith(mdsOverride);
                }
            }
            try {
                reader = new MetadataReader(mdsName + "_override.xml", locale);
                MetadataSet mdsOverride = reader.getMetadatasetForFile(mdsName);
                mds.overrideWith(mdsOverride);
            } catch (org.apache.http.conn.ConnectTimeoutException e) {
                log.error(e.getMessage(), e);
            } catch (IOException e) {
                if (e.toString().contains(mdsName)) {
                    log.info("no {}_override.xml was found -> only default file will be used", mdsName);
                } else {
                    log.error("IOException parsing {}_override.xml:{}", mdsName, e, e);
                }
            }
            mdsCache.put(id, mds);
            return mds;
        } catch (Throwable t) {
            throw new RuntimeException("Unexpected error while parsing metadataset " + mdsSet + ", isDefault " + (mdsName.equals(mdsNameDefault)), t);
        }
    }

    private void handleQueryCondition(Node node, MetadataQueryBase query) {
        MetadataQueryCondition result = new MetadataQueryCondition();
        String id = "<main>";
        if (query instanceof MetadataQuery) {
            id = ((MetadataQuery) query).getId();
        }
        for (int i = 0; i < node.getChildNodes().getLength(); i++) {
            Node condition = node.getChildNodes().item(i);
            String name = condition.getNodeName();
            String value = condition.getTextContent();
            switch (name) {
                case "condition":
                    result.setCondition(getCondition(condition, id));
                    break;
                case "true":
                    result.setQueryTrue(value);
                    break;
                case "false":
                    result.setQueryFalse(value);
                    break;
            }
        }
        query.addCondition(result);
    }

    private Map<String, MetadataQueries> getQueries(MetadataSet mds) throws Exception {
        Map<String, MetadataQueries> result = new HashMap<>();
        NodeList queryNodes = (NodeList) xpath.evaluate("/metadataset/queries", doc, XPathConstants.NODESET);
        for (int a = 0; a < queryNodes.getLength(); a++) {
            MetadataQueries entry = new MetadataQueries();
            Node queryNode = queryNodes.item(a);
            if (queryNode == null) {
                continue;
            }

            NamedNodeMap attr = queryNode.getAttributes();
            Node syntax = attr.getNamedItem("syntax");
            String syntaxName;
            if (syntax == null || syntax.getNodeValue().isEmpty()) {
                syntaxName = MetadataReader.DEFAULT_QUERY_SYNTAX;
            } else {
                syntaxName = syntax.getNodeValue();
            }
            NodeList list = queryNode.getChildNodes();
            for (int i = 0; i < list.getLength(); i++) {
                Node data = list.item(i);
                String name = data.getNodeName();
                String value = data.getTextContent();
                if (name.equals("basequery")) {
                    Map<String, String> basequery = new HashMap<>();
                    basequery.put(null, value);
                    entry.setBasequery(basequery);
                }
                if (name.equals("allowSearchWithoutCriteria"))
                    entry.setAllowSearchWithoutCriteria(value.equalsIgnoreCase("true"));
                if (name.equals("condition")) {
                    handleQueryCondition(data, entry);
                }
            }
            NodeList queriesNode = (NodeList) xpath.evaluate("query", queryNode, XPathConstants.NODESET);
            List<MetadataQuery> queries = new ArrayList<>();
            for (int i = 0; i < queriesNode.getLength(); i++) {
                MetadataQuery query = new MetadataQuery();
                query.setSyntax(syntaxName);
                Map<String, String> basequeries = new HashMap<>();
                Node node = queriesNode.item(i);
                NamedNodeMap nodeMap = node.getAttributes();
                query.setId(nodeMap.getNamedItem("id").getTextContent());
                if (nodeMap.getNamedItem("join") != null) {
                    query.setJoin(nodeMap.getNamedItem("join").getTextContent());
                } else {
                    query.setJoin("AND");
                }

                if (nodeMap.getNamedItem("applyBasequery") != null) {
                    query.setApplyBasequery(nodeMap.getNamedItem("applyBasequery").getTextContent().equals("true"));
                }

                if (nodeMap.getNamedItem("basequeryAsFilter") != null && nodeMap.getNamedItem("basequeryAsFilter").getTextContent() != null) {
                    query.setBasequeryAsFilter(Boolean.parseBoolean(nodeMap.getNamedItem("basequeryAsFilter").getTextContent()));
                }


                List<MetadataQueryParameter> parameters = new ArrayList<>();
                NodeList list2 = node.getChildNodes();
                for (int j = 0; j < list2.getLength(); j++) {
                    Node parameterNode = list2.item(j);
                    if (parameterNode.getNodeName().equals("specialFilter")) {
                        if (parameterNode.getTextContent().isEmpty()) {
                            // in case the mds overrides special filters and wants to remove them
                            query.addSpecialFilter(null);
                        } else {
                            query.addSpecialFilter(MetadataQuery.SpecialFilter.valueOf(parameterNode.getTextContent()));
                        }
                    }
                    if (parameterNode.getNodeName().equals("basequery")) {
                        basequeries.put(
                                parameterNode.getAttributes() == null ? null :
                                        parameterNode.getAttributes().getNamedItem("propertyNull") == null ? null :
                                                parameterNode.getAttributes().getNamedItem("propertyNull").getTextContent(),
                                parameterNode.getTextContent());
                    }
                    if (parameterNode.getNodeName().equals("condition")) {
                        handleQueryCondition(parameterNode, query);
                    }
                    MetadataQueryParameter parameter = new MetadataQueryParameter(syntaxName);
                    NodeList list3 = parameterNode.getChildNodes();
                    NamedNodeMap attributes = parameterNode.getAttributes();
                    if (attributes == null || attributes.getNamedItem("name") == null) {
                        continue;
                    }
                    parameter.setName(attributes.getNamedItem("name").getTextContent());
                    if (attributes.getNamedItem("asFilter") != null && attributes.getNamedItem("asFilter").getTextContent() != null) {
                        parameter.setAsFilter(Boolean.parseBoolean(attributes.getNamedItem("asFilter").getTextContent()));
                    }
                    Map<String, String> statements = new HashMap<>();
                    for (int k = 0; k < list3.getLength(); k++) {
                        Node data = list3.item(k);
                        String name = data.getNodeName();
                        String value = data.getTextContent();
                        switch (name) {
                            case "statement":
                                Node key = data.getAttributes().getNamedItem("value");
                                statements.put(key == null ? null : key.getTextContent(), value);
                                break;
                            case "facets":
                                List<MetadataQueryParameter.MetadataQueryFacet> facetsList = getMetadataQueryFacets(data);
                                if (!facetsList.isEmpty()) {
                                    parameter.setFacets(facetsList);
                                }
                                break;
                            case "ignorable":
                                parameter.setIgnorable(Integer.parseInt(value));
                                break;
                            case "preprocessor":
                                parameter.setPreprocessor(value);
                                break;
                            case "mandatory":
                                parameter.setMandatory(value.equalsIgnoreCase("true"));
                                break;
                            case "exactMatching":
                                parameter.setExactMatching(value.equalsIgnoreCase("true"));
                                break;
                            case "multiple":
                                parameter.setMultiple(value.equalsIgnoreCase("true"));
                                break;
                            case "multiplejoin":
                                parameter.setMultiplejoin(value);
                                break;
                        }
                    }
                    parameter.setStatements(statements);
                    parameters.add(parameter);
                }
                query.setBasequery(basequeries);
                query.setParameters(parameters);
                queries.add(query);
            }
            entry.setSyntax(syntaxName);
            entry.setQueries(queries);
            result.put(syntaxName, entry);
        }
        return result;
    }

    private static List<MetadataQueryParameter.MetadataQueryFacet> getMetadataQueryFacets(Node data) {
        NodeList facets = data.getChildNodes();
        List<MetadataQueryParameter.MetadataQueryFacet> facetsList = new ArrayList<>();
        for (int l = 0; l < facets.getLength(); l++) {
            String facetName = facets.item(l).getNodeName();
            String facetValue = facets.item(l).getTextContent();
            if (facetName.equals("facet")) {
                MetadataQueryParameter.MetadataQueryFacet facet = new MetadataQueryParameter.MetadataQueryFacet();
                facet.setValue(facetValue);
                NamedNodeMap att = facets.item(l).getAttributes();
                if (att != null && att.getNamedItem("nested") != null) {
                    facet.setNested(att.getNamedItem("nested").getTextContent());
                }
                facetsList.add(facet);
            }
        }
        return facetsList;
    }

    private static InputStream getFile(String name, Filetype type) throws IOException {
        String prefix = getPath() + "xml/";
        if (type.equals(Filetype.VALUESPACE)) {
            prefix += "valuespaces/";
        }
        return PropertiesHelper.Config.getInputStreamForFile(prefix + name);
    }

    private MetadataReader(String name, String locale) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringComments(true);
        builder = factory.newDocumentBuilder();
        InputStream is = getFile(name, Filetype.MDS);
        doc = builder.parse(is);
        is.close();
        this.locale = locale;
    }

    private MetadataSet getMetadatasetForFile(String filename) throws Exception {
        MetadataSet mds = new MetadataSet();
        Node nodeMetadataSet = (Node) xpath.evaluate("/metadataset", doc, XPathConstants.NODE);
        String id;
        try {
            id = nodeMetadataSet.getAttributes().getNamedItem("id").getNodeValue();
        } catch (NullPointerException e) {
            //throw new Exception("Mandatory attribute id is missing for the metadataset "+filename+", add this attribute to the main node");
            id = filename;
        }
        Node name = (Node) xpath.evaluate("/metadataset/name", doc, XPathConstants.NODE);
        String mdsName = "";
        if (name != null) {
            mdsName = name.getTextContent();
        }
        Node inherit = (Node) xpath.evaluate("/metadataset/inherit", doc, XPathConstants.NODE);
        String mdsInherit = null;
        if (inherit != null) {
            mdsInherit = inherit.getTextContent();
        }
        Node i18n = (Node) xpath.evaluate("/metadataset/i18n", doc, XPathConstants.NODE);
        if (i18n == null) {
            throw new Exception("Mandatory xml attribute i18n is missing for the metadataset " + filename);
        }
        i18nPath = i18n.getTextContent();
        String label;
        try {
            label = nodeMetadataSet.getAttributes().getNamedItem("label").getNodeValue();
        } catch (NullPointerException e) {
            throw new Exception("Mandatory attribute label is missing for the metadataset " + filename + ", add this attribute to the main node");
        }

        String hidden = (String) xpath.evaluate("/metadataset/@hidden", doc, XPathConstants.STRING);
        if (hidden == null) mds.setHidden(false);
        else mds.setHidden(Boolean.parseBoolean(hidden));

        mds.setId(id);
        mds.setName(mdsName);
        mds.setInherit(mdsInherit);
        mds.setI18n(i18n.getTextContent());
        mds.setLabel(label);

        mds.setCreate(getCreate());
        mds.setWidgets(getWidgets());
        mds.setTemplates(getTemplates());
        mds.setGroups(getGroups());
        mds.setLists(getLists());
        mds.setSorts(getSorts());
        mds.setQueries(getQueries(mds));

        return mds;
    }

    private MetadataCreate getCreate() throws Exception {
        Node createNode = (Node) xpath.evaluate("/metadataset/create", doc, XPathConstants.NODE);
        if (createNode == null) {
            return null;
        }
        MetadataCreate create = new MetadataCreate();
        for (int i = 0; i < createNode.getChildNodes().getLength(); i++) {
            Node node = createNode.getChildNodes().item(i);
            if (node.getNodeName().equals("onlyMetadata")) {
                create.setOnlyMetadata(node.getTextContent().equalsIgnoreCase("true"));
            }
        }
        return create;
    }

    private List<MetadataWidget> getWidgets() throws Exception {
        List<MetadataWidget> widgets = new ArrayList<>();
        NodeList widgetsNode = (NodeList) xpath.evaluate("/metadataset/widgets/widget", doc, XPathConstants.NODESET);
        for (int i = 0; i < widgetsNode.getLength(); i++) {
            Node widgetNode = widgetsNode.item(i);
            NodeList list2 = widgetNode.getChildNodes();
            MetadataWidget widget = new MetadataWidget();
            widget.setI18n(i18nPath);
            String valuespaceI18n = i18nPath;
            String valuespaceI18nPrefix = "";
            for (int j = 0; j < list2.getLength(); j++) {
                Node data = list2.item(j);
                String name = data.getNodeName();
                String value = data.getTextContent();
                switch (name) {
                    case "id":
                        Node rel = data.getAttributes().getNamedItem("rel");
                        if (rel == null) {
                            widget.setId(value);
                        } else {
                            widget.getIds().put(MetadataWidget.IdRelation.valueOf(rel.getNodeValue()), value);
                        }
                        break;
                    case "icon":
                        widget.setIcon(value);
                        break;
                    case "template":
                        widget.setTemplate(value);
                        break;
                    case "caption":
                        //widget.setCaption(value);
                        widget.setCaption(getTranslation(widget, value));
                        break;
                    case "placeholder":
                        //widget.setPlaceholder(value);
                        widget.setPlaceholder(getTranslation(widget, value));
                        break;
                    case "maxlength":
                        widget.setMaxlength(Integer.parseInt(value));
                        break;
                    case "bottomCaption":
                        widget.setBottomCaption(getTranslation(widget, value));
                        break;
                    case "configuration":
                        widget.setConfiguration(value);
                        break;
                    case "unit":
                        widget.setUnit(getTranslation(widget, value));
                        break;
                    case "inherit":
                        widget.setInherit(Boolean.parseBoolean(value));
                        break;
                    case "defaultvalue":
                        widget.setDefaultvalue(value);
                        break;
                    case "countDefaultvalueAsFilter":
                        widget.setCountDefaultvalueAsFilter(Boolean.valueOf(value));
                        break;
                    case "format":
                        widget.setFormat(value);
                        break;
                    case "link":
                        widget.setLink(value);
                        break;
                    case "type":
                        widget.setType(value);
                        break;
                    case "condition":
                        widget.setCondition(getCondition(data, widget.getId()));
                        break;
                    case "suggestionReceiver":
                        widget.setSuggestionReceiver(value);
                        break;
                    case "suggestionSource":
                        widget.setSuggestionSource(value);
                        break;
                    case "suggestionQuery":
                        widget.setSuggestionQuery(value);
                        break;
                    case "suggestDisplayProperty":
                        widget.setSuggestDisplayProperty(value);
                        break;
                    case "required":
                        if (value.equalsIgnoreCase("true")) {
                            widget.setRequired(MetadataWidget.Required.mandatory);
                        } else if (value.equalsIgnoreCase("false")) {
                            widget.setRequired(MetadataWidget.Required.optional);
                        } else {
                            widget.setRequired(MetadataWidget.Required.valueOf(value));
                        }
                        break;
                    case "textEscapingPolicy":
                        widget.setTextEscapingPolicy(MetadataWidget.TextEscapingPolicy.valueOf(value));
                        break;
                    case "hideIfEmpty":
                        widget.setHideIfEmpty(value.equalsIgnoreCase("true"));
                        break;
                    case "valuespace_i18n":
                        valuespaceI18n = value;
                        break;
                    case "valuespace_i18n_prefix":
                        valuespaceI18nPrefix = value;
                        break;
                    case "valuespace_sort":
                        widget.setValuespaceSort(value);
                        break;
                    case "valuespaceClient":
                        widget.setValuespaceClient(value.equalsIgnoreCase("true"));
                        break;
                    case "interactionType":
                        widget.setInteractionType(MetadataWidget.InteractionType.valueOf(value));
                        break;
                    case "filterMode":
                        widget.setFilterMode(MetadataWidget.WidgetFilterMode.valueOf(value));
                        break;
                    case "unfold":
                        widget.setExpandable(MetadataWidget.WidgetExpandable.valueOf(value));
                        break;
                    case "searchable":
                        widget.setSearchable(value.equalsIgnoreCase("true"));
                        break;
                    case "extended":
                        widget.setExtended(value.equalsIgnoreCase("true"));
                        break;
                    case "min":
                        widget.setMin(Integer.parseInt(value));
                        break;
                    case "max":
                        widget.setMax(Integer.parseInt(value));
                        break;
                    case "defaultMin":
                        widget.setDefaultMin(Integer.parseInt(value));
                        break;
                    case "defaultMax":
                        widget.setDefaultMax(Integer.parseInt(value));
                        break;
                    case "step":
                        widget.setStep(Integer.parseInt(value));
                        break;
                    case "allowempty":
                        widget.setAllowempty(value.equalsIgnoreCase("true"));
                        break;
                }
            }
            List<ValuespaceInfo> valuespaces = new ArrayList<>();
            for (int j = 0; j < list2.getLength(); j++) {
                Node data = list2.item(j);
                String name = data.getNodeName();
                String value = data.getTextContent();
                switch (name) {
                    case "valuespace":
                        Node type = data.getAttributes().getNamedItem("type");
                        valuespaces.add(
                                new ValuespaceInfo(
                                        value,
                                        type == null ? null : ValuespaceInfo.ValuespaceType.valueOf(type.getNodeValue())
                                ));
                        break;
                    case "valuespaceCombineStrategy":
                        widget.setValuespaceCombineStrategy(MetadataWidget.ValuespaceMerge.valueOf(value));
                        break;
                    case "values":
                        widget.setValues(getValues(data.getChildNodes(), valuespaceI18n, valuespaceI18nPrefix));
                        break;
                    case "subwidgets":
                        widget.setSubwidgets(getSubwidgets(data.getChildNodes()));
                        break;
                }
            }
            if (valuespaces.size() > 1) {
                List<MetadataKey> keys = new ArrayList<>();
                if (widget.getValuespaceMerge().equals(MetadataWidget.ValuespaceMerge.separate)) {
                    for (ValuespaceInfo v : valuespaces) {
                        ValuespaceData values = getValuespace(v, widget, valuespaceI18n, valuespaceI18nPrefix);
                        if (values.getTitle() == null) {
                            throw new IllegalArgumentException("Multiple valuespace entries are not supported by the given provider used for your vocabularies");
                        }
                        values.getEntries().stream().filter(e -> e.getParent() == null).forEach(e -> e.setParent(values.getTitle().getKey()));
                        keys.add(values.getTitle());
                        keys.addAll(values.getEntries());
                    }
                } else {
                    for (ValuespaceInfo v : valuespaces) {
                        ValuespaceData values = getValuespace(v, widget, valuespaceI18n, valuespaceI18nPrefix);
                        keys.addAll(values.getEntries());
                        // remove keys which have the delete flag attached
                        values.getEntries().stream().filter(MetadataKey::getDelete).forEach(k -> keys.removeIf(k2 -> Objects.equals(k2.getKey(), k.getKey())));
                    }
                }
                widget.setValues(keys);
            } else if (valuespaces.size() == 1) {
                widget.setValues(getValuespace(valuespaces.get(0), widget, valuespaceI18n, valuespaceI18nPrefix).getEntries());
            }
            widgets.add(widget);
        }
        return widgets;
    }

    private MetadataCondition getCondition(Node node, String id) {
        boolean negate = false;
        boolean dynamic = false;
        String pattern = null;
        NamedNodeMap attr = node.getAttributes();
        CONDITION_TYPE type = CONDITION_TYPE.PROPERTY;
        if (attr != null) {
            if (attr.getNamedItem("type") != null) {
                try {
                    type = CONDITION_TYPE.valueOf(attr.getNamedItem("type").getTextContent());
                } catch (Throwable t) {
                    log.warn("Object {} has condition, but the given type {} is invalid. Will use default type {}", id, attr.getNamedItem("type").getTextContent(), type);
                }
            } else {
                log.warn("Object {} has condition, but no type for condition was specified. Using default type {}", id, type);
            }
            if (attr.getNamedItem("negate") != null && attr.getNamedItem("negate").getTextContent().equalsIgnoreCase("true")) {
                negate = true;
            }
            if (attr.getNamedItem("dynamic") != null && attr.getNamedItem("dynamic").getTextContent().equalsIgnoreCase("true")) {
                dynamic = true;
            }
            if (attr.getNamedItem("pattern") != null) {
                pattern = attr.getNamedItem("pattern").getTextContent();
            }
        }
        return new MetadataCondition(node.getTextContent(), type, negate, dynamic, pattern);
    }

    private ValuespaceData getValuespace(ValuespaceInfo info, MetadataWidget widget, String valuespaceI18n, String valuespaceI18nPrefix) throws Exception {
        if (info.getValue().startsWith("http://")
                || info.getValue().startsWith("https://")
                || ValuespaceInfo.ValuespaceType.SKOS.equals(info.getType())
        ) {
            ValuespaceData valuespace = getValuespaceExternal(info);
            if (valuespace == null) {
                throw new Exception("No valuespace data found for " + info.getValue());
            }
            valuespace.sort(widget);
            return valuespace;
        }
        Document docValuespace = builder.parse(getFile(info.getValue(), Filetype.VALUESPACE));
        NodeList keysNode = (NodeList) xpath.evaluate("/valuespaces/valuespace[@property='" + widget.getId() + "']/key", docValuespace, XPathConstants.NODESET);
        if (keysNode.getLength() == 0) {
            throw new Exception("No valuespace found in file " + info.getValue() + ": Searching for a node named /valuespaces/valuespace[@property='" + widget.getId() + "']");
        }
        ValuespaceData valuespace = new ValuespaceData(null, getValues(keysNode, valuespaceI18n, valuespaceI18nPrefix));
        valuespace.sort(widget);
        return valuespace;
    }


    private ValuespaceData getValuespaceExternal(ValuespaceInfo value) throws Exception {
        ValuespaceReader reader = ValuespaceReader.getSupportedReader(value);
        if (reader != null) {
            return reader.getValuespace(locale);
        } else {
            log.warn("No viable metadata reader found for url {}", value);
        }
        return null;
    }

    private List<MetadataKey> getValues(NodeList keysNode, String valuespaceI18n, String valuespaceI18nPrefix) {
        List<MetadataKey> keys = new ArrayList<>();
        for (int i = 0; i < keysNode.getLength(); i++) {
            Node keyNode = keysNode.item(i);
            NamedNodeMap attributes = keyNode.getAttributes();
            String cap = null, description = null;
            if (attributes != null && attributes.getNamedItem("cap") != null) {
                cap = attributes.getNamedItem("cap").getTextContent();
            }
            if (attributes != null && attributes.getNamedItem("description") != null) {
                description = attributes.getNamedItem("description").getTextContent();
            }
            if (cap == null) {
                cap = "";
            }
            if (description == null) {
                description = "";
            }

            if (StringUtils.isBlank(keyNode.getTextContent()) && StringUtils.isBlank(cap)) {
                continue;
            }

            MetadataKey key = new MetadataKey();
            key.setKey(keyNode.getTextContent());
            key.setI18n(valuespaceI18n);
            key.setI18nPrefix(valuespaceI18nPrefix);
            if (attributes != null && attributes.getNamedItem("parent") != null) {
                key.setParent(attributes.getNamedItem("parent").getTextContent());
            }
            if (attributes != null && attributes.getNamedItem("delete") != null) {
                key.setDelete(attributes.getNamedItem("delete").getTextContent().equalsIgnoreCase("true"));
            }
            if (attributes != null && attributes.getNamedItem("icon") != null) {
                key.setIcon(attributes.getNamedItem("icon").getTextContent());
            }
            if (attributes != null && attributes.getNamedItem("url") != null) {
                key.setUrl(attributes.getNamedItem("url").getTextContent());
            }

            String fallback = null;
            if (!cap.isEmpty()) {
                fallback = cap;
            }

            key.setCaption(getTranslation(key,
                    StringUtils.isNotBlank(key.getKey())
                            ? key.getKey()
                            : cap,
                    fallback));
            key.setDescription(getTranslation(key, description, ""));
            keys.add(key);
        }
        return keys;
    }

    private List<MetadataWidget.Subwidget> getSubwidgets(NodeList keysNode) {
        List<MetadataWidget.Subwidget> widgets = new ArrayList<>();
        for (int i = 0; i < keysNode.getLength(); i++) {
            Node keyNode = keysNode.item(i);
            if (StringUtils.isBlank(keyNode.getTextContent())) {
                continue;
            }
            MetadataWidget.Subwidget widget = new MetadataWidget.Subwidget();
            widget.setId(keyNode.getTextContent());
            widgets.add(widget);
        }
        return widgets;
    }

    private List<MetadataTemplate> getTemplates() throws XPathExpressionException {
        List<MetadataTemplate> templates = new ArrayList<>();
        NodeList templatesNode = (NodeList) xpath.evaluate("/metadataset/templates/template", doc, XPathConstants.NODESET);
        for (int i = 0; i < templatesNode.getLength(); i++) {
            Node templateNode = templatesNode.item(i);
            NodeList list2 = templateNode.getChildNodes();
            MetadataTemplate template = new MetadataTemplate();
            template.setI18n(i18nPath);
            for (int j = 0; j < list2.getLength(); j++) {
                Node data = list2.item(j);
                String name = data.getNodeName();
                String value = data.getTextContent();
                switch (name) {
                    case "id":
                        template.setId(value);
                        break;
                    case "caption":
                        //template.setCaption(value);
                        template.setCaption(getTranslation(template, value));
                        break;
                    case "rel":
                        template.setRel(MetadataTemplate.REL_TYPE.valueOf(value));
                        break;
                    case "icon":
                        template.setIcon(value);
                        break;
                    case "html":
                        template.setHtml(translateHtml(i18nPath, value));
                        break;
                    case "hideIfEmpty":
                        template.setHideIfEmpty(value.equalsIgnoreCase("true"));
                        break;
                    case "extended":
                        template.setExtended(Boolean.parseBoolean(value));
                        break;
                }
            }
            templates.add(template);

        }
        return templates;
    }

    private String translateHtml(String i18nPath, String html) {
        String[] parts = StringUtils.splitByWholeSeparator(html, "{{");
        for (int i = 1; i < parts.length; i++) {
            String[] key = StringUtils.splitByWholeSeparator(parts[i], "}}");
            String i18nKey = key[0].trim();
            key[0] = getTranslation(i18nPath, i18nKey, null, locale);
            parts[i] = StringUtils.join(key, "");
        }
        return StringUtils.join(parts, "");
    }

    private List<MetadataGroup> getGroups() throws XPathExpressionException {
        List<MetadataGroup> groups = new ArrayList<>();
        NodeList groupsNode = (NodeList) xpath.evaluate("/metadataset/groups/group", doc, XPathConstants.NODESET);
        for (int i = 0; i < groupsNode.getLength(); i++) {
            Node groupNode = groupsNode.item(i);
            NodeList list2 = groupNode.getChildNodes();
            MetadataGroup group = new MetadataGroup();
            for (int j = 0; j < list2.getLength(); j++) {
                Node data = list2.item(j);
                String name = data.getNodeName();
                String value = data.getTextContent();
                switch (name) {
                    case "id":
                        group.setId(value);
                        break;
                    case "rendering":
                        group.setRendering(MetadataGroup.Rendering.valueOf(value));
                        break;
                    case "views":
                        List<String> views = new ArrayList<>();
                        NodeList list3 = data.getChildNodes();
                        for (int k = 0; k < list3.getLength(); k++) {
                            String view = list3.item(k).getTextContent();
                            if (!view.trim().isEmpty())
                                views.add(view);
                        }
                        group.setViews(views);
                        break;
                }
            }
            groups.add(group);
        }
        return groups;
    }

    private List<MetadataList> getLists() throws XPathExpressionException {
        List<MetadataList> lists = new ArrayList<>();
        NodeList listsNode = (NodeList) xpath.evaluate("/metadataset/lists/list", doc, XPathConstants.NODESET);
        for (int i = 0; i < listsNode.getLength(); i++) {
            Node listNode = listsNode.item(i);
            NodeList list2 = listNode.getChildNodes();
            MetadataList list = new MetadataList();
            for (int j = 0; j < list2.getLength(); j++) {
                Node data = list2.item(j);
                String name = data.getNodeName();
                String value = data.getTextContent();
                if (name.equals("id"))
                    list.setId(value);
                if (name.equals("columns")) {
                    List<MetadataColumn> columns = getMetadataColumns(data);
                    list.setColumns(columns);
                }
            }
            lists.add(list);
        }
        return lists;
    }

    private static List<MetadataColumn> getMetadataColumns(Node data) {
        List<MetadataColumn> columns = new ArrayList<>();
        NodeList list3 = data.getChildNodes();
        for (int k = 0; k < list3.getLength(); k++) {
            String column = list3.item(k).getTextContent();
            NamedNodeMap attributes = list3.item(k).getAttributes();
            if (!column.trim().isEmpty()) {
                MetadataColumn col = new MetadataColumn();
                col.setId(column);
                if (attributes != null) {
                    Node showDefault = attributes.getNamedItem("showDefault");
                    if (showDefault != null)
                        col.setShowDefault(showDefault.getTextContent().equalsIgnoreCase("true"));
                    Node format = attributes.getNamedItem("format");
                    if (format != null)
                        col.setFormat(format.getTextContent());
                }
                columns.add(col);
            }
        }
        return columns;
    }

    private List<MetadataSort> getSorts() throws XPathExpressionException {
        List<MetadataSort> sorts = new ArrayList<>();
        NodeList sortsNode = (NodeList) xpath.evaluate("/metadataset/sorts/sort", doc, XPathConstants.NODESET);
        for (int i = 0; i < sortsNode.getLength(); i++) {
            Node listNode = sortsNode.item(i);
            NodeList list2 = listNode.getChildNodes();
            MetadataSort sort = new MetadataSort();
            for (int j = 0; j < list2.getLength(); j++) {
                Node data = list2.item(j);
                String name = data.getNodeName();
                String value = data.getTextContent();
                if (name.equals("id")) {
                    sort.setId(value);
                }
                if (name.equals("default")) {
                    NodeList list3 = data.getChildNodes();
                    for (int k = 0; k < list3.getLength(); k++) {
                        Node data2 = list3.item(k);
                        if (data2.getNodeName().equals("sortBy")) {
                            sort.getDefaultValue().setSortBy(data2.getTextContent());
                        }
                        if (data2.getNodeName().equals("sortAscending")) {
                            sort.getDefaultValue().setSortAscending(Boolean.parseBoolean(data2.getTextContent()));
                        }
                    }
                }
                if (name.equals("columns")) {
                    List<MetadataSortColumn> columns = getMetadataSortColumns(data);
                    sort.setColumns(columns);
                }
            }
            sorts.add(sort);
        }
        return sorts;
    }

    private static List<MetadataSortColumn> getMetadataSortColumns(Node data) {
        List<MetadataSortColumn> columns = new ArrayList<>();
        NodeList list3 = data.getChildNodes();
        for (int k = 0; k < list3.getLength(); k++) {
            String column = list3.item(k).getTextContent();
            NamedNodeMap attributes = list3.item(k).getAttributes();
            if (!column.trim().isEmpty()) {
                MetadataSortColumn col = new MetadataSortColumn();
                col.setId(column);
                if (attributes != null) {
                    Node mode = attributes.getNamedItem("mode");
                    if (mode != null) {
                        col.setMode(mode.getTextContent());
                    }
                }
                columns.add(col);
            }
        }
        return columns;
    }

    private String getTranslation(MetadataTranslatable translatable, String key, String fallback) {
        return getTranslation(translatable, key, fallback, locale);
    }

    private String getTranslation(MetadataTranslatable translatable, String key) {
        return getTranslation(translatable, key, null);
    }

    private static String getTranslation(MetadataTranslatable translatable, String key, String fallback, String locale) {
        try {
            if (key == null) {
                return null;
            }
            if (translatable.getI18nPrefix() != null) {
                key = translatable.getI18nPrefix() + key;
            }
            return getTranslation(translatable.getI18n(), key, fallback, locale);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            log.warn(e.toString());
            return key;
        }
    }

    private static Map<String, ResourceBundle> translationBundles = new HashMap<>();

    private static ResourceBundle getTranslationCache(String i18nFile) {
        if (translationBundles.containsKey(i18nFile)) {
            return translationBundles.get(i18nFile);
        }

        ResourceBundle bundle = null;
        try {
            bundle = PropertiesHelper.Config.getResourceBundleForFile(getPath() + "i18n/" + i18nFile + ".properties");
            translationBundles.put(i18nFile, bundle);
        } catch (Throwable ignored) {
        }
        return bundle;
    }

    public static String getTranslation(String i18n, @NonNull String key, String fallback, String locale) {
        if (NONE.equals(locale)) {
            return key;
        }

        String defaultValue = key;
        if (fallback != null) {
            defaultValue = fallback;
        }

        ResourceBundle defaultResourceBundleGlobal;
        ResourceBundle defaultResourceBundleLocal;
        ResourceBundle defaultResourceBundleGlobalOverride;
        ResourceBundle defaultResourceBundleLocalOverride;

        key = key.replace(" ", "_");

        defaultResourceBundleLocalOverride = getTranslationCache(i18n + "_override_" + locale);
        if (defaultResourceBundleLocalOverride != null && defaultResourceBundleLocalOverride.containsKey(key)) {
            return replaceGenderSeperator(defaultResourceBundleLocalOverride.getString(key));
        }

        defaultResourceBundleLocal = getTranslationCache(i18n + "_" + locale);
        if (defaultResourceBundleLocal != null && defaultResourceBundleLocal.containsKey(key)) {
            return replaceGenderSeperator(defaultResourceBundleLocal.getString(key));
        }

        defaultResourceBundleGlobalOverride = getTranslationCache(i18n + "_override");
        if (defaultResourceBundleGlobalOverride != null && defaultResourceBundleGlobalOverride.containsKey(key)) {
            return replaceGenderSeperator(defaultResourceBundleGlobalOverride.getString(key));
        }

        defaultResourceBundleGlobal = getTranslationCache(i18n);
        if (defaultResourceBundleGlobal == null) {
            log.warn("No translation file " + i18n + " found while looking for " + key);
        } else if (defaultResourceBundleGlobal.containsKey(key)) {
            defaultValue = defaultResourceBundleGlobal.getString(key);
        }

        return replaceGenderSeperator(defaultValue);
    }

    private static String replaceGenderSeperator(String i18n) {
        return i18n.replace("{{GENDER_SEPARATOR}}", GENDER_SEPARATOR);
    }

    public static void refresh() {
        mdsCache.clear();
        translationBundles = new HashMap<>();
        prepareMetadatasets();
    }

    public static void prepareMetadatasets() {
        ApplicationInfo home = ApplicationInfoList.getHomeRepository();
        try {
            getMetadataset(home, "-default-", "de_DE", true);
            getMetadataset(home, "-default-", "en_US", true);
        } catch (Throwable t) {
            log.error("Error occured while preparing the default mds: ", t);
        }
    }

}
