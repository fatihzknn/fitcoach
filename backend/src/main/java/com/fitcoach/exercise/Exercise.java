package com.fitcoach.exercise;

import com.fitcoach.common.BaseEntity;
import com.fitcoach.exercise.domain.DifficultyLevel;
import com.fitcoach.exercise.domain.MovementPattern;
import com.fitcoach.exercise.domain.MuscleGroup;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "exercises")
public class Exercise extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "primary_muscle_group", nullable = false)
    private MuscleGroup primaryMuscleGroup;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "exercise_secondary_muscles",
            joinColumns = @JoinColumn(name = "exercise_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "muscle_group", nullable = false)
    private Set<MuscleGroup> secondaryMuscleGroups = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_pattern", nullable = false)
    private MovementPattern movementPattern;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_level", nullable = false)
    private DifficultyLevel difficultyLevel;

    @Column(name = "video_url")
    private String videoUrl;

    @Column(name = "form_cue", nullable = false, columnDefinition = "TEXT")
    private String formCue;

    @Column(name = "common_mistake", nullable = false, columnDefinition = "TEXT")
    private String commonMistake;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "exercise_alternatives",
            joinColumns = @JoinColumn(name = "exercise_id"),
            inverseJoinColumns = @JoinColumn(name = "alternative_id"))
    private Set<Exercise> alternatives = new HashSet<>();

    protected Exercise() {
        // for JPA
    }

    public Exercise(String name, MuscleGroup primaryMuscleGroup, MovementPattern movementPattern,
                    DifficultyLevel difficultyLevel, String formCue, String commonMistake) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.primaryMuscleGroup = primaryMuscleGroup;
        this.movementPattern = movementPattern;
        this.difficultyLevel = difficultyLevel;
        this.formCue = formCue;
        this.commonMistake = commonMistake;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public MuscleGroup getPrimaryMuscleGroup() { return primaryMuscleGroup; }
    public Set<MuscleGroup> getSecondaryMuscleGroups() { return secondaryMuscleGroups; }
    public MovementPattern getMovementPattern() { return movementPattern; }
    public DifficultyLevel getDifficultyLevel() { return difficultyLevel; }
    public String getVideoUrl() { return videoUrl; }
    public String getFormCue() { return formCue; }
    public String getCommonMistake() { return commonMistake; }
    public Set<Exercise> getAlternatives() { return alternatives; }
}
