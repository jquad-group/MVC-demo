package de.datev.refsys.aggregation.processing.service;

import de.datev.refsys.aggregation.document.model.CustomColumnStructureContent;
import de.datev.refsys.aggregation.document.model.CustomColumnStructureInfo;
import de.datev.refsys.aggregation.document.model.CustomReportStructureContent;
import de.datev.refsys.aggregation.document.model.CustomReportStructureInfo;
import de.datev.refsys.aggregation.processing.model.CustomStructures;
import de.datev.refsys.generated.acds.api.model.MasterdataContext;
import reactor.core.publisher.Mono;

import java.util.List;

public interface CustomStructuresService {

    Mono<CustomStructures> getCustomStructures(MasterdataContext masterdataContext);

    Mono<Void> insertCustomColumnStructureContents(List<CustomColumnStructureContent> customColumnStructureContents);

    Mono<Void> insertCustomReportStructureContents(List<CustomReportStructureContent> customReportStructureContents);
}
