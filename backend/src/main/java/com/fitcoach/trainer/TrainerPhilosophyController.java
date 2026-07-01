package com.fitcoach.trainer;

import com.fitcoach.auth.jwt.CurrentUser;
import com.fitcoach.trainer.dto.TrainerPhilosophyDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/trainers")
@Tag(name = "Trainer Philosophies", description = "Available coaching philosophies for plan generation")
@SecurityRequirement(name = "bearer-jwt")
public class TrainerPhilosophyController {

    private final TrainerPhilosophyRepository repository;

    public TrainerPhilosophyController(TrainerPhilosophyRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @Operation(summary = "List all available trainer coaching philosophies, ordered for display")
    public List<TrainerPhilosophyDto> listAll(@AuthenticationPrincipal CurrentUser currentUser) {
        return repository.findAllByOrderBySortOrderAsc()
                .stream()
                .map(TrainerPhilosophyDto::from)
                .toList();
    }
}
