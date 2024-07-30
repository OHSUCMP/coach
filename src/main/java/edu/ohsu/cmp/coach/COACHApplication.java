package edu.ohsu.cmp.coach;

import edu.ohsu.cmp.coach.config.RedcapConfigurationValidator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class COACHApplication {
    public static void main(String[] args) {
        SpringApplication.run(COACHApplication.class, args);
    }

    @Bean
    public static RedcapConfigurationValidator configurationPropertiesValidator() {
        return new RedcapConfigurationValidator();
    }
}
