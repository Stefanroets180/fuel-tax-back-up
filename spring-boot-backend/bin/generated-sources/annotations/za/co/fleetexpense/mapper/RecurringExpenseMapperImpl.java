package za.co.fleetexpense.mapper;

import java.math.BigDecimal;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import za.co.fleetexpense.dto.RecurringExpenseCreateRequest;
import za.co.fleetexpense.dto.RecurringExpenseDTO;
import za.co.fleetexpense.entity.RecurringExpense;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-09-21T10:29:40+0200",
    comments = "version: 1.6.2, compiler: Eclipse JDT (IDE) 3.46.100.v20260826-1225, environment: Java 21.0.9 (Oracle Corporation)"
)
@Component
public class RecurringExpenseMapperImpl implements RecurringExpenseMapper {

    @Override
    public RecurringExpenseDTO toDTO(RecurringExpense entity) {
        if ( entity == null ) {
            return null;
        }

        RecurringExpenseDTO recurringExpenseDTO = new RecurringExpenseDTO();

        if ( entity.getAmountZar() != null ) {
            recurringExpenseDTO.setAmountZar( entity.getAmountZar() );
        }
        if ( entity.getCategory() != null ) {
            recurringExpenseDTO.setCategory( entity.getCategory() );
        }
        if ( entity.getCreatedAt() != null ) {
            recurringExpenseDTO.setCreatedAt( entity.getCreatedAt() );
        }
        if ( entity.getDescription() != null ) {
            recurringExpenseDTO.setDescription( entity.getDescription() );
        }
        if ( entity.getId() != null ) {
            recurringExpenseDTO.setId( entity.getId() );
        }
        if ( entity.getInvoiceNumber() != null ) {
            recurringExpenseDTO.setInvoiceNumber( entity.getInvoiceNumber() );
        }
        if ( entity.getIsActive() != null ) {
            recurringExpenseDTO.setIsActive( entity.getIsActive() );
        }
        if ( entity.getIsRecurring() != null ) {
            recurringExpenseDTO.setIsRecurring( entity.getIsRecurring() );
        }
        if ( entity.getIsTaxDeductible() != null ) {
            recurringExpenseDTO.setIsTaxDeductible( entity.getIsTaxDeductible() );
        }
        if ( entity.getOdometerReading() != null ) {
            recurringExpenseDTO.setOdometerReading( entity.getOdometerReading() );
        }
        if ( entity.getRecurrenceDays() != null ) {
            recurringExpenseDTO.setRecurrenceDays( entity.getRecurrenceDays() );
        }
        if ( entity.getRecurrenceDaysOfMonth() != null ) {
            recurringExpenseDTO.setRecurrenceDaysOfMonth( entity.getRecurrenceDaysOfMonth() );
        }
        if ( entity.getRecurrenceEndDate() != null ) {
            recurringExpenseDTO.setRecurrenceEndDate( entity.getRecurrenceEndDate() );
        }
        if ( entity.getRecurrenceStartDate() != null ) {
            recurringExpenseDTO.setRecurrenceStartDate( entity.getRecurrenceStartDate() );
        }
        if ( entity.getSupplierName() != null ) {
            recurringExpenseDTO.setSupplierName( entity.getSupplierName() );
        }
        if ( entity.getUpdatedAt() != null ) {
            recurringExpenseDTO.setUpdatedAt( entity.getUpdatedAt() );
        }
        if ( entity.getVatAmountZar() != null ) {
            recurringExpenseDTO.setVatAmountZar( entity.getVatAmountZar() );
        }

        return recurringExpenseDTO;
    }

    @Override
    public RecurringExpense toEntity(RecurringExpenseCreateRequest request) {
        if ( request == null ) {
            return null;
        }

        RecurringExpense.RecurringExpenseBuilder recurringExpense = RecurringExpense.builder();

        if ( request.getVatAmountZar() != null ) {
            recurringExpense.vatAmountZar( request.getVatAmountZar() );
        }
        else {
            recurringExpense.vatAmountZar( new BigDecimal( "0" ) );
        }
        if ( request.getIsTaxDeductible() != null ) {
            recurringExpense.isTaxDeductible( request.getIsTaxDeductible() );
        }
        else {
            recurringExpense.isTaxDeductible( true );
        }
        if ( request.getIsRecurring() != null ) {
            recurringExpense.isRecurring( request.getIsRecurring() );
        }
        else {
            recurringExpense.isRecurring( false );
        }
        if ( request.getAmountZar() != null ) {
            recurringExpense.amountZar( request.getAmountZar() );
        }
        if ( request.getCategory() != null ) {
            recurringExpense.category( request.getCategory() );
        }
        if ( request.getDescription() != null ) {
            recurringExpense.description( request.getDescription() );
        }
        if ( request.getInvoiceNumber() != null ) {
            recurringExpense.invoiceNumber( request.getInvoiceNumber() );
        }
        if ( request.getOdometerReading() != null ) {
            recurringExpense.odometerReading( request.getOdometerReading() );
        }
        if ( request.getRecurrenceDays() != null ) {
            recurringExpense.recurrenceDays( request.getRecurrenceDays() );
        }
        if ( request.getRecurrenceDaysOfMonth() != null ) {
            recurringExpense.recurrenceDaysOfMonth( request.getRecurrenceDaysOfMonth() );
        }
        if ( request.getRecurrenceEndDate() != null ) {
            recurringExpense.recurrenceEndDate( request.getRecurrenceEndDate() );
        }
        if ( request.getRecurrenceStartDate() != null ) {
            recurringExpense.recurrenceStartDate( request.getRecurrenceStartDate() );
        }
        if ( request.getSupplierName() != null ) {
            recurringExpense.supplierName( request.getSupplierName() );
        }

        return recurringExpense.build();
    }
}
