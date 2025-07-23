package de.datev.refsys.aggregation.processing.model.enums;

import lombok.Getter;

@Getter
public enum SourceEndpoint {
    MASTER_DATA_CONTEXT("MasterDataContext"),
    ACCOUNT_CAPTIONS("AccountCaptions"),
    ACCOUNT_PURPOSE_MAPPINGS("AccountPurposeMappings"),
    COLLECTIVE_ACCOUNTS("CollectiveAccounts"),
    SHAREHOLDER("Shareholder"),
    TRANSLATION("Translation"),
    ACCOUNT_SUM_DAYS("AccountSumDays"),
    ACCOUNT_SUM_MONTHS("AccountSumMonths"),
    MASTER_DATA_INVENTORIES("MasterDataInventories"),
    MOVEMENT_DATA_INVENTORIES("MovementDataInventories"),
    CUSTOM_REPORT_STRUCTURES("CustomReportStructures"),
    CUSTOM_COLUMN_STRUCTURES("CustomColumnStructures");

    private final String value;

    SourceEndpoint(String value) {
        this.value = value;
    }
}
