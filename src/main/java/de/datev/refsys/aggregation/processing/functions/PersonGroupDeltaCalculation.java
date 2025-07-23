package de.datev.refsys.aggregation.processing.functions;

import de.datev.refsys.aggregation.document.model.AccountGroupValue;
import de.datev.refsys.aggregation.document.model.MovementDataPersonGroupDay;
import de.datev.refsys.aggregation.document.model.MovementDataPersonGroupMonth;
import de.datev.refsys.aggregation.processing.model.AccountDbKeyFields;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.datev.refsys.aggregation.processing.util.NumberUtils.nullSafeSubstract;

public class PersonGroupDeltaCalculation {
    // pure function class - needs no instantiation - all methods are static
    private PersonGroupDeltaCalculation() {
    }

    /** Removes AccountNumbers from existing Collection
     * @param minuendIndividualPersonAccountNumbers AccountNumbers to subtract from
     * @param subtrahendIndividualPersonAccountNumbers AccountNumbers to subtract
     * @return a new Set containing only these Account numbers not present in the subtrahend
     */
    public static Set<Integer> subtractIndividualPersonAccounts(Set<Integer> minuendIndividualPersonAccountNumbers,
                                                                Set<Integer> subtrahendIndividualPersonAccountNumbers) {
        return minuendIndividualPersonAccountNumbers.stream().filter(x -> !subtrahendIndividualPersonAccountNumbers.contains(x)).collect(
                Collectors.toSet());
    }

    /** Subtracts PersonGroupDayValues - subtraction is done on base of the containing values.
     *  Extraction from a not existing value results in a negative value.
     * @param minuendDayMap personGroup Values to subtract from
     * @param subtrahendDayMap person Group Values to subtract
     * @return a new Map containing all map entries of minuend and subtrahend
     */
    public static Map<AccountDbKeyFields, MovementDataPersonGroupDay> subtractDays(
            Map<AccountDbKeyFields, MovementDataPersonGroupDay> minuendDayMap,
            Map<AccountDbKeyFields, MovementDataPersonGroupDay> subtrahendDayMap) {
        if (minuendDayMap == null) {
            minuendDayMap = new LinkedHashMap<>();
        }

        if (subtrahendDayMap == null) { //|| subtrahendDayMap.isEmpty()) { führt zu Null Werten bei Neueinträgen
            return minuendDayMap;
        }

        Map<AccountDbKeyFields, MovementDataPersonGroupDay> result = new HashMap<>();

        Set<AccountDbKeyFields> allKeys = Stream.concat(minuendDayMap.keySet().stream(), subtrahendDayMap.keySet().stream())
                                                .collect(Collectors.toSet());

        for (AccountDbKeyFields key : allKeys) {
            MovementDataPersonGroupDay existingValue = minuendDayMap.getOrDefault(key, new MovementDataPersonGroupDay());
            MovementDataPersonGroupDay valueToSubstract = subtrahendDayMap.getOrDefault(key, new MovementDataPersonGroupDay());

            result.put(key, calculateDifferenceDay(existingValue, valueToSubstract));
        }

        return result;
    }

    /** Subtracts PersonGroupMonthValues - subtraction is done on base of the containing values.
     *  Extraction from a not existing value results in a negative value.
     * @param minuendMonthMap personGroup Values to subtract from
     * @param subtrahendMonthMap person Group Values to subtract
     * @return a new Map containing all map entries of minuend and subtrahend
     */
    public static Map<AccountDbKeyFields, MovementDataPersonGroupMonth> subtractMonths(
            Map<AccountDbKeyFields, MovementDataPersonGroupMonth> minuendMonthMap,
            Map<AccountDbKeyFields, MovementDataPersonGroupMonth> subtrahendMonthMap) {

        if (minuendMonthMap == null) {
            minuendMonthMap = new LinkedHashMap<>();
        }

        if (subtrahendMonthMap == null) { //|| subtrahendMonthMap.isEmpty()) { führt zu Null Werten bei Neueinträgen
            return minuendMonthMap;
        }

        Map<AccountDbKeyFields, MovementDataPersonGroupMonth> result = new HashMap<>();

        Set<AccountDbKeyFields> allKeys =
                Stream.concat(minuendMonthMap.keySet().stream(), subtrahendMonthMap.keySet().stream())
                      .collect(Collectors.toSet());

        for (AccountDbKeyFields key : allKeys) {
            MovementDataPersonGroupMonth existingValue = minuendMonthMap.getOrDefault(key, new MovementDataPersonGroupMonth());
            MovementDataPersonGroupMonth valueToSubstract = subtrahendMonthMap.getOrDefault(key, new MovementDataPersonGroupMonth());

            result.put(key, calculateDifferenceMonth(existingValue, valueToSubstract));
        }

        return result;
    }

    static MovementDataPersonGroupDay calculateDifferenceDay(MovementDataPersonGroupDay existingGroupDay,
                                                             MovementDataPersonGroupDay groupDayToSubtract) {
        MovementDataPersonGroupDay reference;
        if (existingGroupDay.getConsultant() == null) {
            reference = groupDayToSubtract;
        } else {
            reference = existingGroupDay;
        }
        return MovementDataPersonGroupDay.builder()
                                         .accountGroupNumber(reference.getAccountGroupNumber())
                                         .accountingReasonId(reference.getAccountingReasonId())
                                         .additionalParams(reference.getAdditionalParams())
                                         .client(reference.getClient())
                                         .consultant(reference.getConsultant())
                                         .fiscalYear(reference.getFiscalYear())
                                         .values(subtractGroupValues(existingGroupDay.getValues(), groupDayToSubtract.getValues()))
                                         .build();
    }

    static MovementDataPersonGroupMonth calculateDifferenceMonth(MovementDataPersonGroupMonth existingGroupMonth,
                                                                        MovementDataPersonGroupMonth groupMonthToSubstract) {
        MovementDataPersonGroupMonth reference;
        if (existingGroupMonth.getConsultant() == null) {
            reference = groupMonthToSubstract;
        } else {
            reference = existingGroupMonth;
        }
        return MovementDataPersonGroupMonth.builder()
                                           .accountGroupNumber(reference.getAccountGroupNumber())
                                           .accountingReasonId(reference.getAccountingReasonId())
                                           .additionalParams(reference.getAdditionalParams())
                                           .client(reference.getClient())
                                           .consultant(reference.getConsultant())
                                           .fiscalYear(reference.getFiscalYear())
                                           .values(subtractGroupValues(existingGroupMonth.getValues(), groupMonthToSubstract.getValues()))
                                           .build();
    }

    static Map<String, AccountGroupValue> subtractGroupValues(Map<String, AccountGroupValue> minuendValues,
                                                                     Map<String, AccountGroupValue> subtrahendValues) {
        Map<String, AccountGroupValue> result = new HashMap<>();

        Set<String> allKeys = Stream.concat(minuendValues.keySet().stream(), subtrahendValues.keySet().stream())
                                    .collect(Collectors.toSet());

        for (String key : allKeys) {
            AccountGroupValue existingValue = minuendValues.getOrDefault(key, new AccountGroupValue());
            AccountGroupValue valueToSubstract = subtrahendValues.getOrDefault(key, new AccountGroupValue());

            result.put(key, new AccountGroupValue(
                    nullSafeSubstract(existingValue.getAmountDebitUsual(), valueToSubstract.getAmountDebitUsual()),
                    nullSafeSubstract(existingValue.getAmountCreditUsual(), valueToSubstract.getAmountCreditUsual()),
                    nullSafeSubstract(existingValue.getQuantityDebit(), valueToSubstract.getQuantityDebit()),
                    nullSafeSubstract(existingValue.getQuantityCredit(), valueToSubstract.getQuantityCredit()),
                    nullSafeSubstract(existingValue.getWeightDebit(), valueToSubstract.getWeightDebit()),
                    nullSafeSubstract(existingValue.getWeightCredit(), valueToSubstract.getWeightCredit()),
                    nullSafeSubstract(existingValue.getAmountDebitUnusual(), valueToSubstract.getAmountDebitUnusual()),
                    nullSafeSubstract(existingValue.getAmountCreditUnusual(), valueToSubstract.getAmountCreditUnusual())
            ));
        }

        return result;
    }
}
