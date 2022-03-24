package edu.ohsu.cmp.coach.model.cqfruler;

import com.google.gson.annotations.SerializedName;

public class CDSHook {
    @SerializedName("id")
    private String id;

    @SerializedName("hook")
    private String hook;

    @SerializedName("name")
    private String name;

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    public CDSHook(String id, String hook, String name, String title, String description) {
        this.id = id;
        this.hook = hook;
        this.name = name;
        this.title = title;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHook() {
        return hook;
    }

    public void setHook(String hook) {
        this.hook = hook;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
