package org.edu_sharing.service.github;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubService {

    @Value("${repository.detectBinderUrls}")
    private boolean enabled;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class GitHubDetails {
        String name;
        String repo;
        String branch;
    }

    public boolean checkForJupyterNotebooks(String url) {
        if (! enabled) {
            return false;
        }
        try {
            GitHubDetails details = getDetails(url);
            List<GitHubContentResponse> contentResponse = callApi(details);
            return checkContent(contentResponse);
        } catch (Exception exception) {
            log.error(exception.getMessage(), exception);
            return false;
        }
    }

    private GitHubDetails getDetails(String url) throws IllegalArgumentException {
        Pattern pattern = Pattern.compile("https://github\\.com/([^/]+)/([^/]+)(?:/tree/([^/]+))?");
        Matcher matcher = pattern.matcher(url);

        if (matcher.find()) {
            String user = matcher.group(1);
            String repository = matcher.group(2);
            String branch = matcher.group(3) != null ? matcher.group(3) : "main";

            return new GitHubDetails(user, repository, branch);
        }
        throw new IllegalArgumentException("Github url invalid");
    }

    private List<GitHubContentResponse> callApi(GitHubDetails details) {
        OkHttpClient client = new OkHttpClient();
        ObjectMapper objectMapper = new ObjectMapper();

        String baseUrl = "https://api.github.com/repos/" + details.name + "/" + details.repo + "/contents";

        HttpUrl.Builder urlBuilder = Objects
                .requireNonNull(HttpUrl.parse(baseUrl))
                .newBuilder()
                .addQueryParameter("ref", details.branch);

        String apiUrl = urlBuilder
                .build()
                .toString();

        Request.Builder requestBuilder = new Request
                .Builder()
                .url(apiUrl)
                .addHeader("Accept", "application/vnd.github.v3+json");

        Request request = requestBuilder.build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                return objectMapper.readValue(responseBody, new TypeReference<List<GitHubContentResponse>>() {
                });
            } else {
                throw new IOException("Request failed: " + response.code() + " " + response.message());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean checkContent(List<GitHubContentResponse> content) {
        List<GitHubContentResponse> notebookEntries = content
                .stream()
                .filter(entry ->
                        entry.getName().endsWith(".ipynb") || entry.getPath().endsWith(".ipynb")
                )
                .collect(Collectors.toList());

        return !notebookEntries.isEmpty();
    }
}
