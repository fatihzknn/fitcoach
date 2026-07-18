package com.fitcoach.measurement;

import com.fitcoach.auth.jwt.CurrentUser;
import com.fitcoach.measurement.dto.BodyMeasurementDto;
import com.fitcoach.measurement.dto.SaveMeasurementRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/measurements")
@Tag(name = "Body Measurements", description = "Body composition tracking with US Navy BF% calculation")
@SecurityRequirement(name = "bearer-jwt")
public class BodyMeasurementController {

    private final BodyMeasurementService service;

    public BodyMeasurementController(BodyMeasurementService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Save a body measurement entry (upsert by date). BF% is auto-computed via the US Navy method.")
    public ResponseEntity<BodyMeasurementDto> save(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestBody SaveMeasurementRequest request) {
        BodyMeasurementDto dto = service.save(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/history")
    @Operation(summary = "Get all body measurements, newest first")
    public List<BodyMeasurementDto> getHistory(@AuthenticationPrincipal CurrentUser currentUser) {
        return service.getHistory(currentUser);
    }

    @GetMapping("/latest")
    @Operation(summary = "Get the most recent body measurement entry")
    public BodyMeasurementDto getLatest(@AuthenticationPrincipal CurrentUser currentUser) {
        return service.getLatest(currentUser);
    }
}
