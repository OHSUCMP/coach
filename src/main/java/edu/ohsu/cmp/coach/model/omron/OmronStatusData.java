package edu.ohsu.cmp.coach.model.omron;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class OmronStatusData {
    private static final DateFormat OMRON_LAST_UPDATED = new SimpleDateFormat("EEEE, MMMM d, YYYY 'at' h:mm a");

    private OmronStatus status;
    private Date lastUpdated;
    private Integer currentlyProcessing;
    private Integer totalToProcess;

    public OmronStatusData(OmronStatus status, Date lastUpdated, Integer currentlyProcessing, Integer totalToProcess) {
        this.status = status;
        this.lastUpdated = lastUpdated;
        this.currentlyProcessing = currentlyProcessing;
        this.totalToProcess = totalToProcess;
    }

    public OmronStatus getStatus() {
        return status;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public String getLastUpdatedString() {
        return OMRON_LAST_UPDATED.format(lastUpdated);
    }

    public Integer getCurrentlyProcessing() {
        return currentlyProcessing;
    }

    public Integer getTotalToProcess() {
        return totalToProcess;
    }
}
