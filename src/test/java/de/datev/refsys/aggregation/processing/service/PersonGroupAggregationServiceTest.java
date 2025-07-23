package de.datev.refsys.aggregation.processing.service;

import de.datev.refsys.generated.acds.api.model.AccountSumDay;
import de.datev.refsys.generated.acds.api.model.MasterdataContext;
import de.datev.refsys.aggregation.document.model.AccountGroupValue;
import de.datev.refsys.aggregation.document.model.AccountValue;
import de.datev.refsys.aggregation.document.model.AdditionalParameters;
import de.datev.refsys.aggregation.document.model.MovementDataDay;
import de.datev.refsys.aggregation.document.model.MovementDataPersonGroupDay;
import de.datev.refsys.aggregation.processing.configuration.ImportServiceConfiguration;
import de.datev.refsys.aggregation.processing.mapper.AccountDbKeyFieldsMapper;
import de.datev.refsys.aggregation.processing.mapper.AccountValueMapper;
import de.datev.refsys.aggregation.processing.mapper.AdditionalParametersMapper;
import de.datev.refsys.aggregation.processing.model.AccountDbKeyFields;
import de.datev.refsys.aggregation.processing.util.Util;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { ImportServiceConfiguration.class })
class PersonGroupAggregationServiceTest {

    public static final int CONSULTANT = 100000;
    public static final int CLIENT = 10;
    public static final int YEAR_BEGIN = 20210101;
    public static final int KREDITOR_GROUP = 1;
    public static final int DEBITOR_GROUP = 7;

    @Autowired
    private AccountValueMapper accountValueMapper;

    @Autowired
    private AccountDbKeyFieldsMapper accountDbKeyFieldsMapper;

    @Autowired
    @Qualifier("additionalParametersMapper")
    private AdditionalParametersMapper additionParametersMapper;

    //Standardfall Personenkonto ohne Besonderheiten
    @Test
    void testKreditorUsual_inputDayValuesWithMoreCreditThanDebitAmounts_expectOnlyUsualValues() {
        PersonGroupAggregationService objectUnderTest = createObjectUnderTest();
        MasterdataContext masterdataContext = createDefaultMasterdataContext();

        Integer accountNumber = 101000000;
        Integer accountGroupNumber = KREDITOR_GROUP;

        AccountSumDay march = AccountSumDay.builder().date(20210301).accountNumber(accountNumber).amountCredit(20522.17).build();
        AccountSumDay firstApril = AccountSumDay.builder().date(20210401).accountNumber(accountNumber).amountDebit(6883.85).build();
        AccountSumDay secondApril = AccountSumDay.builder().date(20210402).accountNumber(accountNumber).amountDebit(9373.43).build();
        AccountSumDay may = AccountSumDay.builder().date(20210501).accountNumber(accountNumber).amountCredit(6637.69).build();
        AccountSumDay june = AccountSumDay.builder().date(20210601).accountNumber(accountNumber).amountCredit(9619.59).build();
        AccountSumDay july = AccountSumDay.builder().date(20210701).accountNumber(accountNumber).amountDebit(16465.6).build();

        // Add all days to the AccountSumList
        Map<String, List<AccountSumDay>> accountSumDays = new TreeMap<>();
        accountSumDays.put("d" + 20210301, List.of(march));
        accountSumDays.put("d" + 20210401, List.of(firstApril));
        accountSumDays.put("d" + 20210402, List.of(secondApril));
        accountSumDays.put("d" + 20210501, List.of(may));
        accountSumDays.put("d" + 20210601, List.of(june));
        accountSumDays.put("d" + 20210701, List.of(july));

        Map<AccountDbKeyFields, MovementDataPersonGroupDay> actualPersonGroupDays =
                doPersonGroupCalculation(masterdataContext, accountSumDays, objectUnderTest, accountGroupNumber);

        // Check for general sanity
        assertThat(actualPersonGroupDays).as("should be one person group").hasSize(1);
        AccountDbKeyFields accountDbGroupFields = accountDbKeyFieldsMapper.accountSumDayToGroupAccountDbKeyFields(march);
        assertThat(actualPersonGroupDays.get(accountDbGroupFields).getValues()).as("The persongroup should have %d day Values", accountSumDays.size())
                                                                               .hasSize(accountSumDays.size());

        assertThat(actualPersonGroupDays).as("There should be no unusual Values").allSatisfy((dbKey, personGroupDay) -> {
            assertThat(personGroupDay.getValues()).allSatisfy((s, accountGroupValue) -> {
                assertThat(accountGroupValue.getAmountDebitUnusual()).as("there should be no unusual debits for %s", s).isNull();
                assertThat(accountGroupValue.getAmountCreditUnusual()).as("there should be no unusual credits for %s", s).isNull();
            });
        });
    }

    @Test
    void testDebitorUsual_inputDayValuesWithMoreDebitThanCreditAmounts_expectOnlyUsualValues() {

        PersonGroupAggregationService objectUnderTest = createObjectUnderTest();
        MasterdataContext masterdataContext = createDefaultMasterdataContext();

        Integer accountNumber = 701000000;
        Integer accountGroupNumber = DEBITOR_GROUP;

        AccountSumDay march = AccountSumDay.builder().date(20210301).accountNumber(accountNumber).amountDebit(20522.17).build();
        AccountSumDay firstApril = AccountSumDay.builder().date(20210401).accountNumber(accountNumber).amountCredit(6883.85).build();
        AccountSumDay secondApril = AccountSumDay.builder().date(20210402).accountNumber(accountNumber).amountCredit(9373.43).build();
        AccountSumDay may = AccountSumDay.builder().date(20210501).accountNumber(accountNumber).amountDebit(6637.69).build();
        AccountSumDay june = AccountSumDay.builder().date(20210601).accountNumber(accountNumber).amountDebit(9619.59).build();
        AccountSumDay july = AccountSumDay.builder().date(20210701).accountNumber(accountNumber).amountCredit(16465.6).build();

        // Add all days to the AccountSumList
        Map<String, List<AccountSumDay>> accountSumDays = new TreeMap<>();
        accountSumDays.put("d" + 20210301, List.of(march));
        accountSumDays.put("d" + 20210401, List.of(firstApril));
        accountSumDays.put("d" + 20210402, List.of(secondApril));
        accountSumDays.put("d" + 20210501, List.of(may));
        accountSumDays.put("d" + 20210601, List.of(june));
        accountSumDays.put("d" + 20210701, List.of(july));

        Map<AccountDbKeyFields, MovementDataPersonGroupDay> actualPersonGroupDays =
                doPersonGroupCalculation(masterdataContext, accountSumDays, objectUnderTest, accountGroupNumber);

        // Check for general sanity
        assertThat(actualPersonGroupDays).as("should be one person group").hasSize(1);
        AccountDbKeyFields accountDbGroupFields = accountDbKeyFieldsMapper.accountSumDayToGroupAccountDbKeyFields(march);
        assertThat(actualPersonGroupDays.get(accountDbGroupFields).getValues()).as("The persongroup should have %d day Values", accountSumDays.size())
                                                                               .hasSize(accountSumDays.size());

        assertThat(actualPersonGroupDays).as("There should be no unusual Values").allSatisfy((dbKey, personGroupDay) -> {
            assertThat(personGroupDay.getValues()).allSatisfy((s, accountGroupValue) -> {
                assertThat(accountGroupValue.getAmountDebitUnusual()).as("there should be no unusual debits for %s", s).isNull();
                assertThat(accountGroupValue.getAmountCreditUnusual()).as("there should be no unusual credits for %s", s).isNull();
            });
        });
    }

    @Test
    void testKreditorUsual_inputDayValuesWithMoreDebitThanCreditAmounts_expectSwitchToUnusual() {
        PersonGroupAggregationService objectUnderTest = createObjectUnderTest();
        MasterdataContext masterdataContext = createDefaultMasterdataContext();

        Integer accountNumber = 101000000;
        Integer accountGroupNumber = KREDITOR_GROUP;

        int firstDay = 20210301;
        int secondDay = 20210401;
        int thirdDay = 20210402;
        AccountSumDay march = AccountSumDay.builder().date(firstDay).accountNumber(accountNumber).amountCredit(20000.00).build();
        AccountSumDay firstApril = AccountSumDay.builder().date(secondDay).accountNumber(accountNumber).amountDebit(20000.00).build();
        AccountSumDay secondApril = AccountSumDay.builder().date(thirdDay).accountNumber(accountNumber).amountDebit(0.01).build();

        // Add all days to the AccountSumList
        Map<String, List<AccountSumDay>> accountSumDays = new TreeMap<>();
        accountSumDays.put("d" + 20210301, List.of(march));
        accountSumDays.put("d" + 20210401, List.of(firstApril));
        accountSumDays.put("d" + 20210402, List.of(secondApril));

        Map<AccountDbKeyFields, MovementDataPersonGroupDay> actualPersonGroupDays =
                doPersonGroupCalculation(masterdataContext, accountSumDays, objectUnderTest, accountGroupNumber);

        // Get Key for account
        AccountDbKeyFields accountKey = accountDbKeyFieldsMapper.accountSumDayToGroupAccountDbKeyFields(march);

        // Check individual Day Values for the single account
        Map<String, AccountGroupValue> movementDataPersonGroupDay = actualPersonGroupDays.get(accountKey).getValues();
        assertThat(movementDataPersonGroupDay.get("d" + firstDay)).as("firstEntry should have usual Values")
                                                                  .hasFieldOrPropertyWithValue("amountCreditUsual", 2000000L)
                                                                  .hasFieldOrPropertyWithValue("amountDebitUsual", null)
                                                                  .hasFieldOrPropertyWithValue("amountCreditUnusual", null)
                                                                  .hasFieldOrPropertyWithValue("amountDebitUnusual", null);

        assertThat(movementDataPersonGroupDay.get("d" + secondDay)).as("Second Day balance is zero - should still be usual ")
                                                                   .hasFieldOrPropertyWithValue("amountCreditUsual", null)
                                                                   .hasFieldOrPropertyWithValue("amountDebitUsual", 2000000L)
                                                                   .hasFieldOrPropertyWithValue("amountCreditUnusual", null)
                                                                   .hasFieldOrPropertyWithValue("amountDebitUnusual", null);

        assertThat(movementDataPersonGroupDay.get("d" + thirdDay)).as("Third Day should have unusual Values")
                                                                  .hasFieldOrPropertyWithValue("amountCreditUsual", -2000000L)
                                                                  .hasFieldOrPropertyWithValue("amountDebitUsual", -2000000L)
                                                                  .hasFieldOrPropertyWithValue("amountCreditUnusual", 2000000L)
                                                                  .hasFieldOrPropertyWithValue("amountDebitUnusual", 2000001L);
    }

    @Test
    void testDebitorUsual_inputDayValuesWithMoreDebitThanCreditAmounts_expectSwitchToUnusual() {
        PersonGroupAggregationService objectUnderTest = createObjectUnderTest();
        MasterdataContext masterdataContext = createDefaultMasterdataContext();

        Integer accountNumber = 701000000;
        Integer accountGroupNumber = DEBITOR_GROUP;

        int firstDay = 20210301;
        int secondDay = 20210401;
        int thirdDay = 20210402;
        AccountSumDay march = AccountSumDay.builder().date(firstDay).accountNumber(accountNumber).amountDebit(20000.00).build();
        AccountSumDay firstApril = AccountSumDay.builder().date(secondDay).accountNumber(accountNumber).amountCredit(20000.00).build();
        AccountSumDay secondApril = AccountSumDay.builder().date(thirdDay).accountNumber(accountNumber).amountCredit(0.01).build();

        // Add all days to the AccountSumList
        Map<String, List<AccountSumDay>> accountSumDays = new TreeMap<>();
        accountSumDays.put("d" + 20210301, List.of(march));
        accountSumDays.put("d" + 20210401, List.of(firstApril));
        accountSumDays.put("d" + 20210402, List.of(secondApril));

        Map<AccountDbKeyFields, MovementDataPersonGroupDay> actualPersonGroupDays =
                doPersonGroupCalculation(masterdataContext, accountSumDays, objectUnderTest, accountGroupNumber);

        // Get Key for account
        AccountDbKeyFields accountKey = accountDbKeyFieldsMapper.accountSumDayToGroupAccountDbKeyFields(march);

        // Check individual Day Values for the single account
        Map<String, AccountGroupValue> movementDataPersonGroupDay = actualPersonGroupDays.get(accountKey).getValues();
        assertThat(movementDataPersonGroupDay.get("d" + firstDay)).as("firstEntry should have usual Values")
                                                                  .hasFieldOrPropertyWithValue("amountCreditUsual", null)
                                                                  .hasFieldOrPropertyWithValue("amountDebitUsual", 2000000L)
                                                                  .hasFieldOrPropertyWithValue("amountCreditUnusual", null)
                                                                  .hasFieldOrPropertyWithValue("amountDebitUnusual", null);

        assertThat(movementDataPersonGroupDay.get("d" + secondDay)).as("Second Day balance is zero - should still be usual ")
                                                                   .hasFieldOrPropertyWithValue("amountCreditUsual", 2000000L)
                                                                   .hasFieldOrPropertyWithValue("amountDebitUsual", null)
                                                                   .hasFieldOrPropertyWithValue("amountCreditUnusual", null)
                                                                   .hasFieldOrPropertyWithValue("amountDebitUnusual", null);

        assertThat(movementDataPersonGroupDay.get("d" + thirdDay)).as("Third Day should have unusual Values")
                                                                  .hasFieldOrPropertyWithValue("amountCreditUsual", -2000000L)
                                                                  .hasFieldOrPropertyWithValue("amountDebitUsual", -2000000L)
                                                                  .hasFieldOrPropertyWithValue("amountCreditUnusual", 2000001L)
                                                                  .hasFieldOrPropertyWithValue("amountDebitUnusual", 2000000L);
    }

    @Test
    void testKreditorUnusual_inputDayValuesToEqualOutTheDebit_expectSwitchToUsual() {
        PersonGroupAggregationService objectUnderTest = createObjectUnderTest();
        MasterdataContext masterdataContext = createDefaultMasterdataContext();

        Integer accountNumber = 101000000;
        Integer accountGroupNumber = KREDITOR_GROUP;

        int firstDay = 20210301;
        int secondDay = 20210401;
        int thirdDay = 20210402;
        AccountSumDay march = AccountSumDay.builder().date(firstDay).accountNumber(accountNumber).amountDebit(20000.00).build();
        AccountSumDay firstApril = AccountSumDay.builder().date(secondDay).accountNumber(accountNumber).amountCredit(19999.99).build();
        AccountSumDay secondApril = AccountSumDay.builder().date(thirdDay).accountNumber(accountNumber).amountCredit(0.01).build();

        // Add all days to the AccountSumList
        Map<String, List<AccountSumDay>> accountSumDays = new TreeMap<>();
        accountSumDays.put("d" + 20210301, List.of(march));
        accountSumDays.put("d" + 20210401, List.of(firstApril));
        accountSumDays.put("d" + 20210402, List.of(secondApril));

        Map<AccountDbKeyFields, MovementDataPersonGroupDay> actualPersonGroupDays =
                doPersonGroupCalculation(masterdataContext, accountSumDays, objectUnderTest, accountGroupNumber);

        // Get Key for account
        AccountDbKeyFields accountKey = accountDbKeyFieldsMapper.accountSumDayToGroupAccountDbKeyFields(march);

        // Check individual Day Values for the single account
        Map<String, AccountGroupValue> movementDataPersonGroupDay = actualPersonGroupDays.get(accountKey).getValues();
        assertThat(movementDataPersonGroupDay.get("d" + firstDay)).as("firstEntry should have unusual Values")
                                                                  .hasFieldOrPropertyWithValue("amountCreditUsual", null)
                                                                  .hasFieldOrPropertyWithValue("amountDebitUsual", null)
                                                                  .hasFieldOrPropertyWithValue("amountCreditUnusual", null)
                                                                  .hasFieldOrPropertyWithValue("amountDebitUnusual", 2000000L);

        assertThat(movementDataPersonGroupDay.get("d" + secondDay)).as("Second Day balance is still be unusual ")
                                                                   .hasFieldOrPropertyWithValue("amountCreditUsual", null)
                                                                   .hasFieldOrPropertyWithValue("amountDebitUsual", null)
                                                                   .hasFieldOrPropertyWithValue("amountCreditUnusual", 1999999L)
                                                                   .hasFieldOrPropertyWithValue("amountDebitUnusual", null);

        assertThat(movementDataPersonGroupDay.get("d" + thirdDay)).as("Third Day should equal to zero and therefore have usual Values")
                                                                  .hasFieldOrPropertyWithValue("amountCreditUsual", 2000000L)
                                                                  .hasFieldOrPropertyWithValue("amountDebitUsual", 2000000L)
                                                                  .hasFieldOrPropertyWithValue("amountCreditUnusual", -1999999L)
                                                                  .hasFieldOrPropertyWithValue("amountDebitUnusual", -2000000L);
    }

    @Test
    void testDebitorUnusual_inputDayValuesToEqualOutTheCredit_expectSwitchToUsual() {
        PersonGroupAggregationService objectUnderTest = createObjectUnderTest();
        MasterdataContext masterdataContext = createDefaultMasterdataContext();

        Integer accountNumber = 701000000;
        Integer accountGroupNumber = DEBITOR_GROUP;

        int firstDay = 20210301;
        int secondDay = 20210401;
        int thirdDay = 20210402;
        AccountSumDay march = AccountSumDay.builder().date(firstDay).accountNumber(accountNumber).amountCredit(20000.00).build();
        AccountSumDay firstApril = AccountSumDay.builder().date(secondDay).accountNumber(accountNumber).amountDebit(19999.99).build();
        AccountSumDay secondApril = AccountSumDay.builder().date(thirdDay).accountNumber(accountNumber).amountDebit(0.01).build();

        // Add all days to the AccountSumList
        Map<String, List<AccountSumDay>> accountSumDays = new TreeMap<>();
        accountSumDays.put("d" + 20210301, List.of(march));
        accountSumDays.put("d" + 20210401, List.of(firstApril));
        accountSumDays.put("d" + 20210402, List.of(secondApril));

        Map<AccountDbKeyFields, MovementDataPersonGroupDay> actualPersonGroupDays =
                doPersonGroupCalculation(masterdataContext, accountSumDays, objectUnderTest, accountGroupNumber);

        // Get Key for account
        AccountDbKeyFields accountKey = accountDbKeyFieldsMapper.accountSumDayToGroupAccountDbKeyFields(march);

        // Check individual Day Values for the single account
        Map<String, AccountGroupValue> movementDataPersonGroupDay = actualPersonGroupDays.get(accountKey).getValues();
        assertThat(movementDataPersonGroupDay.get("d" + firstDay)).as("firstEntry should have unusual Values")
                                                                  .hasFieldOrPropertyWithValue("amountCreditUsual", null)
                                                                  .hasFieldOrPropertyWithValue("amountDebitUsual", null)
                                                                  .hasFieldOrPropertyWithValue("amountCreditUnusual", 2000000L)
                                                                  .hasFieldOrPropertyWithValue("amountDebitUnusual", null);

        assertThat(movementDataPersonGroupDay.get("d" + secondDay)).as("Second Day balance is still be unusual ")
                                                                   .hasFieldOrPropertyWithValue("amountCreditUsual", null)
                                                                   .hasFieldOrPropertyWithValue("amountDebitUsual", null)
                                                                   .hasFieldOrPropertyWithValue("amountCreditUnusual", null)
                                                                   .hasFieldOrPropertyWithValue("amountDebitUnusual", 1999999L);

        assertThat(movementDataPersonGroupDay.get("d" + thirdDay)).as("Third Day should equal to zero and therefore have usual Values")
                                                                  .hasFieldOrPropertyWithValue("amountCreditUsual", 2000000L)
                                                                  .hasFieldOrPropertyWithValue("amountDebitUsual", 2000000L)
                                                                  .hasFieldOrPropertyWithValue("amountCreditUnusual", -2000000L)
                                                                  .hasFieldOrPropertyWithValue("amountDebitUnusual", -1999999L);
    }

    @Test
    void testKreditorUsual_inputMultipleDayValuesWithUnusalSwitch_expectDayToKeepAtUsual() {
        PersonGroupAggregationService objectUnderTest = createObjectUnderTest();
        MasterdataContext masterdataContext = createDefaultMasterdataContext();

        Integer accountNumber = 101000000;
        Integer accountGroupNumber = KREDITOR_GROUP;

        int day = 20210301;
        int existingDay = 20210228;
        AccountSumDay existingUsual = AccountSumDay.builder().date(existingDay).accountNumber(accountNumber).amountCredit(1_000.00).build();
        AccountSumDay firstUsual = AccountSumDay.builder().date(day).accountNumber(accountNumber).amountCredit(20_000.00).build();
        AccountSumDay secondUnusual = AccountSumDay.builder().date(day).accountNumber(accountNumber).amountDebit(30_000.00).build();
        AccountSumDay thirdUsual = AccountSumDay.builder().date(day).accountNumber(accountNumber).amountCredit(10_000.00).build();

        // Add all days to the AccountSumList
        Map<String, List<AccountSumDay>> accountSumDays = new TreeMap<>();
        accountSumDays.put("d" + existingDay, List.of(existingUsual));
        accountSumDays.put("d" + day, List.of(firstUsual, secondUnusual, thirdUsual));

        Map<AccountDbKeyFields, MovementDataPersonGroupDay> actualPersonGroupDays =
                doPersonGroupCalculation(masterdataContext, accountSumDays, objectUnderTest, accountGroupNumber);

        // Get Key for account
        AccountDbKeyFields accountKey = accountDbKeyFieldsMapper.accountSumDayToGroupAccountDbKeyFields(existingUsual);

        // Check individual Day Values for the single account
        Map<String, AccountGroupValue> movementDataPersonGroupDay = actualPersonGroupDays.get(accountKey).getValues();
        assertThat(movementDataPersonGroupDay.get("d" + existingDay)).as("existingEntry should have usual Values")
                                                                     .hasFieldOrPropertyWithValue("amountCreditUsual", 100_000L)
                                                                     .hasFieldOrPropertyWithValue("amountDebitUsual", null)
                                                                     .hasFieldOrPropertyWithValue("amountCreditUnusual", null)
                                                                     .hasFieldOrPropertyWithValue("amountDebitUnusual", null);

        assertThat(movementDataPersonGroupDay.get("d" + day)).as("Day values should be aggregated and still be usual ")
                                                             .hasFieldOrPropertyWithValue("amountCreditUsual", 3_000_000L)
                                                             .hasFieldOrPropertyWithValue("amountDebitUsual", 3_000_000L)
                                                             .hasFieldOrPropertyWithValue("amountCreditUnusual", null)
                                                             .hasFieldOrPropertyWithValue("amountDebitUnusual", null);
    }

    @Test
    void testKreditorUnUsual_inputMultipleDayValuesinCredit_expectSwitchToUsual() {
        PersonGroupAggregationService objectUnderTest = createObjectUnderTest();
        MasterdataContext masterdataContext = createDefaultMasterdataContext();

        Integer accountNumber = 101000000;
        Integer accountGroupNumber = KREDITOR_GROUP;

        int day = 20210301;
        int existingDay = 20210228;
        AccountSumDay existingUsual = AccountSumDay.builder().date(existingDay).accountNumber(accountNumber).amountDebit(1_000.00).build();
        AccountSumDay firstUsual = AccountSumDay.builder().date(day).accountNumber(accountNumber).amountCredit(21_000.00).build();
        AccountSumDay secondUnusual = AccountSumDay.builder().date(day).accountNumber(accountNumber).amountDebit(30_000.00).build();
        AccountSumDay thirdUsual = AccountSumDay.builder().date(day).accountNumber(accountNumber).amountCredit(10_000.00).build();

        // Add all days to the AccountSumList
        Map<String, List<AccountSumDay>> accountSumDays = new TreeMap<>();
        accountSumDays.put("d" + existingDay, List.of(existingUsual));
        accountSumDays.put("d" + day, List.of(firstUsual, secondUnusual, thirdUsual));

        Map<AccountDbKeyFields, MovementDataPersonGroupDay> actualPersonGroupDays =
                doPersonGroupCalculation(masterdataContext, accountSumDays, objectUnderTest, accountGroupNumber);

        // Get Key for account
        AccountDbKeyFields accountKey = accountDbKeyFieldsMapper.accountSumDayToGroupAccountDbKeyFields(existingUsual);

        // Check individual Day Values for the single account
        Map<String, AccountGroupValue> movementDataPersonGroupDay = actualPersonGroupDays.get(accountKey).getValues();
        assertThat(movementDataPersonGroupDay.get("d" + existingDay)).as("existingEntry should have unusual Values")
                                                                     .hasFieldOrPropertyWithValue("amountCreditUsual", null)
                                                                     .hasFieldOrPropertyWithValue("amountDebitUsual", null)
                                                                     .hasFieldOrPropertyWithValue("amountCreditUnusual", null)
                                                                     .hasFieldOrPropertyWithValue("amountDebitUnusual", 100_000L);

        assertThat(movementDataPersonGroupDay.get("d" + day)).as("Day values should be aggregated and back to usual ")
                                                             .hasFieldOrPropertyWithValue("amountCreditUsual", 3_100_000L)
                                                             .hasFieldOrPropertyWithValue("amountDebitUsual", 3_100_000L)
                                                             .hasFieldOrPropertyWithValue("amountCreditUnusual", null)
                                                             .hasFieldOrPropertyWithValue("amountDebitUnusual", -100_000L);
    }

    @Test
    void test_differentAccountingReasons_shouldbeConsideredForUsualUnusual() {
        PersonGroupAggregationService objectUnderTest = createObjectUnderTest();
        MasterdataContext masterdataContext = createDefaultMasterdataContext();

        Integer accountNumber = 101000000;
        Integer accountGroupNumber = KREDITOR_GROUP;

        int day = 20210301;
        int dayWithDifferentAccountingReason = 20210401;
        AccountSumDay firstUsual = AccountSumDay.builder().date(day).accountNumber(accountNumber).amountCredit(21_000.00).build();
        AccountSumDay secondUnusualWithDifferentAccountingReason =
                AccountSumDay.builder().date(day).accountNumber(accountNumber).accountingReasonId(2).amountDebit(30_000.00).build();

        // Add all days to the AccountSumList
        Map<String, List<AccountSumDay>> accountSumDays = new TreeMap<>();
        accountSumDays.put("d" + day, List.of(firstUsual));
        accountSumDays.put("d" + dayWithDifferentAccountingReason, List.of(secondUnusualWithDifferentAccountingReason));

        Map<AccountDbKeyFields, MovementDataPersonGroupDay> actualPersonGroupDays =
                doPersonGroupCalculation(masterdataContext, accountSumDays, objectUnderTest, accountGroupNumber);

        // Get Key for account with default accountingReason
        AccountDbKeyFields accountKey = accountDbKeyFieldsMapper.accountSumDayToGroupAccountDbKeyFields(firstUsual);

        Map<String, AccountGroupValue> movementDataPersonGroupDay = actualPersonGroupDays.get(accountKey).getValues();
        assertThat(movementDataPersonGroupDay.get("d" + day)).as("first Entry with default accounting Reason should have usual Values")
                                                             .hasFieldOrPropertyWithValue("amountCreditUsual", 2_100_000L)
                                                             .hasFieldOrPropertyWithValue("amountDebitUsual", null)
                                                             .hasFieldOrPropertyWithValue("amountCreditUnusual", null)
                                                             .hasFieldOrPropertyWithValue("amountDebitUnusual", null);

        assertThat(movementDataPersonGroupDay.get("d" + dayWithDifferentAccountingReason))
                .as("Day value for default accounting Reason should "
                            + "be moved to unusual, besides only bookings with other AccountingReasons not visible here")
                .hasFieldOrPropertyWithValue("amountCreditUsual", -2_100_000L)
                .hasFieldOrPropertyWithValue("amountDebitUsual", null)
                .hasFieldOrPropertyWithValue("amountCreditUnusual",
                                             2_100_000L)
                .hasFieldOrPropertyWithValue("amountDebitUnusual", null);
    }

    @Test
    void test_differentAccountingReasons_accountingReasonDifferentFromDefaultShouldHaveOwnValues() {
        PersonGroupAggregationService objectUnderTest = createObjectUnderTest();
        MasterdataContext masterdataContext = createDefaultMasterdataContext();

        Integer accountNumber = 101000000;
        Integer accountGroupNumber = KREDITOR_GROUP;

        int day = 20210301;
        int dayWithDifferentAccountingReason = 20210401;
        AccountSumDay firstUsual = AccountSumDay.builder().date(day).accountNumber(accountNumber).amountCredit(21_000.00).build();
        AccountSumDay secondUnusualWithDifferentAccountingReason =
                AccountSumDay.builder().date(day).accountNumber(accountNumber).accountingReasonId(2).amountDebit(30_000.00).build();

        // Add all days to the AccountSumList
        Map<String, List<AccountSumDay>> accountSumDays = new TreeMap<>();
        accountSumDays.put("d" + day, List.of(firstUsual));
        accountSumDays.put("d" + dayWithDifferentAccountingReason, List.of(secondUnusualWithDifferentAccountingReason));

        Map<AccountDbKeyFields, MovementDataPersonGroupDay> actualPersonGroupDays =
                doPersonGroupCalculation(masterdataContext, accountSumDays, objectUnderTest, accountGroupNumber);

        // Get Key for account with different accountingReason
        AccountDbKeyFields accountKey = accountDbKeyFieldsMapper.accountSumDayToGroupAccountDbKeyFields(secondUnusualWithDifferentAccountingReason);

        Map<String, AccountGroupValue> movementDataPersonGroupDay = actualPersonGroupDays.get(accountKey).getValues();
        assertThat(movementDataPersonGroupDay).as("For different accounting Reason key no entry should exist for day with only default reason")
                                              .doesNotContainKey("d" + day);

        assertThat(movementDataPersonGroupDay.get("d" + dayWithDifferentAccountingReason))
                .as("Day value for different reason should be unusual, containing only values with this reason")
                .hasFieldOrPropertyWithValue("amountCreditUsual", null)
                .hasFieldOrPropertyWithValue("amountDebitUsual", null)
                .hasFieldOrPropertyWithValue("amountCreditUnusual", null)
                .hasFieldOrPropertyWithValue("amountDebitUnusual",
                                             3_000_000L);
    }



    private Map<AccountDbKeyFields, MovementDataPersonGroupDay> doPersonGroupCalculation(MasterdataContext masterdataContext,
                                                                                         Map<String, List<AccountSumDay>> accountSumDays,
                                                                                         PersonGroupAggregationService objectUnderTest,
                                                                                         Integer accountGroupNumber) {
        Map<AccountDbKeyFields, MovementDataPersonGroupDay> actualPersonGroupDays = new HashMap<>();

        //Map for the existing movement data, has to be pre populated with all the values for the accounts
        Map<AccountDbKeyFields, MovementDataDay> accountDayMap = new HashMap<>();
        accountSumDays.forEach((dayKey, dayValues) -> {
            dayValues.forEach(dayValue -> {
                AccountDbKeyFields accountDbKeyFields = accountDbKeyFieldsMapper.accountSumDayToAccountDbKeyFields(dayValue);
                AdditionalParameters parameters = additionParametersMapper.accountSumDayToAdditionalParameters(dayValue);
                addToDayMap(masterdataContext, accountDayMap, parameters, accountDbKeyFields, dayValue, dayKey);
            });
        });

        accountSumDays.forEach((dayKey, dayValues) -> {
            dayValues.forEach(dayValue -> {
                objectUnderTest.addToDebitSum(dayValue.getAmountCredit());
                objectUnderTest.addToCreditSum(dayValue.getAmountDebit());
            });

            objectUnderTest.aggregatePersonGroupsDays(masterdataContext, accountGroupNumber, dayValues, actualPersonGroupDays, dayKey, accountDayMap);
            objectUnderTest.resetUsualChanged();

        });
        return actualPersonGroupDays;
    }

    private static MasterdataContext createDefaultMasterdataContext() {
        return MasterdataContext.builder().consultant(CONSULTANT).client(CLIENT).yearBegin(YEAR_BEGIN).build();
    }

    private PersonGroupAggregationService createObjectUnderTest() {
        return new PersonGroupAggregationService(accountValueMapper, accountDbKeyFieldsMapper, additionParametersMapper);
    }

    private void addToDayMap(MasterdataContext mdc, Map<AccountDbKeyFields, MovementDataDay> accountDayMap, AdditionalParameters additionalParameters,
                             AccountDbKeyFields accountMapKey, AccountSumDay accountSumDay, String dayKey) {
        if (accountDayMap.containsKey(accountMapKey)) {
            MovementDataDay movementDataDay = accountDayMap.get(accountMapKey);
            if (movementDataDay.getValues().containsKey(dayKey)) {
                incrementAccountValue(movementDataDay.getValues().get(dayKey), accountSumDay);
            } else {
                movementDataDay.getValues().put(dayKey, accountValueMapper.accountSumDayToAccountValue(accountSumDay));
            }
        } else {
            accountDayMap.put(accountMapKey, createMovementDataDay(mdc, additionalParameters, accountSumDay, dayKey));
        }
    }

    private MovementDataDay createMovementDataDay(MasterdataContext mdc, AdditionalParameters additionalParameters, AccountSumDay accountSumDay,
                                                  String dayKey) {
        return MovementDataDay.builder().consultant(mdc.getConsultant()).client(mdc.getClient()).fiscalYear(mdc.getYearBegin())
                              .accountNumber(accountSumDay.getAccountNumber()).accountingReasonId(accountSumDay.getAccountingReasonId())
                              .additionalParams(additionalParameters)
                              .values(new LinkedHashMap<>(Map.of(dayKey, accountValueMapper.accountSumDayToAccountValue(accountSumDay)))).build();
    }

    protected void incrementAccountValue(AccountValue accountValue, AccountSumDay accountSumDay) {
        accountValue.addAmountCredit(Util.amountToLongInCent(accountSumDay.getAmountCredit()));
        accountValue.addAmountDebit(Util.amountToLongInCent(accountSumDay.getAmountDebit()));
        accountValue.addQuantityCredit(accountSumDay.getQuantityCredit());
        accountValue.addQuantityDebit(accountSumDay.getQuantityDebit());
        accountValue.addWeightCredit(Util.amountToLongInCent(accountSumDay.getWeightCredit()));
        accountValue.addWeightDebit(Util.amountToLongInCent(accountSumDay.getWeightDebit()));
    }

    private static boolean personAccountCantBeGrouped(AccountSumDay asd) {
        return asd.getAccountingReasonId() != 0 ||
                Optional.ofNullable(asd.getAmountCredit()).orElse(0.00) < 0 ||
                Optional.ofNullable(asd.getAmountDebit()).orElse(0.00) < 0;
    }

}