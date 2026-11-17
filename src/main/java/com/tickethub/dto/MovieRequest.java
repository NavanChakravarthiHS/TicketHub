package com.tickethub.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Body of create and update movie. The same checks apply to both. */
public record MovieRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be at most 200 characters")
        String title,

        @NotBlank(message = "Description is required")
        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,

        @NotNull(message = "Duration is required")
        @Min(value = 1, message = "Duration must be at least 1 minute")
        @Max(value = 500, message = "Duration must be at most 500 minutes")
        Integer durationMinutes,

        @NotBlank(message = "Language is required")
        @Size(max = 50, message = "Language must be at most 50 characters")
        String language
) {
}
