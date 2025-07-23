package de.datev.refsys.aggregation.processing.mapper;

import de.datev.refsys.generated.acds.api.model.AccountCaption;
import de.datev.refsys.aggregation.document.model.AccountDescription;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.NullValueMappingStrategy;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface AccountDescriptionMapper {
    AccountDescription mapAcdsToDb(AccountCaption accountCaption);

    @IterableMapping(nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    List<AccountDescription> mapAcdsToDbList(List<AccountCaption> accountCaption);
}
