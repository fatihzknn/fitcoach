package com.fitcoach.roster;

import com.fitcoach.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** A redeemed link between a trainer and a client — created_at doubles as "linked since". */
@Entity
@Table(name = "trainer_clients")
public class TrainerClient extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "trainer_id", nullable = false)
    private UUID trainerId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    protected TrainerClient() {}

    public TrainerClient(UUID trainerId, UUID clientId) {
        this.id = UUID.randomUUID();
        this.trainerId = trainerId;
        this.clientId = clientId;
    }

    public UUID getId() { return id; }
    public UUID getTrainerId() { return trainerId; }
    public UUID getClientId() { return clientId; }
}
