package com.automation.framework.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Data-access layer for the {@code booking_audit} table.
 *
 * <p>Every method opens and closes its own connection via try-with-resources
 * rather than holding one open for the test's lifetime. H2 file-mode
 * connections are cheap to open, and short-lived connections mean a test
 * failure partway through never leaks a dangling connection — the kind of
 * resource leak that's easy to introduce and annoying to debug in a suite
 * that runs unattended in CI.</p>
 */
public class BookingAuditRepository {

    private static final Logger log = LoggerFactory.getLogger(BookingAuditRepository.class);

    private static final String INSERT_SQL = """
            INSERT INTO booking_audit
                (booking_id, firstname, lastname, total_price, deposit_paid, checkin_date, checkout_date, additional_needs)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String SELECT_SQL = "SELECT * FROM booking_audit WHERE booking_id = ?";

    private static final String UPDATE_SQL = """
            UPDATE booking_audit
            SET firstname = ?, lastname = ?, total_price = ?, deposit_paid = ?,
                checkin_date = ?, checkout_date = ?, additional_needs = ?, synced_at = CURRENT_TIMESTAMP
            WHERE booking_id = ?
            """;

    private static final String DELETE_SQL = "DELETE FROM booking_audit WHERE booking_id = ?";

    public void insert(BookingAuditRecord record) {
        try (Connection connection = DbConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            bindRecord(statement, record);
            statement.executeUpdate();
            log.info("Audited booking {} into local H2 store", record.bookingId());
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to insert audit record for booking " + record.bookingId(), e);
        }
    }

    public Optional<BookingAuditRecord> findByBookingId(int bookingId) {
        try (Connection connection = DbConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_SQL)) {
            statement.setInt(1, bookingId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new BookingAuditRecord(
                        rs.getInt("booking_id"),
                        rs.getString("firstname"),
                        rs.getString("lastname"),
                        rs.getInt("total_price"),
                        rs.getBoolean("deposit_paid"),
                        rs.getString("checkin_date"),
                        rs.getString("checkout_date"),
                        rs.getString("additional_needs")));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to query audit record for booking " + bookingId, e);
        }
    }

    public void update(BookingAuditRecord record) {
        try (Connection connection = DbConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            statement.setString(1, record.firstname());
            statement.setString(2, record.lastname());
            statement.setInt(3, record.totalPrice());
            statement.setBoolean(4, record.depositPaid());
            statement.setString(5, record.checkinDate());
            statement.setString(6, record.checkoutDate());
            statement.setString(7, record.additionalNeeds());
            statement.setInt(8, record.bookingId());
            int rowsAffected = statement.executeUpdate();
            if (rowsAffected == 0) {
                throw new IllegalStateException(
                        "No audit row found to update for booking " + record.bookingId()
                                + " — was insert() called first?");
            }
            log.info("Updated audit record for booking {}", record.bookingId());
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update audit record for booking " + record.bookingId(), e);
        }
    }

    public void deleteByBookingId(int bookingId) {
        try (Connection connection = DbConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            statement.setInt(1, bookingId);
            statement.executeUpdate();
            log.info("Removed audit record for booking {}", bookingId);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete audit record for booking " + bookingId, e);
        }
    }

    private void bindRecord(PreparedStatement statement, BookingAuditRecord record) throws SQLException {
        statement.setInt(1, record.bookingId());
        statement.setString(2, record.firstname());
        statement.setString(3, record.lastname());
        statement.setInt(4, record.totalPrice());
        statement.setBoolean(5, record.depositPaid());
        statement.setString(6, record.checkinDate());
        statement.setString(7, record.checkoutDate());
        statement.setString(8, record.additionalNeeds());
    }
}
