package com.school.canteen.repository;

import com.school.canteen.entity.Ward;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WardRepository extends JpaRepository<Ward, UUID> {

    List<Ward> findByParent_Id(UUID parentUserId);

    Optional<Ward> findByIdAndParent_Id(UUID id, UUID parentUserId);
}
