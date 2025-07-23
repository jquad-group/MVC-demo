package de.datev.refsys.aggregation.processing.model;

import de.datev.refsys.aggregation.document.model.CustomColumnStructureContent;
import de.datev.refsys.aggregation.document.model.CustomColumnStructureInfo;
import de.datev.refsys.aggregation.document.model.CustomReportStructureContent;
import de.datev.refsys.aggregation.document.model.CustomReportStructureInfo;

import java.util.List;

public record CustomStructures(List<CustomColumnStructureInfo> customColumnStructureInfos, List<CustomReportStructureInfo> customReportStructureInfos,
                               List<CustomColumnStructureContent> customColumnStructureContents,
                               List<CustomReportStructureContent> customReportStructureContents) {
}
