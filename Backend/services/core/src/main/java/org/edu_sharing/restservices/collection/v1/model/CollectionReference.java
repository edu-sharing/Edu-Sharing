package org.edu_sharing.restservices.collection.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.beanutils.PropertyUtils;
import org.edu_sharing.restservices.shared.Node;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;

;

@Schema(description = "")
@Getter
@Setter
public class CollectionReference extends Node {

	private String originalId;

	/**
	 * access for the current user of the original node this one is referring to
	 */
	@JsonProperty
	private List<String> accessOriginal;

	@JsonProperty
	private boolean originalRestrictedAccess;

	public CollectionReference(){

	}
	public CollectionReference(Node node) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
		PropertyUtils.copyProperties(this, node);
	}

	@Schema(required = false, description = "")
	@JsonProperty("originalId")
	public String getOriginalId() {
		return originalId;
	}

	public void setOriginalId(String originalId) {
		this.originalId = originalId;
	}
}
