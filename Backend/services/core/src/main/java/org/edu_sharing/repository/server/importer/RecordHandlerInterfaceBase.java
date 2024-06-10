package org.edu_sharing.repository.server.importer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface RecordHandlerInterfaceBase {

	Map<String,Object> getProperties();

	default List<String> getPropertiesToRemove(){
		return new ArrayList<>();
	};

	default String getSet(){return null;};

}
