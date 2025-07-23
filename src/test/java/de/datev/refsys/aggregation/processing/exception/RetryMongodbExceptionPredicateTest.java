package de.datev.refsys.aggregation.processing.exception;

import com.mongodb.MongoBulkWriteException;
import com.mongodb.MongoException;
import com.mongodb.MongoWriteException;
import com.mongodb.ServerAddress;
import com.mongodb.WriteError;
import com.mongodb.bulk.BulkWriteError;
import com.mongodb.bulk.BulkWriteResult;
import org.bson.BsonDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
class RetryMongodbExceptionPredicateTest {
    private RetryMongodbExceptionPredicate retryExceptionPredicate;

    @BeforeEach
    void setUp() {
        retryExceptionPredicate = new RetryMongodbExceptionPredicate();
    }

    @Test
    void should_return_false_when_a_duplicate_key_error_occurs() {
        // 11000 code for DUPLICATE_KEY error (see com.mongodb.ErrorCategory class)
        MongoBulkWriteException mongoBulkWriteExceptionDK = new MongoBulkWriteException(BulkWriteResult.unacknowledged(), List.of(
                new BulkWriteError(11000, "duplicate key", new BsonDocument(), 1)), null, new ServerAddress(), Collections.emptySet());
        MongoWriteException mongoWriteExceptionDK =
                new MongoWriteException(new WriteError(11000, "duplicate key", new BsonDocument()), new ServerAddress(), Collections.emptySet());

        assertThat(retryExceptionPredicate.test(mongoBulkWriteExceptionDK)).isFalse();
        assertThat(retryExceptionPredicate.test(mongoWriteExceptionDK)).isFalse();
        assertThat(retryExceptionPredicate.test(new RuntimeException("test"))).isFalse();

        // 50 code for EXECUTION_TIMEOUT error (see com.mongodb.ErrorCategory class)
        MongoBulkWriteException mongoBulkWriteExceptionTimeout = new MongoBulkWriteException(BulkWriteResult.unacknowledged(), List.of(
                new BulkWriteError(50, "timeout", new BsonDocument(), 1)), null, new ServerAddress(), Collections.emptySet());
        MongoWriteException mongoWriteExceptionTimeout =
                new MongoWriteException(new WriteError(50, "timeout", new BsonDocument()), new ServerAddress(), Collections.emptySet());

        assertThat(retryExceptionPredicate.test(mongoBulkWriteExceptionTimeout)).isTrue();
        assertThat(retryExceptionPredicate.test(mongoWriteExceptionTimeout)).isTrue();
        assertThat(retryExceptionPredicate.test(new MongoException("test"))).isTrue();

    }
}