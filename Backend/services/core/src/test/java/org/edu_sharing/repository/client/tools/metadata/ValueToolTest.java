package org.edu_sharing.repository.client.tools.metadata;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class ValueToolTest {

    @Test
    void getMultivalueTest() {
        assertArrayEquals(new String[]{"a"}, ValueTool.getMultivalue("a"));
        assertArrayEquals(new String[]{"a", "b"}, ValueTool.getMultivalue("a[#]b"));
    }

}