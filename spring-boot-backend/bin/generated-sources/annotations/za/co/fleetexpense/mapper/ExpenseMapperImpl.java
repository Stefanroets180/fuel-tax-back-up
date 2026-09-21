package za.co.fleetexpense.mapper;

import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import za.co.fleetexpense.dto.ExpenseCreateRequest;
import za.co.fleetexpense.dto.ExpenseDTO;
import za.co.fleetexpense.dto.ExpenseUpdateRequest;
import za.co.fleetexpense.entity.Expense;
import za.co.fleetexpense.entity.Organization;
import za.co.fleetexpense.entity.User;
import za.co.fleetexpense.entity.Vehicle;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-09-21T10:29:40+0200",
    comments = "version: 1.6.2, compiler: Eclipse JDT (IDE) 3.46.100.v20260826-1225, environment: Java 21.0.9 (Oracle Corporation)"
)
@Component
public class ExpenseMapperImpl implements ExpenseMapper {

    @Autowired
    private VehicleMapper vehicleMapper;

    @Override
    public ExpenseDTO toDTO(Expense entity) {
        if ( entity == null ) {
            return null;
        }

        ExpenseDTO expenseDTO = new ExpenseDTO();

        expenseDTO.setOrganizationId( entityOrganizationId( entity ) );
        expenseDTO.setVehicle( vehicleMapper.toDTO( entity.getVehicle() ) );
        expenseDTO.setVehicleId( entityVehicleId( entity ) );
        expenseDTO.setUserId( entityUserId( entity ) );
        expenseDTO.setUserName( formatUserName( entityUserFirstName( entity ) ) );
        expenseDTO.setAmountIncludesVat( entity.getAmountIncludesVat() );
        expenseDTO.setAmountZar( entity.getAmountZar() );
        expenseDTO.setCategory( entity.getCategory() );
        expenseDTO.setCreatedAt( entity.getCreatedAt() );
        expenseDTO.setDescription( entity.getDescription() );
        expenseDTO.setExpenseDate( entity.getExpenseDate() );
        expenseDTO.setId( entity.getId() );
        expenseDTO.setInvoiceNumber( entity.getInvoiceNumber() );
        expenseDTO.setIsLocked( entity.getIsLocked() );
        expenseDTO.setIsTaxDeductible( entity.getIsTaxDeductible() );
        expenseDTO.setLockedAt( entity.getLockedAt() );
        expenseDTO.setLockedReason( entity.getLockedReason() );
        expenseDTO.setNetAmountCents( entity.getNetAmountCents() );
        expenseDTO.setOdometerReading( entity.getOdometerReading() );
        expenseDTO.setReceiptImageKey( entity.getReceiptImageKey() );
        expenseDTO.setReceiptImageUrl( entity.getReceiptImageUrl() );
        expenseDTO.setSupplierName( entity.getSupplierName() );
        expenseDTO.setTaxExpenseClassification( entity.getTaxExpenseClassification() );
        expenseDTO.setUpdatedAt( entity.getUpdatedAt() );
        expenseDTO.setVatAmountCents( entity.getVatAmountCents() );
        expenseDTO.setVatAmountZar( entity.getVatAmountZar() );

        expenseDTO.setLockedByUserId( extractLockedByUserIdSafely(entity) );

        return expenseDTO;
    }

    @Override
    public Expense toEntity(ExpenseCreateRequest request) {
        if ( request == null ) {
            return null;
        }

        Expense.ExpenseBuilder expense = Expense.builder();

        expense.vehicle( vehicleFromId( request.getVehicleId() ) );
        if ( request.getIsTaxDeductible() != null ) {
            expense.isTaxDeductible( request.getIsTaxDeductible() );
        }
        else {
            expense.isTaxDeductible( true );
        }
        expense.amountZar( request.getAmountZar() );
        expense.category( request.getCategory() );
        expense.description( request.getDescription() );
        expense.expenseDate( request.getExpenseDate() );
        expense.extraFields( request.getExtraFields() );
        expense.invoiceNumber( request.getInvoiceNumber() );
        expense.odometerReading( request.getOdometerReading() );
        expense.supplierName( request.getSupplierName() );
        expense.taxExpenseClassification( request.getTaxExpenseClassification() );

        expense.isLocked( false );

        return expense.build();
    }

    @Override
    public void updateEntity(Expense entity, ExpenseUpdateRequest request) {
        if ( request == null ) {
            return;
        }

        if ( request.getIsTaxDeductible() != null ) {
            entity.setIsTaxDeductible( request.getIsTaxDeductible() );
        }
        else {
            entity.setIsTaxDeductible( true );
        }
        entity.setAmountZar( request.getAmountZar() );
        entity.setDescription( request.getDescription() );
        entity.setExpenseDate( request.getExpenseDate() );
        entity.setInvoiceNumber( request.getInvoiceNumber() );
        entity.setOdometerReading( request.getOdometerReading() );
        entity.setSupplierName( request.getSupplierName() );
        entity.setTaxExpenseClassification( request.getTaxExpenseClassification() );
    }

    private UUID entityOrganizationId(Expense expense) {
        Organization organization = expense.getOrganization();
        if ( organization == null ) {
            return null;
        }
        return organization.getId();
    }

    private UUID entityVehicleId(Expense expense) {
        Vehicle vehicle = expense.getVehicle();
        if ( vehicle == null ) {
            return null;
        }
        return vehicle.getId();
    }

    private UUID entityUserId(Expense expense) {
        User user = expense.getUser();
        if ( user == null ) {
            return null;
        }
        return user.getId();
    }

    private String entityUserFirstName(Expense expense) {
        User user = expense.getUser();
        if ( user == null ) {
            return null;
        }
        return user.getFirstName();
    }
}
