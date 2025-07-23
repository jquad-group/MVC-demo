package de.datev.refsys.aggregation.processing.mapper;

import de.datev.refsys.aggregation.document.model.CustomColumnStructureContent;
import de.datev.refsys.aggregation.processing.util.Util;
import de.datev.refsys.generated.acds.api.model.CustomColumnStructure;
import de.datev.refsys.generated.acds.api.model.MasterdataContext;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.NullValueMappingStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface CustomColumnStructureContentMapper {
    int DEFAULT_NUMBER = 0;

    @Mapping(source = "customColumnStructure.isOrganisationData", target = "indivLevel", qualifiedByName = "organisationDataToIndivLevel")
    @Mapping(source = "customColumnStructure.industryNo", target = "industryNo", qualifiedByName = "mapDefaultNumber")
    @Mapping(source = "customColumnStructure.sectionNo", target = "sectionNo", qualifiedByName = "mapDefaultNumber")
    @Mapping(source = "customColumnStructure.nationalRight", target = "nationalRight")
    @Mapping(source = "mdc.consultant", target = "consultant")
    @Mapping(source = "mdc.client", target = "client")
    @Mapping(source = "mdc.yearBegin", target = "yearBegin")
    @Mapping(source = "mdc.yearEnd", target = "yearEnd")
    CustomColumnStructureContent mapAcdsToDb(CustomColumnStructure customColumnStructure,
                                             MasterdataContext mdc);

    @IterableMapping(nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    default List<CustomColumnStructureContent> mapAcdsToDbList(List<CustomColumnStructure> customColumnStructureList, MasterdataContext mdc) {
        return Optional.ofNullable(customColumnStructureList).orElse(new ArrayList<>()).stream().map(ccs -> {

            return mapAcdsToDb(ccs, mdc);
        }).toList();
    }

    @Named("organisationDataToIndivLevel")
    static Integer organisationDataToIndivLevel(Boolean value) {
        return Util.organisationDataToIndivLevel(value);
    }

    @Named("mapDefaultNumber")
    static Integer mapDefaultNumber(Integer number) {
        return Util.nullIfDefault(number, DEFAULT_NUMBER);
    }
}
