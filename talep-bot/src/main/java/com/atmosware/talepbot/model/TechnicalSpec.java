package com.atmosware.talepbot.model;

import java.util.List;

public record TechnicalSpec(
        String summary,
        List<ApiEndpoint> apiEndpoints,
        List<DataModel> dataModels,
        List<String> constraints,
        List<String> edgeCases,
        List<String> securityConsiderations,
        String impactAnalysis
) {
    public record ApiEndpoint(
            String method,
            String path,
            String description,
            String requestBody,
            String responseBody
    ) {}

    public record DataModel(
            String tableName,
            String description,
            List<String> fields,
            List<String> indexes
    ) {}
}
