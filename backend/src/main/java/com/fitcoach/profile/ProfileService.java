package com.fitcoach.profile;

import com.fitcoach.auth.jwt.CurrentUser;
import com.fitcoach.common.NotFoundException;
import com.fitcoach.profile.domain.PainArea;
import com.fitcoach.profile.dto.FitnessProfileDto;
import com.fitcoach.profile.dto.OnboardingRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;

@Service
public class ProfileService {

    private final FitnessProfileRepository profileRepository;

    public ProfileService(FitnessProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    /**
     * Persists onboarding answers. Idempotent per user: re-submitting updates the existing
     * profile. Completing onboarding stamps {@code onboardingCompletedAt}.
     */
    @Transactional
    public FitnessProfileDto completeOnboarding(CurrentUser currentUser, OnboardingRequest req) {
        FitnessProfile profile = profileRepository.findByUserId(currentUser.id())
                .orElseGet(() -> new FitnessProfile(currentUser.id()));

        profile.setMainGoal(req.mainGoal());
        profile.setTrainingBackground(req.trainingBackground());
        profile.setTrainingDaysPerWeek(req.trainingDaysPerWeek());
        profile.setSessionDurationMinutes(req.sessionDurationMinutes());
        profile.setAge(req.age());
        profile.setHeightCm(req.heightCm());
        profile.setWeightKg(req.weightKg());
        profile.setSex(req.sex());
        profile.setPainAreas(normalizePainAreas(req.painAreas()));
        profile.setBarbellComfort(req.barbellComfort());
        profile.markOnboardingCompleted();

        profileRepository.save(profile);
        return FitnessProfileDto.from(profile);
    }

    @Transactional(readOnly = true)
    public FitnessProfileDto getProfile(CurrentUser currentUser) {
        return profileRepository.findByUserId(currentUser.id())
                .map(FitnessProfileDto::from)
                .orElseThrow(() -> new NotFoundException("No fitness profile yet. Complete onboarding first."));
    }

    /** NONE is exclusive: if any specific area is present, drop NONE; if empty, treat as NONE. */
    private Set<PainArea> normalizePainAreas(Set<PainArea> input) {
        if (input == null || input.isEmpty()) {
            return EnumSet.of(PainArea.NONE);
        }
        EnumSet<PainArea> result = EnumSet.copyOf(input);
        if (result.size() > 1) {
            result.remove(PainArea.NONE);
        }
        return result;
    }
}
