package com.tempoup.api.matching;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A mutual like between two users. To guarantee one row per pair, we always
 * store the two ids in canonical order (userAId &lt; userBId), enforced by a
 * DB CHECK constraint. Use {@link #ordered} to build instances safely.
 */
@Entity
@Table(name = "matches")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Match {

    @Id @GeneratedValue
    private UUID id;

    @Column(name = "user_a_id", nullable = false)
    private UUID userAId;

    @Column(name = "user_b_id", nullable = false)
    private UUID userBId;

    @CreationTimestamp
    @Column(name = "matched_at", updatable = false)
    private OffsetDateTime matchedAt;

    /** Factory that enforces canonical ordering of the two participants. */
    public static Match ordered(UUID u1, UUID u2) {
        boolean u1First = compareUnsigned(u1, u2) < 0;
        UUID a = u1First ? u1 : u2;
        UUID b = u1First ? u2 : u1;
        return Match.builder().userAId(a).userBId(b).build();
    }

    /**
     * Compares two UUIDs the same way PostgreSQL orders its {@code uuid} type:
     * byte-wise unsigned. {@link UUID#compareTo} instead compares the 64-bit
     * halves as <em>signed</em> longs, which disagrees with the database
     * whenever exactly one UUID has the high bit of its most-significant half
     * set — and would violate the {@code chk_match_order} CHECK constraint.
     */
    private static int compareUnsigned(UUID x, UUID y) {
        int hi = Long.compareUnsigned(x.getMostSignificantBits(), y.getMostSignificantBits());
        return hi != 0 ? hi
                : Long.compareUnsigned(x.getLeastSignificantBits(), y.getLeastSignificantBits());
    }
}
