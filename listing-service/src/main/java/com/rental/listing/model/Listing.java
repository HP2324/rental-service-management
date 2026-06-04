package com.rental.listing.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "listings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
// Compound index for the most common search pattern: city + price filter
@CompoundIndex(name = "city_price_idx", def = "{'city': 1, 'pricePerMonth': 1}")
public class Listing {

    @Id
    private String id;

    @Indexed
    private String landlordId;

    private String title;
    private String description;
    private String address;

    @Indexed
    private String city;

    private String state;
    private String zipCode;
    private BigDecimal pricePerMonth;
    private int bedrooms;
    private int bathrooms;
    private double squareFeet;
    private List<String> amenities;
    private List<String> imageUrls;

    @Indexed
    private ListingStatus status;

    private LocalDateTime availableFrom;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
