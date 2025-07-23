package de.datev.refsys.aggregation.processing.service;

import de.datev.refsys.aggregation.processing.model.ImportData;

public interface ImportService {
    /**
     * Imports all data
     *
     * @param importData contains MasterdataContext and StateDoc
     * @return boolean indicating success/failure of the import operation
     */
    Boolean executeFullImport(ImportData importData);
}