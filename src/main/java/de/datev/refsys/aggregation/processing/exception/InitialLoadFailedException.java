package de.datev.refsys.aggregation.processing.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * This Exception will be thrown when error occurred by initial load process
 */
@Getter
public class InitialLoadFailedException extends AggregationProcessingBaseException {
    private final String type;

    /**
     * InitialLoadException constructor to call super class constructor with exception message
     *
     * @param message exception message
     */
    public InitialLoadFailedException(String message, Integer httpStatusCode, String type) {
        super(message, httpStatusCode);
        this.type = type;
    }

    /**
     * InitialLoadException constructor to call super class constructor with message and root cause exception
     *
     * @param message exception message
     * @param cause   exception root cause
     */
    public InitialLoadFailedException(String message, Integer httpStatusCode, String type, Throwable cause) {
        super(message, httpStatusCode, cause);
        this.type = type;
    }
}
