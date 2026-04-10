package com.atmosware.talepbot.service;

import com.atmosware.talepbot.agent.TalepPipelineOrchestrator;
import com.atmosware.talepbot.agent.TalepPipelineOrchestrator.PipelineResult;
import com.atmosware.talepbot.entity.Talep;
import com.atmosware.talepbot.model.PipelineEvent;
import com.atmosware.talepbot.model.PipelineStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class PipelineService {

    private static final Logger log = LoggerFactory.getLogger(PipelineService.class);

    private final TalepPipelineOrchestrator orchestrator;
    private final TalepService talepService;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final Map<String, SseEmitter> activeEmitters = new ConcurrentHashMap<>();

    public PipelineService(TalepPipelineOrchestrator orchestrator, TalepService talepService) {
        this.orchestrator = orchestrator;
        this.talepService = talepService;
    }

    /**
     * Pipeline'ı asenkron olarak başlatır.
     */
    public void runAsync(String talepId) {
        Talep talep = talepService.getById(talepId);

        if (talep.getStatus() != PipelineStatus.PENDING && talep.getStatus() != PipelineStatus.FAILED) {
            throw new IllegalStateException("Talep zaten işleniyor veya tamamlanmış: " + talep.getStatus());
        }

        talep.setStatus(PipelineStatus.PO_IN_PROGRESS);
        talepService.save(talep);

        executor.submit(() -> executePipeline(talepId, talep.getDescription()));
    }

    /**
     * Pipeline'ı senkron olarak çalıştırır (test amaçlı).
     */
    public PipelineResult runSync(String talepId) {
        Talep talep = talepService.getById(talepId);
        talep.setStatus(PipelineStatus.PO_IN_PROGRESS);
        talepService.save(talep);
        return executePipeline(talepId, talep.getDescription());
    }

    /**
     * SSE emitter oluşturur ve pipeline event'lerini stream eder.
     */
    public SseEmitter createEmitter(String talepId) {
        SseEmitter emitter = new SseEmitter(600_000L); // 10 dakika timeout
        activeEmitters.put(talepId, emitter);
        emitter.onCompletion(() -> activeEmitters.remove(talepId));
        emitter.onTimeout(() -> activeEmitters.remove(talepId));
        emitter.onError(e -> activeEmitters.remove(talepId));
        return emitter;
    }

    private PipelineResult executePipeline(String talepId, String description) {
        PipelineResult result = orchestrator.run(talepId, description, event -> {
            updateTalepStatus(talepId, event);
            sendSseEvent(talepId, event);
        });

        // Sonuçları DB'ye kaydet
        Talep talep = talepService.getById(talepId);
        talep.setUserStory(result.userStory());
        talep.setTechSpec(result.techSpec());
        talep.setCodeOutput(result.codeOutput());
        talep.setTestReport(result.testReport());
        talep.setDeployReport(result.deployReport());
        talep.setIterationCount(result.iterationCount());

        if (result.success()) {
            talep.setStatus(PipelineStatus.COMPLETED);
        } else {
            talep.setStatus(PipelineStatus.FAILED);
            talep.setErrorMessage(result.errorMessage());
        }
        talepService.save(talep);

        // SSE tamamla
        SseEmitter emitter = activeEmitters.remove(talepId);
        if (emitter != null) {
            try {
                emitter.complete();
            } catch (Exception e) {
                log.debug("SSE emitter tamamlanamadı: {}", e.getMessage());
            }
        }

        return result;
    }

    private void updateTalepStatus(String talepId, PipelineEvent event) {
        try {
            Talep talep = talepService.getById(talepId);
            PipelineStatus newStatus = mapEventToStatus(event);
            if (newStatus != null) {
                talep.setStatus(newStatus);
                talepService.save(talep);
            }
        } catch (Exception e) {
            log.warn("Talep durumu güncellenemedi: {}", e.getMessage());
        }
    }

    private PipelineStatus mapEventToStatus(PipelineEvent event) {
        if (!"STARTED".equals(event.status())) return null;
        return switch (event.agentName()) {
            case "PO" -> PipelineStatus.PO_IN_PROGRESS;
            case "ANALYST" -> PipelineStatus.ANALYST_IN_PROGRESS;
            case "DEVELOPER" -> PipelineStatus.DEVELOPER_IN_PROGRESS;
            case "TESTER" -> PipelineStatus.TESTER_IN_PROGRESS;
            case "DEPLOYMENT" -> PipelineStatus.DEPLOYMENT_IN_PROGRESS;
            default -> null;
        };
    }

    private void sendSseEvent(String talepId, PipelineEvent event) {
        SseEmitter emitter = activeEmitters.get(talepId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("pipeline-event")
                        .data(event));
            } catch (IOException e) {
                log.debug("SSE event gönderilemedi: {}", e.getMessage());
                activeEmitters.remove(talepId);
            }
        }
    }
}
