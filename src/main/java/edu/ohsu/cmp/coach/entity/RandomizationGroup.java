package edu.ohsu.cmp.coach.entity;

public enum RandomizationGroup {
    ENHANCED(1), USUAL(2);
    private int redcapCode;
    
    RandomizationGroup(int redcapCode) {
        this.redcapCode = redcapCode;
    }

    public int getRedcapCode() {
        return redcapCode;
    }

    public static RandomizationGroup getByRedcapCode(int redcapCode) {
        if (redcapCode == 1) {
            return ENHANCED;
        } else if (redcapCode == 2) {
            return USUAL;
        }

        throw new IllegalArgumentException("No randomization group maps to " + redcapCode);
    }
}
