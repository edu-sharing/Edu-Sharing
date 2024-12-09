package org.edu_sharing.metadataset.v2.valuespace_reader;

import org.apache.commons.io.FileUtils;
import org.edu_sharing.metadataset.v2.MetadataKey;
import org.edu_sharing.metadataset.v2.ValuespaceData;
import org.edu_sharing.metadataset.v2.ValuespaceInfo;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class SKOSReaderTest {

    private SKOSReader underTest;

    @BeforeEach
    void setUp() {
        underTest = Mockito.spy(new SKOSReader(new ValuespaceInfo("", ValuespaceInfo.ValuespaceType.SKOS, false)));
    }

    @Test
    void convertEntryTest() throws Exception {
        JSONObject valuespace = new JSONObject(FileUtils.readFileToString(new File(getClass().getClassLoader().getResource("vocabTest.json").getFile())));
        Mockito.doReturn(valuespace).when(underTest).fetch();
        ValuespaceData actual = underTest.getValuespace(null);
        assertEquals(2, actual.getEntries().size());
        MetadataKey elementarbereich = actual.getEntries().get(0);
        assertEquals("http://w3id.org/openeduhub/vocabs/educationalContext/elementarbereich", elementarbereich.getKey());
        assertEquals("Elementarbereich", elementarbereich.getCaption());
        assertEquals("de", elementarbereich.getLocale());
        assertEquals("https://www.example.com/", elementarbereich.getUrl());
        assertEquals(3, elementarbereich.getRelated().size());

        MetadataKey grundschule = actual.getEntries().get(1);
        assertEquals("http://w3id.org/openeduhub/vocabs/educationalContext/grundschule", grundschule.getKey());
        assertEquals("Primarstufe", grundschule.getCaption());
        assertEquals("de", grundschule.getLocale());
        assertEquals("https://www.example.com/", grundschule.getUrl());
        assertEquals(3, grundschule.getRelated().size());
    }
}