package de.datev.refsys.aggregation.processing.util;

import lombok.experimental.UtilityClass;
import org.bson.types.Decimal128;

@UtilityClass
public class Util {
    public static final int CENT_IN_ONE_EURO = 100;

    public static Long amountToLongInCent(Double value) {
        return value != null ? Math.round(value * CENT_IN_ONE_EURO) : null;
    }

    public static Decimal128 doubleToDecimal128(Double value) {
        return value == null ? null : Decimal128.parse(String.valueOf(value));
    }

    public static boolean isDateBetweenYearBeginAndEnd(Integer date, Integer yearBegin, Integer yearEnd) {
        return date >= yearBegin && date <= yearEnd;
    }

    public static Integer nullIfDefault(Integer value, int defaultValue) {
        if (value == null || value == defaultValue) {
            return null;
        }
        return value;
    }

    public static Boolean nullIfDefault(Boolean value, boolean defaultValue) {
        if (value == null || value == defaultValue) {
            return null;
        }
        return value;
    }

    public static Integer organisationDataToIndivLevel(Boolean value) {
        return Boolean.TRUE.equals(value) ? 10 : 20;
    }
}
