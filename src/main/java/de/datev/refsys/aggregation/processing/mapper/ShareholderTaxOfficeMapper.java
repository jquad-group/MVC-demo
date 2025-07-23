package de.datev.refsys.aggregation.processing.mapper;

import de.datev.refsys.aggregation.document.model.ShareholderTaxOffice;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.NullValueMappingStrategy;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, injectionStrategy = InjectionStrategy.CONSTRUCTOR)

public interface ShareholderTaxOfficeMapper {
    ShareholderTaxOffice mapAcdsToDb(de.datev.refsys.generated.acds.api.model.ShareholderTaxOffice shareholderData);

    @IterableMapping(nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    List<ShareholderTaxOffice> mapAcdsToDb(List<de.datev.refsys.generated.acds.api.model.ShareholderTaxOffice> shareholderData);
}
