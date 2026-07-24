package com.portfolio.framework.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Maps to restful-booker's nested {@code bookingdates} object.
 * Kept as its own class (rather than flattening checkin/checkout onto
 * {@link Booking}) so it serializes/deserializes to the exact nested JSON
 * shape the API expects — a flattened POJO would need custom Jackson
 * mapping to reproduce the same wire format.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookingDates {

    private String checkin;
    private String checkout;

    public BookingDates() {
        // Required by Jackson for deserialization
    }

    public BookingDates(String checkin, String checkout) {
        this.checkin = checkin;
        this.checkout = checkout;
    }

    public String getCheckin() {
        return checkin;
    }

    public void setCheckin(String checkin) {
        this.checkin = checkin;
    }

    public String getCheckout() {
        return checkout;
    }

    public void setCheckout(String checkout) {
        this.checkout = checkout;
    }
}
