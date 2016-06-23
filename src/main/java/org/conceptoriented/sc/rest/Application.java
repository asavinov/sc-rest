package org.conceptoriented.sc.rest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot Root Context class.
 */
@SpringBootApplication
@Configuration
@EnableAutoConfiguration
@ComponentScan
public class Application {

	public static void main(String[] args) {
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
