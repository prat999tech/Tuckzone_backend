package com.school.canteen.repository;

import com.school.canteen.entity.TeacherProfile;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherProfileRepository extends JpaRepository<TeacherProfile, UUID> {

    boolean existsByEmployeeId(String employeeId);

    Optional<TeacherProfile> findByUser_Id(UUID userId);

    /** Batch lookup so mapping a list of users does not become an N+1 query. */
    List<TeacherProfile> findByUser_IdIn(Collection<UUID> userIds);
}
