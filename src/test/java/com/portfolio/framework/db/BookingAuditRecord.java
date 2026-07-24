package com.portfolio.framework.db;

/**
 * Immutable read model for a row in {@code booking_audit}.
 * Plain data carrier — deliberately not reusing the API's {@code Booking}
 * model, since this represents a *different* system's view of the same
 * data (the DB layer), and coupling the two would make an intentional
 * mismatch (the exact thing these tests are designed to catch) impossible
 * to express.
 */
public record BookingAuditRecord(
        int bookingId,
        String firstname,
        String lastname,
        int totalPrice,
        boolean depositPaid,
        String checkinDate,
        String checkoutDate,
        String additionalNeeds
) {
}
