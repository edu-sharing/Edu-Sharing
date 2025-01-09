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

@Slf4j
public abstract class AbstractMetadataFormatWriter implements OaiMetadataFormatWriter {
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

    public abstract void build(Context context) throws ParserConfigurationException;
}
