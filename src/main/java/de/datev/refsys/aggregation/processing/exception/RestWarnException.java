package de.datev.refsys.aggregation.processing.exception;

import lombok.Getter;

/**
 * Exception used to return a status and type to the RestExceptionHandler and log with WARN level
 */
@Getter
public class RestWarnException extends AggregationProcessingBaseException {
    private final String type;

    public RestWarnException(String message, Integer httpStatusCode, String type) {
        super(message, httpStatusCode);
        this.type = type;
    }
}
