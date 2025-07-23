package de.datev.refsys.aggregation.processing.util;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class UtilTest {

    @ParameterizedTest
    @MethodSource("longValueTestProvider")
    void testlongConversion(long expected, double value) {
        assertThat(Util.amountToLongInCent(value)).isEqualTo(expected);
    }

    @Test
    @Disabled("Performance Test - should not run un buildsystem")
    void runtimeTest() {
        long start = System.currentTimeMillis();
        for(int i = 0; i < 1_000_000; i++) {
            Util.amountToLongInCent(1.0);
        }
        long end = System.currentTimeMillis();
        assertThat(end - start).isLessThan(20);
    }

    public static Stream<Arguments> longValueTestProvider() {
        return Stream.of(
                Arguments.arguments(100L, 1.0),
                Arguments.arguments(100L, 1.00),
                Arguments.arguments(100L, 1.000),
                Arguments.arguments(110L, 1.100),
                Arguments.arguments(110L, 1.10),
                Arguments.arguments(110L, 1.1),
                Arguments.arguments(1646560, 16465.60)
        );
    }
}