package de.datev.refsys.aggregation.processing.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * This Exception is only for Business exceptions, in order to not retry it.
 */
@Getter
public class AggregationProcessingBusinessException extends AggregationProcessingBaseException {

    /**
     * AggregationProcessingServiceException constructor to call super class constructor with exception message
     *
     * @param message exception message
     */
    public AggregationProcessingBusinessException(String message, Integer httpStatus) {
        super(message, httpStatus);
    }

    /**
     * AggregationProcessingServiceException constructor to call super class constructor with message and root cause exception
     *
     * @param message exception message
     * @param cause   exception root cause
     */
    public AggregationProcessingBusinessException(String message, Integer httpStatus, Throwable cause) {
        super(message, httpStatus, cause);
    }

    /**
     * AggregationProcessingServiceException constructor to call super class constructor with message and root cause exception
     *
     * @param cause exception root cause
     */
    public AggregationProcessingBusinessException(Integer httpStatus, Throwable cause) {
        super(httpStatus, cause);
    }


}
