package edu.ohsu.cmp.coach.model.omron;

public class MeasurementResponse {
    private Integer status;
    private MeasurementResult result;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public MeasurementResult getResult() {
        return result;
    }

    public void setResult(MeasurementResult result) {
        this.result = result;
    }
}
