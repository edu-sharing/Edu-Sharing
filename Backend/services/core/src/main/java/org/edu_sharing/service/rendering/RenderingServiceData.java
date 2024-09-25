package org.edu_sharing.service.rendering;

import lombok.Getter;
import lombok.Setter;
import org.codehaus.jackson.annotate.JsonProperty;
import org.edu_sharing.repository.client.tools.I18nAngular;
import org.edu_sharing.restservices.NodeDao;
import org.edu_sharing.restservices.shared.*;
import org.edu_sharing.alfresco.service.config.model.Values;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class RenderingServiceData implements Serializable {
    private Node node;
    private List<Node> children;
    private UserRender user;
    private String metadataHTML;
    private Values configValues;

    private NodeUrls nodeUrls;
    
    
    private List<Editor> editors;

    static class Editor implements Serializable {

        private String id;
        private String label;
        private boolean onlyDesktop;

        @JsonProperty
        public void setId(String id) {
            this.id = id;
            this.label = I18nAngular.getTranslationAngular("common", "CONNECTOR." + getId() + ".NAME");
        }

        public String getId() {
            return id;
        }
        public String getLabel() {
            return label;
        }

        public void setOnlyDesktop(boolean onlyDesktop) {
            this.onlyDesktop = onlyDesktop;
        }

        public boolean isOnlyDesktop() {
            return onlyDesktop;
        }
    }
}
