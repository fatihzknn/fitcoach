package com.fitcoach.session;

import com.fitcoach.session.domain.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkoutSessionRepository extends JpaRepository<WorkoutSession, UUID> {

    Optional<WorkoutSession> findByUserIdAndStatus(UUID userId, SessionStatus status);

    List<WorkoutSession> findAllByUserIdAndStatusOrderByStartedAtDesc(UUID userId, SessionStatus status);

    List<WorkoutSession> findAllByUserIdOrderByStartedAtDesc(UUID userId);
}
