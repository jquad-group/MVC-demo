package de.datev.refsys.aggregation.processing.mapper;

import de.datev.refsys.aggregation.document.model.CollectiveAccount;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.NullValueMappingStrategy;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface CollectiveAccountMapper {
    CollectiveAccount mapAcdsToDb(de.datev.refsys.generated.acds.api.model.CollectiveAccount collectiveAccount);

    @IterableMapping(nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    List<CollectiveAccount> mapAcdsToDbList(List<de.datev.refsys.generated.acds.api.model.CollectiveAccount> collectiveAccountList);
}
