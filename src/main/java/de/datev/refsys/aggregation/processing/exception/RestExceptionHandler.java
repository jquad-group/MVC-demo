package de.datev.refsys.aggregation.processing.exception;

import de.datev.refsys.aggregation.document.model.ProblemInfo;
import de.datev.refsys.aggregation.processing.api.model.Problem;
import de.datev.refsys.aggregation.processing.constant.ProcessingServiceConstants;
import de.datev.refsys.aggregation.processing.util.ExceptionUtil;
import de.datev.refsys.aggregation.processing.util.LoggingUtil;
import de.datev.refsys.aggregation.processing.util.Vk3MaskingUtil;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.MissingRequestValueException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Class for handling exceptions and returning response
 * VERY IMPORTANT: exception message or any part of the exception need to be masked, since they can contain VK3 fields
 * Example: Vk3MaskingUtil.maskMessage(e.getMessage(), JAVA_WHITELIST)
 */
@Slf4j
@RestControllerAdvice
public class RestExceptionHandler {

    /**
     * The {@link ExceptionHandler} for {@link AggregationProcessingBusinessException}s
     *
     * @param e The {@link AggregationProcessingBusinessException} object
     * @return The {@link ResponseEntity} with the {@link Problem} message
     */
    @ExceptionHandler(AggregationProcessingBusinessException.class)
    public ResponseEntity<Problem> handleAggregationProcessingException(final AggregationProcessingBusinessException e) {
        log.error(LoggingUtil.MARKER, "A business exception occurred: status={}, ", e.getHttpStatusCode(), e);
        HttpStatus httpStatus = HttpStatus.valueOf(e.getHttpStatusCode());
        Problem problem = new Problem()
                .title(httpStatus.getReasonPhrase())
                .detail(Vk3MaskingUtil.maskMessage(e.getMessage(), ExceptionUtil.JAVA_FIELDS_WHITELIST))
                .status(e.getHttpStatusCode());
        return new ResponseEntity<>(problem, problemJsonHeader(), e.getHttpStatusCode());
    }

    /**
     * The {@link ExceptionHandler} for {@link InitialLoadFailedException}s
     *
     * @param e The {@link InitialLoadFailedException} object
     * @return The {@link ResponseEntity} with the {@link Problem} message
     */
    @ExceptionHandler(InitialLoadFailedException.class)
    public ResponseEntity<Problem> handleImportInProgressException(final InitialLoadFailedException e) {
        log.warn("Initial import is in progress: status={}, ", e.getHttpStatusCode(), e);
        HttpStatus httpStatus = HttpStatus.valueOf(e.getHttpStatusCode());
        Problem problem = new Problem()
                .type(e.getType())
                .title(httpStatus.getReasonPhrase())
                .detail(Vk3MaskingUtil.maskMessage(e.getMessage(), ExceptionUtil.JAVA_FIELDS_WHITELIST))
                .status(e.getHttpStatusCode());
        return new ResponseEntity<>(problem, problemJsonHeader(), e.getHttpStatusCode());
    }

    /**
     * The {@link ExceptionHandler} for {@link RestWarnException}s
     *
     * @param e The {@link RestWarnException} object
     * @return The {@link ResponseEntity} with the {@link Problem} message
     */
    @ExceptionHandler(RestWarnException.class)
    public ResponseEntity<Problem> handleRestWarnException(final RestWarnException e) {
        log.warn("A request was interrupted: status={}, ", e.getHttpStatusCode(), e);
        HttpStatus httpStatus = HttpStatus.valueOf(e.getHttpStatusCode());
        Problem problem = new Problem()
                .type(e.getType())
                .title(httpStatus.getReasonPhrase())
                .detail(Vk3MaskingUtil.maskMessage(e.getMessage(), ExceptionUtil.JAVA_FIELDS_WHITELIST))
                .status(e.getHttpStatusCode());
        return new ResponseEntity<>(problem, problemJsonHeader(), e.getHttpStatusCode());
    }

    /**
     * The {@link ExceptionHandler} for {@link MissingRequestValueException}s
     *
     * @param e The {@link MissingRequestValueException} object
     * @return The {@link ResponseEntity} with the {@link Problem} message
     */
    @ExceptionHandler(MissingRequestValueException.class)
    public ResponseEntity<Object> handleMissingRequestValueException(final MissingRequestValueException e) {
        log.error("A MissingRequestValueException occurred: status={}, ", e.getStatusCode().value(), e);
        Problem problem = new Problem()
                .title(HttpStatus.valueOf(e.getStatusCode().value()).getReasonPhrase())
                .detail(e.getReason())
                .status(e.getStatusCode().value());
        return new ResponseEntity<>(problem, problemJsonHeader(), e.getStatusCode());
    }

    /**
     * The {@link ExceptionHandler} for {@link ResponseStatusException}s
     *
     * @param e The {@link ResponseStatusException} object
     * @return The {@link ResponseEntity} with the {@link Problem} message
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Object> handleResponseStatusException(final ResponseStatusException e) {
        String reason = e.getReason();

        log.warn("A ResponseStatusException occurred: status={}, reason={}", e.getStatusCode().value(), reason, e);

        Problem problem = new Problem()
                .title(HttpStatus.valueOf(e.getStatusCode().value()).getReasonPhrase())
                .detail(e.getReason())
                .status(e.getStatusCode().value());
        return new ResponseEntity<>(problem, problemJsonHeader(), e.getStatusCode());
    }

    /**
     * The {@link ExceptionHandler} for {@link ConstraintViolationException}s
     *
     * @param e The {@link ConstraintViolationException} object
     * @return The {@link ResponseEntity} with the {@link Problem} message
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Problem> handleConstraintViolationException(final ConstraintViolationException e) {
        log.error("A ConstraintViolationException occurred: status={}, ", HttpStatus.BAD_REQUEST.value(), e);
        Problem problem = new Problem()
                .title(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .detail(processConstraintViolations(e.getConstraintViolations()))
                .status(HttpStatus.BAD_REQUEST.value());
        return new ResponseEntity<>(problem, problemJsonHeader(), HttpStatus.BAD_REQUEST);
    }

    /**
     * The {@link ExceptionHandler} for {@link Exception}s
     *
     * @param e The {@link Exception} object
     * @return The {@link ResponseEntity} with the {@link Problem} message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Problem> handleUnexpectedException(final Exception e) {
        log.error(LoggingUtil.MARKER, "An unexpected exception occurred: ", e);
        Problem problem = new Problem()
                .title(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .detail(Vk3MaskingUtil.maskMessage(e.getMessage(), ExceptionUtil.JAVA_FIELDS_WHITELIST))
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value());
        return new ResponseEntity<>(problem, problemJsonHeader(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * The {@link ExceptionHandler} for {@link HttpCallBusinessException}s
     *
     * @param e The {@link HttpCallBusinessException} object
     * @return The {@link ResponseEntity} with the {@link Problem} message
     */
    @ExceptionHandler(HttpCallBusinessException.class)
    public ResponseEntity<Problem> handleHttpCallBusinessException(final HttpCallBusinessException e) {
        log.info("An HttpCallBusiness exception occurred: ", e);
        ProblemInfo problemInfo = e.getProblemInfo();
        Problem problem = new Problem()
                .type(problemInfo.getType())
                .title(problemInfo.getTitle())
                .detail(problemInfo.getDetail())
                .status(e.getHttpStatusCode())
                .instance(problemInfo.getInstance());
        return new ResponseEntity<>(problem, problemJsonHeader(), e.getHttpStatusCode());
    }

    /**
     * The {@link ExceptionHandler} for {@link HttpCallTechnicalException}s
     *
     * @param e The {@link HttpCallTechnicalException} object
     * @return The {@link ResponseEntity} with the {@link Problem} message
     */
    @ExceptionHandler(HttpCallTechnicalException.class)
    public ResponseEntity<Problem> handleHttpCallTechnicalException(final HttpCallTechnicalException e) {
        log.error("An HttpCallTechnical exception occurred: ", e);
        ProblemInfo problemInfo = e.getProblemInfo();
        Problem problem = new Problem()
                .type(problemInfo.getType())
                .title(problemInfo.getTitle())
                .detail(problemInfo.getDetail())
                .status(e.getHttpStatusCode())
                .instance(problemInfo.getInstance());
        return new ResponseEntity<>(problem, problemJsonHeader(), e.getHttpStatusCode());
    }

    /**
     * The {@link ExceptionHandler} for {@link HttpCallNoContentException}s
     *
     * @param e The {@link HttpCallTechnicalException} object
     * @return The {@link ResponseEntity}
     */
    @ExceptionHandler(HttpCallNoContentException.class)
    public ResponseEntity<Problem> handleHttpCallNoContentException(final HttpCallNoContentException e) {
        log.warn("An HttpCallNoContentException occurred: ", e);
        return ResponseEntity.noContent().build();
    }

    /**
     * The {@link ExceptionHandler} for {@link CallNotPermittedException}s
     *
     * @param e The {@link CallNotPermittedException} object
     * @return The {@link ResponseEntity} with the {@link Problem} message
     */
    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<Problem> handleCallNotPermittedException(final CallNotPermittedException e, final ServerWebExchange exchange) {
        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        log.error(LoggingUtil.MARKER, "A call not permitted exception occurred: status={}, ", httpStatus.value(),e);
        Problem problem = new Problem()
                .title(httpStatus.getReasonPhrase())
                .detail(Vk3MaskingUtil.maskMessage(e.getMessage(), ExceptionUtil.JAVA_FIELDS_WHITELIST))
                .status(httpStatus.value());
        return new ResponseEntity<>(problem, problemJsonHeader(), httpStatus.value());
    }


    /**
     * Creates a HttpHeaders instance with an application/problem+json Content-Type header
     *
     * @return a HttpHeaders instance
     */
    private static HttpHeaders problemJsonHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        return headers;
    }

    /**
     * Creates a problem detail message from {@link ConstraintViolation} list
     *
     * @return a problem detail message
     */
    private static String processConstraintViolations(Set<ConstraintViolation<?>> constraintViolations) {
        return constraintViolations.stream().map(constraintViolation -> "The " + getFieldName(constraintViolation.getPropertyPath())
                + " " + constraintViolation.getMessage()).collect(Collectors.joining(";"));
    }

    /**
     * Extracts a field name from {@link Path}
     *
     * @return a field name
     */
    private static String getFieldName(Path propertyPath) {
        String field = null;
        for (Path.Node node : propertyPath) {
            field = node.getName();
        }
        return field;
    }

}
