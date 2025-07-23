package de.datev.refsys.aggregation.processing.mapper;

import de.datev.refsys.generated.acds.api.model.AccountSumDay;
import de.datev.refsys.aggregation.processing.model.AccountDbKeyFields;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface AccountDbKeyFieldsMapper {

    AccountDbKeyFields accountSumDayToAccountDbKeyFields(AccountSumDay accountSumDay);

    @Mapping(source = "accountSumDay.accountNumber", target = "accountNumber", qualifiedByName = "accountGroupNumberFromAccountNumber")
    AccountDbKeyFields accountSumDayToGroupAccountDbKeyFields(AccountSumDay accountSumDay);

    @Named("accountGroupNumberFromAccountNumber")
    static Integer accountGroupNumberFromAccountNumber(Integer accountNumber) {
        return Integer.parseInt(Integer.toString(accountNumber).substring(0, 1));
    }

}
