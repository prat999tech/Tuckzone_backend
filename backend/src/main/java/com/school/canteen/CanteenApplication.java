package com.school.canteen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// @ConfigurationPropertiesScan lets Boot discover our @ConfigurationProperties records
// (JwtProperties, SeedProperties) without listing each one explicitly.
@SpringBootApplication
@ConfigurationPropertiesScan
public class CanteenApplication {

	public static void main(String[] args) {
		SpringApplication.run(CanteenApplication.class, args);
	}

}
