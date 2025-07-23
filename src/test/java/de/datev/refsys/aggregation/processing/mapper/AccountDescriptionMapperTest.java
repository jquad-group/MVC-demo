package de.datev.refsys.aggregation.processing.mapper;

import de.datev.refsys.generated.acds.api.model.AccountCaption;
import de.datev.refsys.aggregation.document.model.AccountDescription;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AccountDescriptionMapperTest {

    private final AccountDescriptionMapper accountDescriptionMapper = new AccountDescriptionMapperImpl();

    @Test
    void should_map_null_to_null_accountDescriptionMapper() {
        assertThat(accountDescriptionMapper.mapAcdsToDb(null)).isNull();
        assertThat(accountDescriptionMapper.mapAcdsToDbList(null)).isEqualTo(new ArrayList<>());
    }

    @Test
    void should_map_acds_to_dbAccountCaptions() {
        //arrange
        ArrayList<AccountCaption> captionList = createInput();
        List<AccountDescription> expectedList = createExpected();

        //act
        List<AccountDescription> accountDescriptions = accountDescriptionMapper.mapAcdsToDbList(captionList);

        //assert
        assertThat(accountDescriptions).isNotEmpty();
        for (int i = 0; i <= 1; i++) {
            assertAccountDescription(accountDescriptions.get(i), expectedList.get(i));
        }
    }

    private void assertAccountDescription(AccountDescription actual, AccountDescription expected) {
        assertThat(actual.getAccountNumber()).isEqualTo(expected.getAccountNumber());
        assertThat(actual.getUsed()).isEqualTo(expected.getUsed());
        assertThat(actual.getCaptions()).isEqualTo(expected.getCaptions());
    }

    private ArrayList<AccountCaption> createInput() {
        ArrayList<AccountCaption> captionList = new ArrayList<>();

        AccountCaption caption1 = new AccountCaption();
        caption1.setAccountNumber(50000);
        caption1.setCaption("Caption1");
        caption1.setCaptionLong("CaptionLong1");
        caption1.setCultureCode("de-DE");
        captionList.add(caption1);

        AccountCaption caption2 = new AccountCaption();
        caption2.setAccountNumber(100000);
        caption2.setCaption("Caption1");
        caption2.setCaptionLong("CaptionLong1");
        caption2.setCultureCode("de-DE");
        captionList.add(caption2);

        return captionList;
    }

    private ArrayList<AccountDescription> createExpected() {
        ArrayList<AccountDescription> expectedList = new ArrayList<>();
        AccountDescription e = new AccountDescription();
        e.setAccountNumber(50000);
        AccountDescription e2 = new AccountDescription();
        e2.setAccountNumber(100000);
        expectedList.add(e);
        expectedList.add(e2);

        return expectedList;
    }

}
