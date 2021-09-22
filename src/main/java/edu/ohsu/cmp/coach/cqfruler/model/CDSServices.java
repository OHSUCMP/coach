package edu.ohsu.cmp.coach.cqfruler.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class CDSServices {
    @SerializedName("services")
    private List<CDSHook> hooks;

    public List<CDSHook> getHooks() {
        return hooks;
    }

    public void setHooks(List<CDSHook> hooks) {
        this.hooks = hooks;
    }
}
