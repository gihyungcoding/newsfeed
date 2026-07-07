package com.newsfeed;

import org.springframework.boot.SpringApplication;

public class TestNewsfeedApplication {

	public static void main(String[] args) {
		SpringApplication.from(NewsfeedApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
