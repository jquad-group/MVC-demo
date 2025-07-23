package de.datev.refsys.aggregation.processing.featuretest;

import de.datev.refsys.aggregation.processing.featuretest.model.BookingKmvz;
import de.datev.refsys.aggregation.processing.featuretest.model.DayBooking;
import de.datev.refsys.aggregation.processing.featuretest.model.FullGroupDayBooking;
import de.datev.refsys.aggregation.processing.featuretest.model.FullGroupMonthBooking;
import de.datev.refsys.aggregation.processing.featuretest.model.GroupDayBooking;
import de.datev.refsys.aggregation.processing.featuretest.model.GroupMonthBooking;
import de.datev.refsys.aggregation.processing.featuretest.model.MonthBooking;
import io.cucumber.java.DataTableType;

import java.text.ParseException;
import java.util.Map;

//A better place might be the models but the DataTableType Annotation doesn't work inside a record
public class DataTableTransformations {
    @DataTableType
    public DayBooking dayBookingTransformer(Map<String, String> inputRow) throws ParseException {
        int day = Integer.parseInt(inputRow.get("Tag"));
        double amountDebit = Utility.parseDouble(inputRow.get("Soll"));
        double amountCredit = Utility.parseDouble(inputRow.get("Haben"));
        return new DayBooking(day, amountDebit, amountCredit);
    }

    @DataTableType
    public MonthBooking monthBookingTransformer(Map<String, String> inputRow) throws ParseException {
        int month = Integer.parseInt(inputRow.get("Monat"));
        double amountDebit = Utility.parseDouble(inputRow.get("Soll"));
        double amountCredit = Utility.parseDouble(inputRow.get("Haben"));
        return new MonthBooking(month, amountDebit, amountCredit);
    }

    @DataTableType
    public GroupDayBooking groupDayBookingTransformer(Map<String, String> inputRow) throws ParseException {
        int day = Integer.parseInt(inputRow.get("Tag"));
        Double amountDebit = null;
        String debit = inputRow.get("Soll");
        if (debit != null && !debit.isEmpty()) {
            amountDebit = Utility.parseDouble(debit);
        }
        Double amountCredit = null;
        String credit = inputRow.get("Haben");
        if (credit != null && !credit.isEmpty()) {
            amountCredit = Utility.parseDouble(credit);
        }
        return new GroupDayBooking(day, amountDebit, amountCredit);
    }

    @DataTableType
    public FullGroupDayBooking fullGroupDayBookingTransformer(Map<String, String> inputRow) throws ParseException {
        int day = Integer.parseInt(inputRow.get("Tag"));
        Double amountDebitUsual = null;
        String debit = inputRow.get("Soll typisch");
        if (debit != null && !debit.isEmpty()) {
            amountDebitUsual = Utility.parseDouble(debit);
        }
        Double amountCreditUsual = null;
        String credit = inputRow.get("Haben typisch");
        if (credit != null && !credit.isEmpty()) {
            amountCreditUsual = Utility.parseDouble(credit);
        }
        Double amountDebitUnusual = null;
        String unusualdebit = inputRow.get("Soll untypisch");
        if (unusualdebit != null && !unusualdebit.isEmpty()) {
            amountDebitUnusual = Utility.parseDouble(unusualdebit);
        }
        Double amountCreditUnusual = null;
        String unusualcredit = inputRow.get("Haben untypisch");
        if (unusualcredit != null && !unusualcredit.isEmpty()) {
            amountCreditUnusual = Utility.parseDouble(unusualcredit);
        }
        return new FullGroupDayBooking(day, amountDebitUsual, amountCreditUsual, amountDebitUnusual, amountCreditUnusual);
    }

    @DataTableType
    public FullGroupMonthBooking fullGroupMonthBookingTransformer(Map<String, String> inputRow) throws ParseException {
        int month = Integer.parseInt(inputRow.get("Monat"));
        Double amountDebitUsual = null;
        String debit = inputRow.get("Soll typisch");
        if (debit != null && !debit.isEmpty()) {
            amountDebitUsual = Utility.parseDouble(debit);
        }
        Double amountCreditUsual = null;
        String credit = inputRow.get("Haben typisch");
        if (credit != null && !credit.isEmpty()) {
            amountCreditUsual = Utility.parseDouble(credit);
        }
        Double amountDebitUnusual = null;
        String unusualdebit = inputRow.get("Soll untypisch");
        if (unusualdebit != null && !unusualdebit.isEmpty()) {
            amountDebitUnusual = Utility.parseDouble(unusualdebit);
        }
        Double amountCreditUnusual = null;
        String unusualcredit = inputRow.get("Haben untypisch");
        if (unusualcredit != null && !unusualcredit.isEmpty()) {
            amountCreditUnusual = Utility.parseDouble(unusualcredit);
        }
        return new FullGroupMonthBooking(month, amountDebitUsual, amountCreditUsual, amountDebitUnusual, amountCreditUnusual);
    }

    @DataTableType
    public GroupMonthBooking groupMonthBookingTransformer(Map<String, String> inputRow) throws ParseException {
        int month = Integer.parseInt(inputRow.get("Monat"));
        Double amountDebit = null;
        String debit = inputRow.get("Soll");
        if (debit != null && !debit.isEmpty()) {
            amountDebit = Utility.parseDouble(debit);
        }
        Double amountCredit = null;
        String credit = inputRow.get("Haben");
        if (credit != null && !credit.isEmpty()) {
            amountCredit = Utility.parseDouble(credit);
        }
        return new GroupMonthBooking(month, amountDebit, amountCredit);
    }

    @DataTableType
    public BookingKmvz kmvzBookingTransformer(Map<String, String> inputRow) throws ParseException {
        int month = Integer.parseInt(inputRow.get("Monat"));
        double amountDebit = Utility.parseDouble(inputRow.get("Soll"));
        double amountCredit = Utility.parseDouble(inputRow.get("Haben"));
        return new BookingKmvz(month, amountDebit, amountCredit);
    }
}
