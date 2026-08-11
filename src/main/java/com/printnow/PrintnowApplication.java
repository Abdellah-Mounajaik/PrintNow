package com.printnow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
// Tâches périodiques : purge des factures archivées dont la conservation
// obligatoire est échue (voir PurgeFacturesArchiveesService).
@EnableScheduling
public class PrintnowApplication {

	public static void main(String[] args) {
		SpringApplication.run(PrintnowApplication.class, args);
	}

}
