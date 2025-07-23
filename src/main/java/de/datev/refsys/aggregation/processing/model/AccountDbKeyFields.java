package de.datev.refsys.aggregation.processing.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Comparator;
import java.util.UUID;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class AccountDbKeyFields implements Comparable<AccountDbKeyFields> {
        private Integer accountNumber;
        private Integer accountingReasonId;
        private Integer recordType;
        private UUID rwShareholderId;
        private Integer businessAssetsAssignment;
        private Float taxRate;
        private Boolean accountingCommitted = false;
        private String cost1;
        private String cost2;
        private Integer agricultureAndForestryAccountType;

        @Override
        public int compareTo(AccountDbKeyFields o) {
                return Comparator.comparing(AccountDbKeyFields::getAccountNumber)
                                 .thenComparing(AccountDbKeyFields::getAccountingReasonId)
                                 .thenComparing(AccountDbKeyFields::getRecordType)
                                 .thenComparing(AccountDbKeyFields::getRwShareholderId)
                                 .thenComparing(AccountDbKeyFields::getBusinessAssetsAssignment)
                                 .thenComparing(AccountDbKeyFields::getTaxRate)
                                 .thenComparing(AccountDbKeyFields::getAccountingCommitted)
                                 .thenComparing(AccountDbKeyFields::getCost1)
                                 .thenComparing(AccountDbKeyFields::getCost2)
                                 .thenComparing(AccountDbKeyFields::getAgricultureAndForestryAccountType)
                                 .compare(this, o);
        }
}
