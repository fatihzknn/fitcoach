package com.fitcoach.exercise;

import com.fitcoach.auth.Role;
import com.fitcoach.auth.jwt.CurrentUser;
import com.fitcoach.auth.jwt.JwtService;
import com.fitcoach.exercise.domain.DifficultyLevel;
import com.fitcoach.exercise.domain.MovementPattern;
import com.fitcoach.exercise.domain.MuscleGroup;
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
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = ExerciseController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class ExerciseControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean ExerciseRepository exerciseRepository;
    @MockBean JwtService jwtService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final CurrentUser CURRENT_USER = new CurrentUser(USER_ID, "test@example.com", Role.USER);

    @BeforeEach
    void setUpSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(CURRENT_USER, null, List.of())
        );
    }

    @Test
    void listExercises_returns200SortedByMuscleGroupThenName() throws Exception {
        Exercise squat = new Exercise("Goblet Squat", MuscleGroup.QUADS, MovementPattern.SQUAT,
                DifficultyLevel.BEGINNER, "form cue", "common mistake");
        Exercise benchPress = new Exercise("Barbell Bench Press", MuscleGroup.CHEST, MovementPattern.PUSH,
                DifficultyLevel.INTERMEDIATE, "form cue", "common mistake");
        when(exerciseRepository.findAll()).thenReturn(List.of(squat, benchPress));

        mockMvc.perform(get("/api/exercises"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Barbell Bench Press"))
                .andExpect(jsonPath("$[1].name").value("Goblet Squat"));
    }
}
