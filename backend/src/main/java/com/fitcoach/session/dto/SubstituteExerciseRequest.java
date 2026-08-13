package com.fitcoach.session.dto;

import java.util.UUID;

/** exerciseId null clears any existing substitution, reverting the slot to its
 *  original template exercise. */
public record SubstituteExerciseRequest(UUID exerciseId) {
}
