package com.fitcoach.session;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SetLogRepository extends JpaRepository<SetLog, UUID> {

    @Query("""
        SELECT s FROM SetLog s
        WHERE s.workoutSession.userId = :userId
          AND s.workoutExercise.exercise.id = :exerciseId
          AND s.workoutSession.status = 'COMPLETED'
        ORDER BY s.workoutSession.startedAt DESC, s.setNumber ASC
        """)
    List<SetLog> findRecentByUserAndExercise(@Param("userId") UUID userId,
                                              @Param("exerciseId") UUID exerciseId);
}
