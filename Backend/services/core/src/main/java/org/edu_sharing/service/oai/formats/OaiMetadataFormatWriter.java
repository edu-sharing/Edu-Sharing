package org.edu_sharing.service.oai.formats;

import io.gdcc.xoai.dataprovider.model.MetadataFormat;
import org.alfresco.service.cmr.repository.NodeRef;
import org.jetbrains.annotations.NotNull;

import java.io.OutputStream;
import java.util.Map;

public interface OaiMetadataFormatWriter {
    @NotNull MetadataFormat getFormat();

    void write(@NotNull OutputStream outputStream, @NotNull NodeRef nodeId, @NotNull Map<String, Object> properties) throws Exception;
}
