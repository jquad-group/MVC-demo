package de.datev.refsys.aggregation.processing.service;

import com.mongodb.client.result.UpdateResult;
import de.datev.refsys.aggregation.document.model.MasterDataContext;
import de.datev.refsys.aggregation.document.model.StateDoc;
import de.datev.refsys.aggregation.processing.client.CustomStructuresClient;
import de.datev.refsys.aggregation.processing.client.MasterDataClient;
import de.datev.refsys.aggregation.processing.client.MovementDataClient;
import de.datev.refsys.aggregation.processing.exception.AggregationProcessingBusinessException;
import de.datev.refsys.aggregation.processing.exception.InitialLoadFailedException;
import de.datev.refsys.aggregation.processing.model.ImportData;
import de.datev.refsys.aggregation.processing.repository.MasterDataRepository;
import de.datev.refsys.aggregation.processing.repository.MovementDataDayRepository;
import de.datev.refsys.aggregation.processing.repository.MovementDataInventoryRepository;
import de.datev.refsys.aggregation.processing.repository.MovementDataMonthRepository;
import de.datev.refsys.aggregation.processing.repository.StateDocRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * MVC version of ImportService implementation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImportServiceMvc implements ImportService {
    
    private final MasterDataClient masterDataClient;
    private final MovementDataClient movementDataClient;
    private final CustomStructuresClient customStructuresClient;
    private final MasterDataRepository masterDataRepository;
    private final MovementDataDayRepository movementDataDayRepository;
    private final MovementDataMonthRepository movementDataMonthRepository;
    private final MovementDataInventoryRepository movementDataInventoryRepository;
    private final StateDocRepository stateDocRepository;
    private final PersonGroupAggregationService personGroupAggregationService;
    private final CustomStructuresService customStructuresService;

    @Override
    @Transactional
    public Boolean executeFullImport(ImportData importData) {
        try {
            log.info("Starting full import for consultant={}, client={}, fiscalYear={}", 
                    importData.masterdataContext().getConsultant(),
                    importData.masterdataContext().getClient(), 
                    importData.masterdataContext().getYearBegin());

            // 1. Import Master Data (synchronous)
            boolean masterDataSuccess = importMasterData(importData.masterdataContext());
            if (!masterDataSuccess) {
                throw new InitialLoadFailedException("Master data import failed");
            }

            // 2. Import Movement Data Days (synchronous)
            boolean movementDaysSuccess = importMovementDataDays(importData.masterdataContext(), importData.stateDoc());
            if (!movementDaysSuccess) {
                throw new InitialLoadFailedException("Movement data days import failed");
            }

            // 3. Import Movement Data Months (synchronous)
            boolean movementMonthsSuccess = importMovementDataMonths(importData.masterdataContext(), importData.stateDoc());
            if (!movementMonthsSuccess) {
                throw new InitialLoadFailedException("Movement data months import failed");
            }

            // 4. Import Movement Data Inventories (synchronous)
            boolean inventorySuccess = importMovementDataInventories(importData.masterdataContext(), importData.stateDoc());
            if (!inventorySuccess) {
                throw new InitialLoadFailedException("Inventory import failed");
            }

            // 5. Process Person Group Aggregations (synchronous)
            boolean personGroupSuccess = processPersonGroupAggregations(importData.masterdataContext());
            if (!personGroupSuccess) {
                throw new InitialLoadFailedException("Person group aggregations failed");
            }

            // 6. Import Custom Structures (synchronous)
            boolean customStructuresSuccess = importCustomStructures(importData.masterdataContext());
            if (!customStructuresSuccess) {
                throw new InitialLoadFailedException("Custom structures import failed");
            }

            // 7. Update State Document to completed
            UpdateResult stateDocUpdate = stateDocRepository.updateStateToCompleted(
                importData.masterdataContext().getConsultant(),
                importData.masterdataContext().getClient(),
                importData.masterdataContext().getYearBegin()
            );

            if (!stateDocUpdate.wasAcknowledged()) {
                throw new InitialLoadFailedException("Failed to update state document");
            }

            log.info("Successfully completed full import for consultant={}, client={}, fiscalYear={}", 
                    importData.masterdataContext().getConsultant(),
                    importData.masterdataContext().getClient(), 
                    importData.masterdataContext().getYearBegin());

            return true;

        } catch (Exception e) {
            log.error("Import failed for consultant={}, client={}, fiscalYear={}", 
                     importData.masterdataContext().getConsultant(),
                     importData.masterdataContext().getClient(), 
                     importData.masterdataContext().getYearBegin(), e);
            
            // Update state document to failed
            try {
                stateDocRepository.updateStateToFailed(
                    importData.masterdataContext().getConsultant(),
                    importData.masterdataContext().getClient(),
                    importData.masterdataContext().getYearBegin(),
                    e.getMessage()
                );
            } catch (Exception stateDocException) {
                log.error("Failed to update state document to failed state", stateDocException);
            }
            
            throw new AggregationProcessingBusinessException("Import operation failed", e);
        }
    }

    private boolean importMasterData(MasterDataContext masterDataContext) {
        try {
            log.debug("Importing master data for consultant={}, client={}, fiscalYear={}", 
                     masterDataContext.getConsultant(), masterDataContext.getClient(), masterDataContext.getYearBegin());

            // Synchronous call to master data client
            var masterDataResponse = masterDataClient.getMasterDataContext(
                masterDataContext.getConsultant(), 
                masterDataContext.getClient(), 
                masterDataContext.getYearBegin()
            );

            // Synchronous repository operations
            UpdateResult result = masterDataRepository.upsertOne(
                masterDataResponse.getContext(),
                masterDataResponse.getCollectiveAccounts(),
                masterDataResponse.getShareholderRelations(),
                masterDataResponse.getShareholderAdditions(),
                masterDataResponse.getShareholderAddresses(),
                masterDataResponse.getShareholderTaxOffices(),
                masterDataResponse.getPreviousYearAccountTranslations(),
                masterDataResponse.getAlternativeAccountTranslations(),
                masterDataResponse.getIndividualPersonAccountNumbers()
            );

            return result.wasAcknowledged();

        } catch (Exception e) {
            log.error("Master data import failed", e);
            return false;
        }
    }

    private boolean importMovementDataDays(MasterDataContext masterDataContext, StateDoc stateDoc) {
        try {
            log.debug("Importing movement data days for consultant={}, client={}, fiscalYear={}", 
                     masterDataContext.getConsultant(), masterDataContext.getClient(), masterDataContext.getYearBegin());

            // Synchronous call to movement data client
            var accountSumDays = movementDataClient.getAccountSumDays(masterDataContext);

            // Process in batches synchronously
            int batchSize = 1000;
            int processedCount = 0;
            
            for (int i = 0; i < accountSumDays.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, accountSumDays.size());
                var batch = accountSumDays.subList(i, endIndex);
                
                boolean batchResult = movementDataDayRepository.bulkUpsertMovementDataDays(
                    masterDataContext, batch, stateDoc
                );
                
                if (!batchResult) {
                    log.error("Failed to process movement data days batch starting at index {}", i);
                    return false;
                }
                
                processedCount += batch.size();
                log.debug("Processed {} of {} movement data days", processedCount, accountSumDays.size());
            }

            return true;

        } catch (Exception e) {
            log.error("Movement data days import failed", e);
            return false;
        }
    }

    private boolean importMovementDataMonths(MasterDataContext masterDataContext, StateDoc stateDoc) {
        try {
            log.debug("Importing movement data months");
            
            var accountSumMonths = movementDataClient.getAccountSumMonths(masterDataContext);
            
            boolean result = movementDataMonthRepository.bulkUpsertMovementDataMonths(
                masterDataContext, accountSumMonths, stateDoc
            );
            
            return result;

        } catch (Exception e) {
            log.error("Movement data months import failed", e);
            return false;
        }
    }

    private boolean importMovementDataInventories(MasterDataContext masterDataContext, StateDoc stateDoc) {
        try {
            log.debug("Importing movement data inventories");
            
            var inventories = movementDataClient.getMovementdataInventories(masterDataContext);
            
            boolean result = movementDataInventoryRepository.bulkUpsertInventories(
                masterDataContext, inventories, stateDoc
            );
            
            return result;

        } catch (Exception e) {
            log.error("Movement data inventories import failed", e);
            return false;
        }
    }

    private boolean processPersonGroupAggregations(MasterDataContext masterDataContext) {
        try {
            log.debug("Processing person group aggregations");
            
            boolean result = personGroupAggregationService.processPersonGroupAggregations(
                masterDataContext.getConsultant(),
                masterDataContext.getClient(),
                masterDataContext.getYearBegin()
            );
            
            return result;

        } catch (Exception e) {
            log.error("Person group aggregations failed", e);
            return false;
        }
    }

    private boolean importCustomStructures(MasterDataContext masterDataContext) {
        try {
            log.debug("Importing custom structures");
            
            boolean result = customStructuresService.importCustomStructures(
                masterDataContext.getConsultant(),
                masterDataContext.getClient(),
                masterDataContext.getYearBegin()
            );
            
            return result;

        } catch (Exception e) {
            log.error("Custom structures import failed", e);
            return false;
        }
    }
}