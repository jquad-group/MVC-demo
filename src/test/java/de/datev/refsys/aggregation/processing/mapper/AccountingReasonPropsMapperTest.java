package de.datev.refsys.aggregation.processing.mapper;

import de.datev.refsys.generated.acds.api.model.AccountingReasonProps;
import de.datev.refsys.aggregation.document.model.enums.AccountingReasonEnum;
import de.datev.refsys.aggregation.document.model.enums.MethodOfDeterminingNetIncomeEnum;
import de.datev.refsys.aggregation.document.model.enums.PermittedAccountingReasonsEnum;
import de.datev.refsys.aggregation.processing.util.TestDataLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class AccountingReasonPropsMapperTest {

    private AccountingReasonPropsMapper accountingReasonPropsMapper;
    private AccountingReasonProps objectToMap;
    private de.datev.refsys.aggregation.document.model.AccountingReasonProps expected;
    private de.datev.refsys.aggregation.document.model.AccountingReasonProps mappedObject;

    @BeforeEach
    void setUp() {
        accountingReasonPropsMapper = new AccountingReasonPropsMapperImpl();
        objectToMap = TestDataLoader.load("json/acds-responses/mapper/accountingReason.json", AccountingReasonProps.class);
        expected =
                TestDataLoader.loadDBElement("json/collections/expected/mapper/AccountingReasonExpected.json", de.datev.refsys.aggregation.document.model.AccountingReasonProps.class);
    }

    @Test
    @DisplayName("Tests AccountingReasonPropsMapper to properly map null")
    void should_map_null_to_null() {
        assertThat(accountingReasonPropsMapper.accountingReasonProps(null)).isNull();
    }

    @ParameterizedTest
    @EnumSource(AccountingReasonEnum.class)
    @DisplayName("Tests AccountingReasonPropsMapper to properly map all AccountingReasonEnum values")
    void should_map_accountingReasonEnum_to_correct_value(AccountingReasonEnum accountingReasonEnum) {
        objectToMap.setAccountingReason(accountingReasonEnum.getValue());
        expected.setAccountingReason(accountingReasonEnum);
        mappedObject = accountingReasonPropsMapper.accountingReasonProps(
                objectToMap);

        assertThat(mappedObject).usingRecursiveComparison().isEqualTo(expected);
    }

    @ParameterizedTest
    @EnumSource(MethodOfDeterminingNetIncomeEnum.class)
    @DisplayName("Tests AccountingReasonPropsMapper to properly map all MethodOfDeterminingNetIncomeEnum values")
    void should_map_methodOfDeterminingNetIncomeEnum_to_correct_value(MethodOfDeterminingNetIncomeEnum methodOfDeterminingNetIncomeEnum) {
        objectToMap.setMethodOfDeterminingNetIncome(methodOfDeterminingNetIncomeEnum.getValue());
        expected.setMethodOfDeterminingNetIncome(methodOfDeterminingNetIncomeEnum);
        mappedObject = accountingReasonPropsMapper.accountingReasonProps(
                objectToMap);

        assertThat(mappedObject).usingRecursiveComparison().isEqualTo(expected);
    }

    @ParameterizedTest
    @EnumSource(PermittedAccountingReasonsEnum.class)
    @DisplayName("Tests AccountingReasonPropsMapper to properly map all PermittedAccountingReasonsEnum values")
    void should_map_PermittedAccountingReasonsEnum_to_correct_value(PermittedAccountingReasonsEnum permittedAccountingReasonsEnum) {
        objectToMap.setPermittedAccountingReasons(String.valueOf(permittedAccountingReasonsEnum));
        expected.setPermittedAccountingReasons(permittedAccountingReasonsEnum);
        mappedObject = accountingReasonPropsMapper.accountingReasonProps(
                objectToMap);

        assertThat(mappedObject).usingRecursiveComparison().isEqualTo(expected);
    }
}