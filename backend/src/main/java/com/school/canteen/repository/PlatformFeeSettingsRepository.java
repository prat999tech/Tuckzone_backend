package com.school.canteen.repository;

import com.school.canteen.entity.PlatformFeeSettings;
import com.school.canteen.enums.PaymentUseCase;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformFeeSettingsRepository extends JpaRepository<PlatformFeeSettings, UUID> {

    Optional<PlatformFeeSettings> findByUseCase(PaymentUseCase useCase);

    List<PlatformFeeSettings> findAllByOrderByUseCase();
}
