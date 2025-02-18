package org.edu_sharing.restservices.shared;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.restservices.NodeDao;
import org.edu_sharing.restservices.collection.v1.model.Collection;
import org.edu_sharing.service.mime.MimeTypesV2;
import org.edu_sharing.service.model.NodeRefImpl;
import org.edu_sharing.service.rating.RatingDetails;

import java.io.Serializable;
import java.util.*;

@Data
public class Node implements Serializable {

	@JsonProperty(required = true)
	private NodeRef ref = null;

	private NodeRef parent = null;
	private NodeLTIDeepLink nodeLTIDeepLink = null;
	private Remote remote = null;
	private String type = null;
	private List<String> aspects = new ArrayList<>();
	@JsonProperty(required = true)
	private String name = null;
	private String title = null;
	private String metadataset = null;
	private String repositoryType = null;

	@JsonProperty(required = true)
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssX")
	private Date createdAt = null;

	@JsonProperty(required = true)
	private Person createdBy = null;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssX")
	private Date modifiedAt = null;

	private Person modifiedBy = null;

	@JsonProperty(required = true)
	private List<String> access = null;
	/**
	 * the effective access
	 * this is the effective access, i.e. if this element is used in a collection, it will get more permissions
	 * please use this field to check access
	 */
	private java.util.Collection<String> accessEffective;

	private String downloadUrl = null;

	private Map<String,String[]> properties = null;
	private String mimetype = null;
	private String mediatype = null;
	private String size = null;
	private Preview preview = null;
	private Content content = null;
	private String iconURL;
	private License license;
	@JsonProperty("isDirectory")
	private boolean directory;
	private Collection collection;

	@JsonProperty(required = true)
	private Person owner;

	private int commentCount;
	private RatingDetails rating;
	private List<Node> usedInCollections = new ArrayList<>();
	private Map<NodeRefImpl.Relation, Node> relations;
	private List<Contributor> contributors;
	@JsonProperty("isPublic")
	private boolean isPublic;

	/**
	 * fake a node from a ref
	 * i.e. if the real node could not be fetched or is deleted
	 * @param ref
	 * @return
	 */
	public static Node FakeFromRef(NodeRef ref) throws IllegalAccessException, InstantiationException {
		Node node = NodeDao.createEmptyDummy(Node.class, ref);
		node.setPreview(new Preview());
		node.setIconURL(new MimeTypesV2().getDefaultIcon());
		HashMap<String, String[]> props = new HashMap<>();
		props.put(CCConstants.getValidLocalName(CCConstants.CM_NAME), new String[]{ref.getId()});
		node.setProperties(props);
		return node;
	}


	@Override
	public String toString() {
        return "class Node {\n" +
                "  ref: " + ref + "\n" +
                "  parent: " + parent + "\n" +
                "  type: " + type + "\n" +
                "  aspects: " + aspects + "\n" +
                "  name: " + name + "\n" +
                "  title: " + title + "\n" +
                "  createdAt: " + createdAt + "\n" +
                "  createdBy: " + createdBy + "\n" +
                "  modifiedAt: " + modifiedAt + "\n" +
                "  modifiedBy: " + modifiedBy + "\n" +
                "  access: " + access + "\n" +
                "  properties: " + properties + "\n" +
                "  mimetype: " + mimetype + "\n" +
                "  size: " + size + "\n" +
                "  preview: " + preview + "\n" +
                "}\n";
	}
}
