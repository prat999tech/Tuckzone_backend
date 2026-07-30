package com.school.canteen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

// @ConfigurationPropertiesScan lets Boot discover our @ConfigurationProperties records
// (JwtProperties, SeedProperties, ...) without listing each one explicitly.
// @EnableScheduling powers rate-limiter eviction and token housekeeping.
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class CanteenApplication {

	public static void main(String[] args) {
		SpringApplication.run(CanteenApplication.class, args);
	}

}
