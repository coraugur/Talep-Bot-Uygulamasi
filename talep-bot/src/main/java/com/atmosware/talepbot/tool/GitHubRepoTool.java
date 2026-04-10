package com.atmosware.talepbot.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class GitHubRepoTool {

    private static final Logger log = LoggerFactory.getLogger(GitHubRepoTool.class);
    private static final String GITHUB_API = "https://api.github.com";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${github.token:}")
    private String githubToken;

    @Value("${github.default-owner:}")
    private String defaultOwner;

    @Value("${github.default-repo:}")
    private String defaultRepo;

    @Tool("GitHub reposundaki dosyaları listeler. owner, repo ve path parametreleri alır. Boş bırakılırsa varsayılan repo kullanılır.")
    public String listFiles(
            @P("Repository sahibi (boş ise varsayılan kullanılır)") String owner,
            @P("Repository adı (boş ise varsayılan kullanılır)") String repo,
            @P("Klasör yolu (örn: src/main/java)") String path
    ) {
        String resolvedOwner = (owner == null || owner.isBlank()) ? defaultOwner : owner;
        String resolvedRepo = (repo == null || repo.isBlank()) ? defaultRepo : repo;

        if (resolvedOwner.isBlank() || resolvedRepo.isBlank()) {
            return "GitHub yapılandırması eksik. owner ve repo belirtilmeli.";
        }

        String url = String.format("%s/repos/%s/%s/contents/%s", GITHUB_API, resolvedOwner, resolvedRepo,
                path == null ? "" : path);

        try {
            String response = doGet(url);
            JsonNode tree = objectMapper.readTree(response);

            StringBuilder result = new StringBuilder("Dosya listesi (" + path + "):\n");
            if (tree.isArray()) {
                for (JsonNode node : tree) {
                    String type = node.get("type").asText();
                    String name = node.get("name").asText();
                    result.append(String.format("  [%s] %s\n", type, name));
                }
            }
            return result.toString();
        } catch (Exception e) {
            log.error("GitHub listFiles hatası: {}", e.getMessage());
            return "GitHub dosya listesi alınamadı: " + e.getMessage();
        }
    }

    @Tool("GitHub reposundan bir dosyanın içeriğini okur. owner, repo ve dosya yolu parametreleri alır.")
    public String readFile(
            @P("Repository sahibi (boş ise varsayılan kullanılır)") String owner,
            @P("Repository adı (boş ise varsayılan kullanılır)") String repo,
            @P("Dosya yolu (örn: src/main/java/App.java)") String filePath
    ) {
        String resolvedOwner = (owner == null || owner.isBlank()) ? defaultOwner : owner;
        String resolvedRepo = (repo == null || repo.isBlank()) ? defaultRepo : repo;

        if (resolvedOwner.isBlank() || resolvedRepo.isBlank()) {
            return "GitHub yapılandırması eksik. owner ve repo belirtilmeli.";
        }

        String url = String.format("%s/repos/%s/%s/contents/%s", GITHUB_API, resolvedOwner, resolvedRepo, filePath);

        try {
            String response = doGet(url);
            JsonNode node = objectMapper.readTree(response);

            if (node.has("content")) {
                String encoded = node.get("content").asText().replaceAll("\\s", "");
                String content = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);

                if (content.length() > 10000) {
                    content = content.substring(0, 10000) + "\n... (dosya 10000 karakterden sonra kesildi)";
                }
                return "Dosya: " + filePath + "\n---\n" + content;
            }
            return "Dosya içeriği okunamadı: " + filePath;
        } catch (Exception e) {
            log.error("GitHub readFile hatası: {}", e.getMessage());
            return "GitHub dosya okunamadı: " + e.getMessage();
        }
    }

    private String doGet(String url) throws IOException, InterruptedException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/vnd.github.v3+json")
                .GET();

        if (githubToken != null && !githubToken.isBlank()) {
            requestBuilder.header("Authorization", "Bearer " + githubToken);
        }

        HttpResponse<String> response = httpClient.send(requestBuilder.build(),
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("GitHub API hata: " + response.statusCode() + " - " + response.body());
        }
        return response.body();
    }
}
