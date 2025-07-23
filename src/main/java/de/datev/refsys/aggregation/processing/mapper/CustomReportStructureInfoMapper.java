package de.datev.refsys.aggregation.processing.mapper;

import de.datev.refsys.aggregation.document.model.CustomReportStructureInfo;
import de.datev.refsys.aggregation.processing.util.Util;
import de.datev.refsys.generated.acds.api.model.CustomReportStructure;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.NullValueMappingStrategy;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface CustomReportStructureInfoMapper {
    int DEFAULT_NUMBER = 0;
    boolean DEFAULT_READ_ONLY = false;

    @Mapping(source = "customReportStructure.isOrganisationData", target = "indivLevel", qualifiedByName = "organisationDataToIndivLevel")
    @Mapping(source = "customReportStructure.caption", target = "displayText")
    @Mapping(source = "customReportStructure.readOnly", target = "readOnly", qualifiedByName = "mapDefaultBoolean")
    @Mapping(source = "customReportStructure.industryNo", target = "industryNo", qualifiedByName = "mapDefaultNumber")
    @Mapping(source = "customReportStructure.sectionNo", target = "sectionNo", qualifiedByName = "mapDefaultNumber")
    CustomReportStructureInfo mapAcdsToDb(CustomReportStructure customReportStructure);

    @IterableMapping(nullValueMappingStrategy = NullValueMappingStrategy.RETURN_NULL)
    List<CustomReportStructureInfo> mapAcdsToDb(List<CustomReportStructure> customReportStructureList);

    @Named("organisationDataToIndivLevel")
    static Integer organisationDataToIndivLevel(Boolean value) {
        return Util.organisationDataToIndivLevel(value);
    }

    @Named("mapDefaultBoolean")
    static Boolean mapDefaultBoolean(Boolean value) {
        return Util.nullIfDefault(value, DEFAULT_READ_ONLY);
    }

    @Named("mapDefaultNumber")
    static Integer mapDefaultNumber(Integer number) {
        return Util.nullIfDefault(number, DEFAULT_NUMBER);
    }
}
