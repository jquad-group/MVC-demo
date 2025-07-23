package de.datev.refsys.aggregation.processing.mapper;

import de.datev.refsys.generated.acds.api.model.ProblemDetails;
import de.datev.refsys.aggregation.document.model.ProblemInfo;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ProblemInfoMapper {
    ProblemInfo problemDetailsToProblemInfo(ProblemDetails problemDetails);
}
