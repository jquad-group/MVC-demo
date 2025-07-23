package de.datev.refsys.aggregation.processing.featuretest;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public final class Utility {
    private Utility() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    static double parseDouble(String value) throws ParseException {
        if (value == null) {
            return 0;
        }
        NumberFormat format = NumberFormat.getInstance(Locale.GERMANY);
        Number number = format.parse(value);
        return number.doubleValue();
    }

    static int getMonthFromDate(int date) {
        return Integer.parseInt(String.valueOf(date).substring(4, 6));
    }

    static Long convertAmountToLongIncludingTwoFractionalDigits(Double amount) {
        if (amount == null) {
            return null;
        }
        return Math.round(amount * 100);
    }

    static long convertAmountToLongIncludingTwoFractionalDigits(double amount) {
        return Math.round(amount * 100);
    }
}
