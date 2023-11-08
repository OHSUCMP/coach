package edu.ohsu.cmp.coach.config;

import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

/**
 * Custom validator for REDCap Configuration
 */
public class RedcapConfigurationValidator implements Validator {

@Override
  public boolean supports(Class<?> clazz) {
    return RedcapConfiguration.class.isAssignableFrom(clazz);
  }

@Override
public void validate(Object target, Errors errors) {
    RedcapConfiguration redcapConfiguration = (RedcapConfiguration) target;

    if (redcapConfiguration.getEnabled()) {
        ValidationUtils.rejectIfEmpty(errors, "apiUrl", "required-non-empty", "redcap.api-url required");
        ValidationUtils.rejectIfEmpty(errors, "apiToken", "required-non-empty", "redcap.api-token required");
        String dag = redcapConfiguration.getDataAccessGroup();
        if (!("mu").equals(dag) && !("ohsu").equals(dag) && !("vumc").equals(dag)) {
            errors.rejectValue("dataAccessGroup", "required-specific", "redcap.data-access-group must be one of: mu, ohsu, vumc");
        }
    }
}

}
