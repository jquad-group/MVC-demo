package de.datev.refsys.aggregation.processing.mapper;

import de.datev.refsys.aggregation.processing.util.Util;
import de.datev.refsys.generated.acds.api.model.AccountSumDay;
import de.datev.refsys.aggregation.document.model.AdditionalParameters;
import org.bson.types.Decimal128;
import org.mapstruct.DecoratedWith;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
@DecoratedWith(AdditionalParametersMapperDecorator.class)
public interface AdditionalParametersMapper {
    int DEFAULT_RECORD_TYPE = 1;
    int DEFAULT_BUSINESS_ASSETS_ASSIGNMENT = 0;
    int DEFAULT_AGRICULTURE_AND_FORESTRY_ACCOUNT_TYPE = 0;
    float DEFAULT_TAX_RATE = 0.00F;
    boolean DEFAULT_ACCOUNTING_COMMITTED = true;

    @Mapping(source = "accountSumDay.taxRate", target = "taxRate", qualifiedByName = "checkTaxRateAndConvertToDecimal")
    @Mapping(source = "accountSumDay.recordType", target = "recordType", qualifiedByName = "checkRecordType")
    @Mapping(source = "accountSumDay.accountingCommitted", target = "accountingCommitted", qualifiedByName = "checkAccountingCommitted")
    @Mapping(source = "accountSumDay.businessAssetsAssignment", target = "businessAssetsAssignment", qualifiedByName = "checkBusinessAssignment")
    @Mapping(source = "accountSumDay.agricultureAndForestryAccountType", target = "agricultureAndForestryAccountType",
            qualifiedByName = "checkAgricultureAndForestryAccountType")
    AdditionalParameters accountSumDayToAdditionalParameters(AccountSumDay accountSumDay);

    @Named("checkTaxRateAndConvertToDecimal")
    static Decimal128 checkTaxRateAndConvertToDecimal(Float value) {
        if (value == null || Float.compare(value, DEFAULT_TAX_RATE) == 0) {
            return null;
        }
        return Decimal128.parse(String.valueOf(value));
    }

    @Named("checkRecordType")
    static Integer checkRecordType(Integer value) {
        return Util.nullIfDefault(value, DEFAULT_RECORD_TYPE);
    }

    @Named("checkAccountingCommitted")
    static Boolean checkAccountingCommitted(Boolean value) {
        return Util.nullIfDefault(value, DEFAULT_ACCOUNTING_COMMITTED);
    }

    @Named("checkAgricultureAndForestryAccountType")
    static Integer checkAgricultureAndForestryAccountType(Integer value) {
        return Util.nullIfDefault(value, DEFAULT_AGRICULTURE_AND_FORESTRY_ACCOUNT_TYPE);
    }

    @Named("checkBusinessAssignment")
    static Integer checkBusinessAssignment(Integer value) {
        return Util.nullIfDefault(value, DEFAULT_BUSINESS_ASSETS_ASSIGNMENT);
    }
}
