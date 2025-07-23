package de.datev.refsys.aggregation.processing.model;

import de.datev.refsys.generated.acds.api.model.MovementdataInventory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ExtendedMovementdataInventory {
    private String inventoryNumber;
    private MovementdataInventory movementdataInventory;
}
