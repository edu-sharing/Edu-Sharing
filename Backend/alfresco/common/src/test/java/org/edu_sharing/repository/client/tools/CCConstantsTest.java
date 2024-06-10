package org.edu_sharing.repository.client.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CCConstantsTest {

    @Test
    void getValidGlobalName() {
        assertEquals(CCConstants.CM_NAME, CCConstants.getValidGlobalName("cm:name"));
        assertEquals(CCConstants.LOM_PROP_GENERAL_TITLE, CCConstants.getValidGlobalName("cclom:title"));
        assertEquals(CCConstants.CCM_PROP_IO_REPL_TAXON_ID, CCConstants.getValidGlobalName("ccm:taxonid"));
        assertEquals(CCConstants.CCM_PROP_IO_TECHNICAL_STATE, CCConstants.getValidGlobalName("ccm:technical_state"));
    }
    @Test
    void getValidLocalName() {
        assertEquals("cm:name", CCConstants.getValidLocalName(CCConstants.CM_NAME));
        assertEquals("cclom:title", CCConstants.getValidLocalName(CCConstants.LOM_PROP_GENERAL_TITLE));
        assertEquals("ccm:taxonid", CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_REPL_TAXON_ID));
        assertEquals("ccm:technical_state", CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_TECHNICAL_STATE));
    }
}