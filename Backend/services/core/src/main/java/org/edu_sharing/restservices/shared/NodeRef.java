package org.edu_sharing.restservices.shared;


import lombok.Data;
import org.edu_sharing.restservices.RepositoryDao;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Objects;

@Data
public class NodeRef implements Serializable, Comparable<NodeRef> {

	@JsonProperty(required = true)
	private String repo = null;

	@JsonProperty("isHomeRepo")
	private boolean isHomeRepo = false;

	@JsonProperty(required = true)
	private String id = null;

	@JsonProperty(required = true)
	boolean archived = false;

	public NodeRef(){}
	public NodeRef(String repoId,String nodeId) {
		repo=repoId;
		id=nodeId;
	}
	public NodeRef(RepositoryDao repo,String nodeId) {
		this.repo=repo.getId();
		this.isHomeRepo=repo.isHomeRepo();
		this.id=nodeId;
	}

	@Override
	public String toString() {
        return "class RepoRef {\n" +
                "  repo: " + repo + "\n" +
                "  id: " + id + "\n" +
                "}\n";
	}

	@Override
	public int compareTo(NodeRef other) {

		String s1 = this.repo + this.id;
		String s2 = other.repo + other.id;
		
		return s1.compareTo(s2);
	}
	@Override
	public boolean equals(Object other) {
		if(other instanceof NodeRef){
			NodeRef o=(NodeRef)other;
			return this.repo.equals(o.repo) && this.id.equals(o.id);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(repo, id, archived);
	}
}
