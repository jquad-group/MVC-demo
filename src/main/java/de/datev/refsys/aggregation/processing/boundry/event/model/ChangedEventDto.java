package de.datev.refsys.aggregation.processing.boundry.event.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ChangedEventDto {
    @JsonProperty("consultant")
    private Integer consultant;

    @JsonProperty("client")
    private Integer client;

    @JsonProperty("fiscal_year")
    private Integer fiscalYear;

    @JsonProperty("base_version")
    private Long baseVersion;

    @JsonProperty("delta_version")
    private Long deltaVersion;
}
