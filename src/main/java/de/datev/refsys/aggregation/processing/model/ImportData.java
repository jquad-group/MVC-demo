package de.datev.refsys.aggregation.processing.model;

import de.datev.refsys.aggregation.document.model.StateDoc;
import de.datev.refsys.generated.acds.api.model.MasterdataContext;

public record ImportData(MasterdataContext masterdataContext, StateDoc stateDoc) {
}

