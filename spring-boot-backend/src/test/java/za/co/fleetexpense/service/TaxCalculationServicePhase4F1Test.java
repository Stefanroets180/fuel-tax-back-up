package za.co.fleetexpense.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.fleetexpense.entity.SarsCostScaleBracket;
import za.co.fleetexpense.entity.VehicleTaxProfile;
import za.co.fleetexpense.entity.enums.TaxpayerType;
import za.co.fleetexpense.repository.SarsCostScaleBracketRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaxCalculationServicePhase4F1Test {

    @Mock
    private SarsCostScaleBracketRepository costScaleBracketRepository;

    @InjectMocks
    private TaxCalculationService taxCalculationService;

    private VehicleTaxProfile profile;
    private SarsCostScaleBracket bracket;

    @BeforeEach
    void setUp() {
        profile = VehicleTaxProfile.builder()
                .id(java.util.UUID.randomUUID())
                .taxpayerType(TaxpayerType.EMPLOYEE)
                .vehicleCostCents(50000000L) // R500,000
                .effectiveFrom(LocalDate.of(2026, Month.MARCH, 1))
                .fuelBorneBy("EMPLOYEE")
                .maintenanceBorneBy("EMPLOYEE")
                .coveredByMaintenancePlan(false)
                .build();

        bracket = SarsCostScaleBracket.builder()
                .id(1L)
                .taxYear(2027)
                .bracketIndex(1)
                .minVehicleValueCents(1L)
                .maxVehicleValueCents(100000000L)
                .fixedCostCents(8000000L) // R80,000
                .fuelCostTenthsCentsPerKm(1467L) // 146.7c/km
                .maintenanceCostTenthsCentsPerKm(474L) // 47.4c/km
                .sourceName("SARS 2027")
                .build();
    }

    @Test
    void testCalculateSarsCostScale_FullYear2027_ExactDays() {
        // Given: assessment year 2027
        LocalDate start = LocalDate.of(2026, Month.MARCH, 1);
        LocalDate endInclusive = LocalDate.of(2027, Month.FEBRUARY, 28);

        when(costScaleBracketRepository.findByTaxYearAndVehicleValue(eq(2027), eq(50000000L)))
                .thenReturn(Optional.of(bracket));

        // When
        var result = taxCalculationService.calculateSarsCostScale(
                profile, start, endInclusive, BigDecimal.valueOf(15000), BigDecimal.valueOf(20000));

        // Then: exact day counts
        assertEquals(365, result.getDaysInTaxYear(), "daysInTaxYear should be 365 for 2027");
        assertEquals(365, result.getBusinessUseDays(), "businessUseDays should be 365 for full year");
    }

    @Test
    void testCalculateSarsCostScale_LeapYear2028_ExactDays() {
        // Given: assessment year 2028 (leap year)
        LocalDate start = LocalDate.of(2027, Month.MARCH, 1);
        LocalDate endInclusive = LocalDate.of(2028, Month.FEBRUARY, 29);

        when(costScaleBracketRepository.findByTaxYearAndVehicleValue(eq(2028), eq(50000000L)))
                .thenReturn(Optional.of(bracket));

        // When
        var result = taxCalculationService.calculateSarsCostScale(
                profile, start, endInclusive, BigDecimal.valueOf(15000), BigDecimal.valueOf(20000));

        // Then: exact day counts
        assertEquals(366, result.getDaysInTaxYear(), "daysInTaxYear should be 366 for leap year 2028");
        assertEquals(366, result.getBusinessUseDays(), "businessUseDays should be 366 for full leap year");
    }

    @Test
    void testCalculateSarsCostScale_MidYearDatePlacedInBusinessUse() {
        // Given: assessment year 2027, datePlacedInBusinessUse = 2026-07-01
        LocalDate start = LocalDate.of(2026, Month.MARCH, 1);
        LocalDate endInclusive = LocalDate.of(2027, Month.FEBRUARY, 28);
        profile.setDatePlacedInBusinessUse(LocalDate.of(2026, Month.JULY, 1));

        when(costScaleBracketRepository.findByTaxYearAndVehicleValue(eq(2027), eq(50000000L)))
                .thenReturn(Optional.of(bracket));

        // When
        var result = taxCalculationService.calculateSarsCostScale(
                profile, start, endInclusive, BigDecimal.valueOf(15000), BigDecimal.valueOf(20000));

        // Then: businessUseDays from 2026-07-01 through 2027-02-28 inclusive
        // July 1 to Feb 28: 31 (July) + 31 (Aug) + 30 (Sep) + 31 (Oct) + 30 (Nov) + 31 (Dec) + 31 (Jan) + 28 (Feb) = 243 days
        assertEquals(243, result.getBusinessUseDays(), "businessUseDays should be 243 from 2026-07-01 to 2027-02-28");
        assertEquals(365, result.getDaysInTaxYear(), "daysInTaxYear should still be 365");
    }

    @Test
    void testCalculateSarsCostScale_EffectiveToIsExclusive() {
        // Given: effectiveTo = 2026-09-01 (exclusive)
        LocalDate start = LocalDate.of(2026, Month.MARCH, 1);
        LocalDate endInclusive = LocalDate.of(2027, Month.FEBRUARY, 28);
        profile.setEffectiveTo(LocalDate.of(2026, Month.SEPTEMBER, 1));

        when(costScaleBracketRepository.findByTaxYearAndVehicleValue(eq(2027), eq(50000000L)))
                .thenReturn(Optional.of(bracket));

        // When
        var result = taxCalculationService.calculateSarsCostScale(
                profile, start, endInclusive, BigDecimal.valueOf(15000), BigDecimal.valueOf(20000));

        // Then: 2026-09-01 itself is NOT counted
        // March 1 to August 31: 31 (Mar) + 30 (Apr) + 31 (May) + 30 (Jun) + 31 (Jul) + 31 (Aug) = 184 days
        assertEquals(184, result.getBusinessUseDays(), "businessUseDays should be 184, excluding 2026-09-01");
    }

    @Test
    void testCalculateSarsCostScale_EffectiveToBeyondTaxYear() {
        // Given: effectiveTo = 2027-06-01 (beyond tax year)
        LocalDate start = LocalDate.of(2026, Month.MARCH, 1);
        LocalDate endInclusive = LocalDate.of(2027, Month.FEBRUARY, 28);
        profile.setEffectiveTo(LocalDate.of(2027, Month.JUNE, 1));

        when(costScaleBracketRepository.findByTaxYearAndVehicleValue(eq(2027), eq(50000000L)))
                .thenReturn(Optional.of(bracket));

        // When
        var result = taxCalculationService.calculateSarsCostScale(
                profile, start, endInclusive, BigDecimal.valueOf(15000), BigDecimal.valueOf(20000));

        // Then: businessUseDays capped at tax year end
        assertEquals(365, result.getBusinessUseDays(), "businessUseDays should be capped at 365 (tax year end)");
    }
}
