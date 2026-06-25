package com.fitcoach.session;

import com.fitcoach.session.domain.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkoutSessionRepository extends JpaRepository<WorkoutSession, UUID> {

    Optional<WorkoutSession> findByUserIdAndStatus(UUID userId, SessionStatus status);

    List<WorkoutSession> findAllByUserIdAndStatusOrderByStartedAtDesc(UUID userId, SessionStatus status);

    List<WorkoutSession> findAllByUserIdOrderByStartedAtDesc(UUID userId);

    @Modifying
    @Query("UPDATE WorkoutSession s SET s.startedAt = :started, s.completedAt = :completed WHERE s.id = :id")
    void updateTimestamps(@Param("id") UUID id, @Param("started") Instant started, @Param("completed") Instant completed);
}
