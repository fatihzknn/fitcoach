package com.fitcoach.session;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SetLogRepository extends JpaRepository<SetLog, UUID> {

    // Matches on the SetLog's own snapshot exercise (whichever was effective —
    // substituted or original — at the moment it was logged), not
    // workoutExercise.exercise directly, so a later swap-back doesn't retroactively
    // change which exercise's history a historical set counts toward.
    @Query("""
        SELECT s FROM SetLog s
        WHERE s.workoutSession.userId = :userId
          AND s.exercise.id = :exerciseId
          AND s.workoutSession.status = 'COMPLETED'
        ORDER BY s.workoutSession.startedAt DESC, s.setNumber ASC
        """)
    List<SetLog> findRecentByUserAndExercise(@Param("userId") UUID userId,
                                              @Param("exerciseId") UUID exerciseId);

    @Query("""
        SELECT s FROM SetLog s
        WHERE s.workoutSession.userId = :userId
          AND s.exercise.id = :exerciseId
          AND s.workoutSession.status = 'COMPLETED'
        ORDER BY s.workoutSession.startedAt ASC, s.setNumber ASC
        """)
    List<SetLog> findAllByUserAndExerciseAsc(@Param("userId") UUID userId,
                                              @Param("exerciseId") UUID exerciseId);
}
