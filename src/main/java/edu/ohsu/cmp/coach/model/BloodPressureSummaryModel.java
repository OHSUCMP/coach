package edu.ohsu.cmp.coach.model;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class BloodPressureSummaryModel {
    private static final Integer MS_IN_DAY = 1000 * 60 * 60 * 24; // 86,400,000

    private Integer avgSystolic;
    private Integer avgDiastolic;
    private Integer mostRecentSystolic;
    private Integer mostRecentDiastolic;
    private Date mostRecentDate;
    private Integer secondMostRecentSystolic;
    private Integer secondMostRecentDiastolic;
    private Date secondMostRecentDate;
    private Integer recentHomeBPReadingsCount;
    private Integer recentHomeBPReadingsDayCount;

    public BloodPressureSummaryModel(List<BloodPressureModel> list) {
        List<BloodPressureModel> bpList = new ArrayList<>();     // create a copy of list to work with, don't want to modify the original
        bpList.addAll(list);
        bpList.sort((o1, o2) -> o1.getReadingDate().compareTo(o2.getReadingDate()) * -1); // sort newest first

        if (bpList.size() > 0) {
            mostRecentSystolic = bpList.get(0).getSystolic().getValue();
            mostRecentDiastolic = bpList.get(0).getDiastolic().getValue();
            mostRecentDate = bpList.get(0).getReadingDate();

            if (bpList.size() > 1) {
                secondMostRecentSystolic = bpList.get(1).getSystolic().getValue();
                secondMostRecentDiastolic = bpList.get(1).getDiastolic().getValue();
                secondMostRecentDate = bpList.get(1).getReadingDate();
            }
        }

        long timeframeStartTS = getStartOfDay30DaysAgoTS();
        Long earliestHomeReadingWithinTimeframeTS = null;
        Long latestHomeReadingWithinTimeframeTS = null;
        recentHomeBPReadingsCount = 0;
        for (BloodPressureModel bpm : bpList) {
            if (bpm.isHomeReading() && bpm.getReadingDateTimestamp() >= timeframeStartTS) {
                recentHomeBPReadingsCount++;
                if (earliestHomeReadingWithinTimeframeTS == null || bpm.getReadingDateTimestamp() < earliestHomeReadingWithinTimeframeTS) {
                    earliestHomeReadingWithinTimeframeTS = bpm.getReadingDateTimestamp();
                }
                if (latestHomeReadingWithinTimeframeTS == null || bpm.getReadingDateTimestamp() > latestHomeReadingWithinTimeframeTS) {
                    latestHomeReadingWithinTimeframeTS = bpm.getReadingDateTimestamp();
                }
            }
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.set(Calendar.HOUR_OF_DAY, 12);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long todayAtNoonTS = cal.getTimeInMillis();

        recentHomeBPReadingsDayCount = earliestHomeReadingWithinTimeframeTS != null ?
                Math.round((float) (todayAtNoonTS - earliestHomeReadingWithinTimeframeTS) / MS_IN_DAY) :
                30; // default to 30 days


        // calculate average using same logic as in the UI
        List<BloodPressureModel> bpSet = buildBPSet(bpList);
        if (bpSet != null) {
            int totalSystolic = 0;
            int totalDiastolic = 0;

            for (BloodPressureModel bpm : bpSet) {
                totalSystolic += bpm.getSystolic().getValue();
                totalDiastolic += bpm.getDiastolic().getValue();
            }

            avgSystolic = Math.round((float) totalSystolic / bpSet.size());
            avgDiastolic = Math.round((float) totalDiastolic / bpSet.size());
        }
    }

    public Boolean hasCalculatedAverage() {
        return avgSystolic != null && avgDiastolic != null;
    }

    public Integer getAvgSystolic() {
        return avgSystolic;
    }

    public Integer getAvgDiastolic() {
        return avgDiastolic;
    }

    public Boolean hasMostRecent() {
        return mostRecentSystolic != null && mostRecentDiastolic != null && mostRecentDate != null;
    }

    public Integer getMostRecentSystolic() {
        return mostRecentSystolic;
    }

    public Integer getMostRecentDiastolic() {
        return mostRecentDiastolic;
    }

    public Date getMostRecentDate() {
        return mostRecentDate;
    }

    public Boolean hasSecondMostRecent() {
        return secondMostRecentSystolic != null && secondMostRecentDiastolic != null && secondMostRecentDate != null;
    }

    public Integer getSecondMostRecentSystolic() {
        return secondMostRecentSystolic;
    }

    public Integer getSecondMostRecentDiastolic() {
        return secondMostRecentDiastolic;
    }

    public Date getSecondMostRecentDate() {
        return secondMostRecentDate;
    }

    public Integer getRecentHomeBPReadingsCount() {
        return recentHomeBPReadingsCount;
    }

    public Integer getRecentHomeBPReadingsDayCount() {
        return recentHomeBPReadingsDayCount;
    }

    private long getStartOfDay30DaysAgoTS() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.DATE, -30);
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime().getTime();
    }

    private List<BloodPressureModel> buildBPSet(List<BloodPressureModel> bpList) {
        List<BloodPressureModel> bpSet = new ArrayList<>();
        double score = 0.0;
        for (BloodPressureModel bp : bpList) {
            if (score >= 4.0) break;
            bpSet.add(bp);
            if (bp.getSource() == ObservationSource.HOME || bp.getSource() == ObservationSource.COACH_UI || bp.getSource() == ObservationSource.OMRON) {
                score += 0.334;
            } else {
                score += 1.0;
            }
        }
        return score > 4.0 ? bpSet : null;
    }

    public boolean isMostRecentBPCrisis() {
        return mostRecentSystolic >= 180 || mostRecentDiastolic >= 120;
    }

    public boolean isMostRecentBPLowCrisis() {
        return mostRecentSystolic < 90 || mostRecentDiastolic < 60;
    }

    public boolean isSecondMostRecentBPCrisis() {
        return secondMostRecentSystolic >= 180 || secondMostRecentDiastolic >= 120;
    }

    public boolean isSecondMostRecentBPLowCrisis() {
        return secondMostRecentSystolic < 90 || secondMostRecentDiastolic < 60;
    }

    public boolean twoMostRecentWithin14Days() {
        if (mostRecentDate != null && secondMostRecentDate != null) {
            Date now = new Date();
            long nowTS = now.getTime();
            long mostRecentTS = mostRecentDate.getTime();
            long secondMostRecentTS = secondMostRecentDate.getTime();
            return (nowTS - mostRecentTS) < MS_IN_DAY * 14 && (nowTS - secondMostRecentTS) < MS_IN_DAY * 14;

        } else {
            return false;
        }
    }
}
