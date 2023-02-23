package edu.ohsu.cmp.coach.model.omron;

import java.util.List;

public class MeasurementResult {
    private Boolean truncated;
    private List<OmronBloodPressureModel> bloodPressure;
    private List<OmronActivityModel> activity;
//    private List<OmronWeightModel> weight;
    private String nextPaginationKey;
    private Integer measurementCount;

    public Boolean getTruncated() {
        return truncated;
    }

    public void setTruncated(Boolean truncated) {
        this.truncated = truncated;
    }

    public boolean hasBloodPressures() {
        return bloodPressure != null && bloodPressure.size() > 0;
    }

    public List<OmronBloodPressureModel> getBloodPressure() {
        return bloodPressure;
    }

    public void setBloodPressure(List<OmronBloodPressureModel> bloodPressure) {
        this.bloodPressure = bloodPressure;
    }

    public boolean hasActivity() {
        return activity != null && activity.size() > 0;
    }

    public List<OmronActivityModel> getActivity() {
        return activity;
    }

    public void setActivity(List<OmronActivityModel> activity) {
        this.activity = activity;
    }

//    public List<OmronWeightModel> getWeight() {
//        return weight;
//    }
//
//    public void setWeight(List<OmronWeightModel> weight) {
//        this.weight = weight;
//    }

    public String getNextPaginationKey() {
        return nextPaginationKey;
    }

    public void setNextPaginationKey(String nextPaginationKey) {
        this.nextPaginationKey = nextPaginationKey;
    }

    public Integer getMeasurementCount() {
        return measurementCount;
    }

    public void setMeasurementCount(Integer measurementCount) {
        this.measurementCount = measurementCount;
    }
}
