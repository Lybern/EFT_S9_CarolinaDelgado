package com.bancoxyz.migracion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class MigracionApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(MigracionApplication.class, args);
		int exitCode = SpringApplication.exit(context);
		System.exit(exitCode);
	}
}
