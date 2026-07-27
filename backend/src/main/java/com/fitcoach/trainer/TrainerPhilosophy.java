package com.fitcoach.trainer;

import com.fitcoach.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "trainer_philosophies")
public class TrainerPhilosophy extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "tagline", nullable = false)
    private String tagline;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "compound_rep_min", nullable = false)
    private int compoundRepMin;

    @Column(name = "compound_rep_max", nullable = false)
    private int compoundRepMax;

    @Column(name = "isolation_rep_min", nullable = false)
    private int isolationRepMin;

    @Column(name = "isolation_rep_max", nullable = false)
    private int isolationRepMax;

    @Column(name = "rest_seconds_compound", nullable = false)
    private int restSecondsCompound;

    @Column(name = "rest_seconds_isolation", nullable = false)
    private int restSecondsIsolation;

    @Column(name = "rir_target", nullable = false)
    private int rirTarget;

    @Column(name = "sets_compound", nullable = false)
    private int setsCompound;

    @Column(name = "sets_isolation", nullable = false)
    private int setsIsolation;

    @Column(name = "deload_frequency_weeks", nullable = false)
    private int deloadFrequencyWeeks;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    /** Null = visible to everyone. Otherwise must match FitnessProfile.sex.name() exactly. */
    @Column(name = "target_sex")
    private String targetSex;

    protected TrainerPhilosophy() {}

    public UUID getId() { return id; }
    public String getSlug() { return slug; }
    public String getDisplayName() { return displayName; }
    public String getTagline() { return tagline; }
    public String getDescription() { return description; }
    public int getCompoundRepMin() { return compoundRepMin; }
    public int getCompoundRepMax() { return compoundRepMax; }
    public int getIsolationRepMin() { return isolationRepMin; }
    public int getIsolationRepMax() { return isolationRepMax; }
    public int getRestSecondsCompound() { return restSecondsCompound; }
    public int getRestSecondsIsolation() { return restSecondsIsolation; }
    public int getRirTarget() { return rirTarget; }
    public int getSetsCompound() { return setsCompound; }
    public int getSetsIsolation() { return setsIsolation; }
    public int getDeloadFrequencyWeeks() { return deloadFrequencyWeeks; }
    public int getSortOrder() { return sortOrder; }
    public String getTargetSex() { return targetSex; }
}
