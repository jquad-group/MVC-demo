package de.datev.refsys.aggregation.processing.model;

import de.datev.refsys.aggregation.document.model.AccountValue;
import de.datev.refsys.aggregation.document.model.AdditionalParameters;

public record MovementDataDayUpsert(Integer accountingReasonId, Integer accountNumber, String dayKey, AdditionalParameters additionalParameters,
                                    AccountValue accountValue) {
}
