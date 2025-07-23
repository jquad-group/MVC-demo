package de.datev.refsys.aggregation.processing.exception;

import com.mongodb.ErrorCategory;
import com.mongodb.MongoBulkWriteException;
import com.mongodb.MongoException;
import com.mongodb.MongoWriteException;
import com.mongodb.bulk.BulkWriteError;
import de.datev.refsys.aggregation.processing.constant.ProcessingErrorMessageConstants;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.function.Predicate;

@Slf4j
public class RetryMongodbExceptionPredicate implements Predicate<Throwable> {

    @Override
    public boolean test(Throwable throwable) {
        log.warn(ProcessingErrorMessageConstants.RETRY_EXCEPTION_OCCURRED, throwable);
        // retry only when any MongoException occurs (parent class for all Mongo errors)
        if (throwable instanceof MongoException) {
            if (throwable instanceof MongoWriteException mongoWriteException
                    && mongoWriteException.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
                return false;
            }
            if (throwable instanceof MongoBulkWriteException mongoBulkWriteException && (!mongoBulkWriteException.getWriteErrors().isEmpty())){
                Optional<BulkWriteError> bulkWriteError = mongoBulkWriteException.getWriteErrors().stream()
                                                                                 .filter(error -> error.getCategory() == ErrorCategory.DUPLICATE_KEY)
                                                                                 .findFirst();
                return bulkWriteError.isEmpty();
            }
            return true;
        }
        return false;
    }
}
