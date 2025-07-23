package de.datev.refsys.aggregation.processing.exception;

import de.datev.refsys.aggregation.document.model.ProblemInfo;
import de.datev.refsys.aggregation.processing.model.enums.SourceEndpoint;
import de.datev.refsys.aggregation.processing.model.enums.SourceError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static de.datev.refsys.aggregation.processing.util.TestUtil.createProblemInfo;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
class RetryHttpExceptionPredicateTest {
    private HttpCallRetryExceptionPredicate httpExceptionPredicate;

    @BeforeEach
    void setUp() {
        httpExceptionPredicate = new HttpCallRetryExceptionPredicate();
    }

    @Test
    void should_return_false_when_a_duplicate_key_error_occurs() {
        ProblemInfo problemInfo = createProblemInfo();
        assertThat(httpExceptionPredicate.test(new RuntimeException("test"))).isFalse();
        assertThat(httpExceptionPredicate.test(new AggregationProcessingBusinessException(HttpStatus.INTERNAL_SERVER_ERROR.value(), new RuntimeException("test")))).isFalse();
        assertThat(httpExceptionPredicate.test(new HttpCallTechnicalException("test", SourceError.PROCESSING_SERVICE, SourceEndpoint.MASTER_DATA_CONTEXT, 204, problemInfo))).isFalse();
        assertThat(httpExceptionPredicate.test(new HttpCallTechnicalException("test", SourceError.PROCESSING_SERVICE, SourceEndpoint.MASTER_DATA_CONTEXT, 400, problemInfo))).isFalse();
        assertThat(httpExceptionPredicate.test(new HttpCallTechnicalException("test", SourceError.PROCESSING_SERVICE, SourceEndpoint.MASTER_DATA_CONTEXT, 404, problemInfo))).isFalse();
        assertThat(httpExceptionPredicate.test(new HttpCallBusinessException("test", SourceError.PROCESSING_SERVICE, SourceEndpoint.MASTER_DATA_CONTEXT, 555, problemInfo))).isFalse();
        assertThat(httpExceptionPredicate.test(new HttpCallTechnicalException("test", SourceError.PROCESSING_SERVICE, SourceEndpoint.ACCOUNT_SUM_DAYS, 200, problemInfo, new OutOfMemoryError("oom")))).isFalse();

        // True cases
        assertThat(httpExceptionPredicate.test(new HttpCallBusinessException("test", SourceError.PROCESSING_SERVICE, SourceEndpoint.MASTER_DATA_CONTEXT, 156, problemInfo))).isTrue();
        assertThat(httpExceptionPredicate.test(new HttpCallTechnicalException("test", SourceError.PROCESSING_SERVICE, SourceEndpoint.MASTER_DATA_CONTEXT, 234, problemInfo))).isTrue();
        assertThat(httpExceptionPredicate.test(new HttpCallBusinessException("test", SourceError.PROCESSING_SERVICE, SourceEndpoint.MASTER_DATA_CONTEXT, 356, problemInfo))).isTrue();
        assertThat(httpExceptionPredicate.test(new HttpCallBusinessException("test", SourceError.PROCESSING_SERVICE, SourceEndpoint.MASTER_DATA_CONTEXT, 556, problemInfo))).isTrue();
        assertThat(httpExceptionPredicate.test(new HttpCallBusinessException("test", SourceError.PROCESSING_SERVICE, SourceEndpoint.MASTER_DATA_CONTEXT, 500, problemInfo))).isTrue();
    }
}