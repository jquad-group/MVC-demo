package de.datev.refsys.aggregation.processing.client.test_model;

import de.datev.refsys.aggregation.processing.exception.HttpCallException;
import de.datev.refsys.aggregation.processing.model.enums.SourceError;
import lombok.Getter;

@Getter
public class StatusCodeErrorInfo {
    private final Class<? extends HttpCallException> httpCallException;
    private final String processingErrorMessage;
    private final SourceError sourceError;

    public StatusCodeErrorInfo(String processingErrorMessage, Class<? extends HttpCallException> httpCallException, SourceError sourceError) {
        this.processingErrorMessage = processingErrorMessage;
        this.httpCallException = httpCallException;
        this.sourceError = sourceError;
    }

    public String getProcessingErrorMessage(String serviceName) {
        if(!processingErrorMessage.contains("%s")) {
            return processingErrorMessage;
        }
        return String.format(processingErrorMessage, serviceName);
    }
}
