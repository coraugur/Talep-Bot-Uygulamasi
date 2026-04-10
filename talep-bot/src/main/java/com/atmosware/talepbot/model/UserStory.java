package com.atmosware.talepbot.model;

import java.util.List;

public record UserStory(
        String title,
        String description,
        String asA,
        String iWant,
        String soThat,
        List<String> acceptanceCriteria,
        String priority,
        List<String> businessRules,
        List<String> openQuestions
) {}
