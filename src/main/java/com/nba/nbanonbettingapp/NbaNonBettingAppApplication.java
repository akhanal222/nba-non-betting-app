package com.nba.nbanonbettingapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NbaNonBettingAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(NbaNonBettingAppApplication.class, args);
	}

}
