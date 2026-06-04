package com.rental.listing.service;

import com.rental.listing.dto.ListingRequest;
import com.rental.listing.dto.ListingResponse;
import com.rental.listing.model.Listing;
import com.rental.listing.model.ListingStatus;
import com.rental.listing.repository.ListingRepository;
import com.rental.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListingService")
class ListingServiceTest {

    @Mock ListingRepository listingRepository;

    @InjectMocks ListingService listingService;

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private static final String LANDLORD_ID = "landlord-1";
    private static final String OTHER_LANDLORD_ID = "landlord-2";
    private static final String LISTING_ID = "listing-1";

    private Listing activeListing;
    private ListingRequest validRequest;

    @BeforeEach
    void setUp() {
        activeListing = Listing.builder()
                .id(LISTING_ID)
                .landlordId(LANDLORD_ID)
                .title("Cozy Studio in Austin")
                .description("Great location")
                .address("123 Main St")
                .city("Austin")
                .state("TX")
                .zipCode("78701")
                .pricePerMonth(BigDecimal.valueOf(1500))
                .bedrooms(1)
                .bathrooms(1)
                .squareFeet(600)
                .amenities(List.of("WiFi", "Parking"))
                .status(ListingStatus.ACTIVE)
                .availableFrom(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        validRequest = new ListingRequest();
        validRequest.setTitle("Cozy Studio in Austin");
        validRequest.setDescription("Great location");
        validRequest.setAddress("123 Main St");
        validRequest.setCity("Austin");
        validRequest.setState("TX");
        validRequest.setZipCode("78701");
        validRequest.setPricePerMonth(BigDecimal.valueOf(1500));
        validRequest.setBedrooms(1);
        validRequest.setBathrooms(1);
        validRequest.setSquareFeet(600);
        validRequest.setAmenities(List.of("WiFi", "Parking"));
        validRequest.setStatus(ListingStatus.ACTIVE);
        validRequest.setAvailableFrom(LocalDateTime.now().plusDays(7));
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("persists listing with correct landlordId and returns mapped response")
        void success() {
            when(listingRepository.save(any(Listing.class))).thenReturn(activeListing);

            ListingResponse result = listingService.create(LANDLORD_ID, validRequest);

            assertThat(result.getId()).isEqualTo(LISTING_ID);
            assertThat(result.getLandlordId()).isEqualTo(LANDLORD_ID);
            assertThat(result.getTitle()).isEqualTo("Cozy Studio in Austin");
            assertThat(result.getPricePerMonth()).isEqualByComparingTo(BigDecimal.valueOf(1500));

            // Verify the landlordId was set on the saved entity
            ArgumentCaptor<Listing> captor = ArgumentCaptor.forClass(Listing.class);
            verify(listingRepository).save(captor.capture());
            assertThat(captor.getValue().getLandlordId()).isEqualTo(LANDLORD_ID);
        }
    }

    // ── getAll ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAll")
    class GetAll {

        @Test
        @DisplayName("returns only ACTIVE listings")
        void returnsActiveListings() {
            Listing inactive = Listing.builder()
                    .id("listing-2").landlordId(LANDLORD_ID).status(ListingStatus.INACTIVE)
                    .title("Inactive").city("Dallas").pricePerMonth(BigDecimal.valueOf(1000))
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

            when(listingRepository.findByStatus(ListingStatus.ACTIVE))
                    .thenReturn(List.of(activeListing));

            List<ListingResponse> result = listingService.getAll();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(ListingStatus.ACTIVE);
        }

        @Test
        @DisplayName("returns empty list when no active listings")
        void empty() {
            when(listingRepository.findByStatus(ListingStatus.ACTIVE)).thenReturn(List.of());

            assertThat(listingService.getAll()).isEmpty();
        }
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("returns mapped response for existing ID")
        void success() {
            when(listingRepository.findById(LISTING_ID)).thenReturn(Optional.of(activeListing));

            ListingResponse result = listingService.getById(LISTING_ID);

            assertThat(result.getId()).isEqualTo(LISTING_ID);
            assertThat(result.getCity()).isEqualTo("Austin");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException for unknown ID")
        void notFound() {
            when(listingRepository.findById("bad-id")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> listingService.getById("bad-id"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("persists and returns updated listing when caller is the owner")
        void success() {
            validRequest.setTitle("Updated Title");
            validRequest.setPricePerMonth(BigDecimal.valueOf(1800));

            when(listingRepository.findById(LISTING_ID)).thenReturn(Optional.of(activeListing));
            when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));

            ListingResponse result = listingService.update(LISTING_ID, LANDLORD_ID, "LANDLORD", validRequest);

            assertThat(result.getTitle()).isEqualTo("Updated Title");
            assertThat(result.getPricePerMonth()).isEqualByComparingTo(BigDecimal.valueOf(1800));
            verify(listingRepository).save(any(Listing.class));
        }

        @Test
        @DisplayName("allows ADMIN to update a listing they do not own")
        void adminBypass() {
            validRequest.setTitle("Admin Override");

            when(listingRepository.findById(LISTING_ID)).thenReturn(Optional.of(activeListing));
            when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));

            ListingResponse result = listingService.update(LISTING_ID, OTHER_LANDLORD_ID, "ADMIN", validRequest);

            assertThat(result.getTitle()).isEqualTo("Admin Override");
            verify(listingRepository).save(any(Listing.class));
        }

        @Test
        @DisplayName("throws SecurityException when non-admin caller does not own the listing")
        void notOwner() {
            when(listingRepository.findById(LISTING_ID)).thenReturn(Optional.of(activeListing));

            assertThatThrownBy(() -> listingService.update(LISTING_ID, OTHER_LANDLORD_ID, "LANDLORD", validRequest))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("not own");

            verify(listingRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when listing ID does not exist")
        void listingNotFound() {
            when(listingRepository.findById("bad-id")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> listingService.update("bad-id", LANDLORD_ID, "LANDLORD", validRequest))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("deletes listing when caller is the owner")
        void success() {
            when(listingRepository.findById(LISTING_ID)).thenReturn(Optional.of(activeListing));

            listingService.delete(LISTING_ID, LANDLORD_ID, "LANDLORD");

            verify(listingRepository).deleteById(LISTING_ID);
        }

        @Test
        @DisplayName("allows ADMIN to delete a listing they do not own")
        void adminBypass() {
            when(listingRepository.findById(LISTING_ID)).thenReturn(Optional.of(activeListing));

            listingService.delete(LISTING_ID, OTHER_LANDLORD_ID, "ADMIN");

            verify(listingRepository).deleteById(LISTING_ID);
        }

        @Test
        @DisplayName("throws SecurityException when non-admin caller does not own the listing")
        void notOwner() {
            when(listingRepository.findById(LISTING_ID)).thenReturn(Optional.of(activeListing));

            assertThatThrownBy(() -> listingService.delete(LISTING_ID, OTHER_LANDLORD_ID, "LANDLORD"))
                    .isInstanceOf(SecurityException.class);

            verify(listingRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when listing does not exist")
        void notFound() {
            when(listingRepository.findById("bad-id")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> listingService.delete("bad-id", LANDLORD_ID, "LANDLORD"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── getMyListings ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getMyListings")
    class GetMyListings {

        @Test
        @DisplayName("returns all listings belonging to the given landlord")
        void success() {
            Listing second = Listing.builder()
                    .id("listing-2").landlordId(LANDLORD_ID).title("Second Place")
                    .city("Austin").status(ListingStatus.ACTIVE)
                    .pricePerMonth(BigDecimal.valueOf(2000))
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

            when(listingRepository.findByLandlordId(LANDLORD_ID))
                    .thenReturn(List.of(activeListing, second));

            List<ListingResponse> result = listingService.getMyListings(LANDLORD_ID);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(ListingResponse::getLandlordId)
                    .allMatch(id -> id.equals(LANDLORD_ID));
        }

        @Test
        @DisplayName("returns empty list when landlord has no listings")
        void noListings() {
            when(listingRepository.findByLandlordId(LANDLORD_ID)).thenReturn(List.of());

            assertThat(listingService.getMyListings(LANDLORD_ID)).isEmpty();
        }
    }

    // ── search ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("search")
    class Search {

        @Test
        @DisplayName("passes all filter params to the repository and returns results")
        void withAllFilters() {
            when(listingRepository.searchListings("Austin", BigDecimal.valueOf(2000), 2))
                    .thenReturn(List.of(activeListing));

            List<ListingResponse> result = listingService.search("Austin", BigDecimal.valueOf(2000), 2);

            assertThat(result).hasSize(1);
            verify(listingRepository).searchListings("Austin", BigDecimal.valueOf(2000), 2);
        }

        @Test
        @DisplayName("uses Long.MAX_VALUE as maxPrice when none is provided")
        void noMaxPrice() {
            // When maxPrice is null, service substitutes a very large ceiling
            listingService.search("Austin", null, 0);

            ArgumentCaptor<BigDecimal> priceCaptor = ArgumentCaptor.forClass(BigDecimal.class);
            verify(listingRepository).searchListings(eq("Austin"), priceCaptor.capture(), eq(0));
            assertThat(priceCaptor.getValue()).isEqualByComparingTo(BigDecimal.valueOf(Long.MAX_VALUE));
        }

        @Test
        @DisplayName("returns empty list when no listings match the criteria")
        void noResults() {
            when(listingRepository.searchListings(any(), any(), anyInt())).thenReturn(List.of());

            List<ListingResponse> result = listingService.search("Nowhere", BigDecimal.valueOf(500), 5);

            assertThat(result).isEmpty();
        }
    }
}
