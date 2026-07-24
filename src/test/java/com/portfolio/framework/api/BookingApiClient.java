package com.portfolio.framework.api;

import com.portfolio.framework.api.model.Booking;
import com.portfolio.framework.config.ConfigReader;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.restassured.RestAssured.given;

/**
 * Thin, typed wrapper around the restful-booker practice API
 * (https://restful-booker.herokuapp.com/apidoc).
 *
 * <p>Tests call methods like {@code createBooking(...)} instead of building
 * raw RestAssured requests inline. This mirrors the Page Object Model
 * philosophy from the UI layer: test classes describe *what* to verify,
 * this class knows *how* to talk to the API — if restful-booker's auth
 * scheme or a header changes, one class absorbs the change.</p>
 */
public class BookingApiClient {

    private static final Logger log = LoggerFactory.getLogger(BookingApiClient.class);

    /**
     * Builds the shared request spec.
     *
     * <p>Headers are set as literal strings rather than via RestAssured's
     * {@code .contentType(ContentType.JSON)} / {@code .accept(ContentType.JSON)}
     * convenience methods on purpose: those helpers send a broad, multi-value
     * {@code Accept: application/json, application/javascript, text/javascript, text/json}
     * header, and restful-booker's server rejects that exact shape with a
     * {@code 418 I'm a teapot} on POST /booking — confirmed by diffing the raw
     * wire request against a plain curl call, which sends a single-value
     * {@code Accept: application/json} and always gets 200. Root-caused and
     * fixed at the header level rather than masked with a retry loop.</p>
     */
    private RequestSpecification baseRequest() {
        return given()
                .baseUri(ConfigReader.apiBaseUrl())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .log().ifValidationFails();
    }

    /**
     * Authenticates against POST /auth and returns the session token used by
     * update/delete calls. restful-booker accepts the token either as a
     * request header or a cookie; this framework uses the header form since
     * it's the one documented in the API's own apidoc page.
     */
    public String authenticate() {
        Response response = baseRequest()
                .body("""
                        {
                          "username": "%s",
                          "password": "%s"
                        }
                        """.formatted(
                        ConfigReader.get("api.auth.username"),
                        ConfigReader.get("api.auth.password")))
                .when()
                .post("/auth")
                .then()
                .statusCode(200)
                .extract().response();

        String token = response.jsonPath().getString("token");
        log.info("Authenticated against restful-booker, token acquired");
        return token;
    }

    /** POST /booking — no auth required. Returns the raw response so the caller can pull both the id and the echoed booking. */
    public Response createBooking(Booking booking) {
        return baseRequest()
                .body(booking)
                .when()
                .post("/booking")
                .then()
                .statusCode(200)
                .extract().response();
    }

    /** GET /booking/{id} — no auth required, deserialized straight into the domain model. */
    public Booking getBooking(int bookingId) {
        return baseRequest()
                .when()
                .get("/booking/{id}", bookingId)
                .then()
                .statusCode(200)
                .extract().as(Booking.class);
    }

    /** Raw GET, used by the negative-path check after deletion where a 404 is the expected outcome. */
    public Response getBookingRaw(int bookingId) {
        return baseRequest()
                .when()
                .get("/booking/{id}", bookingId);
    }

    /** PUT /booking/{id} — full update, requires the auth token from {@link #authenticate()}. */
    public Booking updateBooking(int bookingId, Booking booking, String token) {
        return baseRequest()
                .header("Cookie", "token=" + token)
                .body(booking)
                .when()
                .put("/booking/{id}", bookingId)
                .then()
                .statusCode(200)
                .extract().as(Booking.class);
    }

    /** PATCH /booking/{id} — partial update, e.g. changing only firstname/lastname. */
    public Booking partialUpdateBooking(int bookingId, String partialJsonBody, String token) {
        return baseRequest()
                .header("Cookie", "token=" + token)
                .body(partialJsonBody)
                .when()
                .patch("/booking/{id}", bookingId)
                .then()
                .statusCode(200)
                .extract().as(Booking.class);
    }

    /** DELETE /booking/{id} — requires auth; restful-booker returns 201 (not 204) on success. */
    public Response deleteBooking(int bookingId, String token) {
        return baseRequest()
                .header("Cookie", "token=" + token)
                .when()
                .delete("/booking/{id}", bookingId)
                .then()
                .extract().response();
    }
}
