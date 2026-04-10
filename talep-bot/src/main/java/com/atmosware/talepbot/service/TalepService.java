package com.atmosware.talepbot.service;

import com.atmosware.talepbot.entity.Talep;
import com.atmosware.talepbot.model.PipelineStatus;
import com.atmosware.talepbot.repository.TalepRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TalepService {

    private final TalepRepository talepRepository;

    public TalepService(TalepRepository talepRepository) {
        this.talepRepository = talepRepository;
    }

    @Transactional
    public Talep create(String description) {
        Talep talep = new Talep(description);
        talep.setStatus(PipelineStatus.PENDING);
        return talepRepository.save(talep);
    }

    public Talep getById(String id) {
        return talepRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Talep bulunamadı: " + id));
    }

    public List<Talep> getAll() {
        return talepRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public Talep updateStatus(String id, PipelineStatus status) {
        Talep talep = getById(id);
        talep.setStatus(status);
        return talepRepository.save(talep);
    }

    @Transactional
    public Talep save(Talep talep) {
        return talepRepository.save(talep);
    }
}
