package de.datev.refsys.aggregation.processing.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ComparerUtil {
    public static boolean isSameIntegerValue(Integer firstInteger, Integer secondInteger) {
        return firstInteger.equals(secondInteger);
    }
}
