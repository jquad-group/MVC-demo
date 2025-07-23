package de.datev.refsys.aggregation.processing.functions;

import de.datev.refsys.aggregation.document.model.AccountGroupValue;
import de.datev.refsys.aggregation.document.model.MovementDataPersonGroupDay;
import de.datev.refsys.aggregation.document.model.MovementDataPersonGroupMonth;
import de.datev.refsys.aggregation.processing.model.AccountDbKeyFields;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static de.datev.refsys.aggregation.processing.functions.PersonGroupDeltaCalculation.calculateDifferenceDay;
import static de.datev.refsys.aggregation.processing.functions.PersonGroupDeltaCalculation.subtractGroupValues;
import static org.assertj.core.api.Assertions.assertThat;

class PersonGroupDeltaCalculationTest {

    //ResultCollection should have all values
    @Test
    void subtractDays_ResultShouldHaveAllMetadataFromOrigin() {
        Map<AccountDbKeyFields, MovementDataPersonGroupDay> afterDeltaPersonGroupDayMap = new LinkedHashMap<>();
        Map<AccountDbKeyFields, MovementDataPersonGroupDay> existingPersonGroupDayMap = new LinkedHashMap<>();
        AccountDbKeyFields personGroupOneKey = AccountDbKeyFields.builder().accountNumber(1).accountingReasonId(0).recordType(1).build();
        MovementDataPersonGroupDay afterDeltaPersonGroupDay = MovementDataPersonGroupDay.builder()
                                                                                        .consultant(123)
                                                                                        .client(345)
                                                                                        .fiscalYear(20210101)
                                                                                        .accountGroupNumber(1)
                                                                                        .accountingReasonId(0)
                                                                                        .values(new LinkedHashMap<>())
                                                                                        .build();
        afterDeltaPersonGroupDay.getValues().put("d20210101", AccountGroupValue.builder().amountCreditUsual(200L).build());
        afterDeltaPersonGroupDayMap.put(personGroupOneKey, afterDeltaPersonGroupDay);

        MovementDataPersonGroupDay existingPersonGroupDay = MovementDataPersonGroupDay.builder()
                                                                                      .consultant(123)
                                                                                      .client(345)
                                                                                      .fiscalYear(20210101)
                                                                                      .accountGroupNumber(1)
                                                                                      .accountingReasonId(0)
                                                                                      .values(new LinkedHashMap<>())
                                                                                      .build();
        existingPersonGroupDay.getValues().put("d20210101", AccountGroupValue.builder().amountCreditUsual(100L).build());
        existingPersonGroupDayMap.put(personGroupOneKey, existingPersonGroupDay);

        Map<AccountDbKeyFields, MovementDataPersonGroupDay> actualDifference =

                PersonGroupDeltaCalculation.subtractDays(afterDeltaPersonGroupDayMap, existingPersonGroupDayMap);

        assertThat(actualDifference.get(personGroupOneKey))
                .hasFieldOrPropertyWithValue("consultant", 123)
                .hasFieldOrPropertyWithValue("client", 345)
                .hasFieldOrPropertyWithValue("fiscalYear", 20210101)
                .hasFieldOrPropertyWithValue("accountGroupNumber", 1)
                .hasFieldOrPropertyWithValue("accountingReasonId", 0);
    }

    @Test
    void subtractDays_emptyLeftCollection_ResultShouldHaveAllMetadataFromOrigin() {
        Map<AccountDbKeyFields, MovementDataPersonGroupDay> afterDeltaPersonGroupDayMap = new LinkedHashMap<>();
        Map<AccountDbKeyFields, MovementDataPersonGroupDay> existingPersonGroupDayMap = new LinkedHashMap<>();
        AccountDbKeyFields personGroupOneKey = AccountDbKeyFields.builder().accountNumber(1).accountingReasonId(0).recordType(1).build();

        MovementDataPersonGroupDay existingPersonGroupDay = MovementDataPersonGroupDay.builder()
                                                                                      .consultant(123)
                                                                                      .client(345)
                                                                                      .fiscalYear(20210101)
                                                                                      .accountGroupNumber(1)
                                                                                      .accountingReasonId(0)
                                                                                      .values(new LinkedHashMap<>())
                                                                                      .build();
        existingPersonGroupDay.getValues().put("d20210101", AccountGroupValue.builder().amountCreditUsual(100L).build());
        existingPersonGroupDayMap.put(personGroupOneKey, existingPersonGroupDay);

        Map<AccountDbKeyFields, MovementDataPersonGroupDay> actualDifference =

                PersonGroupDeltaCalculation.subtractDays(afterDeltaPersonGroupDayMap, existingPersonGroupDayMap);

        assertThat(actualDifference.get(personGroupOneKey))
                .hasFieldOrPropertyWithValue("consultant", 123)
                .hasFieldOrPropertyWithValue("client", 345)
                .hasFieldOrPropertyWithValue("fiscalYear", 20210101)
                .hasFieldOrPropertyWithValue("accountGroupNumber", 1)
                .hasFieldOrPropertyWithValue("accountingReasonId", 0);
    }

    @Test
    void subtractMonths_ResultShouldHaveAllMetadataFromOrigin() {
        Map<AccountDbKeyFields, MovementDataPersonGroupMonth> afterDeltaPersonGroupMonthMap = new LinkedHashMap<>();
        Map<AccountDbKeyFields, MovementDataPersonGroupMonth> existingPersonGroupMonthMap = new LinkedHashMap<>();
        AccountDbKeyFields personGroupOneKey = AccountDbKeyFields.builder().accountNumber(1).accountingReasonId(0).recordType(1).build();
        MovementDataPersonGroupMonth afterDeltaPersonGroupMonth = MovementDataPersonGroupMonth.builder()
                                                                                              .consultant(123)
                                                                                              .client(345)
                                                                                              .fiscalYear(20210101)
                                                                                              .accountGroupNumber(1)
                                                                                              .accountingReasonId(0)
                                                                                              .values(new LinkedHashMap<>())
                                                                                              .build();
        afterDeltaPersonGroupMonth.getValues().put("m1", AccountGroupValue.builder().amountCreditUsual(200L).build());
        afterDeltaPersonGroupMonthMap.put(personGroupOneKey, afterDeltaPersonGroupMonth);

        MovementDataPersonGroupMonth existingPersonGroupMonth = MovementDataPersonGroupMonth.builder()
                                                                                      .consultant(123)
                                                                                      .client(345)
                                                                                      .fiscalYear(20210101)
                                                                                      .accountGroupNumber(1)
                                                                                      .accountingReasonId(0)
                                                                                      .values(new LinkedHashMap<>())
                                                                                      .build();
        existingPersonGroupMonth.getValues().put("m1", AccountGroupValue.builder().amountCreditUsual(100L).build());
        existingPersonGroupMonthMap.put(personGroupOneKey, existingPersonGroupMonth);

        Map<AccountDbKeyFields, MovementDataPersonGroupMonth> actualDifference =

                PersonGroupDeltaCalculation.subtractMonths(afterDeltaPersonGroupMonthMap, existingPersonGroupMonthMap);

        assertThat(actualDifference.get(personGroupOneKey))
                .hasFieldOrPropertyWithValue("consultant", 123)
                .hasFieldOrPropertyWithValue("client", 345)
                .hasFieldOrPropertyWithValue("fiscalYear", 20210101)
                .hasFieldOrPropertyWithValue("accountGroupNumber", 1)
                .hasFieldOrPropertyWithValue("accountingReasonId", 0);
    }

    @Test
    void subtractMonths_emptyLeftCollection_ResultShouldHaveAllMetadataFromOrigin() {
        Map<AccountDbKeyFields, MovementDataPersonGroupMonth> afterDeltaPersonGroupMonthMap = new LinkedHashMap<>();
        Map<AccountDbKeyFields, MovementDataPersonGroupMonth> existingPersonGroupMonthMap = new LinkedHashMap<>();
        AccountDbKeyFields personGroupOneKey = AccountDbKeyFields.builder().accountNumber(1).accountingReasonId(0).recordType(1).build();

        MovementDataPersonGroupMonth existingPersonGroupMonth = MovementDataPersonGroupMonth.builder()
                                                                                            .consultant(123)
                                                                                            .client(345)
                                                                                            .fiscalYear(20210101)
                                                                                            .accountGroupNumber(1)
                                                                                            .accountingReasonId(0)
                                                                                            .values(new LinkedHashMap<>())
                                                                                            .build();
        existingPersonGroupMonth.getValues().put("m1", AccountGroupValue.builder().amountCreditUsual(100L).build());
        existingPersonGroupMonthMap.put(personGroupOneKey, existingPersonGroupMonth);

        Map<AccountDbKeyFields, MovementDataPersonGroupMonth> actualDifference =

                PersonGroupDeltaCalculation.subtractMonths(afterDeltaPersonGroupMonthMap, existingPersonGroupMonthMap);

        assertThat(actualDifference.get(personGroupOneKey))
                .hasFieldOrPropertyWithValue("consultant", 123)
                .hasFieldOrPropertyWithValue("client", 345)
                .hasFieldOrPropertyWithValue("fiscalYear", 20210101)
                .hasFieldOrPropertyWithValue("accountGroupNumber", 1)
                .hasFieldOrPropertyWithValue("accountingReasonId", 0);
    }


    //Keys existing in both collections
    @Test
    void subtractDays_AfterListHasValues_ResultsInCorrectDifference() {
        Map<AccountDbKeyFields, MovementDataPersonGroupDay> afterDeltaPersonGroupDayMap = new LinkedHashMap<>();
        Map<AccountDbKeyFields, MovementDataPersonGroupDay> existingPersonGroupDayMap = new LinkedHashMap<>();
        AccountDbKeyFields personGroupOneKey = AccountDbKeyFields.builder().accountNumber(1).accountingReasonId(0).recordType(1).build();
        MovementDataPersonGroupDay afterDeltaPersonGroupDay = MovementDataPersonGroupDay.builder()
                                                                                        .consultant(123)
                                                                                        .client(345)
                                                                                        .fiscalYear(20210101)
                                                                                        .accountGroupNumber(1)
                                                                                        .accountingReasonId(0)
                                                                                        .values(new LinkedHashMap<>())
                                                                                        .build();
        afterDeltaPersonGroupDay.getValues().put("d20210101", AccountGroupValue.builder().amountCreditUsual(200L).build());
        afterDeltaPersonGroupDayMap.put(personGroupOneKey, afterDeltaPersonGroupDay);

        MovementDataPersonGroupDay existingPersonGroupDay = MovementDataPersonGroupDay.builder()
                                                                                      .consultant(123)
                                                                                      .client(345)
                                                                                      .fiscalYear(20210101)
                                                                                      .accountGroupNumber(1)
                                                                                      .accountingReasonId(0)
                                                                                      .values(new LinkedHashMap<>())
                                                                                      .build();
        existingPersonGroupDay.getValues().put("d20210101", AccountGroupValue.builder().amountCreditUsual(100L).build());
        existingPersonGroupDayMap.put(personGroupOneKey, existingPersonGroupDay);

        Map<AccountDbKeyFields, MovementDataPersonGroupDay> actualDifference =

                PersonGroupDeltaCalculation.subtractDays(afterDeltaPersonGroupDayMap, existingPersonGroupDayMap);

        assertThat(actualDifference.get(personGroupOneKey).getValues().get("d20210101").getAmountCreditUsual()).isEqualTo(100L);
    }

    @Test
    void subtractMonths_AfterListHasValues_ResultsInCorrectDifference() {
        Map<AccountDbKeyFields, MovementDataPersonGroupMonth> afterDeltaPersonGroupMonthMap = new LinkedHashMap<>();
        Map<AccountDbKeyFields, MovementDataPersonGroupMonth> existingPersonGroupMonthMap = new LinkedHashMap<>();
        AccountDbKeyFields personGroupOneKey = AccountDbKeyFields.builder().accountNumber(1).accountingReasonId(0).recordType(1).build();
        MovementDataPersonGroupMonth afterDeltaPersonGroupMonth = MovementDataPersonGroupMonth.builder()
                                                                                        .consultant(123)
                                                                                        .client(345)
                                                                                        .fiscalYear(20210101)
                                                                                        .accountGroupNumber(1)
                                                                                        .accountingReasonId(0)
                                                                                        .values(new LinkedHashMap<>())
                                                                                        .build();
        afterDeltaPersonGroupMonth.getValues().put("m1", AccountGroupValue.builder().amountCreditUsual(200L).build());
        afterDeltaPersonGroupMonthMap.put(personGroupOneKey, afterDeltaPersonGroupMonth);

        MovementDataPersonGroupMonth existingPersonGroupMonth = MovementDataPersonGroupMonth.builder()
                                                                                        .consultant(123)
                                                                                        .client(345)
                                                                                        .fiscalYear(20210101)
                                                                                        .accountGroupNumber(1)
                                                                                        .accountingReasonId(0)
                                                                                        .values(new LinkedHashMap<>())
                                                                                        .build();
        existingPersonGroupMonth.getValues().put("m1", AccountGroupValue.builder().amountCreditUsual(100L).build());
        existingPersonGroupMonthMap.put(personGroupOneKey, existingPersonGroupMonth);

        Map<AccountDbKeyFields, MovementDataPersonGroupMonth> actualDifference =

                PersonGroupDeltaCalculation.subtractMonths(afterDeltaPersonGroupMonthMap, existingPersonGroupMonthMap);

        assertThat(actualDifference.get(personGroupOneKey).getValues().get("m1").getAmountCreditUsual()).isEqualTo(100L);
    }


    @Test
    void subtractGroupValues_substractfromMapWithEqualKeyButWithOutValues_shouldResultInNegativeValues() {
        Map<String, AccountGroupValue> emptyMap = Map.of("1", AccountGroupValue.builder().build());
        Map<String, AccountGroupValue> mapWithValues = Map.of("1", AccountGroupValue.builder().amountCreditUsual(100L).build());

        Map<String, AccountGroupValue> actualSubstractionResult = subtractGroupValues(emptyMap, mapWithValues);

        assertThat(actualSubstractionResult).containsOnlyKeys("1");
        assertThat(actualSubstractionResult.get("1").getAmountCreditUsual()).isEqualTo(-100L);
    }

    @Test
    void subtractIndividualPersonAccounts_AllAccountsPresentInBothSets_ShouldReturnEmptySet() {
        Set<Integer> afterDeltaIndividualPersonAccountNumbers = Set.of(1, 2, 3);
        Set<Integer> existingIndividualPersonAccountNumbers = Set.of(1, 2, 3);

        Set<Integer> actual = PersonGroupDeltaCalculation.subtractIndividualPersonAccounts(afterDeltaIndividualPersonAccountNumbers,
                                                                                           existingIndividualPersonAccountNumbers);

        assertThat(actual).isEmpty();
    }



    //Keys only exist in the leftside collection
    @Test
    void subtractGroupValues_substractfromMapWithDifferentKeys_shouldResultInNegativeValuesForTheNonExistingKeys() {
        Map<String, AccountGroupValue> emptyMap = Map.of("1", AccountGroupValue.builder().amountCreditUsual(100L).build());
        Map<String, AccountGroupValue> mapWithValues = Map.of("2", AccountGroupValue.builder().amountCreditUsual(200L).build());

        Map<String, AccountGroupValue> actualSubstractionResult = subtractGroupValues(emptyMap, mapWithValues);

        assertThat(actualSubstractionResult).containsOnlyKeys("1", "2");
        assertThat(actualSubstractionResult.get("1").getAmountCreditUsual()).isEqualTo(100L);
        assertThat(actualSubstractionResult.get("2").getAmountCreditUsual()).isEqualTo(-200L);
    }

    @Test
    void subtractIndividualPersonAccounts_NoCommonAccounts_ShouldReturnAllAfterDeltaAccounts() {
        Set<Integer> afterDeltaIndividualPersonAccountNumbers = Set.of(1, 2, 3);
        Set<Integer> existingIndividualPersonAccountNumbers = Set.of(4, 5, 6);

        Set<Integer> actual = PersonGroupDeltaCalculation.subtractIndividualPersonAccounts(afterDeltaIndividualPersonAccountNumbers,
                                                                                           existingIndividualPersonAccountNumbers);

        assertThat(actual).containsExactlyInAnyOrder(1, 2, 3);
    }

    //Keys only exist in the rightside collection
    @Test
    void subtractIndividualPersonAccounts_SomeCommonAccounts_ShouldReturnNonCommonAfterDeltaAccounts() {
        Set<Integer> afterDeltaIndividualPersonAccountNumbers = Set.of(1, 2, 3, 4);
        Set<Integer> existingIndividualPersonAccountNumbers = Set.of(3, 4, 5, 6);

        Set<Integer> actual = PersonGroupDeltaCalculation.subtractIndividualPersonAccounts(afterDeltaIndividualPersonAccountNumbers,
                                                                                           existingIndividualPersonAccountNumbers);

        assertThat(actual).containsExactlyInAnyOrder(1, 2);
    }

    //Empty Collection on the left side
    @Test
    void substractDays_subtractFromAnEmptyList_ResultsInAnListWithNegativeValues() {

        Map<AccountDbKeyFields, MovementDataPersonGroupDay> afterDeltaPersonGroupDayMap = new LinkedHashMap<>();
        Map<AccountDbKeyFields, MovementDataPersonGroupDay> existingPersonGroupDayMap = new LinkedHashMap<>();
        AccountDbKeyFields personGroupOneKey = AccountDbKeyFields.builder().accountNumber(1).accountingReasonId(0).recordType(1).build();
        MovementDataPersonGroupDay existingPersonGroupDay = MovementDataPersonGroupDay.builder()
                                                                                      .consultant(123)
                                                                                      .client(345)
                                                                                      .fiscalYear(20210101)
                                                                                      .accountGroupNumber(1)
                                                                                      .accountingReasonId(0)
                                                                                      .values(new LinkedHashMap<>())
                                                                                      .build();
        existingPersonGroupDay.getValues().put("d20210101", AccountGroupValue.builder().amountCreditUsual(100L).build());
        existingPersonGroupDayMap.put(personGroupOneKey, existingPersonGroupDay);

        Map<AccountDbKeyFields, MovementDataPersonGroupDay> actualDifference =
                PersonGroupDeltaCalculation.subtractDays(afterDeltaPersonGroupDayMap, existingPersonGroupDayMap);

        assertThat(actualDifference.get(personGroupOneKey).getValues().get("d20210101").getAmountCreditUsual()).isEqualTo(-100L);
    }

    @Test
    void substracMonths_SubstractFromAnEmptyList_ResultsInAnListWithNegativeValues() {

        Map<AccountDbKeyFields, MovementDataPersonGroupMonth> afterDeltaPersonGroupMonthMap = new LinkedHashMap<>();
        Map<AccountDbKeyFields, MovementDataPersonGroupMonth> existingPersonGroupMonthMap = new LinkedHashMap<>();
        AccountDbKeyFields personGroupOneKey = AccountDbKeyFields.builder().accountNumber(1).accountingReasonId(0).recordType(1).build();
        MovementDataPersonGroupMonth existingPersonGroupMonth = MovementDataPersonGroupMonth.builder()
                                                                                          .consultant(123)
                                                                                          .client(345)
                                                                                          .fiscalYear(20210101)
                                                                                          .accountGroupNumber(1)
                                                                                          .accountingReasonId(0)
                                                                                          .values(new LinkedHashMap<>())
                                                                                          .build();
        existingPersonGroupMonth.getValues().put("m1", AccountGroupValue.builder().amountCreditUsual(100L).build());
        existingPersonGroupMonthMap.put(personGroupOneKey, existingPersonGroupMonth);

        Map<AccountDbKeyFields, MovementDataPersonGroupMonth> actualDifference =
                PersonGroupDeltaCalculation.subtractMonths(afterDeltaPersonGroupMonthMap, existingPersonGroupMonthMap);

        assertThat(actualDifference.get(personGroupOneKey).getValues().get("m1").getAmountCreditUsual()).isEqualTo(-100L);
    }

    @Test
    void calculateDifferenceDay_substractFromDayWithOutValues_ExpectResultDayWithNegativeValues() {
        MovementDataPersonGroupDay emptyGroupDay = MovementDataPersonGroupDay.builder().accountGroupNumber(1)
                                                                             .accountingReasonId(0)
                                                                             .additionalParams(null)
                                                                             .client(456)
                                                                             .consultant(123)
                                                                             .fiscalYear(20210101)
                                                                             .values(Map.of())
                                                                             .build();
        MovementDataPersonGroupDay dayToSubstract = MovementDataPersonGroupDay.builder().accountGroupNumber(1)
                                                                              .accountingReasonId(0)
                                                                              .additionalParams(null)
                                                                              .client(456)
                                                                              .consultant(123)
                                                                              .fiscalYear(20210101)
                                                                              .values(Map.of("d20210201", AccountGroupValue.builder()
                                                                                                                           .amountCreditUsual(100L)
                                                                                                                           .build()))
                                                                              .build();
        MovementDataPersonGroupDay actualGroupDayResult = calculateDifferenceDay(emptyGroupDay, dayToSubstract);
        assertThat(actualGroupDayResult.getValues()).containsOnlyKeys("d20210201");
        assertThat(actualGroupDayResult.getValues().get("d20210201").getAmountCreditUsual()).isEqualTo(-100L);
    }

    @Test
    void subtractGroupValues_substractfromEmptyMap_shouldResultInNegativeValues() {
        Map<String, AccountGroupValue> emptyMap = Map.of();
        Map<String, AccountGroupValue> mapWithValues = Map.of("1", AccountGroupValue.builder().amountCreditUsual(100L).build());

        Map<String, AccountGroupValue> actualSubstractionResult = subtractGroupValues(emptyMap, mapWithValues);

        assertThat(actualSubstractionResult).containsOnlyKeys("1");
        assertThat(actualSubstractionResult.get("1").getAmountCreditUsual()).isEqualTo(-100L);
    }

    //Empty Collection on the right side

    //Two Empty Lists

    @Test
    void subtractDays_TwoEmptyLists_ResultsInAnEmptyList() {

        Map<AccountDbKeyFields, MovementDataPersonGroupDay> afterDeltaPersonGroupDayMap = new LinkedHashMap<>();
        Map<AccountDbKeyFields, MovementDataPersonGroupDay> existingPersonGroupDayMap = new LinkedHashMap<>();

        Map<AccountDbKeyFields, MovementDataPersonGroupDay> actualDifference =
                PersonGroupDeltaCalculation.subtractDays(afterDeltaPersonGroupDayMap, existingPersonGroupDayMap);

        assertThat(actualDifference).isEmpty();
    }

    @Test
    void subtractMonths_TwoEmptyLists_ResultsInAnEmptyList() {

        Map<AccountDbKeyFields, MovementDataPersonGroupMonth> afterDeltaPersonGroupMonthMap = new LinkedHashMap<>();
        Map<AccountDbKeyFields, MovementDataPersonGroupMonth> existingPersonGroupMonthMap = new LinkedHashMap<>();

        Map<AccountDbKeyFields, MovementDataPersonGroupMonth> actualDifference =
                PersonGroupDeltaCalculation.subtractMonths(afterDeltaPersonGroupMonthMap, existingPersonGroupMonthMap);

        assertThat(actualDifference).isEmpty();
    }

    //Null values
    @Test
    void subtractDays_NullValuesInBothMaps_ShouldReturnEmptyMap() {
        Map<AccountDbKeyFields, MovementDataPersonGroupDay> afterDeltaPersonGroupDayMap = null;
        Map<AccountDbKeyFields, MovementDataPersonGroupDay> existingPersonGroupDayMap = null;

        Map<AccountDbKeyFields, MovementDataPersonGroupDay> actualDifference =
                PersonGroupDeltaCalculation.subtractDays(afterDeltaPersonGroupDayMap, existingPersonGroupDayMap);

        assertThat(actualDifference).isEmpty();
    }

    @Test
    void subtractDays_NullValuesInAfterDeltaMap_ShouldReturnNegativeValues() {
        Map<AccountDbKeyFields, MovementDataPersonGroupDay> afterDeltaPersonGroupDayMap = null;
        Map<AccountDbKeyFields, MovementDataPersonGroupDay> existingPersonGroupDayMap = new LinkedHashMap<>();
        AccountDbKeyFields personGroupOneKey = AccountDbKeyFields.builder().accountNumber(1).accountingReasonId(0).recordType(1).build();
        MovementDataPersonGroupDay existingPersonGroupDay = MovementDataPersonGroupDay.builder()
                                                                                      .consultant(123)
                                                                                      .client(345)
                                                                                      .fiscalYear(20210101)
                                                                                      .accountGroupNumber(1)
                                                                                      .accountingReasonId(0)
                                                                                      .values(new LinkedHashMap<>())
                                                                                      .build();
        existingPersonGroupDay.getValues().put("d20210101", AccountGroupValue.builder().amountCreditUsual(100L).build());
        existingPersonGroupDayMap.put(personGroupOneKey, existingPersonGroupDay);

        Map<AccountDbKeyFields, MovementDataPersonGroupDay> actualDifference =
                PersonGroupDeltaCalculation.subtractDays(afterDeltaPersonGroupDayMap, existingPersonGroupDayMap);

        assertThat(actualDifference.get(personGroupOneKey).getValues().get("d20210101").getAmountCreditUsual()).isEqualTo(-100L);
    }

    @Test
    void subtractDays_NullValuesInExistingMap_ShouldReturnPositiveValues() {
        Map<AccountDbKeyFields, MovementDataPersonGroupDay> afterDeltaPersonGroupDayMap = new LinkedHashMap<>();
        Map<AccountDbKeyFields, MovementDataPersonGroupDay> existingPersonGroupDayMap = null;
        AccountDbKeyFields personGroupOneKey = AccountDbKeyFields.builder().accountNumber(1).accountingReasonId(0).recordType(1).build();
        MovementDataPersonGroupDay afterDeltaPersonGroupDay = MovementDataPersonGroupDay.builder()
                                                                                        .consultant(123)
                                                                                        .client(345)
                                                                                        .fiscalYear(20210101)
                                                                                        .accountGroupNumber(1)
                                                                                        .accountingReasonId(0)
                                                                                        .values(new LinkedHashMap<>())
                                                                                        .build();
        afterDeltaPersonGroupDay.getValues().put("d20210101", AccountGroupValue.builder().amountCreditUsual(200L).build());
        afterDeltaPersonGroupDayMap.put(personGroupOneKey, afterDeltaPersonGroupDay);

        Map<AccountDbKeyFields, MovementDataPersonGroupDay> actualDifference =
                PersonGroupDeltaCalculation.subtractDays(afterDeltaPersonGroupDayMap, existingPersonGroupDayMap);

        assertThat(actualDifference.get(personGroupOneKey).getValues().get("d20210101").getAmountCreditUsual()).isEqualTo(200L);
    }

    @Test
    void subtractMonths_NullValuesInBothMaps_ShouldReturnEmptyMap() {
        Map<AccountDbKeyFields, MovementDataPersonGroupMonth> afterDeltaPersonGroupMonthMap = null;
        Map<AccountDbKeyFields, MovementDataPersonGroupMonth> existingPersonGroupMonthMap = null;

        Map<AccountDbKeyFields, MovementDataPersonGroupMonth> actualDifference =
                PersonGroupDeltaCalculation.subtractMonths(afterDeltaPersonGroupMonthMap, existingPersonGroupMonthMap);

        assertThat(actualDifference).isEmpty();
    }

    @Test
    void subtractMonths_NullValuesInAfterDeltaMap_ShouldReturnNegativeValues() {
        Map<AccountDbKeyFields, MovementDataPersonGroupMonth> afterDeltaPersonGroupMonthMap = null;
        Map<AccountDbKeyFields, MovementDataPersonGroupMonth> existingPersonGroupMonthMap = new LinkedHashMap<>();
        AccountDbKeyFields personGroupOneKey = AccountDbKeyFields.builder().accountNumber(1).accountingReasonId(0).recordType(1).build();
        MovementDataPersonGroupMonth existingPersonGroupMonth = MovementDataPersonGroupMonth.builder()
                                                                                            .consultant(123)
                                                                                            .client(345)
                                                                                            .fiscalYear(20210101)
                                                                                            .accountGroupNumber(1)
                                                                                            .accountingReasonId(0)
                                                                                            .values(new LinkedHashMap<>())
                                                                                            .build();
        existingPersonGroupMonth.getValues().put("m1", AccountGroupValue.builder().amountCreditUsual(100L).build());
        existingPersonGroupMonthMap.put(personGroupOneKey, existingPersonGroupMonth);

        Map<AccountDbKeyFields, MovementDataPersonGroupMonth> actualDifference =
                PersonGroupDeltaCalculation.subtractMonths(afterDeltaPersonGroupMonthMap, existingPersonGroupMonthMap);

        assertThat(actualDifference.get(personGroupOneKey).getValues().get("m1").getAmountCreditUsual()).isEqualTo(-100L);
    }

    @Test
    void subtractMonths_NullValuesInExistingMap_ShouldReturnPositiveValues() {
        Map<AccountDbKeyFields, MovementDataPersonGroupMonth> afterDeltaPersonGroupMonthMap = new LinkedHashMap<>();
        Map<AccountDbKeyFields, MovementDataPersonGroupMonth> nullMonthMap = null;
        AccountDbKeyFields personGroupOneKey = AccountDbKeyFields.builder().accountNumber(1).accountingReasonId(0).recordType(1).build();
        MovementDataPersonGroupMonth afterDeltaPersonGroupMonth = MovementDataPersonGroupMonth.builder()
                                                                                              .consultant(123)
                                                                                              .client(345)
                                                                                              .fiscalYear(20210101)
                                                                                              .accountGroupNumber(1)
                                                                                              .accountingReasonId(0)
                                                                                              .values(new LinkedHashMap<>())
                                                                                              .build();
        afterDeltaPersonGroupMonth.getValues().put("m1", AccountGroupValue.builder().amountCreditUsual(200L).build());
        afterDeltaPersonGroupMonthMap.put(personGroupOneKey, afterDeltaPersonGroupMonth);

        Map<AccountDbKeyFields, MovementDataPersonGroupMonth> actualDifference =
                PersonGroupDeltaCalculation.subtractMonths(afterDeltaPersonGroupMonthMap, nullMonthMap);

        assertThat(actualDifference.get(personGroupOneKey).getValues().get("m1").getAmountCreditUsual()).isEqualTo(200L);
    }
}