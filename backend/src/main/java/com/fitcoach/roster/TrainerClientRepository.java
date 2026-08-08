package com.fitcoach.roster;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrainerClientRepository extends JpaRepository<TrainerClient, UUID> {
    Optional<TrainerClient> findByTrainerIdAndClientId(UUID trainerId, UUID clientId);
    List<TrainerClient> findAllByTrainerId(UUID trainerId);
}
