package org.edu_sharing.service.oai.core;

import io.gdcc.xoai.dataprovider.model.MetadataFormat;
import org.bouncycastle.util.Arrays;
import org.edu_sharing.service.oai.formats.OaiMetadataFormatWriter;
import org.edu_sharing.spring.scope.refresh.annotations.RefreshScope;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Lazy
@Component
@RefreshScope
public class EduMetadataFormatRegistry {

    private final Map<String, OaiMetadataFormatWriter> formatWriterMap;

    public EduMetadataFormatRegistry( @Value("${exporter.oai.formats}") String[] supportedFormats,  List<OaiMetadataFormatWriter> metadataFormatWriterList) {
        Stream<OaiMetadataFormatWriter> stream = metadataFormatWriterList.stream();
        if(!Arrays.isNullOrEmpty(supportedFormats)) {
            List<String> formats = List.of(supportedFormats);
            stream = stream.filter(x -> formats.contains(x.getFormat().getPrefix()));
        }
        formatWriterMap = stream.collect(Collectors.toMap(x -> x.getFormat().getPrefix(), x -> x));
    }

    public @NotNull OaiMetadataFormatWriter getFormatWriter(String format) {
        OaiMetadataFormatWriter oaiMetadataFormatWriter = formatWriterMap.get(format);
        if (oaiMetadataFormatWriter == null) {
            throw new IllegalArgumentException("Unknown OaiMetadataFormatWriter: " + format);
        }
        return oaiMetadataFormatWriter;
    }

    public @NotNull MetadataFormat getMetadataFormat(String format) {
        return getFormatWriter(format).getFormat();
    }


    public List<MetadataFormat> getSupportedFormats() {
        return formatWriterMap.values().stream().map(OaiMetadataFormatWriter::getFormat).collect(Collectors.toList());
    }
}
