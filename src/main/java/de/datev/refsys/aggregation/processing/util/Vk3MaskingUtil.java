package de.datev.refsys.aggregation.processing.util;

import lombok.experimental.UtilityClass;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 *
 */
@UtilityClass
public class Vk3MaskingUtil {
    // pattern for lombok @ToString annotation usage
    private static final Pattern JAVA_PATTERN = Pattern.compile("(\\w+)=(?![ *{\\[])(.[^(]+?)[,)}\\]]", Pattern.MULTILINE);
    // pattern for mongo/objectMapper json (pretty print with new lines is not supported)
    private static final Pattern MONGO_JSON_PATTERN = Pattern.compile("\"(\\$?\\w+)\": *(?![ *{\\[])\"?(.+?)\"?[,}]", Pattern.MULTILINE);
    // pattern to mask arrays with primitive or wrapper types (int, Integer, long, Long, char, String, ...) for java and json
    private static final Pattern SIMPLE_ARRAY_PATTERN = Pattern.compile("\"?(\\w+)\"? *[=|:] *\\[(.[^*]+?)]", Pattern.MULTILINE);
    private static final List<Pattern> PATTERN_LIST = List.of(MONGO_JSON_PATTERN, JAVA_PATTERN, SIMPLE_ARRAY_PATTERN);
    public static final char MASK = '*';

    public static String maskMessage(String message, List<String> whiteList) {
        if (!StringUtils.hasText(message)) {
            return message;
        }
        StringBuilder result = new StringBuilder(message);
        PATTERN_LIST.forEach(pattern -> {
            Matcher matcher = pattern.matcher(result);
            while (matcher.find()) {
                String fieldName = matcher.group(1).replace("\"", "");
                if (whiteList.stream().anyMatch(fieldName::equals)) {
                    // whiteList field detected, skip masking
                    continue;
                }
                IntStream.range(matcher.start(2), matcher.end(2)).forEach(i -> result.setCharAt(i, MASK));
            }
        });
        return result.toString();
    }
}
