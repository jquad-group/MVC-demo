package de.datev.refsys.aggregation.processing.mapper;

import de.datev.refsys.aggregation.document.model.InventoryValue;
import de.datev.refsys.aggregation.processing.model.ExtendedMovementdataInventory;
import de.datev.refsys.aggregation.processing.util.Util;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface InventoryValueMapper {
    double DEFAULT_CENT_VALUE = 0.00;

    @Mapping(source = "extendedInventory.inventoryNumber", target = "inventoryNumber")
    @Mapping(source = "extendedInventory.movementdataInventory.standWjBegAhk", target = "standWjBegAhk", qualifiedByName =
            "doubleAmountToLongAmountInCent")
    @Mapping(source = "extendedInventory.movementdataInventory.standWjEndAhk", target = "standWjEndAhk", qualifiedByName =
            "doubleAmountToLongAmountInCent")
    @Mapping(source = "extendedInventory.movementdataInventory.standWjBegBw", target = "standWjBegBw", qualifiedByName =
            "doubleAmountToLongAmountInCent")
    @Mapping(source = "extendedInventory.movementdataInventory.standWjEndBw", target = "standWjEndBw", qualifiedByName =
            "doubleAmountToLongAmountInCent")
    @Mapping(source = "extendedInventory.movementdataInventory.standWjBegAbschr", target = "standWjBegAbschr", qualifiedByName =
            "doubleAmountToLongAmountInCent")
    @Mapping(source = "extendedInventory.movementdataInventory.standWjEndAbschr", target = "standWjEndAbschr", qualifiedByName =
            "doubleAmountToLongAmountInCent")
    @Mapping(source = "extendedInventory.movementdataInventory.zugangAhk", target = "zugangAhk", qualifiedByName = "doubleAmountToLongAmountInCent")
    @Mapping(source = "extendedInventory.movementdataInventory.zugangBw", target = "zugangBw", qualifiedByName = "doubleAmountToLongAmountInCent")
    @Mapping(source = "extendedInventory.movementdataInventory.zugangAbschrSumme", target = "zugangAbschrSumme", qualifiedByName =
            "doubleAmountToLongAmountInCent")
    @Mapping(source = "extendedInventory.movementdataInventory.zugangAbschrSoafa", target = "zugangAbschrSoafa", qualifiedByName =
            "doubleAmountToLongAmountInCent")
    @Mapping(source = "extendedInventory.movementdataInventory.zugangAbschrTeilwert", target = "zugangAbschrTeilwert", qualifiedByName =
            "doubleAmountToLongAmountInCent")
    @Mapping(source = "extendedInventory.movementdataInventory.zugangAbschrAhkabzug", target = "zugangAbschrAhkabzug", qualifiedByName =
            "doubleAmountToLongAmountInCent")
    @Mapping(source = "extendedInventory.movementdataInventory.abgangAhk", target = "abgangAhk", qualifiedByName = "doubleAmountToLongAmountInCent")
    @Mapping(source = "extendedInventory.movementdataInventory.abgangBw", target = "abgangBw", qualifiedByName = "doubleAmountToLongAmountInCent")
    @Mapping(source = "extendedInventory.movementdataInventory.abgangAbschr", target = "abgangAbschr", qualifiedByName =
            "doubleAmountToLongAmountInCent")
    @Mapping(source = "extendedInventory.movementdataInventory.umbuchungMinusAhk", target = "umbuchungMinusAhk", qualifiedByName =
            "doubleAmountToLongAmountInCent")
    @Mapping(source = "extendedInventory.movementdataInventory.umbuchungMinusBw", target = "umbuchungMinusBw", qualifiedByName =
            "doubleAmountToLongAmountInCent")
    @Mapping(source = "extendedInventory.movementdataInventory.umbuchungMinusAbschr", target = "umbuchungMinusAbschr", qualifiedByName =
            "doubleAmountToLongAmountInCent")
    @Mapping(source = "extendedInventory.movementdataInventory.umbuchungPlusAhk", target = "umbuchungPlusAhk", qualifiedByName =
            "doubleAmountToLongAmountInCent")
    @Mapping(source = "extendedInventory.movementdataInventory.umbuchungPlusBw", target = "umbuchungPlusBw", qualifiedByName =
            "doubleAmountToLongAmountInCent")
    @Mapping(source = "extendedInventory.movementdataInventory.umbuchungPlusAbschr", target = "umbuchungPlusAbschr", qualifiedByName =
            "doubleAmountToLongAmountInCent")
    @Mapping(source = "extendedInventory.movementdataInventory.abschreibunBwSumme", target = "abschreibunBwSumme", qualifiedByName =
            "doubleAmountToLongAmountInCent")
    @Mapping(source = "extendedInventory.movementdataInventory.zuschreibungNaAbschr", target = "zuschreibungNaAbschr", qualifiedByName =
            "doubleAmountToLongAmountInCent")
    @Mapping(source = "extendedInventory.movementdataInventory.zuschrAhkSumme", target = "zuschrAhkSumme", qualifiedByName =
            "doubleAmountToLongAmountInCent")
    @Mapping(source = "extendedInventory.movementdataInventory.fkZinsen", target = "fkZinsen", qualifiedByName = "doubleAmountToLongAmountInCent")
    @Mapping(source = "extendedInventory.movementdataInventory.kumAbschr", target = "kumAbschr", qualifiedByName = "doubleAmountToLongAmountInCent")
    InventoryValue mapApiToDbModel(ExtendedMovementdataInventory extendedInventory);

    @Named("doubleAmountToLongAmountInCent")
    static Long doubleToLongAmountInCent(Double value) {
        if (value == null || Double.compare(value, DEFAULT_CENT_VALUE) == 0) {
            return null;
        }
        return Math.round(value * Util.CENT_IN_ONE_EURO);
    }
}
