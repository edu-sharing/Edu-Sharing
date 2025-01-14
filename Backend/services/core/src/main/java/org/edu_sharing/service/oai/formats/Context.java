package org.edu_sharing.service.oai.formats;

import lombok.Getter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.metadataset.v2.MetadataSet;
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

/**
 * The {@code Context} class represents a utility for managing and interacting with
 * XML documents and metadata within a specific application context.
 *
 * <p>This class provides functionalities to create and manipulate XML elements,
 * access metadata, determine user permissions, and manage language settings. It is
 * typically initialized with a {@link PropertyMapper}, {@link MetadataSet}, and a
 * node ID, and it encapsulates document handling and permission checks.</p>
 */
@Getter
public class Context {

    private final Document document;
    private final PropertyMapper propertyMapper;
    private final MetadataSet metadataSet;
    private final String nodeId;
    private final String language;

    private final boolean hasWritePermission;

    /**
     * Checks if the user has write permission for the current node.
     *
     * @return {@code true} if the user has write permission, otherwise {@code false}.
     */
    public boolean hasWritePermission() {
        return hasWritePermission;
    }

    /**
     * Constructs a new {@code Context} instance.
     *
     * @param propertyMapper the {@link PropertyMapper} to fetch properties from.
     * @param metadataSet the {@link MetadataSet} associated with the context.
     * @param nodeId the node ID of the current context.
     * @throws ParserConfigurationException if an error occurs during XML document initialization.
     */
    public Context(PropertyMapper propertyMapper, MetadataSet metadataSet, String nodeId) throws ParserConfigurationException {
        this.propertyMapper = propertyMapper;
        this.metadataSet = metadataSet;
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

    /**
     * Creates a new XML element with the specified name.
     *
     * @param name the name of the element to create.
     * @return a new {@link Element} instance.
     */
    public Element createElement(String name) {
        return document.createElement(name);
    }

    /**
     * Creates and appends a new XML element with the specified name to the document root.
     *
     * @param name the name of the element to create.
     * @return the created {@link Element}.
     */
    public Element createAndAppendElement(String name) {
        return createAndAppendElement(name, document);
    }

    /**
     * Creates and appends a new XML element with the specified name to a given parent node.
     *
     * @param name the name of the element to create.
     * @param parent the parent {@link Node} to which the new element will be appended.
     * @return the created {@link Element}.
     */
    public Element createAndAppendElement(String name, Node parent) {
        Element element = document.createElement(name);
        parent.appendChild(element);
        return element;
    }

    /**
     * Creates and appends a new XML element with the specified name and value to a given parent node.
     * If the value is blank, no element is created.
     *
     * @param name the name of the element to create.
     * @param parent the parent {@link Node} to which the new element will be appended.
     * @param value the text value of the new element.
     * @return the created {@link Element}, or {@code null} if the value is blank.
     */
    public Element createAndAppendElement(String name, Node parent, String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        Element element = createAndAppendElement(name, parent);
        element.setTextContent(value);
        return element;
    }
    /**
     * Creates and appends a new XML element with the specified name and value as a CDATA section
     * to a given parent node. If the value is blank, no element is created.
     *
     * @param name the name of the element to create.
     * @param parent the parent {@link Node} to which the new element will be appended.
     * @param value the CDATA section value of the new element.
     * @return the created {@link Element}, or {@code null} if the value is blank.
     */
    public Element createAndAppendElementAsCData(String name, Node parent, String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        Element element = createAndAppendElement(name, parent);
        element.appendChild(document.createCDATASection(value));
        return element;
    }

    /**
     * Creates and appends multiple XML elements with the specified name and values as CDATA sections
     * to a given parent node.
     *
     * @param name the name of the elements to create.
     * @param parent the parent {@link Node} to which the new elements will be appended.
     * @param value a list of values for the CDATA sections of the new elements.
     * @return a list of created {@link Element} instances.
     */
    public List<Element> createAndAppendElementAsCData(String name, Node parent, List<String> value) {
        return value.stream().map(x -> createAndAppendElementAsCData(name, parent, x)).collect(Collectors.toList());
    }

    /**
     * Creates and appends a new XML element with the specified name and Boolean value to a given parent node.
     * If the value is {@code null}, no element is created.
     *
     * @param name the name of the element to create.
     * @param parent the parent {@link Node} to which the new element will be appended.
     * @param value the Boolean value of the new element.
     * @return the created {@link Element}, or {@code null} if the value is {@code null}.
     */
    public Element createAndAppendElement(String name, Node parent, Boolean value) {
        if (value == null) {
            return null;
        }
        return createAndAppendElement(name, parent, Boolean.toString(value));
    }

    /**
     * Creates and appends multiple XML elements with the specified name and values to a given parent node.
     *
     * @param name the name of the elements to create.
     * @param parent the parent {@link Node} to which the new elements will be appended.
     * @param value a list of text values for the new elements.
     * @return a list of created {@link Element} instances.
     */
    public List<Element> createAndAppendElement(String name, Node parent, List<String> value) {
        return value.stream().map(x -> createAndAppendElement(name, parent, x)).collect(Collectors.toList());
    }
}
