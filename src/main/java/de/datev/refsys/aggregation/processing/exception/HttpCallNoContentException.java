package de.datev.refsys.aggregation.processing.exception;

import de.datev.refsys.aggregation.document.model.ProblemInfo;
import de.datev.refsys.aggregation.processing.constant.ProcessingErrorMessageConstants;
import de.datev.refsys.aggregation.processing.model.enums.SourceEndpoint;
import de.datev.refsys.aggregation.processing.model.enums.SourceError;
import lombok.Getter;

@Getter
public class HttpCallNoContentException extends HttpCallException {
    public static final String NO_CONTENT_TITLE = "No Content in MasterdataContext";
    public HttpCallNoContentException(SourceError source, SourceEndpoint sourceEndpoint, Integer sourceHttpStatusCode) {
        super(ProcessingErrorMessageConstants.NO_MASTER_DATA_CONTEXT_IN_ACDS, source, sourceEndpoint, sourceHttpStatusCode, noContentProblemInfo());
    }

    private static ProblemInfo noContentProblemInfo() {
        return ProblemInfo.builder()
                          .type(SourceError.ACDS.getValue())
                          .title(NO_CONTENT_TITLE)
                          .detail(ProcessingErrorMessageConstants.NO_MASTER_DATA_CONTEXT_IN_ACDS)
                          .instance(SourceEndpoint.MASTER_DATA_CONTEXT.getValue())
                          .build();
    }
}
