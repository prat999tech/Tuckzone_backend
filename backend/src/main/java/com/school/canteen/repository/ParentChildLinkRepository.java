package com.school.canteen.repository;

import com.school.canteen.entity.ParentChildLink;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParentChildLinkRepository extends JpaRepository<ParentChildLink, UUID> {

    List<ParentChildLink> findByParent_Id(UUID parentUserId);

    boolean existsByParent_IdAndStudentProfile_Id(UUID parentUserId, UUID studentProfileId);

    Optional<ParentChildLink> findByIdAndParent_Id(UUID id, UUID parentUserId);
}
