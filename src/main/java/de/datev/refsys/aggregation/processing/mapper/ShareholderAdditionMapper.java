package de.datev.refsys.aggregation.processing.mapper;

import de.datev.refsys.aggregation.document.model.ShareholderAddition;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.NullValueMappingStrategy;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, injectionStrategy = InjectionStrategy.CONSTRUCTOR)

public interface ShareholderAdditionMapper {
    ShareholderAddition mapAcdsToDb(de.datev.refsys.generated.acds.api.model.ShareholderAddition shareholderAddition);

    @IterableMapping(nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    List<ShareholderAddition> mapAcdsToDb(List<de.datev.refsys.generated.acds.api.model.ShareholderAddition> shareholderAddition);
}
