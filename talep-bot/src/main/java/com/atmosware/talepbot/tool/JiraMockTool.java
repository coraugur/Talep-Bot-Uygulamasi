package com.atmosware.talepbot.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JiraMockTool {

    private static final Logger log = LoggerFactory.getLogger(JiraMockTool.class);

    private final Map<String, JiraIssue> issues = new ConcurrentHashMap<>();

    public record JiraIssue(
            String id,
            String title,
            String description,
            String status,
            String assignee,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    @Tool("Jira'da yeni bir issue oluşturur. Başlık ve açıklama alır, issue ID döner.")
    public String createIssue(
            @P("Issue başlığı") String title,
            @P("Issue açıklaması") String description
    ) {
        String id = "TALEP-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        JiraIssue issue = new JiraIssue(id, title, description, "TO_DO", "unassigned",
                LocalDateTime.now(), LocalDateTime.now());
        issues.put(id, issue);
        log.info("Jira issue oluşturuldu: {} - {}", id, title);
        return "Issue oluşturuldu: " + id;
    }

    @Tool("Jira issue durumunu günceller. Issue ID ve yeni durum alır.")
    public String updateIssueStatus(
            @P("Issue ID (örn: TALEP-ABC123)") String issueId,
            @P("Yeni durum: TO_DO, IN_PROGRESS, IN_REVIEW, DONE, REJECTED") String newStatus
    ) {
        JiraIssue existing = issues.get(issueId);
        if (existing == null) {
            return "Hata: Issue bulunamadı: " + issueId;
        }
        JiraIssue updated = new JiraIssue(existing.id(), existing.title(), existing.description(),
                newStatus, existing.assignee(), existing.createdAt(), LocalDateTime.now());
        issues.put(issueId, updated);
        log.info("Jira issue güncellendi: {} -> {}", issueId, newStatus);
        return "Issue durumu güncellendi: " + issueId + " -> " + newStatus;
    }

    @Tool("Jira issue detaylarını getirir. Issue ID alır.")
    public String getIssue(@P("Issue ID") String issueId) {
        JiraIssue issue = issues.get(issueId);
        if (issue == null) {
            return "Issue bulunamadı: " + issueId;
        }
        return String.format("ID: %s, Başlık: %s, Durum: %s, Açıklama: %s",
                issue.id(), issue.title(), issue.status(), issue.description());
    }

    public Map<String, JiraIssue> getAllIssues() {
        return Map.copyOf(issues);
    }
}
