package edu.ohsu.cmp.coach.listener;

import edu.ohsu.cmp.coach.service.ValueSetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(0)
public class ApplicationStartupListener implements ApplicationListener<ApplicationReadyEvent> {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ValueSetService valueSetService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        valueSetService.refreshDefinedValueSets();
    }
}
