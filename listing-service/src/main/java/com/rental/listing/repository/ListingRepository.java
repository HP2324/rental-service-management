package com.rental.listing.repository;

import com.rental.listing.model.Listing;
import com.rental.listing.model.ListingStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface ListingRepository extends MongoRepository<Listing, String> {

    List<Listing> findByLandlordId(String landlordId);

    List<Listing> findByStatus(ListingStatus status);

    List<Listing> findByCityIgnoreCaseAndStatus(String city, ListingStatus status);

    // Optimized query using the compound index on city + pricePerMonth
    @Query("{ 'city': { $regex: ?0, $options: 'i' }, 'status': 'ACTIVE', 'pricePerMonth': { $lte: ?1 }, 'bedrooms': { $gte: ?2 } }")
    List<Listing> searchListings(String city, BigDecimal maxPrice, int minBedrooms);
}
