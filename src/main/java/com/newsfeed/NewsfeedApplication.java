package com.newsfeed;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class NewsfeedApplication {

	public static void main(String[] args) {
		SpringApplication.run(NewsfeedApplication.class, args);
	}

}
