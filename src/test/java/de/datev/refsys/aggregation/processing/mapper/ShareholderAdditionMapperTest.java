package de.datev.refsys.aggregation.processing.mapper;

import de.datev.refsys.generated.acds.api.model.ShareholderAddition;
import de.datev.refsys.generated.acds.api.model.ShareholderAddressee;
import de.datev.refsys.generated.acds.api.model.ShareholderData;
import de.datev.refsys.generated.acds.api.model.ShareholderRelation;
import de.datev.refsys.generated.acds.api.model.ShareholderTaxOffice;
import de.datev.refsys.aggregation.document.model.ShareholderAddress;
import de.datev.refsys.aggregation.processing.util.TestDataLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ShareholderAdditionMapperTest {

    private final ShareholderAdditionMapper shareholderAdditionMapper = new ShareholderAdditionMapperImpl();

    private final ShareholderAddressMapper shareholderAddressMapper = new ShareholderAddressMapperImpl();

    private final ShareholderRelationMapper shareholderRelationMapper = new ShareholderRelationMapperImpl();

    private final ShareholderTaxOfficeMapper shareholderTaxOfficeMapper = new ShareholderTaxOfficeMapperImpl();

    private ShareholderData shareholderData;

    @BeforeEach
    void setUp() {
        shareholderData = TestDataLoader.load("json/acds-responses/mapper/shareholder.json", ShareholderData.class);
    }

    @Test
    void should_map_null_to_null_additionalParametersMapper() {
        assertThat(shareholderAdditionMapper.mapAcdsToDb((ShareholderAddition) null)).isNull();
        assertThat(shareholderAdditionMapper.mapAcdsToDb((List<ShareholderAddition>) null)).isEqualTo(new ArrayList<>());

        assertThat(shareholderAddressMapper.mapAcdsToDb((ShareholderAddressee) null)).isNull();
        assertThat(shareholderAddressMapper.mapAcdsToDb((List<ShareholderAddressee>) null)).isEqualTo(new ArrayList<>());

        assertThat(shareholderRelationMapper.mapAcdsToDb((ShareholderRelation) null)).isNull();
        assertThat(shareholderRelationMapper.mapAcdsToDb((List<ShareholderRelation>) null)).isEqualTo(new ArrayList<>());

        assertThat(shareholderTaxOfficeMapper.mapAcdsToDb((ShareholderTaxOffice) null)).isNull();
        assertThat(shareholderTaxOfficeMapper.mapAcdsToDb((List<ShareholderTaxOffice>) null)).isEqualTo(new ArrayList<>());
    }

    @Test
    void should_map_acds_shareholderRelation_to_dbShareholderRelation() {
        de.datev.refsys.aggregation.document.model.ShareholderRelation expected = TestDataLoader.loadDBElement("json/collections/expected/mapper/shareholderRelationExpected.json", de.datev.refsys.aggregation.document.model.ShareholderRelation.class);

        List<de.datev.refsys.aggregation.document.model.ShareholderRelation> shareholderRelations = shareholderRelationMapper.mapAcdsToDb(shareholderData.getShareholderRelations());

        de.datev.refsys.aggregation.document.model.ShareholderRelation shareholderRelation = shareholderRelations.get(0);
        assertThat(shareholderRelation).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void should_map_acdsShareholderAddition_to_dbShareholderAddition() {
        de.datev.refsys.aggregation.document.model.ShareholderAddition expected = TestDataLoader.loadDBElement("json/collections/expected/mapper/shareholderAdditionExpected.json", de.datev.refsys.aggregation.document.model.ShareholderAddition.class);

        List<de.datev.refsys.aggregation.document.model.ShareholderAddition> shareholderAdditions = shareholderAdditionMapper.mapAcdsToDb(shareholderData.getShareholderAdditions());

        de.datev.refsys.aggregation.document.model.ShareholderAddition shareholderAddition = shareholderAdditions.get(0);
        assertThat(shareholderAddition).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void should_map_acdsShareholderAddress_to_dbShareholderAddress() {
        ShareholderAddress expected = TestDataLoader.loadDBElement("json/collections/expected/mapper/shareholderAddressExpected.json", ShareholderAddress.class);

        List<ShareholderAddress> shareholderAddresses = shareholderAddressMapper.mapAcdsToDb(shareholderData.getShareholderAddressees());

        ShareholderAddress shareholderAddress = shareholderAddresses.get(0);
        assertThat(shareholderAddress).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void should_map_acds_shareholderTaxOffice_to_dbShareholderTaxOffice() {
        de.datev.refsys.aggregation.document.model.ShareholderTaxOffice expected = TestDataLoader.loadDBElement("json/collections/expected/mapper/shareholderTaxOfficeExpected.json", de.datev.refsys.aggregation.document.model.ShareholderTaxOffice.class);

        List<de.datev.refsys.aggregation.document.model.ShareholderTaxOffice> shareholderTaxOffices = shareholderTaxOfficeMapper.mapAcdsToDb(shareholderData.getShareholderTaxOffices());

        de.datev.refsys.aggregation.document.model.ShareholderTaxOffice shareholderTaxOffice = shareholderTaxOffices.get(0);
        assertThat(shareholderTaxOffice).usingRecursiveComparison().isEqualTo(expected);
    }

}
