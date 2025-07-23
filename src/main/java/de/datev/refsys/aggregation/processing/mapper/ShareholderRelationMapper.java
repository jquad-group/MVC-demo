package de.datev.refsys.aggregation.processing.mapper;

import de.datev.refsys.aggregation.document.model.ShareholderRelation;
import de.datev.refsys.aggregation.processing.util.Util;
import org.bson.types.Decimal128;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.NullValueMappingStrategy;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface ShareholderRelationMapper {

    @Mapping(source = "shareholderData.personalShareAmount", target = "personalShareAmount", qualifiedByName = "doubleAmountToLongAmountInCent")
    @Mapping(source = "shareholderData.earningsSharePercent", target = "earningsSharePercent", qualifiedByName = "doubleToDecimal128")
    @Mapping(source = "shareholderData.personalSharePercent", target = "personalSharePercent", qualifiedByName = "doubleToDecimal128")
    ShareholderRelation mapAcdsToDb(de.datev.refsys.generated.acds.api.model.ShareholderRelation shareholderData);

    @IterableMapping(nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    List<ShareholderRelation> mapAcdsToDb(List<de.datev.refsys.generated.acds.api.model.ShareholderRelation> shareholderData);

    @Named("doubleAmountToLongAmountInCent")
    static Long doubleToLong(Double value) {
        return Util.amountToLongInCent(value);
    }

    @Named("doubleToDecimal128")
    static Decimal128 doubleToDecimal128(Double value) {
        return Util.doubleToDecimal128(value);
    }
}
