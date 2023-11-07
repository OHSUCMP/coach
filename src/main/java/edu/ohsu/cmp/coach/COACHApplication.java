package edu.ohsu.cmp.coach;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import edu.ohsu.cmp.coach.config.RedcapConfigurationValidator;

@SpringBootApplication
public class COACHApplication {
    public static void main(String[] args) {
        SpringApplication.run(COACHApplication.class, args);
    }

    @Bean
    public static RedcapConfigurationValidator configurationPropertiesValidator() {
        return new RedcapConfigurationValidator();
    }
}
