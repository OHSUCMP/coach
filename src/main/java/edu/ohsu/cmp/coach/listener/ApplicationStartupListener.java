package edu.ohsu.cmp.coach.listener;

import edu.ohsu.cmp.coach.service.MedicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(0)
public class ApplicationStartupListener implements ApplicationListener<ApplicationReadyEvent> {
    @Autowired
    private MedicationService medicationService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        medicationService.refreshAntihypertensiveMedicationValueSets();
    }
}
