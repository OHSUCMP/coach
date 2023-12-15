package edu.ohsu.cmp.coach.config;

import edu.ohsu.cmp.coach.model.RedcapDataAccessGroup;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import java.util.ArrayList;
import java.util.List;

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
            RedcapDataAccessGroup dag = RedcapDataAccessGroup.fromTag(redcapConfiguration.getDataAccessGroup());
            if (dag == null) {
                List<String> list = new ArrayList<>();
                for (RedcapDataAccessGroup item : RedcapDataAccessGroup.values()) {
                    list.add(item.getTag());
                }
                String dagCSV = StringUtils.join(list, ", ");
                errors.rejectValue("dataAccessGroup", "required-specific", "redcap.data-access-group must be one of: " + dagCSV);
            }
        }
    }
}
