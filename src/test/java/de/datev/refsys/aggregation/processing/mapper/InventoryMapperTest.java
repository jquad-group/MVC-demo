package de.datev.refsys.aggregation.processing.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import de.datev.refsys.generated.acds.api.model.MasterdataInventory;
import de.datev.refsys.aggregation.document.model.Inventory;
import de.datev.refsys.aggregation.processing.util.TestDataLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryMapperTest {
    private final InventoryMapper inventoryMapper = new InventoryMapperImpl();

    @Test
    @DisplayName("Map null ACDS MasterDataInventory to null")
    void should_map_acds_masterdata_inventory_to_null_when_is_not_present() {
        Inventory inventory = inventoryMapper.masterdataInventoryToInventory(null);
        assertThat(inventory).isNull();
    }

    @Test
    @DisplayName("Map valid ACDS MasterDataInventory to Inventory model when data is present")
    void should_map_acds_masterdata_inventory_to_db_inventory_when_data_is_present() {
        List<MasterdataInventory> masterdataInventories =
                TestDataLoader.loadList("json/acds-responses/mapper/masterDataInventories.json", new TypeReference<>() {
                });
        List<Inventory> mappingResult = masterdataInventories.stream().map(inventoryMapper::masterdataInventoryToInventory).toList();

        List<Inventory> expectedInventories = TestDataLoader.loadMongoDBList("json/collections/expected/mapper/masterDataInventories.json", Inventory.class);
        assertThat(mappingResult).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expectedInventories);

    }

}