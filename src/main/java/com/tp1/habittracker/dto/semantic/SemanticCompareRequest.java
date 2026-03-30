package com.tp1.habittracker.dto.semantic;

import jakarta.validation.constraints.NotBlank;

public record SemanticCompareRequest(
        @NotBlank(message = "text1 is required")
        String text1,

        @NotBlank(message = "text2 is required")
        String text2
) {
}
