package de.datev.refsys.aggregation.processing.configuration;

import de.datev.refsys.aggregation.processing.mapper.AccountDbKeyFieldsMapper;
import de.datev.refsys.aggregation.processing.mapper.AccountDbKeyFieldsMapperImpl;
import de.datev.refsys.aggregation.processing.mapper.AccountSumDayMapper;
import de.datev.refsys.aggregation.processing.mapper.AccountSumDayMapperImpl;
import de.datev.refsys.aggregation.processing.mapper.AccountValueMapper;
import de.datev.refsys.aggregation.processing.mapper.AccountValueMapperImpl;
import de.datev.refsys.aggregation.processing.mapper.AccountingReasonPropsMapper;
import de.datev.refsys.aggregation.processing.mapper.AccountingReasonPropsMapperImpl;
import de.datev.refsys.aggregation.processing.mapper.AdditionalParametersMapper;
import de.datev.refsys.aggregation.processing.mapper.AdditionalParametersMapperImpl;
import de.datev.refsys.aggregation.processing.mapper.AdditionalParametersMapperImpl_;
import de.datev.refsys.aggregation.processing.mapper.AlternativeAccountTranslationMapper;
import de.datev.refsys.aggregation.processing.mapper.AlternativeAccountTranslationMapperImpl;
import de.datev.refsys.aggregation.processing.mapper.CollectiveAccountMapper;
import de.datev.refsys.aggregation.processing.mapper.CollectiveAccountMapperImpl;
import de.datev.refsys.aggregation.processing.mapper.CustomColumnStructureContentMapper;
import de.datev.refsys.aggregation.processing.mapper.CustomColumnStructureContentMapperImpl;
import de.datev.refsys.aggregation.processing.mapper.CustomColumnStructureInfoMapper;
import de.datev.refsys.aggregation.processing.mapper.CustomColumnStructureInfoMapperImpl;
import de.datev.refsys.aggregation.processing.mapper.CustomReportStructureContentMapper;
import de.datev.refsys.aggregation.processing.mapper.CustomReportStructureContentMapperImpl;
import de.datev.refsys.aggregation.processing.mapper.CustomReportStructureInfoMapper;
import de.datev.refsys.aggregation.processing.mapper.CustomReportStructureInfoMapperImpl;
import de.datev.refsys.aggregation.processing.mapper.InventoryDbKeyFieldsMapper;
import de.datev.refsys.aggregation.processing.mapper.InventoryDbKeyFieldsMapperImpl;
import de.datev.refsys.aggregation.processing.mapper.InventoryMapper;
import de.datev.refsys.aggregation.processing.mapper.InventoryMapperImpl;
import de.datev.refsys.aggregation.processing.mapper.InventoryValueMapper;
import de.datev.refsys.aggregation.processing.mapper.InventoryValueMapperImpl;
import de.datev.refsys.aggregation.processing.mapper.MasterDataContextMapperImpl;
import de.datev.refsys.aggregation.processing.mapper.PreviousYearAccountTranslationMapper;
import de.datev.refsys.aggregation.processing.mapper.PreviousYearAccountTranslationMapperImpl;
import de.datev.refsys.aggregation.processing.mapper.ProblemInfoMapper;
import de.datev.refsys.aggregation.processing.mapper.ProblemInfoMapperImpl;
import de.datev.refsys.aggregation.processing.mapper.ShareholderAdditionMapper;
import de.datev.refsys.aggregation.processing.mapper.ShareholderAdditionMapperImpl;
import de.datev.refsys.aggregation.processing.mapper.ShareholderAddressMapper;
import de.datev.refsys.aggregation.processing.mapper.ShareholderAddressMapperImpl;
import de.datev.refsys.aggregation.processing.mapper.ShareholderRelationMapper;
import de.datev.refsys.aggregation.processing.mapper.ShareholderRelationMapperImpl;
import de.datev.refsys.aggregation.processing.mapper.ShareholderTaxOfficeMapper;
import de.datev.refsys.aggregation.processing.mapper.ShareholderTaxOfficeMapperImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class ImportServiceConfiguration {

    @Bean
    public AccountingReasonPropsMapper accountingReasonPropsMapper() {
        return new AccountingReasonPropsMapperImpl();
    }

    @Bean
    public MasterDataContextMapperImpl masterDataContextMapperImpl(AccountingReasonPropsMapper accountingReasonPropsMapper) {
        return new MasterDataContextMapperImpl(accountingReasonPropsMapper);
    }

    @Bean
    public CollectiveAccountMapper collectiveAccountMapper() {
        return new CollectiveAccountMapperImpl();
    }

    @Bean
    public AlternativeAccountTranslationMapper alternativeAccountTranslationMapper() {
        return new AlternativeAccountTranslationMapperImpl();
    }

    @Bean
    public PreviousYearAccountTranslationMapper previousYearAccountTranslationMapper() {
        return new PreviousYearAccountTranslationMapperImpl();
    }

    @Bean
    public ShareholderRelationMapper shareholderRelationMapper() {
        return new ShareholderRelationMapperImpl();
    }

    @Bean
    public ShareholderAdditionMapper shareholderAdditionMapper() {
        return new ShareholderAdditionMapperImpl();
    }

    @Bean
    public ShareholderAddressMapper shareholderAddressMapper() {
        return new ShareholderAddressMapperImpl();
    }

    @Bean
    public ShareholderTaxOfficeMapper shareholderTaxOfficeMapper() { return new ShareholderTaxOfficeMapperImpl(); }

    @Bean
    public AccountDbKeyFieldsMapper accountDbKeyFieldsMapper() {
        return new AccountDbKeyFieldsMapperImpl();
    }

    @Bean
    public AccountValueMapper accountValueMapper() { return new AccountValueMapperImpl();  }

    @Bean
    @Qualifier("delegate")
    public AdditionalParametersMapper delegate() {
        return new AdditionalParametersMapperImpl_();
    }

    @Bean
    public AdditionalParametersMapper additionalParametersMapper() {
        return new AdditionalParametersMapperImpl();
    }

    @Bean
    public InventoryMapper inventoryMapper() { return new InventoryMapperImpl(); }

    @Bean
    public InventoryValueMapper inventoryValueMapper() {
        return new InventoryValueMapperImpl();
    }

    @Bean
    public InventoryDbKeyFieldsMapper inventoryDbKeyFieldsMapper() {
        return new InventoryDbKeyFieldsMapperImpl();
    }

    @Bean
    public AccountSumDayMapper accountSumDayMapper() {
        return new AccountSumDayMapperImpl();
    }

    @Bean
    public CustomReportStructureInfoMapper customReportStructureInfoMapper() {
        return new CustomReportStructureInfoMapperImpl();
    }

    @Bean
    public CustomColumnStructureInfoMapper customColumnStructureInfoMapper() {
        return new CustomColumnStructureInfoMapperImpl();
    }

    @Bean
    public CustomReportStructureContentMapper customReportStructureContentMapper() {
        return new CustomReportStructureContentMapperImpl();
    }

    @Bean
    public CustomColumnStructureContentMapper customColumnStructureContentMapper() {
        return new CustomColumnStructureContentMapperImpl();
    }
}
