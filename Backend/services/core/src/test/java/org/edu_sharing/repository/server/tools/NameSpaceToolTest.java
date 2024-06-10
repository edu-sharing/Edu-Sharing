package org.edu_sharing.repository.server.tools;

import org.edu_sharing.repository.client.tools.CCConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NameSpaceToolTest {

    @Test
    void transformToLongQName() {
    }

    @Test
    void transformToShortQName() {
        assertEquals("cm:name", NameSpaceTool.transformToShortQName(CCConstants.CM_NAME));
        assertEquals("cm:name", NameSpaceTool.transformToShortQName("cm:name"));
        assertNull(NameSpaceTool.transformToShortQName("Test"));
    }
}