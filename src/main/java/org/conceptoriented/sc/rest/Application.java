package org.conceptoriented.sc.rest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import org.conceptoriented.sc.core.*;

/**
 * Spring Boot Root Context class.
 */
@SpringBootApplication
@Configuration
@EnableAutoConfiguration
@ComponentScan
public class Application {

	public static void main(String[] args) {
		
		// We need to create global repository of all schemas created by users
		// For each request, the controller finds the corresponding schema and then performs the operation
		// Resolving schemas on requests can be performed by using session ids
		// Each created schema has creation time and it will be deleted after some timeout (just because its session is supposed to be expired and deleted)
		Schema schema = new Schema("My Schema");
		
		SpringApplication app = new SpringApplication(Application.class);
		setProfile(app);
		app.run(args);
	}

	/**
	 * Determines and set the {@code Profile} based on the operating system.
	 * Currently supported profiles:
	 * <ul>
	 * <li>NonWin (default)</li>
	 * <li>Win</li>
	 * </ul>
	 * 
	 * @param app the Spring app
	 */
	private static void setProfile(SpringApplication app) {
		String activeProfile = "NonWin";
		if (System.getProperty("os.name").contains("Windows")) {
			activeProfile = "Win";
		}
		System.out.println("ActiveProfile: " + activeProfile);
		app.setAdditionalProfiles(activeProfile);
	}
}
