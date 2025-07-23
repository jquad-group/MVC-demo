package de.datev.refsys.aggregation.processing.functions.processingResultDto;

import de.datev.refsys.aggregation.processing.model.MovementDataAccountValues;

public record DeltaEventDifferenceResult(MovementDataAccountValues resultMovementDataAccountValues,
                                         PersonGroupDelta personGroupDelta) {
}
