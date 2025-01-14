package org.edu_sharing.service.oai.formats;

import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.metadataset.v2.MetadataSet;
import org.edu_sharing.metadataset.v2.tools.MetadataHelper;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.util.PropertyMapper;
import org.jetbrains.annotations.NotNull;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.OutputStream;
import java.util.Map;
import java.util.Objects;

/**
 * Abstract class that provides common functionality for writing metadata in various formats.
 * This class implements the {@link OaiMetadataFormatWriter} interface and handles the
 * process of writing metadata to an output stream. It checks the node type, retrieves the
 * metadata set, and builds the XML document. The actual format-specific writing logic is
 * delegated to the subclasses through the abstract {@link #build(Context)} method.
 */
@Slf4j
public abstract class AbstractMetadataFormatWriter implements OaiMetadataFormatWriter {
    /**
     * Writes the metadata to the given output stream.
     * This method retrieves the metadata set for the given node, validates the node type,
     * builds the context for the metadata, and transforms the metadata into the required format.
     *
     * @param outputStream The output stream to write the metadata to.
     * @param nodeRef The node reference representing the object to retrieve metadata from.
     * @param properties A map containing additional properties that might be used for writing metadata.
     * @throws Exception If an error occurs while processing the metadata or writing it to the output stream.
     */
    @Override
    public void write(@NotNull OutputStream outputStream, @NotNull NodeRef nodeRef, @NotNull Map<String, Object> properties) throws Exception {
        PropertyMapper propertyMapper = new PropertyMapper(properties);

        String type = propertyMapper.getString(CCConstants.NODETYPE);
        if (StringUtils.isBlank(type) || !Objects.equals(type, CCConstants.getValidLocalName(CCConstants.CCM_TYPE_IO))) {
            log.error("Invalid node type");
            return;
        }

        MetadataSet metadataset = MetadataHelper.getMetadataset(nodeRef);
        Context context = new Context(propertyMapper, metadataset, nodeRef.getId());
        build(context);
        DOMSource source = new DOMSource(context.getDocument());
        StreamResult result = new StreamResult(outputStream);

        Transformer transformer = getFormat().getTransformer();
        transformer.transform(source, result);
    }

    /**
     * Abstract method that must be implemented by subclasses to build the metadata in the
     * desired format and populate the provided context with the necessary data.
     *
     * @param context The context object that holds the metadata and related information for the transformation.
     * @throws ParserConfigurationException If there is an error in the configuration of the XML parser.
     */
    public abstract void build(Context context) throws ParserConfigurationException;
}
