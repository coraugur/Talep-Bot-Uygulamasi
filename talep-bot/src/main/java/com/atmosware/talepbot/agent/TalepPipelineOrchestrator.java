package com.atmosware.talepbot.agent;

import com.atmosware.talepbot.model.PipelineEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Pipeline Orchestrator — Talep'i sırasıyla PO → Analist → Developer ↔ Tester (loop) → Deployment
 * agent'larından geçirir. Developer-Tester arasında loop vardır; Tester FAIL dönerse
 * Developer tekrar çağrılır (max N iterasyon).
 */
@Component
public class TalepPipelineOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(TalepPipelineOrchestrator.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ProductOwnerAgent poAgent;
    private final AnalystAgent analystAgent;
    private final DeveloperAgent developerAgent;
    private final TesterAgent testerAgent;
    private final DeploymentAgent deploymentAgent;

    @Value("${pipeline.developer-tester-max-iterations:3}")
    private int maxIterations;

    @Value("${pipeline.agent-delay-seconds:30}")
    private int agentDelaySeconds;

    public TalepPipelineOrchestrator(
            ProductOwnerAgent poAgent,
            AnalystAgent analystAgent,
            DeveloperAgent developerAgent,
            TesterAgent testerAgent,
            DeploymentAgent deploymentAgent
    ) {
        this.poAgent = poAgent;
        this.analystAgent = analystAgent;
        this.developerAgent = developerAgent;
        this.testerAgent = testerAgent;
        this.deploymentAgent = deploymentAgent;
    }

    public record PipelineResult(
            String userStory,
            String techSpec,
            String codeOutput,
            String testReport,
            String deployReport,
            int iterationCount,
            boolean success,
            String errorMessage,
            List<PipelineEvent> events
    ) {}

    /**
     * Pipeline'ı çalıştırır. Her adımda eventListener callback'i çağrılır.
     */
    public PipelineResult run(String talepId, String talepDescription, Consumer<PipelineEvent> eventListener) {
        List<PipelineEvent> events = new ArrayList<>();
        Consumer<PipelineEvent> notify = event -> {
            events.add(event);
            if (eventListener != null) {
                eventListener.accept(event);
            }
        };

        String userStory = null;
        String techSpec = null;
        String codeOutput = null;
        String testReport = null;
        String deployReport = null;
        int iterationCount = 0;

        try {
            // ============ STEP 1: PO Agent ============
            log.info("[Pipeline:{}] PO Agent başlıyor...", talepId);
            notify.accept(PipelineEvent.of(talepId, "PO", "STARTED", "İş talebi analiz ediliyor..."));

            userStory = poAgent.processRequest(talepDescription);
            log.info("[Pipeline:{}] PO Agent tamamlandı.", talepId);
            notify.accept(PipelineEvent.of(talepId, "PO", "COMPLETED", "User Story oluşturuldu."));

            rateLimitDelay(talepId, "PO → ANALYST", notify);

            // ============ STEP 2: Analyst Agent ============
            log.info("[Pipeline:{}] Analist Agent başlıyor...", talepId);
            notify.accept(PipelineEvent.of(talepId, "ANALYST", "STARTED", "Teknik analiz yapılıyor..."));

            String repoContext = "Mevcut repo bilgisi mevcut değil — yeni bir proje olarak tasarla.";
            techSpec = analystAgent.analyze(userStory, repoContext);
            log.info("[Pipeline:{}] Analist Agent tamamlandı.", talepId);
            notify.accept(PipelineEvent.of(talepId, "ANALYST", "COMPLETED", "Teknik spesifikasyon hazırlandı."));

            rateLimitDelay(talepId, "ANALYST → DEVELOPER", notify);

            // ============ STEP 3: Developer ↔ Tester Loop ============
            String previousTestFeedback = "İlk geliştirme — önceki test geri bildirimi yok.";
            String acceptanceCriteria = extractAcceptanceCriteria(userStory);
            boolean testPassed = false;

            for (iterationCount = 1; iterationCount <= maxIterations; iterationCount++) {
                // --- Developer ---
                log.info("[Pipeline:{}] Developer Agent başlıyor (iterasyon {}/{})...", talepId, iterationCount, maxIterations);
                notify.accept(PipelineEvent.of(talepId, "DEVELOPER", "STARTED",
                        "Geliştirme yapılıyor (iterasyon " + iterationCount + "/" + maxIterations + ")..."));

                codeOutput = developerAgent.develop(techSpec, previousTestFeedback, repoContext);
                log.info("[Pipeline:{}] Developer Agent tamamlandı (iterasyon {}).", talepId, iterationCount);
                notify.accept(PipelineEvent.of(talepId, "DEVELOPER", "COMPLETED",
                        "Kod üretildi (iterasyon " + iterationCount + ")."));

                rateLimitDelay(talepId, "DEVELOPER → TESTER", notify);

                // --- Tester ---
                log.info("[Pipeline:{}] Tester Agent başlıyor (iterasyon {}/{})...", talepId, iterationCount, maxIterations);
                notify.accept(PipelineEvent.of(talepId, "TESTER", "STARTED",
                        "Test yapılıyor (iterasyon " + iterationCount + "/" + maxIterations + ")..."));

                testReport = testerAgent.test(codeOutput, acceptanceCriteria, techSpec);
                log.info("[Pipeline:{}] Tester Agent tamamlandı (iterasyon {}).", talepId, iterationCount);

                testPassed = isTestPassed(testReport);

                if (testPassed) {
                    notify.accept(PipelineEvent.of(talepId, "TESTER", "COMPLETED",
                            "Testler BAŞARILI (iterasyon " + iterationCount + "). ✓"));
                    break;
                } else {
                    notify.accept(PipelineEvent.of(talepId, "TESTER", "FAILED",
                            "Testler BAŞARISIZ (iterasyon " + iterationCount + "). Developer'a geri gönderiliyor..."));
                    previousTestFeedback = "Önceki test raporu (iterasyon " + iterationCount + "):\n" + testReport
                            + "\n\nLütfen belirtilen hataları düzelt ve kodu güncelle.";
                    rateLimitDelay(talepId, "TESTER → DEVELOPER (retry)", notify);
                }
            }

            if (!testPassed) {
                String message = "Developer-Tester döngüsü " + maxIterations + " iterasyondan sonra başarısız oldu.";
                log.warn("[Pipeline:{}] {}", talepId, message);
                notify.accept(PipelineEvent.of(talepId, "PIPELINE", "FAILED", message));
                return new PipelineResult(userStory, techSpec, codeOutput, testReport, null,
                        iterationCount, false, message, events);
            }

            rateLimitDelay(talepId, "TESTER → DEPLOYMENT", notify);

            // ============ STEP 4: Deployment Agent ============
            log.info("[Pipeline:{}] Deployment Agent başlıyor...", talepId);
            notify.accept(PipelineEvent.of(talepId, "DEPLOYMENT", "STARTED", "Deployment başlatılıyor..."));

            deployReport = deploymentAgent.deploy(codeOutput, testReport);
            log.info("[Pipeline:{}] Deployment Agent tamamlandı.", talepId);
            notify.accept(PipelineEvent.of(talepId, "DEPLOYMENT", "COMPLETED", "Deployment tamamlandı. ✓"));

            // ============ DONE ============
            notify.accept(PipelineEvent.of(talepId, "PIPELINE", "COMPLETED",
                    "Pipeline başarıyla tamamlandı! Toplam iterasyon: " + iterationCount));

            return new PipelineResult(userStory, techSpec, codeOutput, testReport, deployReport,
                    iterationCount, true, null, events);

        } catch (Exception e) {
            log.error("[Pipeline:{}] Hata: {}", talepId, e.getMessage(), e);
            notify.accept(PipelineEvent.of(talepId, "PIPELINE", "ERROR", "Hata: " + e.getMessage()));
            return new PipelineResult(userStory, techSpec, codeOutput, testReport, deployReport,
                    iterationCount, false, e.getMessage(), events);
        }
    }

    private boolean isTestPassed(String testReport) {
        if (testReport == null) return false;
        try {
            JsonNode node = objectMapper.readTree(testReport);
            if (node.has("status")) {
                return "PASS".equalsIgnoreCase(node.get("status").asText());
            }
        } catch (Exception e) {
            log.warn("Test raporu JSON parse edilemedi, metin analizi yapılıyor...");
        }
        // Fallback: metin içinde PASS/FAIL ara
        String upper = testReport.toUpperCase();
        return upper.contains("\"STATUS\":\"PASS\"") || upper.contains("\"STATUS\": \"PASS\"")
                || (upper.contains("PASS") && !upper.contains("FAIL"));
    }

    private void rateLimitDelay(String talepId, String transition, Consumer<PipelineEvent> notify) {
        if (agentDelaySeconds > 0) {
            log.info("[Pipeline:{}] Rate limit bekleme: {} — {} saniye...", talepId, transition, agentDelaySeconds);
            notify.accept(PipelineEvent.of(talepId, "PIPELINE", "WAITING",
                    "Rate limit bekleniyor (" + agentDelaySeconds + "s): " + transition));
            try {
                Thread.sleep(agentDelaySeconds * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("[Pipeline:{}] Rate limit bekleme kesildi.", talepId);
            }
        }
    }

    private String extractAcceptanceCriteria(String userStory) {
        try {
            JsonNode node = objectMapper.readTree(userStory);
            if (node.has("acceptanceCriteria")) {
                return node.get("acceptanceCriteria").toString();
            }
        } catch (Exception e) {
            log.warn("User Story'den kabul kriterleri çıkarılamadı, tamamını gönderiyorum.");
        }
        return userStory;
    }
}
