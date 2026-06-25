package com.fitcoach.checkin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WeeklyCheckInRepository extends JpaRepository<WeeklyCheckIn, UUID> {

    Optional<WeeklyCheckIn> findByUserIdAndWeekStart(UUID userId, LocalDate weekStart);

    List<WeeklyCheckIn> findAllByUserIdOrderByWeekStartDesc(UUID userId);
}
