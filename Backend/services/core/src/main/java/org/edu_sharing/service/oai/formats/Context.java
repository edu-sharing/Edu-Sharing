package org.edu_sharing.service.oai.formats;

import lombok.Getter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.permission.PermissionServiceHelper;
import org.edu_sharing.service.util.PropertyMapper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Getter
public class Context {

    private final Document document;
    private final PropertyMapper propertyMapper;
    private final String nodeId;
    private final String language;

    private final boolean hasWritePermission;

    public boolean hasWritePermission() {
        return hasWritePermission;
    }

    public Context(PropertyMapper propertyMapper, String nodeId) throws ParserConfigurationException {
        this.propertyMapper = propertyMapper;
        this.nodeId = nodeId;

        language = Optional.ofNullable(propertyMapper.getString(CCConstants.LOM_PROP_GENERAL_LANGUAGE))
                .map(Locale::forLanguageTag)
                .map(Locale::getLanguage)
                .orElseGet(() -> Locale.getDefault().getLanguage());

        hasWritePermission = PermissionServiceHelper.hasPermission(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId), CCConstants.PERMISSION_WRITE);
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = documentBuilderFactory.newDocumentBuilder();
        document = docBuilder.newDocument();
    }

    public Element createElement(String name) {
        return document.createElement(name);
    }

    public Element createAndAppendElement(String name) {
        return createAndAppendElement(name, document);
    }


    public Element createAndAppendElement(String name, Node parent) {
        Element element = document.createElement(name);
        parent.appendChild(element);
        return element;
    }

    public Element createAndAppendElement(String name, Node parent, String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        Element element = createAndAppendElement(name, parent);
        element.setTextContent(value);
        return element;
    }

    public Element createAndAppendElementAsCData(String name, Node parent, String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        Element element = createAndAppendElement(name, parent);
        element.appendChild(document.createCDATASection(value));
        return element;
    }


    public List<Element> createAndAppendElementAsCData(String name, Node parent, List<String> value) {
        return value.stream().map(x -> createAndAppendElementAsCData(name, parent, x)).collect(Collectors.toList());
    }


    public Element createAndAppendElement(String name, Node parent, Boolean value) {
        if (value == null) {
            return null;
        }
        return createAndAppendElement(name, parent, Boolean.toString(value));
    }

    public List<Element> createAndAppendElement(String name, Node parent, List<String> value) {
        return value.stream().map(x -> createAndAppendElement(name, parent, x)).collect(Collectors.toList());
    }
}
