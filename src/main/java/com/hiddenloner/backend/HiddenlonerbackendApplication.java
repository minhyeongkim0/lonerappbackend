package com.hiddenloner.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class HiddenlonerbackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(HiddenlonerbackendApplication.class, args);
	}

}
