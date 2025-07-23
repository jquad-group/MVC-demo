package de.datev.refsys.aggregation.processing.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import de.datev.refsys.aggregation.document.model.CustomColumnStructureInfo;
import de.datev.refsys.aggregation.document.model.CustomReportStructureInfo;
import de.datev.refsys.aggregation.document.model.MasterDataContext;
import de.datev.refsys.aggregation.document.model.enums.AccountPurposeMappingEnabledEnum;
import de.datev.refsys.aggregation.document.model.enums.CompanySizeClassEnum;
import de.datev.refsys.aggregation.document.model.enums.IncomeStatementMethodEnum;
import de.datev.refsys.aggregation.document.model.enums.LegalFormEnum;
import de.datev.refsys.aggregation.document.model.enums.SpecialTaxationEnum;
import de.datev.refsys.aggregation.document.model.enums.TypeOfIncomeEnum;
import de.datev.refsys.aggregation.processing.util.TestDataLoader;
import de.datev.refsys.generated.acds.api.model.AccountingReasonProps;
import de.datev.refsys.generated.acds.api.model.CustomColumnStructure;
import de.datev.refsys.generated.acds.api.model.CustomReportStructure;
import de.datev.refsys.generated.acds.api.model.MasterdataContext;
import de.datev.refsys.generated.acds.api.model.ReportingOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class MasterDataContextMapperTest {
    private final AccountingReasonPropsMapper accountingReasonPropsMapper = new AccountingReasonPropsMapperImpl();

    private final MasterDataContextMapper masterDataContextMapper = new MasterDataContextMapperImpl(accountingReasonPropsMapper);
    private MasterdataContext objectToMap;
    private MasterDataContext expected;

    @BeforeEach
    void setUp() {
        objectToMap = TestDataLoader.load("json/acds-responses/mapper/masterDataContext.json", MasterdataContext.class);
        expected = TestDataLoader.loadDBElement("json/collections/expected/mapper/masterDataContextExpected.json", MasterDataContext.class);
    }

    @Test
    @DisplayName("Tests MasterDataContextMapper to properly map null")
    void should_map_null_to_null() {
        assertThat(masterDataContextMapper.mapAcdsToDb(null)).isNull();
    }

    @Test
    @DisplayName("Tests MasterDataContextMapper to properly map null values")
    void should_map_null_list_to_null_list() {
        objectToMap.setAccountingReasons(null);
        objectToMap.setReportingOptions(null);
        MasterDataContext masterDataContext = masterDataContextMapper.mapAcdsToDb(objectToMap);
        expected.setAccountingReasons(null);
        expected.setReportingOptions(null);
        assertThat(masterDataContext).isNotNull();
        assertThat(masterDataContext).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    @DisplayName("Tests MasterDataContextMapper to properly map null values in lists")
    void should_map_null_in_list_to_null_in_list() {
        ArrayList<AccountingReasonProps> props = new ArrayList<>();
        props.add(null);
        ArrayList<ReportingOption> options = new ArrayList<>();
        options.add(null);
        ArrayList<de.datev.refsys.aggregation.document.model.AccountingReasonProps> propsExpected = new ArrayList<>();
        propsExpected.add(null);
        ArrayList<de.datev.refsys.aggregation.document.model.ReportingOption> optionsExpected = new ArrayList<>();
        optionsExpected.add(null);
        objectToMap.setAccountingReasons(props);
        objectToMap.setReportingOptions(options);
        MasterDataContext masterDataContext = masterDataContextMapper.mapAcdsToDb(objectToMap);
        expected.setAccountingReasons(propsExpected);
        expected.setReportingOptions(optionsExpected);
        assertThat(masterDataContext).isNotNull();
        assertThat(masterDataContext).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void should_map_near_time_data_flag_to_null_if_feature_not_enabled() {
        objectToMap.setContainsNearTimeData(Boolean.TRUE);
        MasterDataContext actualMasterDataContext = masterDataContextMapper.mapAcdsToDb(objectToMap);
        assertThat(actualMasterDataContext.getContainsNearTimeData()).isEqualTo(Boolean.TRUE);
    }

    @Test
    void should_map_near_time_data_flag_false_to_false() {
        objectToMap.setContainsNearTimeData(Boolean.FALSE);
        MasterDataContext actualMasterDataContext = masterDataContextMapper.mapAcdsToDb(objectToMap);
        assertThat(actualMasterDataContext.getContainsNearTimeData()).isEqualTo(Boolean.FALSE);
    }

    @Test
    void should_map_near_time_data_flag_null_to_null() {
        objectToMap.setContainsNearTimeData(null);
        MasterDataContext actualMasterDataContext = masterDataContextMapper.mapAcdsToDb(objectToMap);
        assertThat(actualMasterDataContext.getContainsNearTimeData()).isNull();
    }

    @ParameterizedTest
    @EnumSource(AccountPurposeMappingEnabledEnum.class)
    @DisplayName("Tests MasterDataContextMapper to properly map all AccountPurposeMappingEnabledEnum values")
    void should_map_integer_to_accountPurposeMappingEnabledEnum(AccountPurposeMappingEnabledEnum accountPurposeMappingEnabledEnum) {
        objectToMap.setAccountPurposeMappingEnabled(accountPurposeMappingEnabledEnum.getValue());
        expected.setAccountPurposeMappingEnabled(accountPurposeMappingEnabledEnum);
        MasterDataContext masterDataContext = masterDataContextMapper.mapAcdsToDb(objectToMap);

        assertThat(masterDataContext).isNotNull();
        assertThat(masterDataContext).usingRecursiveComparison().isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("provideLegalFormValues")
    @DisplayName("Tests MasterDataContextMapper to properly map all LegalFormEnum values including exceptional cases 13 and 14")
    void should_map_integer_to_legalFormEnum(Integer enumIntToMap, LegalFormEnum expectedLegalFormEnum) {
        objectToMap.setLegalForm(enumIntToMap);
        expected.setLegalForm(expectedLegalFormEnum);
        MasterDataContext masterDataContext = masterDataContextMapper.mapAcdsToDb(objectToMap);

        assertThat(masterDataContext).isNotNull();
        assertThat(masterDataContext).usingRecursiveComparison().isEqualTo(expected);
    }

    @ParameterizedTest
    @EnumSource(CompanySizeClassEnum.class)
    @DisplayName("Tests MasterDataContextMapper to properly map all CompanySizeClassEnum values")
    void should_map_integer_to_companySizeClassEnum(CompanySizeClassEnum companySizeClassEnum) {
        objectToMap.setCompanySizeClass(companySizeClassEnum.getValue());
        expected.setCompanySizeClass(companySizeClassEnum);
        MasterDataContext masterDataContext = masterDataContextMapper.mapAcdsToDb(objectToMap);

        assertThat(masterDataContext).isNotNull();
        assertThat(masterDataContext).usingRecursiveComparison().isEqualTo(expected);
    }

    @ParameterizedTest
    @EnumSource(IncomeStatementMethodEnum.class)
    @DisplayName("Tests MasterDataContextMapper to properly map all IncomeStatementMethodEnum values")
    void should_map_integer_to_incomeStatementMethodEnum(IncomeStatementMethodEnum incomeStatementMethodEnum) {
        objectToMap.setIncomeStatementMethod(incomeStatementMethodEnum.getValue());
        expected.setIncomeStatementMethod(incomeStatementMethodEnum);
        MasterDataContext masterDataContext = masterDataContextMapper.mapAcdsToDb(objectToMap);

        assertThat(masterDataContext).isNotNull();
        assertThat(masterDataContext).usingRecursiveComparison().isEqualTo(expected);
    }

    @ParameterizedTest
    @EnumSource(TypeOfIncomeEnum.class)
    @DisplayName("Tests MasterDataContextMapper to properly map all TypeOfIncomeEnum values")
    void should_map_integer_to_typeOfIncomeEnum(TypeOfIncomeEnum typeOfIncomeEnum) {
        objectToMap.setTypeOfIncome(typeOfIncomeEnum.getValue());
        expected.setTypeOfIncome(typeOfIncomeEnum);
        MasterDataContext masterDataContext = masterDataContextMapper.mapAcdsToDb(objectToMap);

        assertThat(masterDataContext).isNotNull();
        assertThat(masterDataContext).usingRecursiveComparison().isEqualTo(expected);
    }

    @ParameterizedTest
    @EnumSource(SpecialTaxationEnum.class)
    @DisplayName("Tests MasterDataContextMapper to properly map all SpecialTaxationEnum values")
    void should_map_integer_to_specialTaxationEnum(SpecialTaxationEnum specialTaxationEnum) {
        objectToMap.setSpecialTaxation(specialTaxationEnum.getValue());
        expected.setSpecialTaxation(specialTaxationEnum);
        MasterDataContext masterDataContext = masterDataContextMapper.mapAcdsToDb(objectToMap);

        assertThat(masterDataContext).isNotNull();
        assertThat(masterDataContext).usingRecursiveComparison().isEqualTo(expected);
    }

    private static Stream<Arguments> provideLegalFormValues() {
        return Stream.of(Arguments.of(0, LegalFormEnum.UNBEKANNT), Arguments.of(1, LegalFormEnum.EU), Arguments.of(2, LegalFormEnum.KAP),
                         Arguments.of(3, LegalFormEnum.GENOSSENSCHAFT), Arguments.of(4, LegalFormEnum.GBR), Arguments.of(5, LegalFormEnum.KAPCO),
                         Arguments.of(6, LegalFormEnum.KG), Arguments.of(7, LegalFormEnum.OHG), Arguments.of(8, LegalFormEnum.VEREIN),
                         Arguments.of(9, LegalFormEnum.STIFTUNG), Arguments.of(10, LegalFormEnum.KDOERNACHHGB),
                         Arguments.of(11, LegalFormEnum.KOERPERSCHAFTGEMHVKOMMHV), Arguments.of(12, LegalFormEnum.GKAP),
                         Arguments.of(13, LegalFormEnum.EU), Arguments.of(14, LegalFormEnum.GBR));
    }
}
