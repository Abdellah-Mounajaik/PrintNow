package com.printnow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class PrintnowApplication {

	public static void main(String[] args) {
		SpringApplication.run(PrintnowApplication.class, args);
	}

}
