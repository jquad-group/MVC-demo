package de.datev.refsys.aggregation.processing.util;

public class NumberUtils {
    /**
     * Gets a value from a Long object, turning null values to zero. Necessary to do simple calculations.
     * To turn back in a null type after the calculation is done use the storeNullIfZero method
     * @param amount
     * @return
     */
    public static long nullSafeGet(Long amount) {
        return amount != null ? amount : 0L;
    }

    /**
     * Turns the value in a nullable Type, converting Zeros to null values.
     * Used to store these values in mongo, saving disk space. Be careful if null and zero are different values!
     * @param amount
     * @return
     */
    public static Long storeNullIfZero(long amount) {
        return amount != 0L ? amount : null;
    }

    /**
     * Gets a value from a Long object, turning null values to zero. Necessary to do simple calculations.
     * To turn back in a null type after the calculation is done use the storeNullIfZero method
     * @param amount
     * @return
     */
    public static int nullSafeGet(Integer amount) {
        return amount != null ? amount : 0;
    }

    /**
     * Turns the value in a nullable Type, converting Zeros to null values.
     * Used to store these values in mongo, saving disk space. Be careful if null and zero are different values!
     * @param amount
     * @return
     */
    public static Integer storeNullIfZero(int amount) {
        return amount != 0 ? amount : null;
    }

    public static Long nullSafeSubstract(Long existingValue, Long valueToSubstract) {
        return storeNullIfZero(nullSafeGet(existingValue) - nullSafeGet(
                valueToSubstract));
    }

    public static Integer nullSafeSubstract(Integer existingValue, Integer valueToSubstract) {
        return storeNullIfZero(nullSafeGet(existingValue) - nullSafeGet(
                valueToSubstract));
    }
}
