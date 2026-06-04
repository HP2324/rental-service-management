package com.rental.listing.service;

import com.rental.listing.dto.ListingRequest;
import com.rental.listing.dto.ListingResponse;
import com.rental.listing.model.Listing;
import com.rental.listing.model.ListingStatus;
import com.rental.listing.repository.ListingRepository;
import com.rental.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ListingService {

    private final ListingRepository listingRepository;

    public ListingResponse create(String landlordId, ListingRequest request) {
        Listing listing = Listing.builder()
                .landlordId(landlordId)
                .title(request.getTitle())
                .description(request.getDescription())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .zipCode(request.getZipCode())
                .pricePerMonth(request.getPricePerMonth())
                .bedrooms(request.getBedrooms())
                .bathrooms(request.getBathrooms())
                .squareFeet(request.getSquareFeet())
                .amenities(request.getAmenities())
                .imageUrls(request.getImageUrls())
                .status(request.getStatus())
                .availableFrom(request.getAvailableFrom())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return toResponse(listingRepository.save(listing));
    }

    public List<ListingResponse> getAll() {
        return listingRepository.findByStatus(ListingStatus.ACTIVE)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public ListingResponse getById(String id) {
        return listingRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", id));
    }

    public ListingResponse update(String id, String callerId, String callerRole, ListingRequest request) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", id));

        boolean isAdmin = "ADMIN".equals(callerRole);
        if (!isAdmin && !listing.getLandlordId().equals(callerId)) {
            throw new SecurityException("You do not own this listing");
        }

        listing.setTitle(request.getTitle());
        listing.setDescription(request.getDescription());
        listing.setAddress(request.getAddress());
        listing.setCity(request.getCity());
        listing.setState(request.getState());
        listing.setZipCode(request.getZipCode());
        listing.setPricePerMonth(request.getPricePerMonth());
        listing.setBedrooms(request.getBedrooms());
        listing.setBathrooms(request.getBathrooms());
        listing.setSquareFeet(request.getSquareFeet());
        listing.setAmenities(request.getAmenities());
        listing.setImageUrls(request.getImageUrls());
        listing.setStatus(request.getStatus());
        listing.setAvailableFrom(request.getAvailableFrom());
        listing.setUpdatedAt(LocalDateTime.now());

        return toResponse(listingRepository.save(listing));
    }

    public void delete(String id, String callerId, String callerRole) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", id));

        boolean isAdmin = "ADMIN".equals(callerRole);
        if (!isAdmin && !listing.getLandlordId().equals(callerId)) {
            throw new SecurityException("You do not own this listing");
        }

        listingRepository.deleteById(id);
    }

    public List<ListingResponse> getMyListings(String landlordId) {
        return listingRepository.findByLandlordId(landlordId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * Updates the status of a listing with role-based rules:
     *   TENANT  → can only set INACTIVE (e.g. after paying rent)
     *   LANDLORD → can set any status, but only on their own listings
     *   ADMIN    → can set any status on any listing
     */
    public ListingResponse updateStatus(String id, String newStatus, String callerId, String callerRole) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", id));

        boolean isAdmin    = "ADMIN".equals(callerRole);
        boolean isLandlord = "LANDLORD".equals(callerRole);
        boolean isTenant   = "TENANT".equals(callerRole);

        if (isTenant && !"INACTIVE".equals(newStatus)) {
            throw new SecurityException("Tenants can only mark a listing as inactive");
        }
        if (isLandlord && !listing.getLandlordId().equals(callerId)) {
            throw new SecurityException("You do not own this listing");
        }

        listing.setStatus(ListingStatus.valueOf(newStatus));
        listing.setUpdatedAt(LocalDateTime.now());
        return toResponse(listingRepository.save(listing));
    }

    public List<ListingResponse> search(String city, BigDecimal maxPrice, int minBedrooms) {
        BigDecimal effectiveMaxPrice = maxPrice != null ? maxPrice : BigDecimal.valueOf(Long.MAX_VALUE);
        return listingRepository.searchListings(city, effectiveMaxPrice, minBedrooms)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private ListingResponse toResponse(Listing listing) {
        return ListingResponse.builder()
                .id(listing.getId())
                .landlordId(listing.getLandlordId())
                .title(listing.getTitle())
                .description(listing.getDescription())
                .address(listing.getAddress())
                .city(listing.getCity())
                .state(listing.getState())
                .zipCode(listing.getZipCode())
                .pricePerMonth(listing.getPricePerMonth())
                .bedrooms(listing.getBedrooms())
                .bathrooms(listing.getBathrooms())
                .squareFeet(listing.getSquareFeet())
                .amenities(listing.getAmenities())
                .imageUrls(listing.getImageUrls())
                .status(listing.getStatus())
                .availableFrom(listing.getAvailableFrom())
                .createdAt(listing.getCreatedAt())
                .build();
    }
}
