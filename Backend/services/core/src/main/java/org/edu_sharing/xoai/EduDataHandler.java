package org.edu_sharing.xoai;

import io.gdcc.xoai.dataprovider.repository.ItemRepository;

import java.util.Collections;
import java.util.List;

public interface EduDataHandler extends ItemRepository {
    default List<String> getSets() {
        return Collections.singletonList("default");
    };

}
