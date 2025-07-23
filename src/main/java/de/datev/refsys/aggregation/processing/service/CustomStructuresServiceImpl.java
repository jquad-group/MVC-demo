package de.datev.refsys.aggregation.processing.service;

import de.datev.refsys.aggregation.document.model.CustomColumnStructureContent;
import de.datev.refsys.aggregation.document.model.CustomColumnStructureInfo;
import de.datev.refsys.aggregation.document.model.CustomReportStructureContent;
import de.datev.refsys.aggregation.document.model.CustomReportStructureInfo;
import de.datev.refsys.aggregation.processing.client.CustomStructuresClient;
import de.datev.refsys.aggregation.processing.constant.ProcessingErrorMessageConstants;
import de.datev.refsys.aggregation.processing.exception.AggregationProcessingBusinessException;
import de.datev.refsys.aggregation.processing.mapper.CustomColumnStructureContentMapper;
import de.datev.refsys.aggregation.processing.mapper.CustomColumnStructureInfoMapper;
import de.datev.refsys.aggregation.processing.mapper.CustomReportStructureContentMapper;
import de.datev.refsys.aggregation.processing.mapper.CustomReportStructureInfoMapper;
import de.datev.refsys.aggregation.processing.model.CustomStructures;
import de.datev.refsys.aggregation.processing.repository.CustomColumnStructureContentRepository;
import de.datev.refsys.aggregation.processing.repository.CustomReportStructureContentRepository;
import de.datev.refsys.generated.acds.api.model.CustomColumnStructure;
import de.datev.refsys.generated.acds.api.model.CustomReportStructure;
import de.datev.refsys.generated.acds.api.model.MasterdataContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomStructuresServiceImpl implements CustomStructuresService {
    private final CustomStructuresClient customStructuresClient;
    private final CustomReportStructureContentRepository customReportStructureContentRepository;
    private final CustomColumnStructureContentRepository customColumnStructureContentRepository;
    private final CustomReportStructureContentMapper customReportStructureContentMapper;
    private final CustomColumnStructureContentMapper customColumnStructureContentMapper;
    private final CustomReportStructureInfoMapper customReportStructureInfoMapper;
    private final CustomColumnStructureInfoMapper customColumnStructureInfoMapper;

    @Override
    public Mono<CustomStructures> getCustomStructures(MasterdataContext masterdataContext) {
        return Mono.zip(customStructuresClient.getCustomReportStructuresList(masterdataContext),
                        customStructuresClient.getCustomColumnStructuresList(masterdataContext))
                   .flatMap(ccs -> validateAndMapCustomStructures(masterdataContext, ccs.getT1(), ccs.getT2()));
    }

    @Override
    public Mono<Void> insertCustomColumnStructureContents(List<CustomColumnStructureContent> customColumnStructureContents) {
        return customColumnStructureContents.isEmpty() ?
                Mono.empty() :
                customColumnStructureContentRepository.bulkInsert(customColumnStructureContents).then();
    }

    @Override
    public Mono<Void> insertCustomReportStructureContents(List<CustomReportStructureContent> customReportStructureContents) {
        return customReportStructureContents.isEmpty() ?
                Mono.empty() :
                customReportStructureContentRepository.bulkInsert(customReportStructureContents).then();
    }

    private Mono<CustomStructures> validateAndMapCustomStructures(MasterdataContext masterdataContext,
                                                                  List<CustomReportStructure> customReportStructures,
                                                                  List<CustomColumnStructure> customColumnStructures) {
        // Verwende jetzt die neuen Validierungs-/Enhance-Methoden
        List<String> customColumnStructureErrors = validateAndEnhanceCustomColumnStructures(customColumnStructures);
        if (!customColumnStructureErrors.isEmpty()) {
            return Mono.error(new AggregationProcessingBusinessException(
                    String.format(ProcessingErrorMessageConstants.COLUMN_STRUCTURE_REQUIRED_FIELDS_ARE_NULL_ERROR,
                                  String.join(", ", customColumnStructureErrors)), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
        List<String> customReportStructureErrors = validateAndEnhanceCustomReportStructures(customReportStructures);
        if (!customReportStructureErrors.isEmpty()) {
            return Mono.error(new AggregationProcessingBusinessException(
                    String.format(ProcessingErrorMessageConstants.REPORT_STRUCTURE_REQUIRED_FIELDS_ARE_NULL_ERROR,
                                  String.join(", ", customReportStructureErrors)), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
        List<CustomColumnStructureInfo> customColumnStructureInfos = customColumnStructureInfoMapper.mapAcdsToDb(customColumnStructures);
        List<CustomReportStructureInfo> customReportStructureInfos = customReportStructureInfoMapper.mapAcdsToDb(customReportStructures);
        List<CustomColumnStructureContent> customColumnStructureContents =
                customColumnStructureContentMapper.mapAcdsToDbList(customColumnStructures, masterdataContext);
        List<CustomReportStructureContent> customReportStructureContents =
                customReportStructureContentMapper.mapAcdsToDbList(customReportStructures, masterdataContext);
        return Mono.just(new CustomStructures(customColumnStructureInfos, customReportStructureInfos, customColumnStructureContents,
                                              customReportStructureContents));
    }

    /**
     * Validiert die Inhalte der CustomColumnStructures, prüft zusätzlich, ob copyFromComprehensiveConsultant gesetzt wurde.
     * Falls copyFromComprehensiveConsultant einen Wert ungleich null und 0 enthält, wird isOrganisationData auf true gesetzt.
     *
     * @param customColumnStructures Liste der CustomColumnStructures
     * @return Liste der Fehlerfelder; ist die Liste leer, ist die Validierung erfolgreich.
     */
    static List<String> validateAndEnhanceCustomColumnStructures(List<CustomColumnStructure> customColumnStructures) {
        List<String> errors = new ArrayList<>();
        for (CustomColumnStructure ccs : customColumnStructures) {
            if (ccs.getColumnStructureId() == null) {
                errors.add("columnStructureId");
            }
            // Falls copyFromComprehensiveConsultant einen sinnvollen Wert enthält,
            // setze das Flag isOrganisationData auf true.
            if (ccs.getCopyFromComprehensiveConsultant() != null && ccs.getCopyFromComprehensiveConsultant() != 0) {
                ccs.setIsOrganisationData(true);
            }
            if (commonFieldsAreInvalid(errors,
                                       ccs.getIndivNo(),
                                       ccs.getIndustryNo(),
                                       ccs.getSectionNo(),
                                       ccs.getNationalRight(),
                                       ccs.getIsOrganisationData())) {
                break;
            }
        }
        return errors;
    }

    /**
     * Validiert die Inhalte der CustomReportStructures, prüft zusätzlich, ob copyFromComprehensiveConsultant gesetzt wurde.
     * Falls copyFromComprehensiveConsultant einen Wert ungleich null und 0 enthält, wird isOrganisationData auf true gesetzt.
     *
     * @param customReportStructures Liste der CustomReportStructures
     * @return Liste der Fehlerfelder; ist die Liste leer, ist die Validierung erfolgreich.
     */
    static List<String> validateAndEnhanceCustomReportStructures(List<CustomReportStructure> customReportStructures) {
        List<String> errors = new ArrayList<>();
        for (CustomReportStructure crs : customReportStructures) {
            if (crs.getReportStructureId() == null) {
                errors.add("reportStructureId");
            }
            // Falls copyFromComprehensiveConsultant einen sinnvollen Wert enthält,
            // setze das Flag isOrganisationData auf true.
            if (crs.getCopyFromComprehensiveConsultant() != null && crs.getCopyFromComprehensiveConsultant() != 0) {
                crs.setIsOrganisationData(true);
            }
            if (commonFieldsAreInvalid(errors,
                                       crs.getIndivNo(),
                                       crs.getIndustryNo(),
                                       crs.getSectionNo(),
                                       crs.getNationalRight(),
                                       crs.getIsOrganisationData())) {
                break;
            }
        }
        return errors;
    }

    private static boolean commonFieldsAreInvalid(List<String> errors, Integer indivNo, Integer industryNo, Integer sectionNo, String nationalRight,
                                                  Boolean isOrganisationData) {
        if (indivNo == null) {
            errors.add("indivNo");
        }
        if (industryNo == null) {
            errors.add("industryNo");
        }
        if (sectionNo == null) {
            errors.add("sectionNo");
        }
        if (nationalRight == null) {
            errors.add("nationalRight");
        }
        if (isOrganisationData == null) {
            errors.add("indivLevel");
        }
        return !errors.isEmpty();
    }
}
