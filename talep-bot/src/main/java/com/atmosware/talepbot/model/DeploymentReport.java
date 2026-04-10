package com.atmosware.talepbot.model;

import java.time.LocalDateTime;
import java.util.List;

public record DeploymentReport(
        boolean success,
        String environment,
        String version,
        LocalDateTime deployedAt,
        List<String> steps,
        String rollbackPlan,
        List<String> healthCheckResults,
        String notes
) {}
