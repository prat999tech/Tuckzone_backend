package com.school.canteen.repository;

import com.school.canteen.entity.TeacherProfile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherProfileRepository extends JpaRepository<TeacherProfile, UUID> {

    boolean existsByEmployeeId(String employeeId);

    Optional<TeacherProfile> findByUser_Id(UUID userId);
}
