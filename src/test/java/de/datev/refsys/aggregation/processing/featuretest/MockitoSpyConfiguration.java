package de.datev.refsys.aggregation.processing.featuretest;

import de.datev.refsys.aggregation.processing.client.CustomStructuresClient;
import de.datev.refsys.aggregation.processing.client.MasterDataClient;
import de.datev.refsys.aggregation.processing.client.MovementDataClient;
import de.datev.refsys.aggregation.processing.config.InitialLoadConfiguration;
import de.datev.refsys.aggregation.processing.mapper.AccountDbKeyFieldsMapper;
import de.datev.refsys.aggregation.processing.mapper.AccountSumDayMapper;
import de.datev.refsys.aggregation.processing.mapper.AccountValueMapper;
import de.datev.refsys.aggregation.processing.mapper.AdditionalParametersMapper;
import de.datev.refsys.aggregation.processing.mapper.AlternativeAccountTranslationMapper;
import de.datev.refsys.aggregation.processing.mapper.CollectiveAccountMapper;
import de.datev.refsys.aggregation.processing.mapper.CustomColumnStructureContentMapper;
import de.datev.refsys.aggregation.processing.mapper.CustomReportStructureContentMapper;
import de.datev.refsys.aggregation.processing.mapper.InventoryDbKeyFieldsMapper;
import de.datev.refsys.aggregation.processing.mapper.MasterDataContextMapper;
import de.datev.refsys.aggregation.processing.mapper.PreviousYearAccountTranslationMapper;
import de.datev.refsys.aggregation.processing.mapper.ShareholderAdditionMapper;
import de.datev.refsys.aggregation.processing.mapper.ShareholderAddressMapper;
import de.datev.refsys.aggregation.processing.mapper.ShareholderRelationMapper;
import de.datev.refsys.aggregation.processing.mapper.ShareholderTaxOfficeMapper;
import de.datev.refsys.aggregation.processing.repository.CustomColumnStructureContentRepository;
import de.datev.refsys.aggregation.processing.repository.CustomReportStructureContentRepository;
import de.datev.refsys.aggregation.processing.repository.MasterDataAccountRepository;
import de.datev.refsys.aggregation.processing.repository.MasterDataRepository;
import de.datev.refsys.aggregation.processing.repository.MovementDataDayRepository;
import de.datev.refsys.aggregation.processing.repository.MovementDataInventoryRepository;
import de.datev.refsys.aggregation.processing.repository.MovementDataMonthRepository;
import de.datev.refsys.aggregation.processing.repository.MovementDataPersonGroupDayRepository;
import de.datev.refsys.aggregation.processing.repository.MovementDataPersonGroupMonthRepository;
import de.datev.refsys.aggregation.processing.repository.StateDocRepository;
import de.datev.refsys.aggregation.processing.service.CustomStructuresService;
import de.datev.refsys.aggregation.processing.service.ImportService;
import de.datev.refsys.aggregation.processing.service.ImportServiceImpl;
import io.micrometer.core.instrument.MeterRegistry;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_PROFILE;

@Profile(TEST_PROFILE)
@Configuration
public class MockitoSpyConfiguration {

    @Primary
    @Bean
    public ImportService importService(StateDocRepository stateDocRepository, MasterDataRepository masterDataRepository,
                                       MovementDataDayRepository movementDataDayRepository, MovementDataMonthRepository movementDataMonthRepository,
                                       MovementDataPersonGroupDayRepository personGroupDayRepository,
                                       MovementDataPersonGroupMonthRepository personGroupMonthRepository,
                                       MasterDataAccountRepository masterDataAccountRepository,
                                       MovementDataInventoryRepository movementDataInventoryRepository, MeterRegistry meterRegistry,
                                       AdditionalParametersMapper additionalParametersMapper, MasterDataContextMapper masterDataContextMapper,
                                       CollectiveAccountMapper collectiveAccountMapper,
                                       AlternativeAccountTranslationMapper alternativeAccountTranslationMapper,
                                       PreviousYearAccountTranslationMapper previousYearAccountTranslationMapper,
                                       ShareholderRelationMapper shareholderRelationMapper, AccountDbKeyFieldsMapper accountDbKeyFieldsMapper,
                                       AccountValueMapper accountValueMapper, ShareholderAdditionMapper shareholderAdditionMapper,
                                       ShareholderAddressMapper shareholderAddressMapper, ShareholderTaxOfficeMapper shareholderTaxOfficeMapper,
                                       MasterDataClient masterDataClient, MovementDataClient movementDataClient,
                                       InitialLoadConfiguration initialLoadConfiguration, InventoryDbKeyFieldsMapper inventoryDbKeyFieldsMapper,
                                       AccountSumDayMapper accountSumDayMapper, CustomStructuresService customStructuresService) {
        return Mockito.spy(new ImportServiceImpl(stateDocRepository, masterDataRepository, movementDataDayRepository, movementDataMonthRepository,
                                                 personGroupDayRepository, personGroupMonthRepository, masterDataAccountRepository,
                                                 movementDataInventoryRepository, meterRegistry, additionalParametersMapper, masterDataContextMapper,
                                                 collectiveAccountMapper, alternativeAccountTranslationMapper, previousYearAccountTranslationMapper,
                                                 shareholderRelationMapper, accountDbKeyFieldsMapper, accountValueMapper, shareholderAdditionMapper,
                                                 shareholderAddressMapper, shareholderTaxOfficeMapper, masterDataClient, movementDataClient,
                                                 initialLoadConfiguration, inventoryDbKeyFieldsMapper, accountSumDayMapper, customStructuresService));
    }
}
