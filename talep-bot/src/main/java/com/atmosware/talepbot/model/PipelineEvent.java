package com.atmosware.talepbot.model;

public record PipelineEvent(
        String talepId,
        String agentName,
        String status,
        String message,
        long timestamp
) {
    public static PipelineEvent of(String talepId, String agentName, String status, String message) {
        return new PipelineEvent(talepId, agentName, status, message, System.currentTimeMillis());
    }
}
