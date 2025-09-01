package ru.brombin.incident_retrieval_service.entity;

import java.util.List;

public record PreprocessedQuestion(
        String normalized, List<String> variants
) {}