package com.fitcoach.roster;

import com.fitcoach.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** A single message within a trainer/client thread, anchored on the {@link TrainerClient} link. */
@Entity
@Table(name = "trainer_messages")
public class TrainerMessage extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "trainer_client_id", nullable = false, updatable = false)
    private UUID trainerClientId;

    @Column(name = "sender_id", nullable = false, updatable = false)
    private UUID senderId;

    @Column(name = "content", nullable = false, updatable = false)
    private String content;

    protected TrainerMessage() {}

    public TrainerMessage(UUID trainerClientId, UUID senderId, String content) {
        this.id = UUID.randomUUID();
        this.trainerClientId = trainerClientId;
        this.senderId = senderId;
        this.content = content;
    }

    public UUID getId() { return id; }
    public UUID getTrainerClientId() { return trainerClientId; }
    public UUID getSenderId() { return senderId; }
    public String getContent() { return content; }
}
