package de.datev.refsys.aggregation.processing.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PersonGroupAmountValuesTest {

    @Test
    void substract_allValuesZero_substractZeroValues_expectNewResultObjectWithOnlyNullValues() {
        PersonGroupAmountValues objectUnderTest = new PersonGroupAmountValues(0L,0L,0L,0L);
        PersonGroupAmountValues objectToSubstract = new PersonGroupAmountValues(0L, 0L, 0L, 0L);
        PersonGroupAmountValues expectedObject = new PersonGroupAmountValues(null, null, null, null);

        PersonGroupAmountValues actualValue = objectUnderTest.substract(objectToSubstract);

        assertThat(actualValue).isEqualTo(expectedObject);
        assertThat(actualValue).as("the result should be a new object not the testObject").isNotSameAs(objectUnderTest);
        assertThat(actualValue).as("the result should be a new object not the substracted Object").isNotSameAs(objectToSubstract);
    }

    @Test
    void substract_allValuesZero_substractPositiveValues_expectNewResultObjectWithNegativeValues() {
        PersonGroupAmountValues objectUnderTest = new PersonGroupAmountValues(0L,0L,0L,0L);
        PersonGroupAmountValues objectToSubstract = new PersonGroupAmountValues(1L, 2L, 3L, 4L);
        PersonGroupAmountValues expectedObject = new PersonGroupAmountValues(-1L, -2L, -3L, -4L);

        PersonGroupAmountValues actualValue = objectUnderTest.substract(objectToSubstract);

        assertThat(actualValue).usingRecursiveComparison().isEqualTo(expectedObject);
        assertThat(actualValue).as("the result should be a new object not the testObject").isNotSameAs(objectUnderTest);
        assertThat(actualValue).as("the result should be a new object not the substracted Object").isNotSameAs(objectToSubstract);
    }

    @Test
    void substract_allValuesZero_substractNegativeValues_expectNewResultObjectWithPositiveValues() {
        PersonGroupAmountValues objectUnderTest = new PersonGroupAmountValues(0L,0L,0L,0L);
        PersonGroupAmountValues objectToSubstract = new PersonGroupAmountValues(-1L, -2L, -3L, -4L);
        PersonGroupAmountValues expectedObject = new PersonGroupAmountValues(1L, 2L, 3L, 4L);

        PersonGroupAmountValues actualValue = objectUnderTest.substract(objectToSubstract);

        assertThat(actualValue).usingRecursiveComparison().isEqualTo(expectedObject);
        assertThat(actualValue).as("the result should be a new object not the testObject").isNotSameAs(objectUnderTest);
        assertThat(actualValue).as("the result should be a new objectnot the substracted Object").isNotSameAs(objectToSubstract);
    }

    @Test
    void substract_allValuesSet_substractEmptyObject_expectNewResultObjectWithNoChanges() {
        PersonGroupAmountValues objectUnderTest = new PersonGroupAmountValues(1L, 2L, 3L, 4L);
        PersonGroupAmountValues objectToSubstract = PersonGroupAmountValues.builder().build();
        PersonGroupAmountValues expectedObject = new PersonGroupAmountValues(1L, 2L, 3L, 4L);

        PersonGroupAmountValues actualValue = objectUnderTest.substract(objectToSubstract);

        assertThat(actualValue).usingRecursiveComparison().isEqualTo(expectedObject);
        assertThat(actualValue).as("the result should be a new object not the testObject").isNotSameAs(objectUnderTest);
        assertThat(actualValue).as("the result should be a new objectnot the substracted Object").isNotSameAs(objectToSubstract);
    }
}