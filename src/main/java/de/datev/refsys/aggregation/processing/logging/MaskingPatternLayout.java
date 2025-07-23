package de.datev.refsys.aggregation.processing.logging;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import de.datev.refsys.aggregation.processing.util.Vk3MaskingUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public class MaskingPatternLayout extends PatternLayout {
    private static final String DATEV_BASE_PACKAGE = "de.datev.refsys";
    private static final String ON_ERROR_DROPPED_LOGGER = "reactor.core.publisher.Operators";
    // pattern to capture the whole mongo DuplicateKeyException where VK3 data is logged
    private static final Pattern MONGO_ERROR_PATTERN = Pattern.compile("key: \\{(.+?)}");
    // pattern to get each field and value from the DuplicateKeyException key
    private static final Pattern MONGO_ERROR_FIELDS_PATTERN = Pattern.compile("(\\w+): (?![{( B])\"?(.+?)\"?[, ]");
    private final List<String> maskPatterns = new ArrayList<>();

    public void addMaskPattern(String maskPattern) {
        maskPatterns.add(maskPattern);
    }

    @Override
    public String doLayout(ILoggingEvent event) {
        // apply mask pattern only for DATEV loggers and when an unhandled onErrorDropped log occurs
        if (event.getLoggerName().contains(DATEV_BASE_PACKAGE) || event.getLoggerName().equals(ON_ERROR_DROPPED_LOGGER)) {
            String maskedMessage = Vk3MaskingUtil.maskMessage(super.doLayout(event), maskPatterns);
            return maskMongoErrorMessage(maskedMessage);
        }
        return super.doLayout(event);
    }

    private String maskMongoErrorMessage(String message) {
        StringBuilder result = new StringBuilder(message);
        Matcher matcher = MONGO_ERROR_PATTERN.matcher(result);
        while (matcher.find()) {
            StringBuilder fieldResult = new StringBuilder(matcher.group(1));
            Matcher fieldMatcher = MONGO_ERROR_FIELDS_PATTERN.matcher(fieldResult);
            while (fieldMatcher.find()) {
                String fieldName = fieldMatcher.group(1);
                if (maskPatterns.stream().anyMatch(fieldName::contains)) {
                    // whiteList field detected, skip masking
                    continue;
                }
                IntStream.range(fieldMatcher.start(2), fieldMatcher.end(2)).forEach(i -> fieldResult.setCharAt(i, Vk3MaskingUtil.MASK));
            }
            result.replace(matcher.start(1), matcher.end(1), fieldResult.toString());
        }
        return result.toString();
    }
}
