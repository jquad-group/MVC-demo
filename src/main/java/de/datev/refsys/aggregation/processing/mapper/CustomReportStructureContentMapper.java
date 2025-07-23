package de.datev.refsys.aggregation.processing.mapper;

import de.datev.refsys.aggregation.document.model.CustomReportStructureContent;
import de.datev.refsys.aggregation.processing.util.Util;
import de.datev.refsys.generated.acds.api.model.CustomReportStructure;
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
public interface CustomReportStructureContentMapper {
    int DEFAULT_NUMBER = 0;

    @Mapping(source = "customReportStructure.isOrganisationData", target = "indivLevel", qualifiedByName = "organisationDataToIndivLevel")
    @Mapping(source = "customReportStructure.industryNo", target = "industryNo", qualifiedByName = "mapDefaultNumber")
    @Mapping(source = "customReportStructure.sectionNo", target = "sectionNo", qualifiedByName = "mapDefaultNumber")
    @Mapping(source = "customReportStructure.nationalRight", target = "nationalRight")
    @Mapping(source = "mdc.consultant", target = "consultant")
    @Mapping(source = "mdc.client", target = "client")
    @Mapping(source = "mdc.yearBegin", target = "yearBegin")
    @Mapping(source = "mdc.yearEnd", target = "yearEnd")
    CustomReportStructureContent mapAcdsToDb(CustomReportStructure customReportStructure,
                                             MasterdataContext mdc);

    @IterableMapping(nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    default List<CustomReportStructureContent> mapAcdsToDbList(List<CustomReportStructure> customReportStructureList, MasterdataContext mdc) {
        return Optional.ofNullable(customReportStructureList).orElse(new ArrayList<>()).stream().map(crs -> mapAcdsToDb(crs, mdc)).toList();
    }

    @Named("organisationDataToIndivLevel")
    static Integer organisationDataToIndivLevel(Boolean value) {
        return Util.organisationDataToIndivLevel(value);
    }

    @Named("mapDefaultNumber")
    static Integer mapDefaultNo(Integer number) {
        return Util.nullIfDefault(number, DEFAULT_NUMBER);
    }
}
