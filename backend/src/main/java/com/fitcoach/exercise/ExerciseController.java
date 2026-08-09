package com.fitcoach.exercise;

import com.fitcoach.exercise.dto.ExerciseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/exercises")
@Tag(name = "Exercises", description = "Read-only exercise library")
@SecurityRequirement(name = "bearer-jwt")
public class ExerciseController {

    private final ExerciseRepository exerciseRepository;

    public ExerciseController(ExerciseRepository exerciseRepository) {
        this.exerciseRepository = exerciseRepository;
    }

    @GetMapping
    @Operation(summary = "List the full exercise library, sorted by muscle group then name")
    public List<ExerciseDto> listExercises() {
        return exerciseRepository.findAll().stream()
                .sorted(Comparator.comparing(Exercise::getPrimaryMuscleGroup)
                        .thenComparing(Exercise::getName))
                .map(ExerciseDto::from)
                .toList();
    }
}
