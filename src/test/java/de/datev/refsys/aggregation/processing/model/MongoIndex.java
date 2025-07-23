package de.datev.refsys.aggregation.processing.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MongoIndex {
    private Integer v;
    private Map<String, Integer> key;
    private String name;
    private Boolean unique;
}