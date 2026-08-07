package com.school.canteen.repository;

import com.school.canteen.entity.OtpCode;
import com.school.canteen.enums.OtpPurpose;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OtpCodeRepository extends JpaRepository<OtpCode, UUID> {

    /**
     * Most recently issued code for this email + purpose, locked for update.
     *
     * The lock serialises verification attempts: without it two parallel guesses could
     * both read attempts=4 and each think they were within the cap, letting an attacker
     * multiply their allowance by firing requests concurrently.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select o from OtpCode o
             where o.email = :email
               and o.purpose = :purpose
             order by o.createdAt desc
             limit 1
            """)
    Optional<OtpCode> lockLatest(@Param("email") String email,
                                 @Param("purpose") OtpPurpose purpose);

    /** Invalidates any outstanding codes so only the newest one can be used. */
    @Modifying(flushAutomatically = true)
    @Query("""
            update OtpCode o
               set o.consumedAt = :now
             where o.email = :email
               and o.purpose = :purpose
               and o.consumedAt is null
            """)
    int consumeOutstanding(@Param("email") String email,
                           @Param("purpose") OtpPurpose purpose,
                           @Param("now") Instant now);

    @Modifying
    @Query("delete from OtpCode o where o.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") Instant cutoff);

    /**
     * When the most recent code (consumed or not) for this email + purpose was created.
     * Backs the resend cooldown: a read of "was one issued in the last N seconds" that
     * does not need the pessimistic lock {@link #lockLatest} takes for verification.
     */
    @Query("""
            select max(o.createdAt) from OtpCode o
             where o.email = :email
               and o.purpose = :purpose
            """)
    Optional<Instant> latestIssuedAt(@Param("email") String email,
                                     @Param("purpose") OtpPurpose purpose);
}
