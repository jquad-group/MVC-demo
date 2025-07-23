package de.datev.refsys.aggregation.processing.mapper;

import de.datev.refsys.aggregation.document.model.AlternativeAccountTranslation;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.NullValueMappingStrategy;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface AlternativeAccountTranslationMapper {
    AlternativeAccountTranslation mapAcdsToDb(de.datev.refsys.generated.acds.api.model.AlternativeAccountTranslation alternativeAccountTranslation);

    @IterableMapping(nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    List<AlternativeAccountTranslation> mapAcdsToDbList(
            List<de.datev.refsys.generated.acds.api.model.AlternativeAccountTranslation> alternativeAccountTranslation);
}
