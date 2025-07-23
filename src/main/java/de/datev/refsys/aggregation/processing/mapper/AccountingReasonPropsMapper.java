package de.datev.refsys.aggregation.processing.mapper;

import de.datev.refsys.aggregation.document.model.AccountingReasonProps;
import de.datev.refsys.aggregation.document.model.enums.AccountingReasonEnum;
import de.datev.refsys.aggregation.document.model.enums.MethodOfDeterminingNetIncomeEnum;
import de.datev.refsys.aggregation.document.model.enums.PermittedAccountingReasonsEnum;
import de.datev.refsys.aggregation.processing.constant.ProcessingErrorMessageConstants;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface AccountingReasonPropsMapper {

    @Mapping(source = "accountingReason", target = "accountingReason", qualifiedByName = "integerToAccountingReasonEnum")
    @Mapping(source = "methodOfDeterminingNetIncome", target = "methodOfDeterminingNetIncome",
            qualifiedByName = "integerToMethodOfDeterminingNetIncomeEnum")
    @Mapping(source = "permittedAccountingReasons", target = "permittedAccountingReasons", qualifiedByName =
            "integerToPermittedAccountingReasonsEnum")
    AccountingReasonProps accountingReasonProps(de.datev.refsys.generated.acds.api.model.AccountingReasonProps accountingReasonProps);

    @Named("integerToAccountingReasonEnum")
    static AccountingReasonEnum integerToAccountingReasonEnum(Integer value) {
        for (AccountingReasonEnum b : AccountingReasonEnum.values()) {
            if (b.getValue().equals(value)) {
                return b;
            }
        }
        throw new IllegalArgumentException(ProcessingErrorMessageConstants.UNEXPECTED_VALUE_ERROR + value + "'");
    }

    @Named("integerToMethodOfDeterminingNetIncomeEnum")
    static MethodOfDeterminingNetIncomeEnum integerToMethodOfDeterminingNetIncomeEnum(Integer value) {
        for (MethodOfDeterminingNetIncomeEnum b : MethodOfDeterminingNetIncomeEnum.values()) {
            if (b.getValue().equals(value)) {
                return b;
            }
        }
        throw new IllegalArgumentException(ProcessingErrorMessageConstants.UNEXPECTED_VALUE_ERROR + value + "'");
    }

    @Named("integerToPermittedAccountingReasonsEnum")
    static PermittedAccountingReasonsEnum integerToPermittedAccountingReasonsEnum(Integer value) {
        for (PermittedAccountingReasonsEnum b : PermittedAccountingReasonsEnum.values()) {
            if (b.getValue().equals(value)) {
                return b;
            }
        }
        throw new IllegalArgumentException(ProcessingErrorMessageConstants.UNEXPECTED_VALUE_ERROR + value + "'");
    }
}
