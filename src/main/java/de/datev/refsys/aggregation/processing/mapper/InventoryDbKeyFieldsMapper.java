package de.datev.refsys.aggregation.processing.mapper;

import de.datev.refsys.generated.acds.api.model.MasterdataContext;
import de.datev.refsys.aggregation.processing.model.ExtendedMovementdataInventory;
import de.datev.refsys.aggregation.processing.model.InventoryDbKeyFields;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface InventoryDbKeyFieldsMapper {

    @Mapping(source = "mdc.consultant", target = "consultant")
    @Mapping(source = "mdc.client", target = "client")
    @Mapping(source = "mdc.yearBegin", target = "fiscalYear")
    @Mapping(source = "extendedInventory.movementdataInventory.kontonr", target = "accountNumber")
    @Mapping(source = "extendedInventory.movementdataInventory.accountingReason", target = "accountingReason")
    InventoryDbKeyFields mapToDbModel(MasterdataContext mdc, ExtendedMovementdataInventory extendedInventory);
}
