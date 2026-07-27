package com.fitcoach.trainer;

import com.fitcoach.auth.jwt.CurrentUser;
import com.fitcoach.profile.FitnessProfileRepository;
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
    private final FitnessProfileRepository profileRepository;

    public TrainerPhilosophyController(TrainerPhilosophyRepository repository,
                                        FitnessProfileRepository profileRepository) {
        this.repository = repository;
        this.profileRepository = profileRepository;
    }

    @GetMapping
    @Operation(summary = "List trainer coaching philosophies visible to the current user, ordered for display. " +
            "Physiology-specific philosophies are filtered to the user's profile sex.")
    public List<TrainerPhilosophyDto> listAll(@AuthenticationPrincipal CurrentUser currentUser) {
        // Sex is optional context, not a hard requirement — if the profile isn't found
        // (shouldn't happen post-onboarding), fall back to only the sex-neutral trainers.
        String sex = profileRepository.findByUserId(currentUser.id())
                .map(p -> p.getSex().name())
                .orElse(null);

        return repository.findAllByOrderBySortOrderAsc()
                .stream()
                .filter(t -> TrainerVisibility.isVisible(t.getTargetSex(), sex))
                .map(TrainerPhilosophyDto::from)
                .toList();
    }
}
