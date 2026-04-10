package com.atmosware.talepbot.entity;

import com.atmosware.talepbot.model.PipelineStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "talep")
public class Talep {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PipelineStatus status = PipelineStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "user_story", columnDefinition = "TEXT")
    private String userStory;

    @Column(name = "tech_spec", columnDefinition = "TEXT")
    private String techSpec;

    @Column(name = "code_output", columnDefinition = "TEXT")
    private String codeOutput;

    @Column(name = "test_report", columnDefinition = "TEXT")
    private String testReport;

    @Column(name = "deploy_report", columnDefinition = "TEXT")
    private String deployReport;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "iteration_count")
    private int iterationCount = 0;

    public Talep() {}

    public Talep(String description) {
        this.description = description;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // --- Getters & Setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public PipelineStatus getStatus() { return status; }
    public void setStatus(PipelineStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getUserStory() { return userStory; }
    public void setUserStory(String userStory) { this.userStory = userStory; }

    public String getTechSpec() { return techSpec; }
    public void setTechSpec(String techSpec) { this.techSpec = techSpec; }

    public String getCodeOutput() { return codeOutput; }
    public void setCodeOutput(String codeOutput) { this.codeOutput = codeOutput; }

    public String getTestReport() { return testReport; }
    public void setTestReport(String testReport) { this.testReport = testReport; }

    public String getDeployReport() { return deployReport; }
    public void setDeployReport(String deployReport) { this.deployReport = deployReport; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public int getIterationCount() { return iterationCount; }
    public void setIterationCount(int iterationCount) { this.iterationCount = iterationCount; }
}
