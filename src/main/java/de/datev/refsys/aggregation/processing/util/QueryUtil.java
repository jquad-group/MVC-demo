package de.datev.refsys.aggregation.processing.util;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import de.datev.refsys.aggregation.document.model.AccountGroupValue;
import de.datev.refsys.aggregation.document.model.AccountValue;
import de.datev.refsys.aggregation.document.model.AdditionalParameters;
import de.datev.refsys.aggregation.document.model.StateDoc;
import de.datev.refsys.aggregation.document.model.constants.FieldConstants;
import de.datev.refsys.aggregation.document.model.enums.StateDocState;
import lombok.experimental.UtilityClass;
import org.bson.conversions.Bson;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@UtilityClass
public class QueryUtil {
    public static final Bson MOVEMENT_DATA_SORT = Sorts.orderBy(Sorts.descending(FieldConstants.FISCAL_YEAR),
                                                                Sorts.ascending(FieldConstants.ACCOUNT_NUMBER));
    public static final UpdateOptions UPSERT_UPDATE_OPTIONS = new UpdateOptions().upsert(true);
    public static final FindOneAndUpdateOptions UPDATE_FIND_OPTIONS =
            new FindOneAndUpdateOptions().upsert(false).returnDocument(ReturnDocument.AFTER);

    // StateDoc Queries
    public static Bson updateStateDoc(StateDocState state) {
        return Updates.combine(Updates.set(FieldConstants.STATE, state),
                               Updates.set(FieldConstants.STATE_TIMESTAMP, OffsetDateTime.now()));
    }

    // MovementData Queries

    public static Bson deleteMovementData(Integer consultant, Integer client, Integer fiscalYear) {
        List<Bson> query = new ArrayList<>();
        query.add(Filters.eq(FieldConstants.CONSULTANT, consultant));
        query.add(Filters.eq(FieldConstants.CLIENT, client));
        query.add(Filters.eq(FieldConstants.FISCAL_YEAR, fiscalYear));
        return Filters.and(query);
    }

    public static Bson getOneMovementDataPersonGroupDocument(Integer consultant, Integer client, Integer fiscalYear, Integer accountReasonId,
                                                             Integer accountGroupNumber, AdditionalParameters additionalParameters) {
        return getOneMovementDataDocument(consultant, client, fiscalYear, accountReasonId, FieldConstants.ACCOUNT_GROUP_NUMBER, accountGroupNumber,
                                          additionalParameters);
    }

    public static Bson getOneMovementDataDocument(Integer consultant, Integer client, Integer fiscalYear, Integer accountReasonId,
                                                  Integer accountNumber, AdditionalParameters additionalParameters) {
        return getOneMovementDataDocument(consultant, client, fiscalYear, accountReasonId, FieldConstants.ACCOUNT_NUMBER, accountNumber,
                                          additionalParameters);
    }

    public static Bson incrementMovementData(Map<String, AccountValue> accountValueMap) {
        List<Bson> query = new ArrayList<>();
        accountValueMap.forEach((key, accountValue) -> {
            String incrementKey = FieldConstants.INC_VALUES + key;
            query.add(incrementMovementData(accountValue, incrementKey));
        });
        return Updates.combine(query);
    }

    public static Bson incrementMovementData(AccountValue accountValue, String incrementKey) {
        List<Bson> query = new ArrayList<>();
        if (accountValue.getAmountDebit() != null) {
            query.add(Updates.inc(incrementKey + FieldConstants.INC_AMOUNT_DEBIT, accountValue.getAmountDebit()));
        }
        if (accountValue.getAmountCredit() != null) {
            query.add(Updates.inc(incrementKey + FieldConstants.INC_AMOUNT_CREDIT, accountValue.getAmountCredit()));
        }
        if (accountValue.getQuantityDebit() != null) {
            query.add(Updates.inc(incrementKey + FieldConstants.INC_QUANTITY_DEBIT, accountValue.getQuantityDebit()));
        }
        if (accountValue.getQuantityCredit() != null) {
            query.add(Updates.inc(incrementKey + FieldConstants.INC_QUANTITY_CREDIT, accountValue.getQuantityCredit()));
        }
        if (accountValue.getWeightDebit() != null) {
            query.add(Updates.inc(incrementKey + FieldConstants.INC_WEIGHT_DEBIT, accountValue.getWeightDebit()));
        }
        if (accountValue.getWeightCredit() != null) {
            query.add(Updates.inc(incrementKey + FieldConstants.INC_WEIGHT_CREDIT, accountValue.getWeightCredit()));
        }
        return Updates.combine(query);
    }

    public static Bson incrementMovementDataPersonGroup(Map<String, AccountGroupValue> accountValueMap) {
        List<Bson> query = new ArrayList<>();
        accountValueMap.forEach((key, accountValue) -> {
            String incrementKey = FieldConstants.INC_VALUES + key;
            if (accountValue.getAmountDebitUsual() != null) {
                query.add(Updates.inc(incrementKey + FieldConstants.INC_AMOUNT_DEBIT_USUAL, accountValue.getAmountDebitUsual()));
            }
            if (accountValue.getAmountCreditUsual() != null) {
                query.add(Updates.inc(incrementKey + FieldConstants.INC_AMOUNT_CREDIT_USUAL, accountValue.getAmountCreditUsual()));
            }
            if (accountValue.getAmountDebitUnusual() != null) {
                query.add(Updates.inc(incrementKey + FieldConstants.INC_AMOUNT_DEBIT_UNUSUAL, accountValue.getAmountDebitUnusual()));
            }
            if (accountValue.getAmountCreditUnusual() != null) {
                query.add(Updates.inc(incrementKey + FieldConstants.INC_AMOUNT_CREDIT_UNUSUAL, accountValue.getAmountCreditUnusual()));
            }
            if (accountValue.getQuantityDebit() != null) {
                query.add(Updates.inc(incrementKey + FieldConstants.INC_QUANTITY_DEBIT, accountValue.getQuantityDebit()));
            }
            if (accountValue.getQuantityCredit() != null) {
                query.add(Updates.inc(incrementKey + FieldConstants.INC_QUANTITY_CREDIT, accountValue.getQuantityCredit()));
            }
            if (accountValue.getWeightDebit() != null) {
                query.add(Updates.inc(incrementKey + FieldConstants.INC_WEIGHT_DEBIT, accountValue.getWeightDebit()));
            }
            if (accountValue.getWeightCredit() != null) {
                query.add(Updates.inc(incrementKey + FieldConstants.INC_WEIGHT_CREDIT, accountValue.getWeightCredit()));
            }
        });
        return Updates.combine(query);
    }

    public static Bson getByMasterDataBusinessKey(Integer consultant, Integer client, Integer fiscalYear) {
        List<Bson> query = new ArrayList<>();
        query.add(Filters.eq(FieldConstants.CONSULTANT, consultant));
        query.add(Filters.eq(FieldConstants.CLIENT, client));
        query.add(Filters.eq(FieldConstants.YEAR_BEGIN, fiscalYear));
        return Filters.and(query);
    }

    public static Bson getByStateDoc(StateDoc actualStateDoc) {
        List<Bson> query = new ArrayList<>();
        query.add(Filters.eq(FieldConstants.CONSULTANT, actualStateDoc.getConsultant()));
        query.add(Filters.eq(FieldConstants.CLIENT, actualStateDoc.getClient()));
        query.add(Filters.eq(FieldConstants.YEAR_BEGIN, actualStateDoc.getYearBegin()));
        query.add(Filters.eq(FieldConstants.YEAR_END, actualStateDoc.getYearEnd()));
        query.add(Filters.eq(FieldConstants.STATE, actualStateDoc.getState()));
        query.add(Filters.eq(FieldConstants.STATE_TIMESTAMP, actualStateDoc.getStateTimestamp()));
        return Filters.and(query);
    }

    private static Bson getOneMovementDataDocument(Integer consultant, Integer client, Integer fiscalYear, Integer accountReasonId,
                                                   String accountNumberKey, Integer accountNumber, AdditionalParameters additionalParameters) {
        List<Bson> query = new ArrayList<>();
        query.add(Filters.eq(FieldConstants.CONSULTANT, consultant));
        query.add(Filters.eq(FieldConstants.CLIENT, client));
        query.add(Filters.eq(FieldConstants.FISCAL_YEAR, fiscalYear));
        query.add(Filters.eq(FieldConstants.ACCOUNTING_REASON_ID, accountReasonId));
        query.add(Filters.eq(accountNumberKey, accountNumber));
        if (additionalParameters != null) {
            query.add(Filters.eq(FieldConstants.ADDITIONAL_PARAMS, additionalParameters));
        } else {
            query.add(Filters.exists(FieldConstants.ADDITIONAL_PARAMS, false));
        }
        return Filters.and(query);
    }

    public static Bson getMovementDataPerAccountQuery(Integer consultant, Integer client, Integer fiscalYear, List<Integer> accountNumbers) {
        List<Bson> query = new ArrayList<>();
        query.add(Filters.eq(FieldConstants.CONSULTANT, consultant));
        query.add(Filters.eq(FieldConstants.CLIENT, client));
        query.add(Filters.eq(FieldConstants.FISCAL_YEAR, fiscalYear));
        query.add(Filters.in(FieldConstants.ACCOUNT_NUMBER, accountNumbers));
        return Filters.and(query);
    }
}
