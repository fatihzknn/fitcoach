package com.fitcoach.profile;

import com.fitcoach.auth.Role;
import com.fitcoach.auth.jwt.CurrentUser;
import com.fitcoach.common.NotFoundException;
import com.fitcoach.profile.domain.BarbellComfort;
import com.fitcoach.profile.domain.MainGoal;
import com.fitcoach.profile.domain.PainArea;
import com.fitcoach.profile.domain.Sex;
import com.fitcoach.profile.domain.TrainingBackground;
import com.fitcoach.profile.dto.FitnessProfileDto;
import com.fitcoach.profile.dto.OnboardingRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ProfileService had zero dedicated unit tests before this — only exercised
 * indirectly through the onboarding flow's Flyway/DB CHECK constraints.
 */
@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock private FitnessProfileRepository profileRepository;

    @InjectMocks
    private ProfileService service;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final CurrentUser CURRENT_USER = new CurrentUser(USER_ID, "test@example.com", Role.USER);
    private static final UUID TRAINER_ID = UUID.randomUUID();
    private static final CurrentUser CURRENT_TRAINER = new CurrentUser(TRAINER_ID, "trainer@example.com", Role.TRAINER);

    private OnboardingRequest request(Set<PainArea> painAreas) {
        return new OnboardingRequest(
                MainGoal.MUSCLE_GAIN, TrainingBackground.REGULAR, 4, 60,
                28, 178, 80.0, Sex.MALE, painAreas, BarbellComfort.COMFORTABLE);
    }

    // ─── completeOnboarding ─────────────────────────────────────────────────────

    @Test
    void completeOnboarding_createsNewProfileWhenNoneExists() {
        when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        FitnessProfileDto result = service.completeOnboarding(CURRENT_USER, request(Set.of(PainArea.NONE)));

        assertThat(result.mainGoal()).isEqualTo(MainGoal.MUSCLE_GAIN);
        assertThat(result.barbellComfort()).isEqualTo(BarbellComfort.COMFORTABLE);
        assertThat(result.onboardingCompleted()).isTrue();
        assertThat(result.onboardingCompletedAt()).isNotNull();

        ArgumentCaptor<FitnessProfile> captor = ArgumentCaptor.forClass(FitnessProfile.class);
        verify(profileRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
    }

    @Test
    void completeOnboarding_updatesExistingProfileInPlace() {
        FitnessProfile existing = new FitnessProfile(USER_ID);
        when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));

        FitnessProfileDto result = service.completeOnboarding(CURRENT_USER, request(Set.of(PainArea.NONE)));

        assertThat(result.mainGoal()).isEqualTo(MainGoal.MUSCLE_GAIN);
        assertThat(existing.getMainGoal()).isEqualTo(MainGoal.MUSCLE_GAIN);
        assertThat(existing.getAge()).isEqualTo(28);

        ArgumentCaptor<FitnessProfile> captor = ArgumentCaptor.forClass(FitnessProfile.class);
        verify(profileRepository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(existing);
    }

    @Test
    void completeOnboarding_emptyPainAreasNormalizesToNone() {
        when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        FitnessProfileDto result = service.completeOnboarding(CURRENT_USER, request(Set.of()));

        assertThat(result.painAreas()).containsExactly(PainArea.NONE);
    }

    @Test
    void completeOnboarding_specificAreaDropsNone() {
        when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        FitnessProfileDto result = service.completeOnboarding(
                CURRENT_USER, request(Set.of(PainArea.NONE, PainArea.KNEE)));

        assertThat(result.painAreas()).containsExactly(PainArea.KNEE);
    }

    @Test
    void completeOnboarding_multipleSpecificAreasKeptTogether() {
        when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        FitnessProfileDto result = service.completeOnboarding(
                CURRENT_USER, request(Set.of(PainArea.KNEE, PainArea.LOWER_BACK)));

        assertThat(result.painAreas()).containsExactlyInAnyOrder(PainArea.KNEE, PainArea.LOWER_BACK);
    }

    // ─── getProfile ─────────────────────────────────────────────────────────────

    @Test
    void getProfile_returnsDtoWhenProfileExists() {
        FitnessProfile profile = new FitnessProfile(USER_ID);
        profile.setMainGoal(MainGoal.FAT_LOSS);
        when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));

        FitnessProfileDto result = service.getProfile(CURRENT_USER);

        assertThat(result.mainGoal()).isEqualTo(MainGoal.FAT_LOSS);
    }

    @Test
    void getProfile_throwsWhenNoProfileYet() {
        when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProfile(CURRENT_USER))
                .isInstanceOf(NotFoundException.class);
    }

    // ─── a TRAINER can also onboard and track their own training ───────────────
    // Regression guard: nothing here inspects CurrentUser.role(), and it should
    // stay that way — a trainer's own "Kendi Programım" self-tracking reuses
    // this exact same service, unmodified.

    @Test
    void completeOnboarding_succeedsForTrainerRoleCaller() {
        when(profileRepository.findByUserId(TRAINER_ID)).thenReturn(Optional.empty());

        FitnessProfileDto result = service.completeOnboarding(CURRENT_TRAINER, request(Set.of(PainArea.NONE)));

        assertThat(result.onboardingCompleted()).isTrue();
        ArgumentCaptor<FitnessProfile> captor = ArgumentCaptor.forClass(FitnessProfile.class);
        verify(profileRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(TRAINER_ID);
    }

    @Test
    void getProfile_succeedsForTrainerRoleCaller() {
        FitnessProfile profile = new FitnessProfile(TRAINER_ID);
        profile.setMainGoal(MainGoal.STRENGTH);
        when(profileRepository.findByUserId(TRAINER_ID)).thenReturn(Optional.of(profile));

        FitnessProfileDto result = service.getProfile(CURRENT_TRAINER);

        assertThat(result.mainGoal()).isEqualTo(MainGoal.STRENGTH);
    }
}
