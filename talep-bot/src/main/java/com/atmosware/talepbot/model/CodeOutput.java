package com.atmosware.talepbot.model;

import java.util.List;

public record CodeOutput(
        String summary,
        List<CodeFile> files,
        List<String> configChanges,
        List<String> dependencies
) {
    public record CodeFile(
            String filePath,
            String language,
            String content,
            String description
    ) {}
}
