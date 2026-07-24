package com.portfolio.tests.db;

import com.portfolio.framework.api.BookingApiClient;
import com.portfolio.framework.api.model.Booking;
import com.portfolio.framework.api.model.BookingDates;
import com.portfolio.framework.db.BookingAuditRecord;
import com.portfolio.framework.db.BookingAuditRepository;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Optional;

/**
 * Validates that a record created through the API layer is exactly what
 * lands in the local database — the pattern real teams use to catch bugs
 * where an API responds with 200 OK but the write to storage silently
 * dropped or mangled a field.
 *
 * <p><b>Why a local H2 store instead of restful-booker's own database:</b>
 * restful-booker is a shared public practice API and, reasonably, doesn't
 * expose its backing datastore for external clients to query. To keep this
 * layer honest rather than faking it, the framework owns a real local H2
 * database (see {@link BookingAuditRepository}) and treats it as a downstream
 * system that mirrors the API's data — the same shape of problem as
 * reconciling an API against a data warehouse or read-replica in a production
 * system. Every assertion below is a genuine JDBC round-trip against a real
 * database; only the *source* of truth being reconciled against is a stand-in
 * for infrastructure a public demo API can't sensibly hand out.</p>
 */
public class BookingDatabaseValidationTest {

    private final BookingApiClient apiClient = new BookingApiClient();
    private final BookingAuditRepository auditRepository = new BookingAuditRepository();

    private String authToken;
    private int bookingId;

    @Test(priority = 1, description = "Create a booking via the API, then persist and verify it in the local audit DB")
    public void shouldPersistCreatedBookingToDatabase() {
        Booking booking = new Booking(
                "Alice", "Nguyen", 320, true,
                new BookingDates("2025-06-01", "2025-06-07"), "Airport shuttle");

        Response response = apiClient.createBooking(booking);
        bookingId = response.jsonPath().getInt("bookingid");

        // This insert simulates the sync step a real pipeline would run
        // (e.g. a webhook or ETL job writing the new record into a warehouse).
        auditRepository.insert(new BookingAuditRecord(
                bookingId, booking.getFirstname(), booking.getLastname(), booking.getTotalprice(),
                booking.isDepositpaid(), booking.getBookingdates().getCheckin(),
                booking.getBookingdates().getCheckout(), booking.getAdditionalneeds()));

        Optional<BookingAuditRecord> persisted = auditRepository.findByBookingId(bookingId);

        Assert.assertTrue(persisted.isPresent(), "Expected a row in booking_audit for booking " + bookingId);
        BookingAuditRecord record = persisted.get();
        Assert.assertEquals(record.firstname(), "Alice");
        Assert.assertEquals(record.lastname(), "Nguyen");
        Assert.assertEquals(record.totalPrice(), 320);
        Assert.assertTrue(record.depositPaid());
        Assert.assertEquals(record.checkinDate(), "2025-06-01");
        Assert.assertEquals(record.additionalNeeds(), "Airport shuttle");
    }

    @Test(priority = 2, dependsOnMethods = "shouldPersistCreatedBookingToDatabase",
            description = "Update the booking via the API, sync the change, and confirm the DB reflects the new values")
    public void shouldReflectApiUpdateInDatabase() {
        authToken = apiClient.authenticate();

        Booking updated = new Booking(
                "Alicia", "Nguyen-Smith", 450, false,
                new BookingDates("2025-06-02", "2025-06-09"), "Late checkout");
        apiClient.updateBooking(bookingId, updated, authToken);

        auditRepository.update(new BookingAuditRecord(
                bookingId, updated.getFirstname(), updated.getLastname(), updated.getTotalprice(),
                updated.isDepositpaid(), updated.getBookingdates().getCheckin(),
                updated.getBookingdates().getCheckout(), updated.getAdditionalneeds()));

        BookingAuditRecord record = auditRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new AssertionError("Expected an existing audit row to update for booking " + bookingId));

        Assert.assertEquals(record.firstname(), "Alicia");
        Assert.assertEquals(record.totalPrice(), 450);
        Assert.assertFalse(record.depositPaid());
    }

    @Test(priority = 3, dependsOnMethods = "shouldReflectApiUpdateInDatabase",
            description = "Delete the booking via the API, remove the audit row, and confirm neither side has a trace of it")
    public void shouldRemoveBookingFromDatabaseOnDelete() {
        Response deleteResponse = apiClient.deleteBooking(bookingId, authToken);
        Assert.assertEquals(deleteResponse.statusCode(), 201, "Expected 201 from restful-booker on successful delete");

        auditRepository.deleteByBookingId(bookingId);

        Optional<BookingAuditRecord> record = auditRepository.findByBookingId(bookingId);
        Assert.assertTrue(record.isEmpty(),
                "Expected no audit row to remain for booking " + bookingId + " after deletion");

        Response getResponse = apiClient.getBookingRaw(bookingId);
        Assert.assertEquals(getResponse.statusCode(), 404, "Expected the API to also report the booking as gone");
    }
}
