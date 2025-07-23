package de.datev.refsys.aggregation.processing.exception;

import de.datev.refsys.aggregation.processing.constant.ProcessingErrorMessageConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.function.Predicate;

@Slf4j
public class HttpCallRetryExceptionPredicate implements Predicate<Throwable> {
    // status 204 is for MasterDataContext endpoint, on other endpoint no exception occurs with 204 status
    private static final List<Integer> UNRECORDED_HTTP_CODES = List.of(204, 555);

    @Override
    public boolean test(Throwable throwable) {
        log.warn(ProcessingErrorMessageConstants.RETRY_EXCEPTION_OCCURRED, throwable);
        if (throwable instanceof HttpCallException httpCallException) {
            Integer sourceHttpStatusCode = httpCallException.getHttpStatusCode();
            if (sourceHttpStatusCode == HttpStatus.OK.value()) {
                return false;
            }
            return !is4xxClientError(sourceHttpStatusCode) && !UNRECORDED_HTTP_CODES.contains(sourceHttpStatusCode);
        }
        return false;
    }

    private boolean is4xxClientError(Integer statusCode) {
        return statusCode >= 400 && statusCode < 500;
    }
}
