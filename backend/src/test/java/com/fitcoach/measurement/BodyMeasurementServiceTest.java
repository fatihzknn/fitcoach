package com.fitcoach.measurement;

import com.fitcoach.measurement.BodyMeasurementService.BfResult;
import com.fitcoach.measurement.dto.SaveMeasurementRequest;
import com.fitcoach.profile.FitnessProfile;
import com.fitcoach.profile.domain.Sex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for sex-specific body fat formulas:
 *   Women → BAI  (Body Adiposity Index, Bergman et al. 2011)
 *   Men   → US Navy circumference method (Hodgdon & Beckett 1984)
 */
@ExtendWith(MockitoExtension.class)
class BodyMeasurementServiceTest {

    @Mock private BodyMeasurementRepository repository;
    @Mock private com.fitcoach.profile.FitnessProfileRepository profileRepository;

    @InjectMocks
    private BodyMeasurementService service;

    // ─── helpers ─────────────────────────────────────────────────────────────

    private FitnessProfile profile(int heightCm, Sex sex) {
        FitnessProfile p = mock(FitnessProfile.class);
        lenient().when(p.getHeightCm()).thenReturn(heightCm);
        lenient().when(p.getSex()).thenReturn(sex);
        return p;
    }

    private SaveMeasurementRequest req(Double neck, Double waist, Double hip) {
        return new SaveMeasurementRequest(
                LocalDate.now(), null,
                neck  != null ? BigDecimal.valueOf(neck)  : null,
                waist != null ? BigDecimal.valueOf(waist) : null,
                hip   != null ? BigDecimal.valueOf(hip)   : null,
                null, null, null, null, null
        );
    }

    // ─── Women: BAI formula ───────────────────────────────────────────────────

    @Test
    void female_bai_knownValues() {
        // hip=95, height=165cm → BAI = 95/(1.65^1.5)-18 ≈ 26.8%
        BfResult r = service.computeBai(BigDecimal.valueOf(95), 165);
        assertThat(r.value()).isNotNull();
        assertThat(r.value().doubleValue()).isCloseTo(26.8, within(0.2));
        assertThat(r.method()).isEqualTo("BAI");
    }

    @Test
    void female_bai_slimmer() {
        // hip=85, height=170cm → lower BF%
        BfResult r1 = service.computeBai(BigDecimal.valueOf(85), 170);
        BfResult r2 = service.computeBai(BigDecimal.valueOf(95), 170);
        assertThat(r1.value().doubleValue()).isLessThan(r2.value().doubleValue());
    }

    @Test
    void female_bai_tallerSameHip_lowerBf() {
        BfResult short_ = service.computeBai(BigDecimal.valueOf(95), 160);
        BfResult tall   = service.computeBai(BigDecimal.valueOf(95), 175);
        assertThat(tall.value().doubleValue()).isLessThan(short_.value().doubleValue());
    }

    @Test
    void female_bai_missingHip_returnsNone() {
        BfResult r = service.computeBai(null, 165);
        assertThat(r.value()).isNull();
        assertThat(r.method()).isNull();
    }

    @Test
    void female_bai_result_hasOneDecimalPlace() {
        BfResult r = service.computeBai(BigDecimal.valueOf(95), 165);
        assertThat(r.value().scale()).isEqualTo(1);
    }

    @Test
    void female_routedToBai_viaComputeBodyFat() {
        // computeBodyFat() should select BAI for FEMALE sex
        BfResult r = service.computeBodyFat(req(null, null, 95.0), profile(165, Sex.FEMALE));
        assertThat(r.method()).isEqualTo("BAI");
    }

    // ─── Men: US Navy formula ─────────────────────────────────────────────────

    @Test
    void male_navy_knownValues() {
        // height=180, neck=38.5, waist=84 → ~14.9% (verified against Navy calculator)
        BfResult r = service.computeNavy(BigDecimal.valueOf(38.5), BigDecimal.valueOf(84), 180);
        assertThat(r.value()).isNotNull();
        assertThat(r.value().doubleValue()).isCloseTo(14.9, within(0.2));
        assertThat(r.method()).isEqualTo("NAVY");
    }

    @Test
    void male_navy_higherWaist_higherBf() {
        BfResult r1 = service.computeNavy(BigDecimal.valueOf(38), BigDecimal.valueOf(80), 175);
        BfResult r2 = service.computeNavy(BigDecimal.valueOf(38), BigDecimal.valueOf(95), 175);
        assertThat(r2.value().doubleValue()).isGreaterThan(r1.value().doubleValue());
    }

    @Test
    void male_navy_tallPersonSameMeasurements_lowerBf() {
        BfResult short_ = service.computeNavy(BigDecimal.valueOf(38), BigDecimal.valueOf(85), 165);
        BfResult tall   = service.computeNavy(BigDecimal.valueOf(38), BigDecimal.valueOf(85), 190);
        assertThat(tall.value().doubleValue()).isLessThan(short_.value().doubleValue());
    }

    @Test
    void male_navy_waistEqualsNeck_returnsNone() {
        BfResult r = service.computeNavy(BigDecimal.valueOf(40), BigDecimal.valueOf(40), 180);
        assertThat(r.value()).isNull();
    }

    @Test
    void male_navy_missingNeck_returnsNone() {
        BfResult r = service.computeNavy(null, BigDecimal.valueOf(84), 180);
        assertThat(r.value()).isNull();
    }

    @Test
    void male_navy_missingWaist_returnsNone() {
        BfResult r = service.computeNavy(BigDecimal.valueOf(38.5), null, 180);
        assertThat(r.value()).isNull();
    }

    @Test
    void male_routedToNavy_viaComputeBodyFat() {
        BfResult r = service.computeBodyFat(req(38.5, 84.0, null), profile(180, Sex.MALE));
        assertThat(r.method()).isEqualTo("NAVY");
    }

    @Test
    void other_routedToNavy_viaComputeBodyFat() {
        BfResult r = service.computeBodyFat(req(38.5, 84.0, null), profile(175, Sex.OTHER));
        assertThat(r.method()).isEqualTo("NAVY");
    }
}
