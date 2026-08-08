package com.fitcoach.roster;

import com.fitcoach.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A trainer's standing invite code — one row per trainer, not one per
 * invitation event. Any client who enters the code links to this trainer.
 * Regenerating replaces the code and pushes out the expiry rather than
 * inserting a new row.
 */
@Entity
@Table(name = "trainer_invites")
public class TrainerInvite extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "trainer_id", nullable = false, unique = true)
    private UUID trainerId;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected TrainerInvite() {}

    public TrainerInvite(UUID trainerId, String code, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.trainerId = trainerId;
        this.code = code;
        this.expiresAt = expiresAt;
    }

    public void rotate(String newCode, Instant newExpiresAt) {
        this.code = newCode;
        this.expiresAt = newExpiresAt;
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    public UUID getId() { return id; }
    public UUID getTrainerId() { return trainerId; }
    public String getCode() { return code; }
    public Instant getExpiresAt() { return expiresAt; }
}
