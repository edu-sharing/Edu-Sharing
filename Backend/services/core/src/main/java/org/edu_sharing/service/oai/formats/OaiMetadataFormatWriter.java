package org.edu_sharing.service.oai.formats;

import io.gdcc.xoai.dataprovider.model.MetadataFormat;
import org.jetbrains.annotations.NotNull;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.OutputStream;
import java.util.Map;

public interface OaiMetadataFormatWriter {
    @NotNull MetadataFormat getFormat();

    void write(@NotNull OutputStream outputStream, @NotNull String nodeId, @NotNull Map<String, Object> properties) throws TransformerException, ParserConfigurationException;
}
