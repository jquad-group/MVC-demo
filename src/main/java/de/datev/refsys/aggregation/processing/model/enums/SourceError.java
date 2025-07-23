package de.datev.refsys.aggregation.processing.model.enums;

import lombok.Getter;

@Getter
public enum SourceError {
    ACDS("ACDS"),
    PROCESSING_SERVICE("Aggregation-Processing-Service");

    private final String value;

    SourceError(String value) {
        this.value = value;
    }
}
