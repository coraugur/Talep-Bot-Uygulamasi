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
public class DeploySimulatorTool {

    private static final Logger log = LoggerFactory.getLogger(DeploySimulatorTool.class);

    private final Map<String, DeploymentRecord> deployments = new ConcurrentHashMap<>();

    public record DeploymentRecord(
            String id,
            String artifactName,
            String version,
            String status,
            LocalDateTime deployedAt,
            String previousVersion
    ) {}

    @Tool("Uygulamayı canlı ortama deploy eder (simülasyon). Artifact adı ve versiyon alır.")
    public String deploy(
            @P("Deploy edilecek artifact adı") String artifactName,
            @P("Versiyon numarası") String version
    ) {
        String deployId = "DEPLOY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        String previousVersion = deployments.values().stream()
                .filter(d -> d.artifactName().equals(artifactName) && "ACTIVE".equals(d.status()))
                .map(DeploymentRecord::version)
                .findFirst()
                .orElse("none");

        // Eski deployment'ı pasifleştir
        deployments.values().stream()
                .filter(d -> d.artifactName().equals(artifactName) && "ACTIVE".equals(d.status()))
                .forEach(d -> deployments.put(d.id(), new DeploymentRecord(
                        d.id(), d.artifactName(), d.version(), "SUPERSEDED",
                        d.deployedAt(), d.previousVersion())));

        DeploymentRecord record = new DeploymentRecord(
                deployId, artifactName, version, "ACTIVE",
                LocalDateTime.now(), previousVersion);
        deployments.put(deployId, record);

        log.info("Deploy tamamlandı: {} v{} (ID: {})", artifactName, version, deployId);

        return String.format(
                "Deploy başarılı!\n" +
                "  Deploy ID: %s\n" +
                "  Artifact: %s\n" +
                "  Versiyon: %s\n" +
                "  Önceki versiyon: %s\n" +
                "  Zaman: %s\n" +
                "  Durum: ACTIVE",
                deployId, artifactName, version, previousVersion, record.deployedAt());
    }

    @Tool("Bir deployment'ı geri alır (rollback). Deploy ID alır.")
    public String rollback(@P("Geri alınacak deployment ID") String deploymentId) {
        DeploymentRecord record = deployments.get(deploymentId);
        if (record == null) {
            return "Deployment bulunamadı: " + deploymentId;
        }

        deployments.put(deploymentId, new DeploymentRecord(
                record.id(), record.artifactName(), record.version(), "ROLLED_BACK",
                record.deployedAt(), record.previousVersion()));

        log.info("Rollback tamamlandı: {} (önceki versiyon: {})", deploymentId, record.previousVersion());

        return String.format("Rollback başarılı! %s geri alındı. Önceki versiyon: %s",
                deploymentId, record.previousVersion());
    }

    @Tool("Deployment durumunu kontrol eder. Basit bir health check simülasyonu yapar.")
    public String healthCheck(@P("Kontrol edilecek deployment ID") String deploymentId) {
        DeploymentRecord record = deployments.get(deploymentId);
        if (record == null) {
            return "Deployment bulunamadı: " + deploymentId;
        }

        return String.format(
                "Health Check Sonuçları:\n" +
                "  Deploy ID: %s\n" +
                "  Durum: %s\n" +
                "  /actuator/health → 200 OK\n" +
                "  /api/status → 200 OK\n" +
                "  Database bağlantısı → OK\n" +
                "  Memory kullanımı → Normal\n" +
                "  CPU kullanımı → Normal",
                deploymentId, record.status());
    }
}
