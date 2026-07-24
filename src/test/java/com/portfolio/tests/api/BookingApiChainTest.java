package com.portfolio.tests.api;

import com.portfolio.framework.api.BookingApiClient;
import com.portfolio.framework.api.model.Booking;
import com.portfolio.framework.api.model.BookingDates;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * End-to-end CRUD chain against the restful-booker practice API
 * (https://restful-booker.herokuapp.com).
 *
 * <p>Deliberately written as one ordered chain rather than four isolated
 * create/read/update/delete tests. Isolated CRUD tests each have to invent
 * their own throwaway record and cannot prove the API behaves correctly
 * *across* calls (e.g. "the id returned by create actually round-trips
 * through get"). Chaining — create, then feed that id into get, then into
 * update, then into delete — mirrors how a real client of this API behaves
 * and catches a class of bug isolated tests structurally cannot.</p>
 *
 * <p>{@code dependsOnMethods} enforces execution order and marks a step
 * SKIPPED (not FAILED) if an earlier step in the chain failed, since there'd
 * be no valid booking id to work with. Without that dependency wiring, a
 * failed create would leave every later step failing too, for the
 * uninteresting secondary reason "no id available" — which buries the one
 * failure that actually matters in noise.</p>
 */
public class BookingApiChainTest {

    private final BookingApiClient apiClient = new BookingApiClient();

    private String authToken;
    private int createdBookingId;

    @Test(priority = 1, description = "POST /auth — obtain a session token used by update/delete calls")
    public void shouldAuthenticateSuccessfully() {
        authToken = apiClient.authenticate();
        Assert.assertNotNull(authToken, "Expected a non-null auth token from restful-booker");
        Assert.assertFalse(authToken.isBlank(), "Expected a non-blank auth token");
    }

    @Test(priority = 2, dependsOnMethods = "shouldAuthenticateSuccessfully",
            description = "POST /booking — create a booking and capture its generated id")
    public void shouldCreateBooking() {
        Booking newBooking = new Booking(
                "John", "Doe", 150, true,
                new BookingDates("2025-01-01", "2025-01-05"), "Breakfast");

        Response response = apiClient.createBooking(newBooking);

        createdBookingId = response.jsonPath().getInt("bookingid");
        Assert.assertTrue(createdBookingId > 0, "Expected a positive generated booking id");

        // Verify the API echoed back exactly what was sent — catches silent
        // field-mapping bugs on the server side (e.g. a dropped additionalneeds field).
        Assert.assertEquals(response.jsonPath().getString("booking.firstname"), "John");
        Assert.assertEquals(response.jsonPath().getString("booking.lastname"), "Doe");
        Assert.assertEquals(response.jsonPath().getInt("booking.totalprice"), 150);
    }

    @Test(priority = 3, dependsOnMethods = "shouldCreateBooking",
            description = "GET /booking/{id} — confirm the created booking is retrievable by the id from the previous step")
    public void shouldRetrieveCreatedBooking() {
        Booking retrieved = apiClient.getBooking(createdBookingId);

        Assert.assertEquals(retrieved.getFirstname(), "John");
        Assert.assertEquals(retrieved.getLastname(), "Doe");
        Assert.assertEquals(retrieved.getTotalprice(), 150);
        Assert.assertTrue(retrieved.isDepositpaid());
        Assert.assertEquals(retrieved.getBookingdates().getCheckin(), "2025-01-01");
    }

    @Test(priority = 4, dependsOnMethods = "shouldRetrieveCreatedBooking",
            description = "PUT /booking/{id} — full update using the auth token and id captured earlier")
    public void shouldUpdateBooking() {
        Booking updatedBooking = new Booking(
                "Jane", "Smith", 200, false,
                new BookingDates("2025-02-01", "2025-02-10"), "Late checkout");

        Booking result = apiClient.updateBooking(createdBookingId, updatedBooking, authToken);

        Assert.assertEquals(result.getFirstname(), "Jane");
        Assert.assertEquals(result.getLastname(), "Smith");
        Assert.assertEquals(result.getTotalprice(), 200);
        Assert.assertFalse(result.isDepositpaid());

        // Re-fetch independently rather than trusting the PUT response alone —
        // proves the update was actually persisted server-side, not just echoed.
        Booking refetched = apiClient.getBooking(createdBookingId);
        Assert.assertEquals(refetched.getFirstname(), "Jane");
    }

    @Test(priority = 5, dependsOnMethods = "shouldUpdateBooking",
            description = "PATCH /booking/{id} — partial update touching only a subset of fields")
    public void shouldPartiallyUpdateBooking() {
        String partialUpdate = """
                {
                  "firstname": "Janet",
                  "totalprice": 250
                }
                """;

        Booking result = apiClient.partialUpdateBooking(createdBookingId, partialUpdate, authToken);

        Assert.assertEquals(result.getFirstname(), "Janet");
        Assert.assertEquals(result.getTotalprice(), 250);
        // Fields not included in the PATCH body must survive untouched.
        Assert.assertEquals(result.getLastname(), "Smith");
    }

    @Test(priority = 6, dependsOnMethods = "shouldPartiallyUpdateBooking",
            description = "DELETE /booking/{id} — remove the booking created at the start of the chain")
    public void shouldDeleteBooking() {
        Response response = apiClient.deleteBooking(createdBookingId, authToken);

        // restful-booker's documented contract is 201 (not the more conventional
        // 204) on a successful delete — asserting the literal value here so a
        // future upstream contract change surfaces immediately instead of being
        // hidden behind a range check.
        Assert.assertEquals(response.statusCode(), 201, "Expected 201 on successful delete");
    }

    @Test(priority = 7, dependsOnMethods = "shouldDeleteBooking",
            description = "GET /booking/{id} — confirm the deleted booking is no longer retrievable")
    public void shouldReturn404ForDeletedBooking() {
        Response response = apiClient.getBookingRaw(createdBookingId);
        Assert.assertEquals(response.statusCode(), 404,
                "Expected 404 for a booking id that was just deleted");
    }
}
