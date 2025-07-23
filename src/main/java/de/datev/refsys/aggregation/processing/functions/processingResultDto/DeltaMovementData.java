package de.datev.refsys.aggregation.processing.functions.processingResultDto;

import de.datev.refsys.generated.acds.api.model.AccountSumDay;

import java.util.List;

public record DeltaMovementData(
        List<AccountSumDay> existingValues,
        List<AccountSumDay> deltaValues) {
}
