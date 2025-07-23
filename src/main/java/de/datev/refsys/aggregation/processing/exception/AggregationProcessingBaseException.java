package de.datev.refsys.aggregation.processing.exception;

import lombok.Getter;

/**
 * This Exception is abstract for all customized exceptions.
 */
@Getter
public abstract class AggregationProcessingBaseException extends RuntimeException {
    private final Integer httpStatusCode;

    /**
     * AggregationProcessingServiceException constructor to call super class constructor with exception message
     *
     * @param message exception message
     */
    protected AggregationProcessingBaseException(String message, Integer httpStatusCode) {
        super(message);
        this.httpStatusCode = httpStatusCode;
    }

    /**
     * AggregationProcessingServiceException constructor to call super class constructor with message and root cause exception
     *
     * @param message exception message
     * @param cause   exception root cause
     */
    protected AggregationProcessingBaseException(String message, Integer httpStatusCode, Throwable cause) {
        super(message, cause);
        this.httpStatusCode = httpStatusCode;
    }

    /**
     * AggregationProcessingServiceException constructor to call super class constructor with message and root cause exception
     *
     * @param cause exception root cause
     */
    protected AggregationProcessingBaseException(Integer httpStatusCode, Throwable cause) {
        super(cause.getMessage(), cause);
        this.httpStatusCode = httpStatusCode;
    }

}
