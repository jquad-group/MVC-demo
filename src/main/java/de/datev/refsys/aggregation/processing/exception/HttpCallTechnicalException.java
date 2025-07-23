package de.datev.refsys.aggregation.processing.exception;

import de.datev.refsys.aggregation.document.model.ProblemInfo;
import de.datev.refsys.aggregation.processing.model.enums.SourceEndpoint;
import de.datev.refsys.aggregation.processing.model.enums.SourceError;
import lombok.Getter;

@Getter
public class HttpCallTechnicalException extends HttpCallException {
    public HttpCallTechnicalException(String message, SourceError source, SourceEndpoint sourceEndpoint, Integer sourceHttpStatusCode,
                                      ProblemInfo problemInfo) {
        super(message, source, sourceEndpoint, sourceHttpStatusCode, problemInfo);
    }

    public HttpCallTechnicalException(String message, SourceError source, SourceEndpoint sourceEndpoint,
                                      Integer sourceHttpStatusCode, ProblemInfo problemInfo, Throwable cause) {
        super(message, cause, source, sourceEndpoint, sourceHttpStatusCode, problemInfo);
    }
}
