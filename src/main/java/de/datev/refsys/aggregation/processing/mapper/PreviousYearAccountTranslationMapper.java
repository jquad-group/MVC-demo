package de.datev.refsys.aggregation.processing.mapper;

import de.datev.refsys.aggregation.document.model.PreviousYearAccountTranslation;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.NullValueMappingStrategy;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface PreviousYearAccountTranslationMapper {
    PreviousYearAccountTranslation mapAcdsToDb(de.datev.refsys.generated.acds.api.model.PreviousYearAccountTranslation accountTranslation);

    @IterableMapping(nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    List<PreviousYearAccountTranslation> mapAcdsToDbList(List<de.datev.refsys.generated.acds.api.model.PreviousYearAccountTranslation> accountTranslation);
}
