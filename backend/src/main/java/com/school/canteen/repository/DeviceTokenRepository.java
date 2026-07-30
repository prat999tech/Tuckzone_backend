package com.school.canteen.repository;

import com.school.canteen.entity.DeviceToken;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {

    List<DeviceToken> findByUser_Id(UUID userId);

    Optional<DeviceToken> findByToken(String token);

    /** Removes tokens FCM reported as dead, so we stop paying to message nothing. */
    @Modifying
    @Query("delete from DeviceToken d where d.token in :tokens")
    int deleteByTokenIn(@Param("tokens") Collection<String> tokens);
}
