package de.datev.refsys.aggregation.processing.mapper;

import de.datev.refsys.generated.acds.api.model.ShareholderAddressee;
import de.datev.refsys.aggregation.document.model.ShareholderAddress;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.NullValueMappingStrategy;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, injectionStrategy = InjectionStrategy.CONSTRUCTOR)

public interface ShareholderAddressMapper {
    ShareholderAddress mapAcdsToDb(ShareholderAddressee shareholderAddressee);

    @IterableMapping(nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    List<ShareholderAddress> mapAcdsToDb(List<ShareholderAddressee> shareholderAddressee);
}
