package com.rental.listing.dto;

import com.rental.listing.model.ListingStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ListingResponse {
    private String id;
    private String landlordId;
    private String title;
    private String description;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private BigDecimal pricePerMonth;
    private int bedrooms;
    private int bathrooms;
    private double squareFeet;
    private List<String> amenities;
    private List<String> imageUrls;
    private ListingStatus status;
    private LocalDateTime availableFrom;
    private LocalDateTime createdAt;
}
