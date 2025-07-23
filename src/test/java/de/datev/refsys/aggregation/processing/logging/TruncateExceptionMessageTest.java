package de.datev.refsys.aggregation.processing.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class TruncateExceptionMessageTest {
    private static Logger logger;

    private static PrintStream defaultOut;

    private static final int MAX_DEBUG_MESSAGE_LENGTH = 10;

    java.io.ByteArrayOutputStream caughtDebugMessage;

    @BeforeAll
    static void setup() throws IOException, JoranException {
        defaultOut = System.out;
        File file = new File("src/test/resources/logback-with-limit.xml");
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.reset();
        JoranConfigurator configurator = new JoranConfigurator();
        InputStream configStream = FileUtils.openInputStream(file);
        configurator.setContext(loggerContext);
        configurator.doConfigure(configStream);
        configStream.close();
        logger = loggerContext.getLogger(TruncateExceptionMessageTest.class);
    }

    @BeforeEach
    void beforeEach() {
        caughtDebugMessage = new java.io.ByteArrayOutputStream();
        System.setOut(new java.io.PrintStream(caughtDebugMessage));
    }

    @AfterAll
    static void tearDown() {
        System.setOut(defaultOut);
    }

    @Test
    void should_truncate_log_message_to_10_chars_at_start() {
        String debugMsgTooLong = "This Debug Message is too long";
        assertThat(debugMsgTooLong.length()).isGreaterThan(MAX_DEBUG_MESSAGE_LENGTH);
        String logMessageExpected = debugMsgTooLong.substring(0, MAX_DEBUG_MESSAGE_LENGTH);
        logger.debug(debugMsgTooLong);

        assertThat(caughtDebugMessage).isNotNull();
        assertThat(caughtDebugMessage.toString().length()).isEqualTo(MAX_DEBUG_MESSAGE_LENGTH);
        assertThat(caughtDebugMessage.toString()).isEqualTo(logMessageExpected);
    }

    @Test
    void should_not_truncate_log_message_if_log_message_is_less_than_10_chars() {
        String debugMsgTooLong = "#########";
        assertThat(debugMsgTooLong.length()).isEqualTo(9);
        logger.debug(debugMsgTooLong);

        assertThat(caughtDebugMessage).isNotNull();
        assertThat(caughtDebugMessage.toString().length()).isEqualTo(9);
    }

    @Test
    void should_not_truncate_log_message_if_log_message_is_10_chars() {
        String debugMsgTooLong = "##########";
        assertThat(debugMsgTooLong.length()).isEqualTo(MAX_DEBUG_MESSAGE_LENGTH);
        logger.debug(debugMsgTooLong);

        assertThat(caughtDebugMessage).isNotNull();
        assertThat(caughtDebugMessage.toString().length()).isEqualTo(MAX_DEBUG_MESSAGE_LENGTH);
    }
}
