package com.fitcoach.trainer;

import com.fitcoach.auth.jwt.CurrentUser;
import com.fitcoach.auth.jwt.JwtService;
import com.fitcoach.profile.FitnessProfile;
import com.fitcoach.profile.FitnessProfileRepository;
import com.fitcoach.profile.domain.Sex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = TrainerPhilosophyController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class TrainerPhilosophyControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean TrainerPhilosophyRepository repository;
    @MockBean FitnessProfileRepository profileRepository;
    @MockBean JwtService jwtService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final CurrentUser CURRENT_USER = new CurrentUser(USER_ID, "test@example.com");

    @BeforeEach
    void setUpSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(CURRENT_USER, null, List.of())
        );
        List<TrainerPhilosophy> all = List.of(
                trainer("evidence-based", null),
                trainer("classic-bodybuilding", null),
                trainer("womens-physiology-focused", "FEMALE")
        );
        when(repository.findAllByOrderBySortOrderAsc()).thenReturn(all);
    }

    @Test
    void femaleUser_seesAllTrainersIncludingWomensPhysiology() throws Exception {
        when(profileRepository.findByUserId(any())).thenReturn(Optional.of(profileWithSex(Sex.FEMALE)));

        mockMvc.perform(get("/api/trainers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[2].slug").value("womens-physiology-focused"));
    }

    @Test
    void maleUser_doesNotSeeWomensPhysiologyTrainer() throws Exception {
        when(profileRepository.findByUserId(any())).thenReturn(Optional.of(profileWithSex(Sex.MALE)));

        mockMvc.perform(get("/api/trainers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].slug").value("evidence-based"))
                .andExpect(jsonPath("$[1].slug").value("classic-bodybuilding"));
    }

    @Test
    void noProfileFound_fallsBackToSexNeutralTrainersOnly() throws Exception {
        when(profileRepository.findByUserId(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/trainers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    private TrainerPhilosophy trainer(String slug, String targetSex) {
        TrainerPhilosophy t = mock(TrainerPhilosophy.class);
        when(t.getId()).thenReturn(UUID.randomUUID());
        when(t.getSlug()).thenReturn(slug);
        when(t.getDisplayName()).thenReturn(slug);
        when(t.getTagline()).thenReturn("tagline");
        when(t.getDescription()).thenReturn("description");
        when(t.getTargetSex()).thenReturn(targetSex);
        return t;
    }

    private FitnessProfile profileWithSex(Sex sex) {
        FitnessProfile p = new FitnessProfile(USER_ID);
        p.setSex(sex);
        return p;
    }
}
