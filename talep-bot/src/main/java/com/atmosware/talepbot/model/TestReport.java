package com.atmosware.talepbot.model;

import java.util.List;

public record TestReport(
        TestStatus status,
        String summary,
        int totalTests,
        int passedTests,
        int failedTests,
        List<TestCase> testCases,
        List<String> bugs,
        List<String> suggestions
) {
    public record TestCase(
            String name,
            String type,
            TestStatus status,
            String description,
            String failureReason
    ) {}
}
