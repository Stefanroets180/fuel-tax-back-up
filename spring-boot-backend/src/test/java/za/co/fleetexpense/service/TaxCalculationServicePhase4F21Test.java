package za.co.fleetexpense.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.fleetexpense.entity.enums.TaxCalculationMethod;
import za.co.fleetexpense.exception.ValidationException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TaxCalculationServicePhase4F21Test {

    @InjectMocks
    private TaxCalculationService taxCalculationService;

    @Test
    void testResolveCalculationMethod_ExplicitRequestWins() {
        // Given: explicit request and organization default
        TaxCalculationMethod requested = TaxCalculationMethod.SARS_COST_SCALE;
        TaxCalculationMethod orgDefault = TaxCalculationMethod.ACTUAL_COSTS;

        // When
        TaxCalculationMethod resolved = taxCalculationService.resolveCalculationMethod(requested, orgDefault);

        // Then: explicit request wins
        assertEquals(TaxCalculationMethod.SARS_COST_SCALE, resolved);
    }

    @Test
    void testResolveCalculationMethod_NullRequestUsesOrgDefault() {
        // Given: null request, organization default set
        TaxCalculationMethod requested = null;
        TaxCalculationMethod orgDefault = TaxCalculationMethod.SARS_COST_SCALE;

        // When
        TaxCalculationMethod resolved = taxCalculationService.resolveCalculationMethod(requested, orgDefault);

        // Then: organization default is used
        assertEquals(TaxCalculationMethod.SARS_COST_SCALE, resolved);
    }

    @Test
    void testResolveCalculationMethod_NullRequestNullOrgDefault_ThrowsError() {
        // Given: both null
        TaxCalculationMethod requested = null;
        TaxCalculationMethod orgDefault = null;

        // When/Then: explicit validation error
        ValidationException ex = assertThrows(ValidationException.class, () ->
            taxCalculationService.resolveCalculationMethod(requested, orgDefault)
        );
        assertEquals("Organization default tax calculation method is not set.", ex.getMessage());
    }

    @Test
    void testResolveCalculationMethod_ExplicitRequestNullOrgDefault_Wins() {
        // Given: explicit request, null org default
        TaxCalculationMethod requested = TaxCalculationMethod.ACTUAL_COSTS;
        TaxCalculationMethod orgDefault = null;

        // When
        TaxCalculationMethod resolved = taxCalculationService.resolveCalculationMethod(requested, orgDefault);

        // Then: explicit request wins
        assertEquals(TaxCalculationMethod.ACTUAL_COSTS, resolved);
    }
}
