package de.datev.refsys.aggregation.processing.util;

import de.datev.refsys.aggregation.document.model.ProblemInfo;
import de.datev.refsys.aggregation.document.model.ProblemInfo;
import de.datev.refsys.aggregation.document.model.ProcessingError;
import de.datev.refsys.aggregation.processing.constant.ProcessingErrorMessageConstants;
import de.datev.refsys.aggregation.processing.exception.AggregationProcessingBusinessException;
import de.datev.refsys.aggregation.processing.exception.HttpCallBusinessException;
import de.datev.refsys.aggregation.processing.exception.HttpCallException;
import de.datev.refsys.aggregation.processing.exception.HttpCallNoContentException;
import de.datev.refsys.aggregation.processing.exception.HttpCallTechnicalException;
import de.datev.refsys.aggregation.processing.mapper.ProblemInfoMapper;
import de.datev.refsys.aggregation.processing.mapper.ProblemInfoMapperImpl;
import de.datev.refsys.aggregation.processing.model.enums.SourceEndpoint;
import de.datev.refsys.aggregation.processing.model.enums.SourceError;
import de.datev.refsys.generated.acds.api.model.ProblemDetails;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;

@Slf4j
@UtilityClass
public class ExceptionUtil {
    private static final ProblemInfoMapper PROBLEM_INFO_MAPPER = new ProblemInfoMapperImpl();
    private static final String MISSING_PROBLEM_DETAILS_TITLE = "ACDS response doesn't contain a ProblemDetails response";
    private static final String REQUEST_PROBLEM_TITLE = "WebClientRequestException";
    public static final String DEFAULT_ERROR_SOURCE = SourceError.PROCESSING_SERVICE.getValue();
    public static final Integer DEFAULT_ERROR_SOURCE_STATUS_CODE = HttpStatus.INTERNAL_SERVER_ERROR.value();
    public static final String DEFAULT_PROBLEM_INFO_TITLE = "Server Error Exception";
    public static final List<Integer> BUSINESS_ERROR_HTTP_CODE = List.of(555);
    public static final List<String> JAVA_FIELDS_WHITELIST =
            List.of("consultant", "client", "fiscalYear", "yearBegin", "yearEnd", "baseVersion", "deltaVersion", "used");

    public static Throwable handleWebClientException(SourceEndpoint sourceEndpoint, Throwable cause) {
        if (cause instanceof WebClientResponseException webClientResponseException) {
            Integer httpStatusCode = webClientResponseException.getStatusCode().value();
            ProblemInfo problemInfo = getProblemInfo(webClientResponseException, sourceEndpoint);
            if (isBusinessError(httpStatusCode)) {
                return new HttpCallBusinessException(String.format(ProcessingErrorMessageConstants.ACDS_BUSINESS_ERROR, sourceEndpoint.getValue()),
                                                     SourceError.ACDS, sourceEndpoint, httpStatusCode, problemInfo, webClientResponseException);
            }
            return new HttpCallTechnicalException(String.format(ProcessingErrorMessageConstants.ACDS_TECHNICAL_ERROR, sourceEndpoint.getValue()),
                                                  SourceError.ACDS, sourceEndpoint, httpStatusCode, problemInfo, webClientResponseException);
        }
        if (cause instanceof WebClientRequestException webClientRequestException) {
            ProblemInfo problemInfo = getProblemInfo(webClientRequestException, sourceEndpoint);
            return new HttpCallTechnicalException(String.format(ProcessingErrorMessageConstants.ACDS_TECHNICAL_ERROR, sourceEndpoint.getValue()),
                                                  SourceError.ACDS, sourceEndpoint, HttpStatus.REQUEST_TIMEOUT.value(), problemInfo, cause);
        }
        if (cause instanceof HttpCallNoContentException) {
            return cause;
        }
        return new AggregationProcessingBusinessException(
                String.format(ProcessingErrorMessageConstants.UNEXPECTED_CLIENT_ERROR, sourceEndpoint.getValue()),
                HttpStatus.INTERNAL_SERVER_ERROR.value(), cause);
    }

    public static ProcessingError getProcessingError(String correlationId, HttpCallException e) {
        return ProcessingError.builder()
                              .source(e.getSource().getValue())
                              .sourceEndpoint(e.getSourceEndpoint().getValue())
                              .sourceStatusCode(e.getHttpStatusCode())
                              .correlationId(correlationId)
                              .problemInfo(e.getProblemInfo())
                              .build();
    }

    public static ProcessingError getDefaultProcessingError(String correlationId, ProblemInfo problemInfo) {
        return ProcessingError.builder()
                              .source(DEFAULT_ERROR_SOURCE)
                              .sourceStatusCode(DEFAULT_ERROR_SOURCE_STATUS_CODE)
                              .correlationId(correlationId)
                              .problemInfo(problemInfo)
                              .build();
    }

    private static ProblemInfo getProblemInfo(WebClientRequestException webClientRequestException, SourceEndpoint sourceEndpoint) {
        return ProblemInfo.builder()
                          .type(SourceError.ACDS.getValue())
                          .title(REQUEST_PROBLEM_TITLE)
                          .detail(Vk3MaskingUtil.maskMessage(webClientRequestException.getMessage(), JAVA_FIELDS_WHITELIST))
                          .instance(sourceEndpoint.getValue())
                          .build();
    }

    private static ProblemInfo getProblemInfo(WebClientResponseException webClientResponseException, SourceEndpoint sourceEndpoint) {
        ProblemDetails problemDetails = getProblemDetails(webClientResponseException);
        ProblemInfo problemInfo;
        if (problemDetails == null) {
            problemInfo = ProblemInfo.builder()
                                     .type(SourceError.ACDS.getValue())
                                     .title(MISSING_PROBLEM_DETAILS_TITLE)
                                     .detail("ACDS Response in text: " + webClientResponseException.getResponseBodyAsString())
                                     .instance(sourceEndpoint.getValue())
                                     .build();
        } else {
            problemInfo = PROBLEM_INFO_MAPPER.problemDetailsToProblemInfo(problemDetails);
            problemInfo.setDetail(Vk3MaskingUtil.maskMessage(problemDetails.getDetail(), JAVA_FIELDS_WHITELIST));
        }
        return problemInfo;
    }

    private static ProblemDetails getProblemDetails(WebClientResponseException webClientResponseException) {
        try {
            return webClientResponseException.getResponseBodyAs(ProblemDetails.class);
        } catch (IllegalStateException | DecodingException e) {
            log.warn("An error occurred while decoding the body of the webClientResponseException in ProblemDetails.", e);
            return null;
        }
    }

    private static boolean isBusinessError(Integer httpStatusCode) {
        return BUSINESS_ERROR_HTTP_CODE.contains(httpStatusCode);
    }
}
