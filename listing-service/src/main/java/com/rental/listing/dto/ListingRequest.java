package com.rental.listing.dto;

import com.rental.listing.model.ListingStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ListingRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "City is required")
    private String city;

    private String state;
    private String zipCode;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    private BigDecimal pricePerMonth;

    private int bedrooms;
    private int bathrooms;
    private double squareFeet;
    private List<String> amenities;
    private List<String> imageUrls;

    private ListingStatus status = ListingStatus.ACTIVE;
    private LocalDateTime availableFrom;
}
