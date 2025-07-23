package de.datev.refsys.aggregation.processing.featuretest;

import de.datev.refsys.aggregation.document.model.MovementDataDay;
import de.datev.refsys.aggregation.document.model.MovementDataMonth;
import de.datev.refsys.aggregation.document.model.MovementDataPersonGroupDay;
import de.datev.refsys.aggregation.document.model.MovementDataPersonGroupMonth;
import de.datev.refsys.aggregation.processing.api.model.DeltaInfo;
import de.datev.refsys.aggregation.processing.api.model.DeltaRequest;
import de.datev.refsys.aggregation.processing.exception.RestWarnException;
import de.datev.refsys.aggregation.processing.featuretest.model.BookingKmvz;
import de.datev.refsys.aggregation.processing.featuretest.model.DeltaRequestContent;
import io.cucumber.spring.ScenarioScope;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@ScenarioScope(proxyMode = ScopedProxyMode.NO)
@Component
@NoArgsConstructor
@Getter
@Setter
public class DeltaEventFeatureContext {

    @Setter(AccessLevel.NONE)
    private List<MovementDataDay> existingDayValues = new ArrayList<>();

    @Setter(AccessLevel.NONE)
    private List<MovementDataMonth> existingMonthValues = new ArrayList<>();

    @Setter(AccessLevel.NONE)
    private List<MovementDataPersonGroupDay> existingPersonGroupDayValues = new ArrayList<>();

    @Setter(AccessLevel.NONE)
    private List<MovementDataPersonGroupMonth> existingPersonGroupMonthValues = new ArrayList<>();

    private int consultant;
    private int client;
    private int fiscalYearBegin;
    private int fiscalYearEnd;
    private double personGroupUsual;
    private DeltaRequest deltaRequest;
    private List<DeltaRequestContent> deltaRequestContents;
    private long baseVersion;
    private long deltaVersion;
    private long stateDocBaseVersion;
    private long stateDocDeltaVersion;
    List<BookingKmvz> bookingsInEvent;
    private RestWarnException restWarnException;
    private ResponseStatusException responseStatusException;
    private List<DeltaInfo> deltaInfos;

    public void addExistingDayValues(List<MovementDataDay> newDayValues) {
        this.existingDayValues.addAll(newDayValues);
    }

    public void addExistingMonthValues(List<MovementDataMonth> newMonthValues) {
        this.existingMonthValues.addAll(newMonthValues);
    }

    public void addExistingPersonGroupDayValues(List<MovementDataPersonGroupDay> newDayValues) {
        this.existingPersonGroupDayValues.addAll(newDayValues);
    }

    public void addExistingPersonGroupMonthValues(List<MovementDataPersonGroupMonth> newMonthValues) {
        this.existingPersonGroupMonthValues.addAll(newMonthValues);
    }

}
