package com.rental.listing.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StatusUpdateRequest {

    @NotBlank(message = "Status is required")
    private String status; // ACTIVE | INACTIVE
}
