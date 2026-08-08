package com.fitcoach.roster;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TrainerInviteRepository extends JpaRepository<TrainerInvite, UUID> {
    Optional<TrainerInvite> findByTrainerId(UUID trainerId);
    Optional<TrainerInvite> findByCode(String code);
    boolean existsByCode(String code);
}
