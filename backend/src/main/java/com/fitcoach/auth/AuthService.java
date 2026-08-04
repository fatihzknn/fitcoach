package com.fitcoach.auth;

import com.fitcoach.auth.dto.AuthResponse;
import com.fitcoach.auth.dto.LoginRequest;
import com.fitcoach.auth.dto.RegisterRequest;
import com.fitcoach.auth.dto.UserDto;
import com.fitcoach.auth.jwt.CurrentUser;
import com.fitcoach.auth.jwt.JwtService;
import com.fitcoach.common.ConflictException;
import com.fitcoach.common.NotFoundException;
import com.fitcoach.profile.FitnessProfile;
import com.fitcoach.profile.FitnessProfileRepository;
import com.fitcoach.profile.dto.FitnessProfileDto;
import com.fitcoach.profile.dto.MeResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final FitnessProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(
            UserRepository userRepository,
            FitnessProfileRepository profileRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email already exists.");
        }
        Role role = request.isTrainer() ? Role.TRAINER : Role.USER;
        User user = new User(email, passwordEncoder.encode(request.password()), request.displayName().trim(), role);
        userRepository.save(user);
        return new AuthResponse(jwtService.generateToken(user), UserDto.from(user), false);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        // Throws BadCredentialsException on failure -> handled as 401 by the advice.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password()));

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Account not found."));
        boolean onboarded = isOnboarded(user);
        return new AuthResponse(jwtService.generateToken(user), UserDto.from(user), onboarded);
    }

    @Transactional(readOnly = true)
    public MeResponse getMe(CurrentUser currentUser) {
        User user = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new NotFoundException("Account not found."));
        Optional<FitnessProfile> profile = profileRepository.findByUserId(user.getId());
        boolean onboarded = profile.map(FitnessProfile::isOnboardingCompleted).orElse(false);
        FitnessProfileDto dto = profile.map(FitnessProfileDto::from).orElse(null);
        return new MeResponse(UserDto.from(user), onboarded, dto);
    }

    private boolean isOnboarded(User user) {
        return profileRepository.findByUserId(user.getId())
                .map(FitnessProfile::isOnboardingCompleted)
                .orElse(false);
    }
}
