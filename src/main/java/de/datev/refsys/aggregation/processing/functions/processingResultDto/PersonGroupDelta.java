package de.datev.refsys.aggregation.processing.functions.processingResultDto;

import de.datev.refsys.generated.acds.api.model.MasterdataContext;
import de.datev.refsys.aggregation.document.model.MovementDataPersonGroupDay;
import de.datev.refsys.aggregation.document.model.MovementDataPersonGroupMonth;
import de.datev.refsys.aggregation.processing.model.AccountDbKeyFields;

import java.util.Map;
import java.util.Set;

public record PersonGroupDelta(
        Map<AccountDbKeyFields, MovementDataPersonGroupDay> personGroupDayMap,
        Map<AccountDbKeyFields, MovementDataPersonGroupMonth> personGroupMonthMap,
        Set<Integer> additionalIndividualPersonAccounts, MasterdataContext masterdataContext) {

}
