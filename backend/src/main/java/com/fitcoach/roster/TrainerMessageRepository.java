package com.fitcoach.roster;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TrainerMessageRepository extends JpaRepository<TrainerMessage, UUID> {
    List<TrainerMessage> findByTrainerClientIdOrderByCreatedAtAsc(UUID trainerClientId);
}
