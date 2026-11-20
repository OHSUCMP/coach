package edu.ohsu.cmp.coach.model.omron;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OmronStatusData {
    private static final DateFormat OMRON_LAST_UPDATED = new SimpleDateFormat("EEEE, MMMM d, yyyy 'at' h:mm a");

    private final OmronStatus status;
    private final Date lastUpdated;
    private final Integer currentlyProcessing;
    private final Integer totalToProcess;
    private final List<SyncError> errors;

    public OmronStatusData(OmronStatus status, Date lastUpdated, Integer currentlyProcessing, Integer totalToProcess,
                           List<String> errors) {
        this.status = status;
        this.lastUpdated = lastUpdated;
        this.currentlyProcessing = currentlyProcessing;
        this.totalToProcess = totalToProcess;

        // group identical error messages, preserving the order in which they first occurred
        Map<String, Integer> counts = new LinkedHashMap<>();
        if (errors != null) {
            for (String message : errors) {
                counts.merge(message, 1, Integer::sum);
            }
        }
        this.errors = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            this.errors.add(new SyncError(entry.getKey(), entry.getValue()));
        }
    }

    public OmronStatus getStatus() {
        return status;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public String getLastUpdatedString() {
        if (lastUpdated == null) return "";
        synchronized (OMRON_LAST_UPDATED) {     // SimpleDateFormat is not thread-safe
            return OMRON_LAST_UPDATED.format(lastUpdated);
        }
    }

    public Integer getCurrentlyProcessing() {
        return currentlyProcessing;
    }

    public Integer getTotalToProcess() {
        return totalToProcess;
    }

    public List<SyncError> getErrors() {
        return errors;
    }

    public static class SyncError {
        private final String message;
        private final int count;

        public SyncError(String message, int count) {
            this.message = message;
            this.count = count;
        }

        public String getMessage() {
            return message;
        }

        public int getCount() {
            return count;
        }
    }
}
