package com.rental.shared.security;

/**
 * Represents the authenticated principal extracted from a validated JWT.
 * Used as the Authentication principal in listing-service and payment-service
 * so controllers can access userId and role without a DB lookup.
 */
public record AuthenticatedUser(String userId, String email, String role) {}
