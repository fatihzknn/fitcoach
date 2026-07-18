package com.fitcoach.measurement;

import com.fitcoach.auth.jwt.CurrentUser;
import com.fitcoach.common.NotFoundException;
import com.fitcoach.measurement.dto.BodyMeasurementDto;
import com.fitcoach.measurement.dto.SaveMeasurementRequest;
import com.fitcoach.profile.FitnessProfile;
import com.fitcoach.profile.FitnessProfileRepository;
import com.fitcoach.profile.domain.Sex;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class BodyMeasurementService {

    private final BodyMeasurementRepository repository;
    private final FitnessProfileRepository profileRepository;

    public BodyMeasurementService(BodyMeasurementRepository repository,
                                   FitnessProfileRepository profileRepository) {
        this.repository = repository;
        this.profileRepository = profileRepository;
    }

    @Transactional
    public BodyMeasurementDto save(CurrentUser currentUser, SaveMeasurementRequest req) {
        FitnessProfile profile = profileRepository.findByUserId(currentUser.id())
                .orElseThrow(() -> new NotFoundException("Profile not found."));

        LocalDate date = req.measuredAt() != null ? req.measuredAt() : LocalDate.now();

        // Upsert: one measurement set per user per day
        BodyMeasurement measurement = repository
                .findByUserIdAndMeasuredAt(currentUser.id(), date)
                .orElseGet(() -> new BodyMeasurement(currentUser.id(), date));

        measurement.setWeightKg(req.weightKg());
        measurement.setNeckCm(req.neckCm());
        measurement.setWaistCm(req.waistCm());
        measurement.setHipCm(req.hipCm());
        measurement.setChestCm(req.chestCm());
        measurement.setBicepCm(req.bicepCm());
        measurement.setThighCm(req.thighCm());
        measurement.setCalfCm(req.calfCm());
        measurement.setNotes(req.notes());

        BfResult bf = computeBodyFat(req, profile);
        measurement.setBodyFatPercentage(bf.value());
        measurement.setBodyFatMethod(bf.method());

        return BodyMeasurementDto.from(repository.save(measurement));
    }

    @Transactional(readOnly = true)
    public List<BodyMeasurementDto> getHistory(CurrentUser currentUser) {
        return repository.findAllByUserIdOrderByMeasuredAtDesc(currentUser.id())
                .stream()
                .map(BodyMeasurementDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BodyMeasurementDto getLatest(CurrentUser currentUser) {
        return repository.findFirstByUserIdOrderByMeasuredAtDesc(currentUser.id())
                .map(BodyMeasurementDto::from)
                .orElseThrow(() -> new NotFoundException("No measurements recorded yet."));
    }

    // ─── Body fat estimation ──────────────────────────────────────────────────
    //
    // Formula chosen per sex based on accuracy vs DEXA scan (gold standard):
    //
    // WOMEN — Body Adiposity Index (BAI, Bergman et al. 2011)
    //   BAI = hip_cm / (height_m ^ 1.5) − 18
    //   Inputs: hip circumference + height (from profile)
    //   Accuracy vs DEXA: r ≈ 0.85 for women — better than US Navy (r ≈ 0.73)
    //   Simpler: women only need one circumference measurement (hip).
    //
    // MEN — US Navy circumference method (Hodgdon & Beckett 1984)
    //   BF% = 495 / (1.0324 − 0.19077×log10(waist−neck) + 0.15456×log10(height)) − 450
    //   Inputs: waist, neck, height (from profile)
    //   Accuracy vs DEXA: r ≈ 0.84 for men — better than BAI for men (r ≈ 0.79)
    //
    // SEX.OTHER falls back to the men's Navy formula as the closest approximation.
    //
    // All circumference inputs are in centimetres.

    BfResult computeBodyFat(SaveMeasurementRequest req, FitnessProfile profile) {
        if (profile.getHeightCm() <= 0) return BfResult.NONE;
        double height = profile.getHeightCm();
        Sex sex = profile.getSex();

        if (sex == Sex.FEMALE) {
            return computeBai(req.hipCm(), height);
        }

        // MALE / OTHER → US Navy
        return computeNavy(req.neckCm(), req.waistCm(), height);
    }

    // Package-visible so the test can call directly
    BfResult computeBai(BigDecimal hipCm, double heightCm) {
        if (hipCm == null) return BfResult.NONE;
        double hip = hipCm.doubleValue();
        double heightM = heightCm / 100.0;
        double bai = hip / Math.pow(heightM, 1.5) - 18.0;
        if (Double.isNaN(bai) || Double.isInfinite(bai) || bai < 3 || bai > 75) return BfResult.NONE;
        return new BfResult(round(bai), "BAI");
    }

    BfResult computeNavy(BigDecimal neckCm, BigDecimal waistCm, double heightCm) {
        if (neckCm == null || waistCm == null) return BfResult.NONE;
        double neck = neckCm.doubleValue();
        double waist = waistCm.doubleValue();
        double denom = waist - neck;
        if (denom <= 0) return BfResult.NONE;
        double bf = 495.0 / (1.0324 - 0.19077 * Math.log10(denom) + 0.15456 * Math.log10(heightCm)) - 450.0;
        if (Double.isNaN(bf) || Double.isInfinite(bf) || bf < 0 || bf > 75) return BfResult.NONE;
        return new BfResult(round(bf), "NAVY");
    }

    private static BigDecimal round(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP);
    }

    /** Carries the result of a body-fat computation: the value and the formula name. */
    record BfResult(BigDecimal value, String method) {
        static final BfResult NONE = new BfResult(null, null);
    }
}
