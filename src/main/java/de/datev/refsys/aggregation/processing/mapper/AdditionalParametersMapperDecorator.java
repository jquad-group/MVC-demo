package de.datev.refsys.aggregation.processing.mapper;

import de.datev.refsys.generated.acds.api.model.AccountSumDay;
import de.datev.refsys.aggregation.document.model.AdditionalParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Objects;
import java.util.stream.Stream;

public abstract class AdditionalParametersMapperDecorator implements AdditionalParametersMapper {

    @Autowired
    @Qualifier("delegate")
    private AdditionalParametersMapper delegate;

    @Override
    public AdditionalParameters accountSumDayToAdditionalParameters(AccountSumDay accountSumDay) {
        AdditionalParameters additionalParameters = delegate.accountSumDayToAdditionalParameters(accountSumDay);
        if (additionalParameters == null || allFieldsAreNull(additionalParameters)) {
            return null;
        }
        return additionalParameters;
    }

    private static boolean allFieldsAreNull(AdditionalParameters ap) {
        return Stream.of(ap.getRecordType(), ap.getCost1(), ap.getAccountingCommitted(), ap.getCost2(), ap.getTaxRate(),
                        ap.getAgricultureAndForestryAccountType(), ap.getBusinessAssetsAssignment(), ap.getRwShareholderId())
                .allMatch(Objects::isNull);
    }
}
