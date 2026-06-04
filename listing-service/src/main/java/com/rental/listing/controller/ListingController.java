package com.rental.listing.controller;

import com.rental.listing.dto.ListingRequest;
import com.rental.listing.dto.ListingResponse;
import com.rental.listing.dto.StatusUpdateRequest;
import com.rental.listing.service.ListingService;
import com.rental.shared.dto.ApiResponse;
import com.rental.shared.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/listings")
@RequiredArgsConstructor
public class ListingController {

    private final ListingService listingService;

    // POST /api/listings  [LANDLORD or ADMIN]
    @PostMapping
    @PreAuthorize("hasRole('LANDLORD') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ListingResponse>> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody ListingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Listing created", listingService.create(user.userId(), request)));
    }

    // GET /api/listings  [public — active listings only]
    @GetMapping
    public ResponseEntity<ApiResponse<List<ListingResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(listingService.getAll()));
    }

    // GET /api/listings/search?city=Austin&maxPrice=2000&minBedrooms=2
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ListingResponse>>> search(
            @RequestParam String city,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int minBedrooms) {
        return ResponseEntity.ok(ApiResponse.success(listingService.search(city, maxPrice, minBedrooms)));
    }

    // GET /api/listings/my-listings  [LANDLORD]
    @GetMapping("/my-listings")
    @PreAuthorize("hasRole('LANDLORD') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ListingResponse>>> getMyListings(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(ApiResponse.success(listingService.getMyListings(user.userId())));
    }

    // GET /api/listings/{id}
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ListingResponse>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(listingService.getById(id)));
    }

    // PUT /api/listings/{id}  [LANDLORD owner or ADMIN]
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('LANDLORD') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ListingResponse>> update(
            @PathVariable String id,
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody ListingRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Listing updated",
                listingService.update(id, user.userId(), user.role(), request)));
    }

    // PATCH /api/listings/{id}/status  [any authenticated role — rules enforced in service]
    // TENANT  → INACTIVE only (after paying)
    // LANDLORD → ACTIVE or INACTIVE on own listings
    // ADMIN   → any status on any listing
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ListingResponse>> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody StatusUpdateRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(ApiResponse.success("Status updated",
                listingService.updateStatus(id, request.getStatus(), user.userId(), user.role())));
    }

    // DELETE /api/listings/{id}  [LANDLORD owner or ADMIN]
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('LANDLORD') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable String id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        listingService.delete(id, user.userId(), user.role());
        return ResponseEntity.ok(ApiResponse.success("Listing deleted", null));
    }
}
