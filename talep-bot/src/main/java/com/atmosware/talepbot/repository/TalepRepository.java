package com.atmosware.talepbot.repository;

import com.atmosware.talepbot.entity.Talep;
import com.atmosware.talepbot.model.PipelineStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TalepRepository extends JpaRepository<Talep, String> {

    List<Talep> findByStatusOrderByCreatedAtDesc(PipelineStatus status);

    List<Talep> findAllByOrderByCreatedAtDesc();
}
