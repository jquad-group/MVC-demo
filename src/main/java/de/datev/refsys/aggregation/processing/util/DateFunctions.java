package de.datev.refsys.aggregation.processing.util;

public final class DateFunctions {

    public static final int YEAR_POSITION = 10_000;
    public static final int MONTH_POSITION = 100;
    public static final int MONTHS_PER_YEAR = 12;

    public static final int MONTH_DATE_BEGIN_INDEX = 4;
    public static final int MONTH_DATE_END_INDEX = 6;

    //prevent instantiation
    private DateFunctions() {
    }

    public static int geFirstPossibleAccountingDateForMonth(int yearBegin, int monthOfWJ) {
        if (monthOfWJ <= 1) {
            return yearBegin;
        }

        int year = yearBegin / YEAR_POSITION;
        int monthday = yearBegin - year * YEAR_POSITION;
        int month = monthday / MONTH_POSITION;
        int day = 1;

        int calenderMonth = month + monthOfWJ - 1;
        if (calenderMonth > MONTHS_PER_YEAR) {
            year++;
            calenderMonth -= MONTHS_PER_YEAR;
        }

        return year * YEAR_POSITION + calenderMonth * MONTH_POSITION + day;
    }
}
