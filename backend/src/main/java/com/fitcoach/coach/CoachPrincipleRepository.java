package com.fitcoach.coach;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CoachPrincipleRepository extends JpaRepository<CoachPrinciple, UUID> {
}
