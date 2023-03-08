package edu.ohsu.cmp.coach.model.omron;

public class OmronNotifyModel {
    private String id;
    private String timestamp;

    @Override
    public String toString() {
        return "OmronNotifyModel{" +
                "id='" + id + '\'' +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
