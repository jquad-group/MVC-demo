package de.datev.refsys.aggregation.processing.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import de.datev.refsys.aggregation.document.model.InventoryValue;
import de.datev.refsys.aggregation.processing.model.ExtendedMovementdataInventory;
import de.datev.refsys.aggregation.processing.util.TestDataLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryValueMapperTest {
    private final InventoryValueMapper inventoryValueMapper = new InventoryValueMapperImpl();

    @Test
    @DisplayName("Map null ACDS MovementDataInventory to null")
    void should_map_acds_movementdata_inventory_to_null_when_is_not_present() {
        InventoryValue inventoryValue = inventoryValueMapper.mapApiToDbModel(null);
        assertThat(inventoryValue).isNull();
    }

    @Test
    @DisplayName("Map valid ACDS MovementDataInventory to InventoryValue model when data is present")
    void should_map_acds_movementdata_inventory_to_db_inventory_when_data_is_present() {
        List<ExtendedMovementdataInventory> movementdataInventories =
                TestDataLoader.loadList("json/acds-responses/mapper/extendedMovementdataInventoriesInput.json", new TypeReference<>() {
                });
        List<InventoryValue> mappingResult = movementdataInventories.stream().map(inventoryValueMapper::mapApiToDbModel).toList();

        List<InventoryValue> expectedInventoryValues =
                TestDataLoader.loadMongoDBList("json/collections/expected/mapper/movementDataInventoryValues.json", InventoryValue.class);
        assertThat(mappingResult).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expectedInventoryValues);

    }

}