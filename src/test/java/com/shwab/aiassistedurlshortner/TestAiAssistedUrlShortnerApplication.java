package com.shwab.aiassistedurlshortner;

import org.springframework.boot.SpringApplication;

public class TestAiAssistedUrlShortnerApplication {

	public static void main(String[] args) {
		SpringApplication.from(AiAssistedUrlShortnerApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
