package org.edu_sharing.service.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubContentResponse {
    private String name;
    private String path;
    private String sha;
    private int size;
    private String url;
    private String html_url;
    private String git_url;
    private String download_url;
    private String type;

    @JsonProperty("_links")
    private Links links;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Links {
        private String self;
        private String git;
        private String html;
    }
}
