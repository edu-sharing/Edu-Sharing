package org.edu_sharing.restservices.usage.v1.model;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.edu_sharing.restservices.collection.v1.model.Collection;
import org.edu_sharing.restservices.shared.Node;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;;

@Data
public class Usages {

    private List<Usage> usages;

    public Usages() {
        // TODO Auto-generated constructor stub
    }

    public Usages(List<Usage> usages) {
        this.usages = usages;
    }

    @Data
    public static class Usage {
        @JsonProperty(required = true)
        private String appUser;
        @JsonProperty(required = true)
        private String appUserMail;
        @JsonProperty(required = true)
        private String courseId;
        private Integer distinctPersons;
        private Calendar fromUsed;
        @JsonProperty(required = true)
        private String appId;
        @JsonProperty(required = true)
        private String nodeId;
        private Calendar toUsed;
        private Integer usageCounter;
        @JsonProperty(required = true)
        private String parentNodeId;
        @JsonProperty(required = true)
        private String usageVersion;
        private Parameters usageXmlParams;
        private String usageXmlParamsRaw;
        @JsonProperty(required = true)
        private String resourceId;
        private String guid;
        private String appSubtype;
        private String appType;
        private String type;
        private Date created;
        private Date modified;


        @Data
        @XmlRootElement(name = "usage")
        public static class Parameters {
            @XmlElement
            public General general;

            @Data
            public static class General {
                @XmlElement
                public String referencedInName;
                @XmlElement
                public String referencedInType;
                @XmlElement
                public String referencedInInstance;
            }
        }
    }

    @Data
    public static class CollectionUsage extends Usage {
        private Node collection;
        private CollectionUsageType collectionUsageType;

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof CollectionUsage) {
                CollectionUsage that = (CollectionUsage) obj;
                return that.collection.getRef().equals(collection.getRef());
            }
            return super.equals(obj);
        }

        @Override
        public int hashCode() {
            if (collection != null)
                return Objects.hash(collection.getRef());
            return super.hashCode();
        }
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class NodeUsage extends Usage {
        private Node node;
    }

    public enum CollectionUsageType {
        ACTIVE,
        PROPOSAL_PENDING,
        PROPOSAL_DECLINED,
    }
}
