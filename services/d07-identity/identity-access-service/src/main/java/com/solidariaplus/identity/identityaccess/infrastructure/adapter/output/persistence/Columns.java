package com.solidariaplus.identity.identityaccess.infrastructure.adapter.output.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

final class Columns {
    private Columns() {}

    static Timestamp timestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    static Timestamp timestamp(Optional<Instant> instant) {
        return timestamp(instant.orElse(null));
    }

    static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    static UUID uuid(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, UUID.class);
    }

    static Set<String> textSet(ResultSet rs, String column) throws SQLException {
        return Set.of((String[]) rs.getArray(column).getArray());
    }

    static String[] textArray(Set<String> values) {
        return values.toArray(String[]::new);
    }
}
