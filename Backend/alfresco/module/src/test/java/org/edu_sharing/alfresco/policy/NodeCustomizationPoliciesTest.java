package org.edu_sharing.alfresco.policy;

import org.alfresco.service.cmr.repository.ContentReader;
import org.apache.tika.mime.MediaType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class NodeCustomizationPoliciesTest {

    @Test
    void verifyMimetypeByMagicBytes() throws UnsupportedEncodingException {
        Map<String, byte[]> LIST = new HashMap<>() {{
            put("image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0});
            put("text/plain", "TEST".getBytes(StandardCharsets.UTF_8));
            put("application/xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><a></a>".getBytes(StandardCharsets.UTF_8));
            //put("text/xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><a></a>".getBytes(StandardCharsets.UTF_8));
            put("application/zip", new byte[]{(byte) 0x50, (byte) 0x4B, (byte) 0x05, (byte) 0x06});
        }};

        LIST.forEach((mimetype, magic) -> {
            ContentReader contentReader =
                    Mockito.mock(ContentReader.class);
            Mockito.when(contentReader.getContentInputStream()).thenReturn(
                    new ByteArrayInputStream(magic)
            );
            String filename = UUID.randomUUID() + "." + mimetype.split("/")[1];
            Mockito.when(contentReader.getMimetype()).thenReturn(mimetype);
            Map<String, List<String>> allowList = new HashMap<>() {{
                put(mimetype, Collections.singletonList(mimetype.split("/")[1]));
            }};
            NodeCustomizationPolicies.verifyMimetype(
                    contentReader,
                    filename,
                    allowList,
                    false);
            NodeCustomizationPolicies.verifyMimetype(
                    contentReader,
                    null,
                    allowList,
                    false);
            Map<String, List<String>> allowListWrongMimetype = new HashMap<String, List<String>>() {{
                put("test/sample", Collections.singletonList(mimetype.split("/")[1]));
            }};
            Mockito.when(contentReader.getContentInputStream()).thenReturn(
                    new ByteArrayInputStream(magic)
            );
            assertThrowsExactly(NodeMimetypeValidationException.class, () -> NodeCustomizationPolicies.verifyMimetype(
                    contentReader,
                    filename,
                    allowListWrongMimetype,
                    false));
            Map<String, List<String>> allowListWrongFileExtension = new HashMap<>() {{
                put(mimetype, Collections.singletonList("wrong"));
            }};
            Mockito.when(contentReader.getContentInputStream()).thenReturn(
                    new ByteArrayInputStream(magic)
            );
            assertThrows(NodeFileExtensionValidationException.class, () -> NodeCustomizationPolicies.verifyMimetype(
                    contentReader,
                    filename,
                    allowListWrongFileExtension,
                    false));
        });
    }
    @Test
    void verifyMimetypeUnknown() {
        ContentReader contentReader =
                Mockito.mock(ContentReader.class);
        Mockito.when(contentReader.getMimetype()).thenReturn("image/jpeg");
        Mockito.when(contentReader.getContentInputStream()).thenReturn(
                new ByteArrayInputStream(new byte[]{})
        );
        assertThrows(NodeMimetypeUnknownValidationException.class,() -> NodeCustomizationPolicies.verifyMimetype(
                contentReader,
                "test.dummy",
                new HashMap<>(),
                false));

        assertDoesNotThrow(() -> NodeCustomizationPolicies.verifyMimetype(
                contentReader,
                "test.dummy",
                new HashMap<>() {{
                    put(MediaType.OCTET_STREAM.toString(), Collections.singletonList("dummy"));
                }},
                true));
    }
}
