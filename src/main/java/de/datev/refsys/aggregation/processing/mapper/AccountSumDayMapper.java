package de.datev.refsys.aggregation.processing.mapper;

import de.datev.refsys.aggregation.processing.api.model.AccountSumDayDelta;
import de.datev.refsys.generated.acds.api.model.AccountSumDay;
import de.datev.refsys.aggregation.document.model.AccountValue;
import de.datev.refsys.aggregation.document.model.MovementDataDay;
import de.datev.refsys.aggregation.processing.util.DateFunctions;
import org.bson.types.Decimal128;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import static de.datev.refsys.aggregation.document.model.constants.CollectionConstants.OPENING_BALANCE;
import static de.datev.refsys.aggregation.processing.service.PersonGroupAggregationService.getDateFromKey;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface AccountSumDayMapper {
    @Mapping(source = "movementDataDay.accountNumber", target = "accountNumber")
    @Mapping(source = "movementDataDay.accountingReasonId", target = "accountingReasonId")
    @Mapping(source = "movementDataDay.additionalParams.recordType", target = "recordType", qualifiedByName = "defaultIfNull")
    @Mapping(source = "movementDataDay.additionalParams.rwShareholderId", target = "rwShareholderId")
    @Mapping(source = "movementDataDay.additionalParams.businessAssetsAssignment", target = "businessAssetsAssignment")
    @Mapping(source = "movementDataDay.additionalParams.accountingCommitted", target = "accountingCommitted")
    @Mapping(source = "movementDataDay.additionalParams.cost1", target = "cost1")
    @Mapping(source = "movementDataDay.additionalParams.cost2", target = "cost2")
    @Mapping(source = "movementDataDay.additionalParams.agricultureAndForestryAccountType", target = "agricultureAndForestryAccountType")
    @Mapping(source = "movementDataDay.additionalParams.taxRate", target = "taxRate", qualifiedByName = "decimal128ToBigDecimal")
    @Mapping(source = "month", target = "month")
    @Mapping(source = "date", target = "date")
    @Mapping(source = "value.amountDebit", target = "amountDebit", qualifiedByName = "longInCentToDouble")
    @Mapping(source = "value.amountCredit", target = "amountCredit", qualifiedByName = "longInCentToDouble")
    @Mapping(source = "value.quantityCredit", target = "quantityCredit")
    @Mapping(source = "value.quantityDebit", target = "quantityDebit")
    @Mapping(source = "value.weightCredit", target = "weightCredit", qualifiedByName = "longInCentToDouble")
    @Mapping(source = "value.weightDebit", target = "weightDebit", qualifiedByName = "longInCentToDouble")
    AccountSumDay mapDbToApiModel(AccountValue value, MovementDataDay movementDataDay, Integer date, Integer month);

    AccountSumDay mapAccountSumDayDelta(AccountSumDayDelta accountSumDayDelta);

    default List<AccountSumDay> mapDbToApiModel(MovementDataDay movementDataDay) {
        return movementDataDay.getValues()
                .entrySet()
                .stream()
                .map(entry -> {
                    Integer date = getDateFromKey(entry.getKey(), movementDataDay.getFiscalYear());
                    Integer month = getMonthFromDate(date, entry.getKey(), movementDataDay.getFiscalYear());
                    return mapDbToApiModel(entry.getValue(), movementDataDay, date, month);
                })
                .collect(Collectors.toList());
    }

    @Named("decimal128ToBigDecimal")
    static BigDecimal decimal128ToBigDecimal(Decimal128 value) {
        return decimal128ToBigDecimalConverter(value);
    }

    @Named("longInCentToDouble")
    static Double longInCentToDouble(Long value) {
        if (value == null) {
            return null;
        }
        return value / 100.0;
    }

    @Named("defaultIfNull")
    static Integer defaultIfNull(Integer value) {
        return value != null ? value : 1;
    }

    static BigDecimal decimal128ToBigDecimalConverter(Decimal128 value) {
        return value != null ? value.bigDecimalValue() : null;
    }

    static int getMonthFromDate(int date, String key, int yearBegin) {
        if (key.equals(OPENING_BALANCE)) {
            return 0;
        }
        int dateYear = date / DateFunctions.YEAR_POSITION;
        int yearBeginYear = yearBegin / DateFunctions.YEAR_POSITION;
        int dateMonthDay = date - dateYear * DateFunctions.YEAR_POSITION;
        int yearBeginMonthDay = yearBegin - yearBeginYear * DateFunctions.YEAR_POSITION;
        int monthDifference;
        if (dateYear == yearBeginYear) {
            monthDifference = dateMonthDay / DateFunctions.MONTH_POSITION - yearBeginMonthDay / DateFunctions.MONTH_POSITION;
        } else {
            monthDifference =
                    DateFunctions.MONTHS_PER_YEAR + dateMonthDay / DateFunctions.MONTH_POSITION - yearBeginMonthDay / DateFunctions.MONTH_POSITION;
        }
        return monthDifference + 1;
    }
}
