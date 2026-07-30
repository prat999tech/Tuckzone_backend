package com.school.canteen.repository;

import com.school.canteen.entity.NotificationOutbox;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, UUID> {

    /**
     * Claims a batch of due notifications for this worker.
     *
     * FOR UPDATE SKIP LOCKED is the important part: rows already being handled by another
     * worker (or by the immediate after-commit attempt) are stepped over instead of
     * blocking, so nothing is ever delivered twice and the sweeper never stalls behind a
     * slow send. Rows are flipped to PROCESSING in the same statement, which is what
     * actually reserves them.
     */
    @Modifying
    @Query(value = """
            update notification_outbox
               set status = 'PROCESSING', updated_at = now()
             where id in (
                 select id from notification_outbox
                  where status in ('PENDING', 'FAILED')
                    and next_attempt_at <= now()
                  order by created_at
                  limit :batchSize
                  for update skip locked
             )
            returning id
            """, nativeQuery = true)
    List<UUID> claimDueBatch(@Param("batchSize") int batchSize);

    /** Rows stuck in PROCESSING because the app died mid-send are returned to the queue. */
    @Modifying
    @Query("""
            update NotificationOutbox o
               set o.status = com.school.canteen.enums.OutboxStatus.PENDING
             where o.status = com.school.canteen.enums.OutboxStatus.PROCESSING
               and o.updatedAt < :stuckBefore
            """)
    int requeueStuck(@Param("stuckBefore") Instant stuckBefore);

    /** The in-app notification feed for a user. */
    List<NotificationOutbox> findByUser_IdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @Modifying
    @Query("delete from NotificationOutbox o where o.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);
}
