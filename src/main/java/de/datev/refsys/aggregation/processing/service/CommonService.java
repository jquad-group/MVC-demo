package de.datev.refsys.aggregation.processing.service;

import de.datev.refsys.aggregation.document.model.AccountValue;
import de.datev.refsys.aggregation.document.model.AdditionalParameters;
import de.datev.refsys.aggregation.document.model.MovementDataDay;
import de.datev.refsys.aggregation.document.model.MovementDataMonth;
import de.datev.refsys.aggregation.document.model.MovementDataPersonGroupDay;
import de.datev.refsys.aggregation.document.model.MovementDataPersonGroupMonth;
import de.datev.refsys.aggregation.processing.exception.AggregationProcessingBusinessException;
import de.datev.refsys.aggregation.processing.mapper.AccountDbKeyFieldsMapper;
import de.datev.refsys.aggregation.processing.mapper.AccountValueMapper;
import de.datev.refsys.aggregation.processing.mapper.AdditionalParametersMapper;
import de.datev.refsys.aggregation.processing.model.AccountDbKeyFields;
import de.datev.refsys.aggregation.processing.model.DateCorrection;
import de.datev.refsys.aggregation.processing.model.MovementDataAccountValues;
import de.datev.refsys.aggregation.processing.util.Util;
import de.datev.refsys.generated.acds.api.model.AccountSumDay;
import de.datev.refsys.generated.acds.api.model.MasterdataContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Range;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import static de.datev.refsys.aggregation.document.model.constants.CollectionConstants.DAY_PREFIX;
import static de.datev.refsys.aggregation.document.model.constants.CollectionConstants.MONTH_PREFIX;
import static de.datev.refsys.aggregation.document.model.constants.CollectionConstants.OPENING_BALANCE;
import static de.datev.refsys.aggregation.processing.constant.ProcessingErrorMessageConstants.MOVEMENT_DATA_INVALID_FISCAL_MONTH_ERROR;
import static de.datev.refsys.aggregation.processing.constant.ProcessingErrorMessageConstants.MOVEMENT_DATA_INVALID_FISCAL_MONTH_VALUE_ERROR;
import static de.datev.refsys.aggregation.processing.mapper.AccountDbKeyFieldsMapper.accountGroupNumberFromAccountNumber;
import static java.time.format.DateTimeFormatter.BASIC_ISO_DATE;

@Slf4j
public abstract class CommonService {
    public static final Integer MIN_ACCOUNT_PERSON_GROUP_NUMBER = 100000000;
    public static final int MINIMUM_FISCAL_MONTH_VALUE = 0;
    public static final int MAXIMAL_FISCAL_MONTH_VALUE = 13;
    protected final AdditionalParametersMapper additionalParametersMapper;
    protected final AccountDbKeyFieldsMapper accountDbKeyFieldsMapper;
    protected final AccountValueMapper accountValueMapper;

    protected CommonService(AdditionalParametersMapper additionalParametersMapper, AccountDbKeyFieldsMapper accountDbKeyFieldsMapper,
                            AccountValueMapper accountValueMapper) {
        this.additionalParametersMapper = additionalParametersMapper;
        this.accountDbKeyFieldsMapper = accountDbKeyFieldsMapper;
        this.accountValueMapper = accountValueMapper;
    }

    protected MovementDataAccountValues getMovementDataAccountValues(MasterdataContext mdc, List<AccountSumDay> accountSumDays,
                                                                     Set<Integer> usedAccountNumbers,
                                                                     Map<AccountDbKeyFields, MovementDataPersonGroupDay> personGroupDayMap,
                                                                     Map<AccountDbKeyFields, MovementDataPersonGroupMonth> personGroupMonthMap) {
        MovementDataAccountValues movementDataAccountValues = new MovementDataAccountValues();
        if (accountSumDays.isEmpty()) {
            return movementDataAccountValues;
        }
        boolean eliminationEntryExists = false;
        boolean isPersonAccount = accountSumDays.get(0).getAccountNumber() >= MIN_ACCOUNT_PERSON_GROUP_NUMBER;
        Map<String, List<AccountSumDay>> personAccountsGroupedByDay = new TreeMap<>();
        for (AccountSumDay asd : accountSumDays) {
            AccountDbKeyFields accountDbKeyFields = accountDbKeyFieldsMapper.accountSumDayToAccountDbKeyFields(asd);
            AdditionalParameters additionalParameters = additionalParametersMapper.accountSumDayToAdditionalParameters(asd);
            DateCorrection dateCorrection = new DateCorrection(asd, mdc.getYearBegin(), mdc.getYearEnd());
            String dayKey = getDayKey(asd, dateCorrection);
            String monthKey = getMonthKey(asd, dateCorrection);
            addToDayMap(mdc, movementDataAccountValues.getAccountDayMap(),
                    additionalParameters, accountDbKeyFields, asd, dayKey);
            addToMonthMap(mdc, movementDataAccountValues.getAccountMonthMap(),
                    additionalParameters, accountDbKeyFields, asd, monthKey);
            if (isPersonAccount) {
                if (personAccountCantBeGrouped(asd)) {
                    eliminationEntryExists = true;
                } else {
                    addToAccountSumDaysGroupMap(asd, personAccountsGroupedByDay, dayKey);
                }
            }

        }
        if (isPersonAccount) {
            if (eliminationEntryExists) {
                movementDataAccountValues.getIndividualPersonAccountNumbers().add(accountSumDays.get(0).getAccountNumber());
            } else {
                processPersonAccountsToGroups(mdc, movementDataAccountValues, personAccountsGroupedByDay,
                        personGroupDayMap, personGroupMonthMap);
            }
        } else {
            usedAccountNumbers.add(accountSumDays.get(0).getAccountNumber());
        }
        return movementDataAccountValues;
    }

    private static boolean personAccountCantBeGrouped(AccountSumDay asd) {
        return asd.getAccountingReasonId() != 0 ||
                Optional.ofNullable(asd.getAmountCredit()).orElse(0.00) < 0 ||
                Optional.ofNullable(asd.getAmountDebit()).orElse(0.00) < 0;
    }

    private static void addToAccountSumDaysGroupMap(AccountSumDay asd, Map<String, List<AccountSumDay>> accountSumDayGroupMap, String groupKey) {
        if (accountSumDayGroupMap.containsKey(groupKey)) {
            accountSumDayGroupMap.get(groupKey).add(asd);
        } else {
            accountSumDayGroupMap.put(groupKey, new ArrayList<>(List.of(asd)));
        }
    }

    private void addToDayMap(MasterdataContext mdc, Map<AccountDbKeyFields, MovementDataDay> accountDayMap,
                             AdditionalParameters additionalParameters, AccountDbKeyFields accountMapKey,
                             AccountSumDay accountSumDay, String dayKey) {
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

    protected static String getDayKey(AccountSumDay accountSumDay, DateCorrection dateCorrection) {
        checkDateValidation(accountSumDay, dateCorrection);
        //check if the movement data is an opening balance when the fiscal month is 0
        if (accountSumDay.getMonth() == 0) {
            return OPENING_BALANCE;
        }
        else if (dateCorrection.getProvidedDate().isBefore(dateCorrection.getFiscalYearStart())) {
            //calendar date is before movement data date
            if (datesHaveSameYearAndMonth(dateCorrection.getCalculatedCalenderMonth(), dateCorrection.getFiscalYearStart())) {
                return DAY_PREFIX + BASIC_ISO_DATE.format(dateCorrection.getFiscalYearStart());
            } else {
                return DAY_PREFIX + BASIC_ISO_DATE.format(dateCorrection.getCalculatedCalenderMonth().withDayOfMonth(1));
            }
        } else if (dateCorrection.getProvidedDate().isAfter(dateCorrection.getFiscalYearEnd())) {
            //calendar date is after movement data date
            if (datesHaveSameYearAndMonth(dateCorrection.getCalculatedCalenderMonth(), dateCorrection.getFiscalYearEnd())) {
                return DAY_PREFIX + BASIC_ISO_DATE.format(dateCorrection.getFiscalYearEnd());
            } else {
                return DAY_PREFIX + BASIC_ISO_DATE.format(YearMonth.from(dateCorrection.getCalculatedCalenderMonth()).atEndOfMonth());
            }
        } else {
            //check if the month and year of movement data are the same as the calendar month and year
            if (datesHaveSameYearAndMonth(dateCorrection.getCalculatedCalenderMonth(), dateCorrection.getProvidedDate())) {
                return DAY_PREFIX + accountSumDay.getDate();
            }
            //check if calendar date is later than movement data date
            if (dateCorrection.getCalculatedCalenderMonth().isAfter(dateCorrection.getProvidedDate())) {
                return DAY_PREFIX + BASIC_ISO_DATE.format(dateCorrection.getCalculatedCalenderMonth().withDayOfMonth(1));
            }
            //calendar date is earlier than movement data date
            return DAY_PREFIX + BASIC_ISO_DATE.format(YearMonth.from(dateCorrection.getCalculatedCalenderMonth()).atEndOfMonth());
        }
    }

    private static boolean datesHaveSameYearAndMonth(LocalDate firstDate, LocalDate secondDate) {
        return YearMonth.of(firstDate.getYear(), firstDate.getMonth()).equals(YearMonth.of(secondDate.getYear(), secondDate.getMonth()));
    }

    private static void checkDateValidation(AccountSumDay accountSumDay, DateCorrection dateCorrection) {
        //check if fiscal month is outside of the fiscal year
        if (YearMonth.from(dateCorrection.getCalculatedCalenderMonth()).isAfter(YearMonth.from(dateCorrection.getFiscalYearEnd()))) {
            throw new AggregationProcessingBusinessException(String.format(MOVEMENT_DATA_INVALID_FISCAL_MONTH_ERROR,
                    accountSumDay.getMonth(), accountSumDay), HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        if (!Range.between(MINIMUM_FISCAL_MONTH_VALUE, MAXIMAL_FISCAL_MONTH_VALUE).contains(accountSumDay.getMonth())) {
            throw new AggregationProcessingBusinessException(String.format(MOVEMENT_DATA_INVALID_FISCAL_MONTH_VALUE_ERROR,
                    accountSumDay.getMonth(), accountSumDay), HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    protected static String getMonthKey(AccountSumDay accountSumDay, DateCorrection dateCorrection) {
        checkDateValidation(accountSumDay, dateCorrection);
        //check if movement data is an opening balance when fiscal month is 0
        if (accountSumDay.getMonth() == 0) {
            return OPENING_BALANCE;
        }
        return MONTH_PREFIX + accountSumDay.getMonth();
    }

    private void addToMonthMap(MasterdataContext mdc, Map<AccountDbKeyFields, MovementDataMonth> accountMap,
                               AdditionalParameters additionalParameters, AccountDbKeyFields accountMapKey,
                               AccountSumDay accountSumDay, String monthKey) {
        if (accountMap.containsKey(accountMapKey)) {
            MovementDataMonth movementDataMonth = accountMap.get(accountMapKey);
            if (movementDataMonth.getValues().containsKey(monthKey)) {
                incrementAccountValue(movementDataMonth.getValues().get(monthKey), accountSumDay);
            } else {
                movementDataMonth.getValues().put(monthKey, accountValueMapper.accountSumDayToAccountValue(accountSumDay));
            }
        } else {
            accountMap.put(accountMapKey, createMovementDataMonth(mdc, additionalParameters, accountSumDay, monthKey));
        }
    }

    protected void processPersonAccountsToGroups(MasterdataContext mdc, MovementDataAccountValues movementDataAccountValues,
                                                 Map<String, List<AccountSumDay>> personAccountsGroupedByDay,
                                                 Map<AccountDbKeyFields, MovementDataPersonGroupDay> personGroupDays,
                                                 Map<AccountDbKeyFields, MovementDataPersonGroupMonth> personGroupMonths) {
        Integer accountGroupNumber = accountGroupNumberFromAccountNumber(personAccountsGroupedByDay.entrySet().stream()
                .iterator().next().getValue().get(0).getAccountNumber());
        Map<String, List<AccountSumDay>> personAccountsGroupedByMonth = new LinkedHashMap<>();
        aggregatePersonGroupDays(mdc, accountGroupNumber, movementDataAccountValues.getAccountDayMap(),
                personGroupDays, personAccountsGroupedByDay, personAccountsGroupedByMonth);
        aggregatePersonGroupMonths(mdc, accountGroupNumber, movementDataAccountValues.getAccountMonthMap(),
                personGroupMonths, personAccountsGroupedByMonth);
    }

    private void aggregatePersonGroupDays(MasterdataContext mdc, Integer accountGroupNumber,
                                          Map<AccountDbKeyFields, MovementDataDay> accountDayMap,
                                          // out
                                          Map<AccountDbKeyFields, MovementDataPersonGroupDay> personGroupDays,
                                          Map<String, List<AccountSumDay>> personAccountsGroupedByDay,
                                          //out
                                          Map<String, List<AccountSumDay>> accountSumDaysForMonth) {
        PersonGroupAggregationService personGroupAggregationService = new PersonGroupAggregationService(accountValueMapper,
                accountDbKeyFieldsMapper, additionalParametersMapper);
        for (Map.Entry<String, List<AccountSumDay>> entry : personAccountsGroupedByDay.entrySet()) {
            for (AccountSumDay asd : entry.getValue()) {
                personGroupAggregationService.addToCreditSum(asd.getAmountCredit());
                personGroupAggregationService.addToDebitSum(asd.getAmountDebit());
                DateCorrection dateCorrection = new DateCorrection(asd, mdc.getYearBegin(), mdc.getYearEnd());
                addToAccountSumDaysGroupMap(asd, accountSumDaysForMonth, getMonthKey(asd, dateCorrection));
            }
            personGroupAggregationService.aggregatePersonGroupsDays(mdc, accountGroupNumber, entry.getValue(),
                    personGroupDays, entry.getKey(), accountDayMap);
            personGroupAggregationService.resetUsualChanged();
        }
    }

    private void aggregatePersonGroupMonths(MasterdataContext mdc, Integer accountGroupNumber,
                                            Map<AccountDbKeyFields, MovementDataMonth> accountMonthMap,
                                            Map<AccountDbKeyFields, MovementDataPersonGroupMonth> personGroupMonths,
                                            Map<String, List<AccountSumDay>> personAccountsGroupedByMonth) {
        PersonGroupAggregationService personGroupAggregationService = new PersonGroupAggregationService(accountValueMapper,
                accountDbKeyFieldsMapper, additionalParametersMapper);
        for (Map.Entry<String, List<AccountSumDay>> entry : personAccountsGroupedByMonth.entrySet()) {
            for (AccountSumDay asd : entry.getValue()) {
                personGroupAggregationService.addToCreditSum(asd.getAmountCredit());
                personGroupAggregationService.addToDebitSum(asd.getAmountDebit());
            }
            personGroupAggregationService.aggregatePersonGroupsMonths(mdc, accountGroupNumber, entry.getValue(),
                    personGroupMonths, entry.getKey(), accountMonthMap);
            personGroupAggregationService.resetUsualChanged();
        }
    }

    private MovementDataDay createMovementDataDay(MasterdataContext mdc, AdditionalParameters additionalParameters,
                                                  AccountSumDay accountSumDay, String dayKey) {
        return MovementDataDay.builder()
                .consultant(mdc.getConsultant())
                .client(mdc.getClient())
                .fiscalYear(mdc.getYearBegin())
                .accountNumber(accountSumDay.getAccountNumber())
                .accountingReasonId(accountSumDay.getAccountingReasonId())
                .additionalParams(additionalParameters)
                .values(new LinkedHashMap<>(Map.of(dayKey, accountValueMapper.accountSumDayToAccountValue(accountSumDay))))
                .build();
    }

    protected void incrementAccountValue(AccountValue accountValue, AccountSumDay accountSumDay) {
        accountValue.addAmountCredit(Util.amountToLongInCent(accountSumDay.getAmountCredit()));
        accountValue.addAmountDebit(Util.amountToLongInCent(accountSumDay.getAmountDebit()));
        accountValue.addQuantityCredit(accountSumDay.getQuantityCredit());
        accountValue.addQuantityDebit(accountSumDay.getQuantityDebit());
        accountValue.addWeightCredit(Util.amountToLongInCent(accountSumDay.getWeightCredit()));
        accountValue.addWeightDebit(Util.amountToLongInCent(accountSumDay.getWeightDebit()));
    }

    private MovementDataMonth createMovementDataMonth(MasterdataContext mdc, AdditionalParameters additionalParameters,
                                                      AccountSumDay accountSumDay, String monthKey) {
        return MovementDataMonth.builder()
                .consultant(mdc.getConsultant())
                .client(mdc.getClient())
                .fiscalYear(mdc.getYearBegin())
                .accountNumber(accountSumDay.getAccountNumber())
                .accountingReasonId(accountSumDay.getAccountingReasonId())
                .additionalParams(additionalParameters)
                .values(new LinkedHashMap<>(Map.of(monthKey, accountValueMapper.accountSumDayToAccountValue(accountSumDay))))
                .build();
    }
}
