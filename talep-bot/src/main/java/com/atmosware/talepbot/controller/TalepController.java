package com.atmosware.talepbot.controller;

import com.atmosware.talepbot.entity.Talep;
import com.atmosware.talepbot.service.TalepService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/talep")
public class TalepController {

    private final TalepService talepService;

    public TalepController(TalepService talepService) {
        this.talepService = talepService;
    }

    @PostMapping
    public ResponseEntity<Talep> create(@RequestBody Map<String, String> body) {
        String description = body.get("description");
        if (description == null || description.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        Talep talep = talepService.create(description);
        return ResponseEntity.status(HttpStatus.CREATED).body(talep);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Talep> getById(@PathVariable String id) {
        Talep talep = talepService.getById(id);
        return ResponseEntity.ok(talep);
    }

    @GetMapping
    public ResponseEntity<List<Talep>> getAll() {
        return ResponseEntity.ok(talepService.getAll());
    }
}
