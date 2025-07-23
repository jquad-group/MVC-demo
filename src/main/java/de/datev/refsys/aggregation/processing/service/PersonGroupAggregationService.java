package de.datev.refsys.aggregation.processing.service;

import de.datev.refsys.generated.acds.api.model.AccountSumDay;
import de.datev.refsys.generated.acds.api.model.MasterdataContext;
import de.datev.refsys.aggregation.document.model.AccountGroupValue;
import de.datev.refsys.aggregation.document.model.AccountValue;
import de.datev.refsys.aggregation.document.model.AdditionalParameters;
import de.datev.refsys.aggregation.document.model.MovementDataDay;
import de.datev.refsys.aggregation.document.model.MovementDataMonth;
import de.datev.refsys.aggregation.document.model.MovementDataPersonGroupDay;
import de.datev.refsys.aggregation.document.model.MovementDataPersonGroupMonth;
import de.datev.refsys.aggregation.processing.mapper.AccountDbKeyFieldsMapper;
import de.datev.refsys.aggregation.processing.mapper.AccountValueMapper;
import de.datev.refsys.aggregation.processing.mapper.AdditionalParametersMapper;
import de.datev.refsys.aggregation.processing.model.AccountDbKeyFields;
import de.datev.refsys.aggregation.processing.model.PersonGroupAmountValues;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static de.datev.refsys.aggregation.document.model.constants.CollectionConstants.DAY_PREFIX;
import static de.datev.refsys.aggregation.document.model.constants.CollectionConstants.MONTH_PREFIX;
import static de.datev.refsys.aggregation.document.model.constants.CollectionConstants.OPENING_BALANCE;
import static de.datev.refsys.aggregation.processing.mapper.AccountValueMapper.doubleToLongAmountInCent;

@Getter
@RequiredArgsConstructor
public class PersonGroupAggregationService {
    private static final Integer MAX_DEBITOR_ACCOUNT_GROUP_NUMBER = 6;

    private long totalCreditSum;
    private long totalDebitSum;
    private Boolean usual;
    private boolean usualChanged;

    private final AccountValueMapper accountValueMapper;
    private final AccountDbKeyFieldsMapper accountDbKeyFieldsMapper;
    private final AdditionalParametersMapper additionalParametersMapper;

    public void addToCreditSum(Double amountCredit) {
        if (amountCredit != null) {
            this.totalCreditSum = this.totalCreditSum + doubleToLongAmountInCent(amountCredit);
        }
    }

    public void addToDebitSum(Double amountDebit) {
        if (amountDebit != null) {
            this.totalDebitSum = this.totalDebitSum + doubleToLongAmountInCent(amountDebit);
        }
    }

    /**
     * Aggregates the account sum days for person accounts. Does the calculation of unusual and usual values by account.
     * Person Accounts are grouped by the first digit of their accountNumber.
     *
     * @param mdc MasterdataContext is used to access consultant, client and fiscalYear
     * @param accountGroupNumber for the unusual/usual check - is the account a Kreditor or a Debitor?
     * @param accountSumDays list of account sum days to be aggregated (read-only) - must be in the correct order
     * @param personGroupDays output value, the aggregated values are added to this map
     * @param dayKey the key of the day to be aggregated in the format "d"+day //TODO: consider refactoring? and use an int here instead
     * @param accountDayMap a prefilled map with all the movementdata - has to be a sorted Map //Values here are the same (or more?) as in the accountSumDaysList?
     */
    // TODO: refactor to use return values instead of modifying the input
    // Careful: Assumes that the list of account sum days contains all values to a day - if used with a day twice, the results are not correct
    public void aggregatePersonGroupsDays(MasterdataContext mdc, Integer accountGroupNumber, List<AccountSumDay> accountSumDays,
                                          //output
                                          Map<AccountDbKeyFields, MovementDataPersonGroupDay> personGroupDays, String dayKey,
                                          //input
                                          Map<AccountDbKeyFields, MovementDataDay> accountDayMap) {
        calculateUsual(accountGroupNumber);
        // check if usual to unusual or unusual to usual change occurred
        if (this.usualChanged) {
            personGroupDays.entrySet().stream()
                    .filter(personGroupDayEntry -> personGroupDayEntry.getKey().getAccountNumber().equals(accountGroupNumber))
                    .forEach(entry -> calculateSumAndAddToPersonGroupDayMap(mdc, dayKey, accountDayMap, entry,
                            entry.getKey().toBuilder().accountNumber(accountSumDays.get(0).getAccountNumber()).build()));
        }
        accountSumDays.forEach(accountSumDay -> addToPersonGroupDayMap(mdc,
                accountDbKeyFieldsMapper.accountSumDayToGroupAccountDbKeyFields(accountSumDay),
                accountSumDay, dayKey, personGroupDays));
    }

    private void calculateSumAndAddToPersonGroupDayMap(MasterdataContext mdc, String dayKey, Map<AccountDbKeyFields, MovementDataDay> accountDayMap,
                                                       Map.Entry<AccountDbKeyFields, MovementDataPersonGroupDay> entry,
                                                       AccountDbKeyFields accountDbKeyFields) {
        long creditSum = 0L;
        long debitSum = 0L;
        if (accountDayMap.containsKey(accountDbKeyFields)) {
            for (Map.Entry<String, AccountValue> accountValueEntry : accountDayMap.get(accountDbKeyFields).getValues().entrySet()) {
                if (getDateFromKey(accountValueEntry.getKey(), mdc.getYearBegin())
                        .compareTo(getDateFromKey(dayKey, mdc.getYearBegin())) < 0) {
                    creditSum += Optional.ofNullable(accountValueEntry.getValue().getAmountCredit()).orElse(0L);
                    debitSum += Optional.ofNullable(accountValueEntry.getValue().getAmountDebit()).orElse(0L);
                }
            }
            if (creditSum != 0 || debitSum != 0) {
                addToAccountGroupValueDayMap(dayKey, entry.getValue(), creditSum, debitSum);
            }
        }
    }

    private void addToAccountGroupValueDayMap(String dayKey, MovementDataPersonGroupDay personGroupDay, long creditSum, long debitSum) {
        if (Boolean.TRUE.equals(this.usual)) {
            // previous amounts were unusual
            if (personGroupDay.getValues().containsKey(dayKey)) {
                personGroupDay.getValues().get(dayKey).addAmountCreditUsual(creditSum);
                personGroupDay.getValues().get(dayKey).addAmountCreditUnusual(creditSum * -1L);
                personGroupDay.getValues().get(dayKey).addAmountDebitUsual(debitSum);
                personGroupDay.getValues().get(dayKey).addAmountDebitUnusual(debitSum * -1L);
            } else {
                PersonGroupAmountValues.PersonGroupAmountValuesBuilder amountValuesBuilder = PersonGroupAmountValues.builder();
                if (creditSum != 0L) {
                    amountValuesBuilder.amountCreditUsual(creditSum).amountCreditUnusual(creditSum * -1L);
                }
                if (debitSum != 0L) {
                    amountValuesBuilder.amountDebitUsual(debitSum).amountDebitUnusual(debitSum * -1L);
                }
                personGroupDay.getValues().put(dayKey, accountValueMapper.amountValuesToAccountGroupValue(amountValuesBuilder.build()));
            }
        } else {
            // previous amounts were usual
            if (personGroupDay.getValues().containsKey(dayKey)) {
                personGroupDay.getValues().get(dayKey).addAmountCreditUsual(creditSum * -1L);
                personGroupDay.getValues().get(dayKey).addAmountCreditUnusual(creditSum);
                personGroupDay.getValues().get(dayKey).addAmountDebitUsual(debitSum * -1L);
                personGroupDay.getValues().get(dayKey).addAmountDebitUnusual(debitSum);
            } else {
                PersonGroupAmountValues.PersonGroupAmountValuesBuilder amountValuesBuilder = PersonGroupAmountValues.builder();
                if (creditSum != 0L) {
                    amountValuesBuilder.amountCreditUsual(creditSum * -1L).amountCreditUnusual(creditSum);
                }
                if (debitSum != 0L) {
                    amountValuesBuilder.amountDebitUsual(debitSum * -1L).amountDebitUnusual(debitSum);
                }
                personGroupDay.getValues().put(dayKey, accountValueMapper.amountValuesToAccountGroupValue(amountValuesBuilder.build()));
            }
        }
    }

    public void aggregatePersonGroupsMonths(MasterdataContext mdc, Integer accountGroupNumber, List<AccountSumDay> accountSumDays,
                                            Map<AccountDbKeyFields, MovementDataPersonGroupMonth> personGroupMonths, String monthKey,
                                            Map<AccountDbKeyFields, MovementDataMonth> accountMonthMap) {
        calculateUsual(accountGroupNumber);
        // check if usual change to unusual or unusual to usual
        if (this.usualChanged) {
            personGroupMonths.entrySet().stream()
                    .filter(personGroupMonthEntry -> personGroupMonthEntry.getKey().getAccountNumber().equals(accountGroupNumber))
                    .forEach(entry -> calculateSumAndAddToPersonGroupMonthMap(monthKey, accountMonthMap, entry,
                            entry.getKey().toBuilder().accountNumber(accountSumDays.get(0).getAccountNumber()).build()));
        }
        accountSumDays.forEach(accountSumDay -> addToPersonGroupMonthMap(mdc,
                accountDbKeyFieldsMapper.accountSumDayToGroupAccountDbKeyFields(accountSumDay),
                accountSumDay, monthKey, personGroupMonths));
    }

    private void calculateSumAndAddToPersonGroupMonthMap(String monthKey, Map<AccountDbKeyFields, MovementDataMonth> accountMonthMap,
                                                         Map.Entry<AccountDbKeyFields, MovementDataPersonGroupMonth> entry,
                                                         AccountDbKeyFields accountDbKeyFields) {
        long creditSum = 0L;
        long debitSum = 0L;
        if (accountMonthMap.containsKey(accountDbKeyFields)) {
            for (Map.Entry<String, AccountValue> accountValueEntry : accountMonthMap.get(accountDbKeyFields).getValues().entrySet()) {
                if (getMonthFromKey(accountValueEntry.getKey()).compareTo(getMonthFromKey(monthKey)) < 0) {
                    creditSum += Optional.ofNullable(accountValueEntry.getValue().getAmountCredit()).orElse(0L);
                    debitSum += Optional.ofNullable(accountValueEntry.getValue().getAmountDebit()).orElse(0L);
                }
            }
            if (creditSum != 0 || debitSum != 0) {
                addToAccountGroupValueMonthMap(monthKey, entry.getValue(), creditSum, debitSum);
            }
        }
    }

    private void addToAccountGroupValueMonthMap(String monthKey, MovementDataPersonGroupMonth personGroupMonth, long creditSum, long debitSum) {
        if (Boolean.TRUE.equals(this.usual)) {
            // previous amounts were unusual
            if (personGroupMonth.getValues().containsKey(monthKey)) {
                personGroupMonth.getValues().get(monthKey).addAmountCreditUsual(creditSum);
                personGroupMonth.getValues().get(monthKey).addAmountCreditUnusual(creditSum * -1L);
                personGroupMonth.getValues().get(monthKey).addAmountDebitUsual(debitSum);
                personGroupMonth.getValues().get(monthKey).addAmountDebitUnusual(debitSum * -1L);
            } else {
                PersonGroupAmountValues.PersonGroupAmountValuesBuilder amountValuesBuilder = PersonGroupAmountValues.builder();
                if (creditSum != 0L) {
                    amountValuesBuilder.amountCreditUsual(creditSum).amountCreditUnusual(creditSum * -1L);
                }
                if (debitSum != 0L) {
                    amountValuesBuilder.amountDebitUsual(debitSum).amountDebitUnusual(debitSum * -1L);
                }
                personGroupMonth.getValues().put(monthKey, accountValueMapper.amountValuesToAccountGroupValue(amountValuesBuilder.build()));
            }
        } else {
            // previous amounts were usual
            if (personGroupMonth.getValues().containsKey(monthKey)) {
                personGroupMonth.getValues().get(monthKey).addAmountCreditUsual(creditSum * -1L);
                personGroupMonth.getValues().get(monthKey).addAmountCreditUnusual(creditSum);
                personGroupMonth.getValues().get(monthKey).addAmountDebitUsual(debitSum * -1L);
                personGroupMonth.getValues().get(monthKey).addAmountDebitUnusual(debitSum);
            } else {
                PersonGroupAmountValues.PersonGroupAmountValuesBuilder amountValuesBuilder = PersonGroupAmountValues.builder();
                if (creditSum != 0L) {
                    amountValuesBuilder.amountCreditUsual(creditSum * -1L).amountCreditUnusual(creditSum);
                }
                if (debitSum != 0L) {
                    amountValuesBuilder.amountDebitUsual(debitSum * -1L).amountDebitUnusual(debitSum);
                }
                personGroupMonth.getValues().put(monthKey, accountValueMapper.amountValuesToAccountGroupValue(amountValuesBuilder.build()));
            }
        }
    }

    public void resetUsualChanged() {
        this.usualChanged = false;
    }

    public static Integer getDateFromKey(String key, Integer fiscalYearStart) {
        if (key.equals(OPENING_BALANCE)) {
            return fiscalYearStart;
        }
        return Integer.valueOf(key.replace(DAY_PREFIX, ""));
    }

    public static Integer getMonthFromKey(String key) {
        if (key.equals(OPENING_BALANCE)) {
            return 0;
        }
        return Integer.valueOf(key.replace(MONTH_PREFIX, ""));
    }

    private void calculateUsual(Integer accountGroupNumber) {
        if (this.totalDebitSum == this.totalCreditSum) {
            if (this.usual != null && !this.usual) {
                this.usualChanged = true;
            }
            this.usual = true;
            return;
        }
        // 1 - 6 debitors, 7 - 9 creditors
        boolean isDebitSmaller = totalDebitSum < totalCreditSum;
        if (accountGroupNumber > MAX_DEBITOR_ACCOUNT_GROUP_NUMBER) {
            if (this.usual != null && debitorHasChangedUsual(isDebitSmaller)) {
                this.usualChanged = true;
            }
            this.usual = isDebitSmaller;
        } else {
            if (this.usual != null && creditorHasChangedUsual(isDebitSmaller)) {
                this.usualChanged = true;
            }
            this.usual = !isDebitSmaller;
        }
    }

    private boolean debitorHasChangedUsual(boolean isDebitSmaller) {
        return (isDebitSmaller && !this.usual) || (!isDebitSmaller && this.usual);
    }

    private boolean creditorHasChangedUsual(boolean isDebitSmaller) {
        return (!isDebitSmaller && !this.usual) || (isDebitSmaller && this.usual);
    }

    private PersonGroupAmountValues getPersonGroupAmountValues(AccountSumDay accountSumDay) {
        PersonGroupAmountValues.PersonGroupAmountValuesBuilder amountValuesBuilder = PersonGroupAmountValues.builder();
        if (Boolean.TRUE.equals(this.usual)) {
            amountValuesBuilder.amountCreditUsual(doubleToLongAmountInCent(accountSumDay.getAmountCredit()));
            amountValuesBuilder.amountDebitUsual(doubleToLongAmountInCent(accountSumDay.getAmountDebit()));
        } else {
            amountValuesBuilder.amountCreditUnusual(doubleToLongAmountInCent(accountSumDay.getAmountCredit()));
            amountValuesBuilder.amountDebitUnusual(doubleToLongAmountInCent(accountSumDay.getAmountDebit()));
        }
        return amountValuesBuilder.build();
    }

    private void addToPersonGroupDayMap(MasterdataContext mdc, AccountDbKeyFields accountMapKey, AccountSumDay accountSumDay, String dayKey,
                                        Map<AccountDbKeyFields, MovementDataPersonGroupDay> personGroupDays) {
        PersonGroupAmountValues amountValues = getPersonGroupAmountValues(accountSumDay);
        if (personGroupDays.containsKey(accountMapKey)) {
            MovementDataPersonGroupDay movementDataPersonGroupDay = personGroupDays.get(accountMapKey);
            if (movementDataPersonGroupDay.getValues().containsKey(dayKey)) {
                incrementAccountGroupValue(movementDataPersonGroupDay.getValues().get(dayKey), accountSumDay, amountValues);
            } else {
                movementDataPersonGroupDay.getValues().put(dayKey,
                        accountValueMapper.accountSumDayToAccountGroupValue(accountSumDay, amountValues));
            }
        } else {
            personGroupDays.put(accountMapKey, createMovementDataPersonGroupDay(mdc, accountMapKey.getAccountNumber(), dayKey,
                    accountSumDay, additionalParametersMapper.accountSumDayToAdditionalParameters(accountSumDay), amountValues));
        }
    }

    private void addToPersonGroupMonthMap(MasterdataContext mdc, AccountDbKeyFields accountMapKey, AccountSumDay accountSumDay, String monthKey,
                                          Map<AccountDbKeyFields, MovementDataPersonGroupMonth> personGroupMonths) {
        PersonGroupAmountValues amountValues = getPersonGroupAmountValues(accountSumDay);
        if (personGroupMonths.containsKey(accountMapKey)) {
            MovementDataPersonGroupMonth movementDataPersonGroupMonth = personGroupMonths.get(accountMapKey);
            if (movementDataPersonGroupMonth.getValues().containsKey(monthKey)) {
                incrementAccountGroupValue(movementDataPersonGroupMonth.getValues().get(monthKey), accountSumDay, amountValues);
            } else {
                movementDataPersonGroupMonth.getValues().put(monthKey,
                        accountValueMapper.accountSumDayToAccountGroupValue(accountSumDay, amountValues));
            }
        } else {
            personGroupMonths.put(accountMapKey, createMovementDataPersonGroupMonth(mdc, accountMapKey.getAccountNumber(),
                    monthKey, accountSumDay, additionalParametersMapper.accountSumDayToAdditionalParameters(accountSumDay), amountValues));
        }
    }

    private static void incrementAccountGroupValue(AccountGroupValue accountValue, AccountSumDay accountSumDay,
                                                   PersonGroupAmountValues amountValues) {
        accountValue.addAmountCreditUsual(amountValues.getAmountCreditUsual());
        accountValue.addAmountCreditUnusual(amountValues.getAmountCreditUnusual());
        accountValue.addAmountDebitUsual(amountValues.getAmountDebitUsual());
        accountValue.addAmountDebitUnusual(amountValues.getAmountDebitUnusual());
        accountValue.addQuantityCredit(accountSumDay.getQuantityCredit());
        accountValue.addQuantityDebit(accountSumDay.getQuantityDebit());
        accountValue.addWeightCredit(doubleToLongAmountInCent(accountSumDay.getWeightCredit()));
        accountValue.addWeightDebit(doubleToLongAmountInCent(accountSumDay.getWeightDebit()));
    }

    private MovementDataPersonGroupDay createMovementDataPersonGroupDay(MasterdataContext mdc, Integer accountGroupNumber, String dayKey,
                                                                        AccountSumDay accountSumDay, AdditionalParameters additionalParameters,
                                                                        PersonGroupAmountValues amountValues) {
        return MovementDataPersonGroupDay.builder()
                .consultant(mdc.getConsultant())
                .client(mdc.getClient())
                .fiscalYear(mdc.getYearBegin())
                .accountGroupNumber(accountGroupNumber)
                .accountingReasonId(accountSumDay.getAccountingReasonId())
                .additionalParams(additionalParameters)
                .values(new LinkedHashMap<>(Map.of(dayKey, accountValueMapper.accountSumDayToAccountGroupValue(accountSumDay, amountValues))))
                .build();
    }

    private MovementDataPersonGroupMonth createMovementDataPersonGroupMonth(MasterdataContext mdc, Integer accountGroupNumber, String monthKey,
                                                                            AccountSumDay accountSumDay, AdditionalParameters additionalParameters,
                                                                            PersonGroupAmountValues amountValues) {
        return MovementDataPersonGroupMonth.builder()
                .consultant(mdc.getConsultant())
                .client(mdc.getClient())
                .fiscalYear(mdc.getYearBegin())
                .accountGroupNumber(accountGroupNumber)
                .accountingReasonId(accountSumDay.getAccountingReasonId())
                .additionalParams(additionalParameters)
                .values(new LinkedHashMap<>(Map.of(monthKey, accountValueMapper.accountSumDayToAccountGroupValue(accountSumDay, amountValues))))
                .build();
    }
}
