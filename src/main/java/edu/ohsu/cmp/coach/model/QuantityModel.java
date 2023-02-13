package edu.ohsu.cmp.coach.model;

import org.hl7.fhir.r4.model.Quantity;

import java.math.BigDecimal;

public class QuantityModel {
    private Integer value;
    private String unit;

    public QuantityModel(Quantity q, String unit) {
        this.value = (int) Math.round(q.getValue().doubleValue());
        this.unit = q.hasUnit() ? q.getUnit() : unit;
    }

    public QuantityModel(Integer value, String unit) {
        this.value = value;
        this.unit = unit;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    @Override
    public String toString() {
        return value + unit;
    }
}
