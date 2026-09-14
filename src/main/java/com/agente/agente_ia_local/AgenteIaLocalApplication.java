package com.agente.agente_ia_local;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AgenteIaLocalApplication {

	public static void main(String[] args) {
		SpringApplication.run(AgenteIaLocalApplication.class, args);
	}

}
