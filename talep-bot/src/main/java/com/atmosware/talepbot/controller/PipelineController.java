package com.atmosware.talepbot.controller;

import com.atmosware.talepbot.service.PipelineService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/pipeline")
public class PipelineController {

    private final PipelineService pipelineService;

    public PipelineController(PipelineService pipelineService) {
        this.pipelineService = pipelineService;
    }

    /**
     * Pipeline'ı asenkron olarak başlatır.
     * POST /api/pipeline/run/{talepId}
     */
    @PostMapping("/run/{talepId}")
    public ResponseEntity<Map<String, String>> runPipeline(@PathVariable String talepId) {
        pipelineService.runAsync(talepId);
        return ResponseEntity.accepted().body(Map.of(
                "message", "Pipeline başlatıldı",
                "talepId", talepId,
                "streamUrl", "/api/pipeline/stream/" + talepId
        ));
    }

    /**
     * Pipeline'ı senkron olarak çalıştırır (test/debug amaçlı).
     * POST /api/pipeline/run-sync/{talepId}
     */
    @PostMapping("/run-sync/{talepId}")
    public ResponseEntity<?> runPipelineSync(@PathVariable String talepId) {
        var result = pipelineService.runSync(talepId);
        return ResponseEntity.ok(result);
    }

    /**
     * Pipeline event'lerini SSE ile stream eder.
     * GET /api/pipeline/stream/{talepId}
     */
    @GetMapping(value = "/stream/{talepId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamPipelineEvents(@PathVariable String talepId) {
        return pipelineService.createEmitter(talepId);
    }
}
