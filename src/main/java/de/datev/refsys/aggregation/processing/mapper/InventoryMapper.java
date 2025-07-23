package de.datev.refsys.aggregation.processing.mapper;

import de.datev.refsys.generated.acds.api.model.MasterdataInventory;
import de.datev.refsys.aggregation.document.model.Inventory;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface InventoryMapper {

    @Mapping(source = "masterdataInventory.inventoryNumber", target = "inventoryNumber")
    @Mapping(source = "masterdataInventory.accountingReason", target = "anlagAccountingReason")
    @Mapping(source = "masterdataInventory.accountNumber", target = "accountNumber")
    @Mapping(source = "masterdataInventory.description", target = "inventoryName")
    @Mapping(source = "masterdataInventory.separationMark", target = "lineBreakDelimiter")
    @Mapping(source = "masterdataInventory.ustObject", target = "assetCategory")
    Inventory masterdataInventoryToInventory(MasterdataInventory masterdataInventory);
}
