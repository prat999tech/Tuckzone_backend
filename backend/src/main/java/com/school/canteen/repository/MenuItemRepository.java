package com.school.canteen.repository;

import com.school.canteen.entity.MenuItem;
import com.school.canteen.enums.MenuType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {

    List<MenuItem> findByActiveTrue();

    List<MenuItem> findByMenuTypeAndActiveTrue(MenuType menuType);

    /** The Fixed Menu as customers see it: active, in-stock items only. */
    List<MenuItem> findByMenuTypeAndActiveTrueAndAvailableTrue(MenuType menuType);
}
