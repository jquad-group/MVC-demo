package de.datev.refsys.aggregation.processing.model;

import de.datev.refsys.aggregation.document.model.MovementDataDay;
import de.datev.refsys.aggregation.document.model.MovementDataMonth;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class MovementDataAccountValues {
    private Map<AccountDbKeyFields, MovementDataDay> accountDayMap = new LinkedHashMap<>();
    private Map<AccountDbKeyFields, MovementDataMonth> accountMonthMap = new LinkedHashMap<>();
    private Set<Integer> individualPersonAccountNumbers = new HashSet<>();
}
