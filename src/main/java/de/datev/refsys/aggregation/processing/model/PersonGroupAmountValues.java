package de.datev.refsys.aggregation.processing.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import static de.datev.refsys.aggregation.processing.util.NumberUtils.nullSafeGet;
import static de.datev.refsys.aggregation.processing.util.NumberUtils.storeNullIfZero;

//TODO: Consider using Record?
@Getter
@Builder
@EqualsAndHashCode
@ToString
public class PersonGroupAmountValues {
    // TODO: use internal long
    private final Long amountCreditUsual;
    private final Long amountCreditUnusual;
    private final Long amountDebitUsual;
    private final Long amountDebitUnusual;

    public PersonGroupAmountValues substract(PersonGroupAmountValues valuesToSubstract) {
        return new PersonGroupAmountValues(
                storeNullIfZero(nullSafeGet(amountCreditUsual) - nullSafeGet(valuesToSubstract.getAmountCreditUsual())),
                storeNullIfZero(nullSafeGet(amountCreditUnusual) - nullSafeGet(valuesToSubstract.getAmountCreditUnusual())),
                storeNullIfZero(nullSafeGet(amountDebitUsual) - nullSafeGet(valuesToSubstract.getAmountDebitUsual())),
                storeNullIfZero(nullSafeGet(amountDebitUnusual) - nullSafeGet(valuesToSubstract.getAmountDebitUnusual())));
    }
}
