package de.datev.refsys.aggregation.processing.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Comparator;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class InventoryDbKeyFields implements Comparable<InventoryDbKeyFields> {
    private Integer consultant;
    private Integer client;
    private Integer fiscalYear;
    private Integer accountNumber;
    private Integer accountingReason;

    @Override
    public int compareTo(InventoryDbKeyFields o) {
        return Comparator.comparing(InventoryDbKeyFields::getConsultant)
                         .thenComparing(InventoryDbKeyFields::getClient)
                         .thenComparing(InventoryDbKeyFields::getFiscalYear)
                         .thenComparing(InventoryDbKeyFields::getAccountNumber)
                         .thenComparing(InventoryDbKeyFields::getAccountingReason)
                         .compare(this, o);
    }
}
