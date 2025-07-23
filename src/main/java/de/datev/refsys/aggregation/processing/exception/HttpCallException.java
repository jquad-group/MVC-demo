package de.datev.refsys.aggregation.processing.exception;

import de.datev.refsys.aggregation.document.model.ProblemInfo;
import de.datev.refsys.aggregation.processing.model.enums.SourceEndpoint;
import de.datev.refsys.aggregation.processing.model.enums.SourceError;
import lombok.Getter;

@Getter
public abstract class HttpCallException extends AggregationProcessingBaseException {
    private final SourceError source;
    private final SourceEndpoint sourceEndpoint;
    private final transient ProblemInfo problemInfo;

    HttpCallException(String message, SourceError source, SourceEndpoint sourceEndpoint, Integer sourceHttpStatusCode, ProblemInfo problemInfo) {
        super(message, sourceHttpStatusCode);
        this.source = source;
        this.sourceEndpoint = sourceEndpoint;
        this.problemInfo = problemInfo;
    }

    HttpCallException(String message, Throwable cause, SourceError source, SourceEndpoint sourceEndpoint, Integer sourceHttpStatusCode,
                      ProblemInfo problemInfo) {
        super(message, sourceHttpStatusCode, cause);
        this.source = source;
        this.sourceEndpoint = sourceEndpoint;
        this.problemInfo = problemInfo;
    }
}
