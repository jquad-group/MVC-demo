package de.datev.refsys.aggregation.processing.util;

import org.junit.jupiter.api.Test;

import static de.datev.refsys.aggregation.processing.util.NumberUtils.nullSafeSubstract;
import static org.assertj.core.api.Assertions.assertThat;

class NumberUtilsTest {

    @Test
    void nullSafeSubstract_SubstractLongFromNull_ShouldBeNegativeNumber(){
        Long nullValue = null;
        Long valueToSubstract = 100L;
        Long expectedValue = -100L;

        Long actual = nullSafeSubstract(nullValue, valueToSubstract);

        assertThat(actual).isEqualTo(expectedValue);
    }

    @Test
    void nullSafeSubstract_SubstractIntegerFromNull_ShouldBeNegativeNumber(){
        Integer nullValue = null;
        Integer valueToSubstract = 100;
        Integer expectedValue = -100;

        Integer actual = nullSafeSubstract(nullValue, valueToSubstract);

        assertThat(actual).isEqualTo(expectedValue);
    }

    @Test
    void nullSafeSubstract_SubstractZeroFromNull_ShouldBeNull() {
        Long nullValue = null;
        Long valueToSubstract = 0L;
        Long expectedValue = null;

        Long actual = nullSafeSubstract(nullValue, valueToSubstract);

        assertThat(actual).isEqualTo(expectedValue);
    }

    @Test
    void nullSafeSubstract_SubstractNullFromZero_ShouldBeNull() {
        Long existingValue = 0L;
        Long nullValue = null;
        Long expectedValue = null;

        Long actual = nullSafeSubstract(existingValue, nullValue);

        assertThat(actual).isEqualTo(expectedValue);
    }

    @Test
    void nullSafeSubstract_SubstractNullFromNull_ShouldBeNull() {
        Long nullValue1 = null;
        Long nullValue2 = null;
        Long expectedValue = null;

        Long actual = nullSafeSubstract(nullValue1, nullValue2);

        assertThat(actual).isEqualTo(expectedValue);
    }

    @Test
    void nullSafeSubstract_SubstractZeroFromZero_ShouldBeNull() {
        Long value1 = 0L;
        Long value2 = 0L;
        Long expectedValue = null;

        Long actual = nullSafeSubstract(value1, value2);

        assertThat(actual).isEqualTo(expectedValue);
    }

    @Test
    void nullSafeSubstract_SubstractPositiveFromPositive_ShouldBePositive() {
        Long value1 = 200L;
        Long value2 = 100L;
        Long expectedValue = 100L;

        Long actual = nullSafeSubstract(value1, value2);

        assertThat(actual).isEqualTo(expectedValue);
    }

    @Test
    void nullSafeSubstract_SubstractNegativeFromPositive_ShouldBePositive() {
        Long value1 = 100L;
        Long value2 = -50L;
        Long expectedValue = 150L;

        Long actual = nullSafeSubstract(value1, value2);

        assertThat(actual).isEqualTo(expectedValue);
    }

    @Test
    void nullSafeSubstract_SubstractPositiveFromNegative_ShouldBeNegative() {
        Long value1 = -100L;
        Long value2 = 50L;
        Long expectedValue = -150L;

        Long actual = nullSafeSubstract(value1, value2);

        assertThat(actual).isEqualTo(expectedValue);
    }

    @Test
    void nullSafeSubstract_SubstractNegativeFromNegative_ShouldBeNegative() {
        Long value1 = -100L;
        Long value2 = -50L;
        Long expectedValue = -50L;

        Long actual = nullSafeSubstract(value1, value2);

        assertThat(actual).isEqualTo(expectedValue);
    }

}