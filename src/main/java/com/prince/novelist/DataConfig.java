package com.prince.novelist;

import org.neo4j.driver.Driver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class DataConfig {

	@Bean
	@Profile("dev | tes")
	public Driver driver() {
		return null;
	}

	@Bean
	@Profile("production")
	public Driver prodDriver() {
		return null;
	}
}
