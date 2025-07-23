package de.datev.refsys.aggregation.processing.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryDbKeyFieldsTest {

    @Test
    void should_compare_by_consultant() {
        InventoryDbKeyFields first = InventoryDbKeyFields.builder().consultant(1).build();
        InventoryDbKeyFields second = InventoryDbKeyFields.builder().consultant(2).build();
        assertThat(first).isLessThan(second);
    }

    @Test
    void should_compare_by_client() {
        InventoryDbKeyFields first = InventoryDbKeyFields.builder().consultant(1).client(1).build();
        InventoryDbKeyFields second = InventoryDbKeyFields.builder().consultant(1).client(2).build();
        assertThat(first).isLessThan(second);
    }

    @Test
    void should_compare_by_fiscal_year() {
        InventoryDbKeyFields first = InventoryDbKeyFields.builder().consultant(1).client(1).fiscalYear(20210101).build();
        InventoryDbKeyFields second = InventoryDbKeyFields.builder().consultant(1).client(1).fiscalYear(20220101).build();
        assertThat(first).isLessThan(second);
    }

    @Test
    void should_compare_by_account_number() {
        InventoryDbKeyFields first = InventoryDbKeyFields.builder().consultant(1).client(1).fiscalYear(20210101).accountNumber(1450000).build();
        InventoryDbKeyFields second = InventoryDbKeyFields.builder().consultant(1).client(1).fiscalYear(20210101).accountNumber(1450001).build();
        assertThat(first).isLessThan(second);
    }

    @Test
    void should_compare_by_accounting_reason() {
        InventoryDbKeyFields first =
                InventoryDbKeyFields.builder().consultant(1).client(1).fiscalYear(20210101).accountNumber(1450000).accountingReason(0).build();
        InventoryDbKeyFields second =
                InventoryDbKeyFields.builder().consultant(1).client(1).fiscalYear(20210101).accountNumber(1450000).accountingReason(1).build();
        assertThat(first).isLessThan(second);
    }

}