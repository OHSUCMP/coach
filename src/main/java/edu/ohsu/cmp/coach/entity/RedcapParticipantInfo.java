package edu.ohsu.cmp.coach.entity;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ohsu.cmp.coach.service.REDCapService;

/**
 * An entity that represents the participant's current state in REDCap
 */
public class RedcapParticipantInfo {
    
    private static final Logger logger = LoggerFactory.getLogger(RedcapParticipantInfo.class);

    private static final String PARTICIPANT_INFORMATION_SHEET_FORM = "information_sheet";
    private static final String PARTICIPANT_CONSENT_FORM = "coach_informed_consent";
    private static final String PARTICIPANT_CONSENT_FIELD = "icf_consent_73fb68";
    private static final String PARTICIPANT_RANDOMIZATION_FORM = "staff_coach_randomization";
    private static final String PARTICIPANT_RANDOMIZATION_FIELD = "randomized_assignment";
    private static final String PARTICIPANT_DISPOSITION_WITHDRAW_FIELD = "withdraw";

    private String redcapId;
    private boolean exists;
    private boolean isInformationSheetComplete;
    private boolean hasConsentRecord;
    private boolean isConsentGranted;
    private boolean isRandomized;
    private RandomizationGroup randomizationGroup;
    private boolean isWithdrawn;

    /**
     * Return an object representing a participant that doesn't exist in REDCap yet.
     * @param redcapId
     * @return
     */
    public static RedcapParticipantInfo buildNotExists(String redcapId) {
        RedcapParticipantInfo pi = new RedcapParticipantInfo();
        pi.setRedcapId(redcapId);
        pi.setExists(false);
        return pi;
    }

    /**
     * Build the relevant REDCap participant information using data from the baseline event and ongoing
     * event if it exists.
     * @param redcapId
     * @param baseline
     * @param ongoing
     * @return
     */
    public static RedcapParticipantInfo buildFromRecord(String redcapId, Map<String, String> baseline, Map<String,String> ongoing) {
        RedcapParticipantInfo pi = new RedcapParticipantInfo();
        pi.setRedcapId(redcapId);
        pi.setExists(true);
        pi.setIsInformationSheetComplete(StringUtils.equals(baseline.get(PARTICIPANT_INFORMATION_SHEET_FORM + "_complete"), REDCapService.FORM_COMPLETE));
        pi.setHasConsentRecord(StringUtils.equals(baseline.get(PARTICIPANT_CONSENT_FORM + "_complete"), REDCapService.FORM_COMPLETE));
        pi.setIsConsentGranted(pi.getHasConsentRecord() && 
            StringUtils.equals(baseline.get(PARTICIPANT_CONSENT_FIELD), REDCapService.YES)
        );
        // Check both that the form is complete and that the field is not blank
        pi.setIsRandomized(StringUtils.equals(baseline.get(PARTICIPANT_RANDOMIZATION_FORM + "_complete"), REDCapService.FORM_COMPLETE) && StringUtils.isNotBlank(baseline.get(PARTICIPANT_RANDOMIZATION_FIELD)));
        if (pi.getIsRandomized()) {
            String randString = baseline.get(PARTICIPANT_RANDOMIZATION_FIELD);
            try {
                int rand = Integer.parseInt(randString);
                pi.setRandomizationGroup(RandomizationGroup.getByRedcapCode(rand));
            } catch (IllegalArgumentException e) {
                pi.setRandomizationGroup(RandomizationGroup.ENHANCED);
                RedcapParticipantInfo.logger.error("Randomization Group " + randString + " is not understood. User will get ENHANCED experience.");
            }
        }
        pi.setIsWithdrawn(StringUtils.equals(ongoing.get(PARTICIPANT_DISPOSITION_WITHDRAW_FIELD), REDCapService.YES));
        return pi;
    }

    public String getRedcapId() {
        return this.redcapId;
    }

    public void setRedcapId(String redcapId) {
        this.redcapId = redcapId;
    }

    /**
     * Return whether the record exists in REDCap
     * @return
     */
    public boolean getExists() {
        return exists;
    }

    public void setExists(boolean exists) {
        this.exists = exists;
    }

    /**
     * Return whether the participant information sheet is marked complete
     * @return
     */
    public boolean getIsInformationSheetComplete() {
        return isInformationSheetComplete;
    }

    public void setIsInformationSheetComplete(boolean isInformationSheetComplete) {
        this.isInformationSheetComplete = isInformationSheetComplete;
    }

    /**
     * Return whether the participant has a completed consent record
     * @return
     */
    public boolean getHasConsentRecord() {
        return hasConsentRecord;
    }

    public void setHasConsentRecord(boolean hasConsentRecord) {
        this.hasConsentRecord = hasConsentRecord;
    }

    /**
     * Return whether the participant has a completed consent record that is 'Yes'
     * @return
     */
    public boolean getIsConsentGranted() {
        return isConsentGranted;
    }

    public void setIsConsentGranted(boolean isConsentGranted) {
        this.isConsentGranted = isConsentGranted;
    }

    /**
     * Return whether the participant has a completed randomization form
     * @return
     */
    public boolean getIsRandomized() {
        return isRandomized;
    }

    public void setIsRandomized(boolean isRandomized) {
        this.isRandomized = isRandomized;
    }

    /**
     * Return the participant's randomization
     * @return
     */
    public RandomizationGroup getRandomizationGroup() {
        return randomizationGroup;
    }

    public void setRandomizationGroup(RandomizationGroup randomizationGroup) {
        this.randomizationGroup = randomizationGroup;
    }

    /**
     * Return whether the participant is withdrawn from the study
     * @return
     */
    public boolean getIsWithdrawn() {
        return isWithdrawn;
    }

    public void setIsWithdrawn(boolean isWithdrawn) {
        this.isWithdrawn = isWithdrawn;
    }

    /**
     * Return whether the participant is actively enrolled by looking as consent, randomization, and disposition
     * @return
     */
    public boolean getIsActivelyEnrolled() {
        return getExists() && getIsConsentGranted() && getIsRandomized() && !getIsWithdrawn();
    }
    
}

