package com.school.canteen.repository;

import com.school.canteen.entity.StudentProfile;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, UUID> {

    boolean existsByAdmissionNumber(String admissionNumber);

    Optional<StudentProfile> findByAdmissionNumber(String admissionNumber);

    Optional<StudentProfile> findByUser_Id(UUID userId);

    /** Batch lookup so mapping a list of users does not become an N+1 query. */
    List<StudentProfile> findByUser_IdIn(Collection<UUID> userIds);
}
