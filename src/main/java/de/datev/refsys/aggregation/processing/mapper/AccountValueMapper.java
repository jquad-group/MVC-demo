package de.datev.refsys.aggregation.processing.mapper;

import de.datev.refsys.generated.acds.api.model.AccountSumDay;
import de.datev.refsys.aggregation.document.model.AccountGroupValue;
import de.datev.refsys.aggregation.document.model.AccountValue;
import de.datev.refsys.aggregation.processing.model.PersonGroupAmountValues;
import de.datev.refsys.aggregation.processing.util.Util;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface AccountValueMapper {

    @Mapping(source = "accountSumDay.amountDebit", target = "amountDebit", qualifiedByName = "doubleAmountToLongAmountInCent")
    @Mapping(source = "accountSumDay.amountCredit", target = "amountCredit", qualifiedByName = "doubleAmountToLongAmountInCent")
    @Mapping(source = "accountSumDay.weightDebit", target = "weightDebit", qualifiedByName = "doubleAmountToLongAmountInCent")
    @Mapping(source = "accountSumDay.weightCredit", target = "weightCredit", qualifiedByName = "doubleAmountToLongAmountInCent")
    AccountValue accountSumDayToAccountValue(AccountSumDay accountSumDay);

    AccountGroupValue amountValuesToAccountGroupValue(PersonGroupAmountValues amountValues);

    @Mapping(source = "accountSumDay.weightDebit", target = "weightDebit", qualifiedByName = "doubleAmountToLongAmountInCent")
    @Mapping(source = "accountSumDay.weightCredit", target = "weightCredit", qualifiedByName = "doubleAmountToLongAmountInCent")
    AccountGroupValue accountSumDayToAccountGroupValue(AccountSumDay accountSumDay, PersonGroupAmountValues amountValues);

    @Named("doubleAmountToLongAmountInCent")
    static Long doubleToLongAmountInCent(Double value) {
        return Util.amountToLongInCent(value);
    }

}
