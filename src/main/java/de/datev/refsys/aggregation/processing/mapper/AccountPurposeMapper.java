package de.datev.refsys.aggregation.processing.mapper;

import de.datev.refsys.generated.acds.api.model.AccountPurposeMapping;
import de.datev.refsys.aggregation.document.model.AccountPurpose;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.NullValueMappingStrategy;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface AccountPurposeMapper {
    AccountPurpose mapAcdsToDb(AccountPurposeMapping accountPurposeMapping);

    @IterableMapping(nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    List<AccountPurpose> mapAcdsToDbList(List<AccountPurposeMapping> accountPurposeMappingList);
}
