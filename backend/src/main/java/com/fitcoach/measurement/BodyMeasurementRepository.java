package com.fitcoach.measurement;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BodyMeasurementRepository extends JpaRepository<BodyMeasurement, UUID> {

    List<BodyMeasurement> findAllByUserIdOrderByMeasuredAtDesc(UUID userId);

    Optional<BodyMeasurement> findByUserIdAndMeasuredAt(UUID userId, LocalDate measuredAt);

    Optional<BodyMeasurement> findFirstByUserIdOrderByMeasuredAtDesc(UUID userId);
}
