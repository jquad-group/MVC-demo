package de.datev.refsys.aggregation.processing.mapper;

import de.datev.refsys.aggregation.document.model.CustomColumnStructureInfo;
import de.datev.refsys.aggregation.processing.util.Util;
import de.datev.refsys.generated.acds.api.model.CustomColumnStructure;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.NullValueMappingStrategy;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface CustomColumnStructureInfoMapper {
    int DEFAULT_NUMBER = 0;
    boolean DEFAULT_READ_ONLY = false;

    @Mapping(source = "customColumnStructure.isOrganisationData", target = "indivLevel", qualifiedByName = "organisationDataToIndivLevel")
    @Mapping(source = "customColumnStructure.caption", target = "displayText")
    @Mapping(source = "customColumnStructure.readOnly", target = "readOnly", qualifiedByName = "mapDefaultBoolean")
    @Mapping(source = "customColumnStructure.industryNo", target = "industryNo", qualifiedByName = "mapDefaultNumber")
    @Mapping(source = "customColumnStructure.sectionNo", target = "sectionNo", qualifiedByName = "mapDefaultNumber")
    CustomColumnStructureInfo mapAcdsToDb(CustomColumnStructure customColumnStructure);

    @IterableMapping(nullValueMappingStrategy = NullValueMappingStrategy.RETURN_NULL)
    List<CustomColumnStructureInfo> mapAcdsToDb(List<CustomColumnStructure> customColumnStructureList);

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
