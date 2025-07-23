package de.datev.refsys.aggregation.processing.mapper;

import de.datev.refsys.aggregation.document.model.MasterDataContext;
import de.datev.refsys.aggregation.document.model.enums.AccountPurposeMappingEnabledEnum;
import de.datev.refsys.aggregation.document.model.enums.CompanySizeClassEnum;
import de.datev.refsys.aggregation.document.model.enums.IncomeStatementMethodEnum;
import de.datev.refsys.aggregation.document.model.enums.LegalFormEnum;
import de.datev.refsys.aggregation.document.model.enums.SpecialTaxationEnum;
import de.datev.refsys.aggregation.document.model.enums.TypeOfIncomeEnum;
import de.datev.refsys.aggregation.processing.constant.ProcessingErrorMessageConstants;
import de.datev.refsys.generated.acds.api.model.CustomColumnStructure;
import de.datev.refsys.generated.acds.api.model.CustomReportStructure;
import de.datev.refsys.generated.acds.api.model.MasterdataContext;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = {
        AccountingReasonPropsMapper.class })
public interface MasterDataContextMapper {

    @Mapping(source = "masterdataContext.accountPurposeMappingEnabled", target = "accountPurposeMappingEnabled",
            qualifiedByName = "integerToAccountPurposeMappingEnabledEnum")
    @Mapping(source = "masterdataContext.legalForm", target = "legalForm", qualifiedByName = "integerToLegalFormEnum")
    @Mapping(source = "masterdataContext.companySizeClass", target = "companySizeClass", qualifiedByName = "integerToCompanySizeClassEnum")
    @Mapping(source = "masterdataContext.incomeStatementMethod", target = "incomeStatementMethod",
            qualifiedByName = "integerToIncomeStatementMethodEnum")
    @Mapping(source = "masterdataContext.typeOfIncome", target = "typeOfIncome", qualifiedByName = "integerToTypeOfIncomeEnum")
    @Mapping(source = "masterdataContext.specialTaxation", target = "specialTaxation", qualifiedByName = "integerToSpecialTaxationEnum")
    @Mapping(source = "masterdataContext.industryNo", target = "industryNo")
    @Mapping(source = "masterdataContext.sectionNo", target = "sectionNo")
    @Mapping(source = "masterdataContext.nationalRight", target = "nationalRight")
    MasterDataContext mapAcdsToDb(MasterdataContext masterdataContext);

    @Named("integerToAccountPurposeMappingEnabledEnum")
    static AccountPurposeMappingEnabledEnum integerToAccountPurposeMappingEnabledEnum(Integer value) {
        for (AccountPurposeMappingEnabledEnum b : AccountPurposeMappingEnabledEnum.values()) {
            if (b.getValue().equals(value)) {
                return b;
            }
        }
        throw new IllegalArgumentException(ProcessingErrorMessageConstants.UNEXPECTED_VALUE_ERROR + value + "'");
    }

    @Named("integerToLegalFormEnum")
    static LegalFormEnum integerToLegalFormEnum(Integer value) {
        return switch (value) {
            case 0 -> LegalFormEnum.UNBEKANNT;
            case 1, 13 -> LegalFormEnum.EU;
            case 2 -> LegalFormEnum.KAP;
            case 3 -> LegalFormEnum.GENOSSENSCHAFT;
            case 4, 14 -> LegalFormEnum.GBR;
            case 5 -> LegalFormEnum.KAPCO;
            case 6 -> LegalFormEnum.KG;
            case 7 -> LegalFormEnum.OHG;
            case 8 -> LegalFormEnum.VEREIN;
            case 9 -> LegalFormEnum.STIFTUNG;
            case 10 -> LegalFormEnum.KDOERNACHHGB;
            case 11 -> LegalFormEnum.KOERPERSCHAFTGEMHVKOMMHV;
            case 12 -> LegalFormEnum.GKAP;
            default -> throw new IllegalArgumentException(ProcessingErrorMessageConstants.UNEXPECTED_VALUE_ERROR + value + "'");
        };
    }

    @Named("integerToCompanySizeClassEnum")
    static CompanySizeClassEnum integerToCompanySizeClassEnum(Integer value) {
        for (CompanySizeClassEnum b : CompanySizeClassEnum.values()) {
            if (b.getValue().equals(value)) {
                return b;
            }
        }
        return null;
    }

    @Named("integerToIncomeStatementMethodEnum")
    static IncomeStatementMethodEnum integerToIncomeStatementMethodEnum(Integer value) {
        for (IncomeStatementMethodEnum b : IncomeStatementMethodEnum.values()) {
            if (b.getValue().equals(value)) {
                return b;
            }
        }
        return null;
    }

    @Named("integerToTypeOfIncomeEnum")
    static TypeOfIncomeEnum integerToTypeOfIncomeEnum(Integer value) {
        for (TypeOfIncomeEnum b : TypeOfIncomeEnum.values()) {
            if (b.getValue().equals(value)) {
                return b;
            }
        }
        return null;
    }

    @Named("integerToSpecialTaxationEnum")
    static SpecialTaxationEnum integerToSpecialTaxationEnum(Integer value) {
        for (SpecialTaxationEnum b : SpecialTaxationEnum.values()) {
            if (b.getValue().equals(value)) {
                return b;
            }
        }
        throw new IllegalArgumentException(ProcessingErrorMessageConstants.UNEXPECTED_VALUE_ERROR + value + "'");
    }
}
