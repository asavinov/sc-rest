package org.conceptoriented.sc.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.logging.Log;
import org.conceptoriented.sc.core.*;

/**
 * Spring Boot Root Context class.
 */
//@SpringBootApplication
@Configuration
@EnableAutoConfiguration
@ComponentScan
public class Application {

	private static final Logger LOG = LoggerFactory.getLogger(Application.class.getName());

	// Default list of allowed hosts. Use * for all origins.
	public static String origins = "*,http://localhost,http://localhost:8080,http://dc.conceptoriented.com,http://conceptoriented.com,http://datacommandr.eastus2.cloudapp.azure.com";

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
		LOG.info("ActiveProfile: {}", activeProfile);
		app.setAdditionalProfiles(activeProfile);
	}

    @Autowired
    private Environment environment;

    // Setting origins via application.properties as a comma separated list of hosts:
    // endpoints.cors.allowed-origins=
    // More info: http://docs.spring.io/spring-boot/docs/current/reference/html/common-application-properties.html
    	
    // Here we configure Spring to do two things:
	// 1. Filter incoming requests by comparing their Origin header with this white list (as string comparison) and reject request if there is no match
	// 2. Set allowed origin header of the response to the matched host from the white list (that is, to what is in the request origin header) so that the browser will accept it 
	// http://stackoverflow.com/questions/39623211/add-multiple-cross-origin-urls-in-spring-boot
	@Bean
    public WebMvcConfigurer corsConfigurer() {

		return new WebMvcConfigurerAdapter() {

            @Override
            public void addCorsMappings(CorsRegistry registry) {

            	// Overwrite default values from the environment
            	if(environment.getProperty("origins") != null && !environment.getProperty("origins").trim().isEmpty())
    				origins = environment.getProperty("origins");

            	registry
                	.addMapping("/api/**")
                	.allowedOrigins(origins.split(","))
                	.allowedMethods("GET", "PUT", "POST", "DELETE", "OPTIONS") // Note that not all methods are included by default
                	.allowedHeaders("*")
                	.allowCredentials(true)
                	.maxAge(3600);
                	;
            }
        };
    }

}

// Here we configure a filter for each request
// See also white-listing multiple origins using intercepter: http://dontpanic.42.nl/2015/04/cors-with-spring-mvc.html
/*
@Component
class SimpleCorsFilter implements Filter {
	// Here we only provide the necessary headers for the browser to decide what to filter but do not do our own filtering

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {

    	HttpServletResponse response = (HttpServletResponse) res;
        HttpServletRequest request = (HttpServletRequest) req;
        
        System.out.println( ">>>>>>> Origin: " + request.getHeader("Origin") );
        // 'http://localhost:8080' if the request is from browser 
        // 'bla2.com' if the request is from curl --header "Origin: bla2.com" 
        
        //
        // Browser will use these headers to decide what to do and what expose to the application
        //

        // This header is for browser to decide whether to accept or reject this response (if allowed origin is equal to the origin of the page loaded by the browser). We say that the page must be loaded only from this origin (where our html application is located). 
        response.addHeader("Access-Control-Allow-Origin", "http://bla.com http://localhost:8080");
        // We could dynamically set the allowed host. For example, by maitaining a while list of hosts and then looking up the Origin sent in the request: request.getHeader("Origin")
        // But how it is done in Spring Boot using annotations?

        // What methods are allowed
        response.setHeader("Access-Control-Allow-Methods", "GET, PUT, POST, DELETE, OPTIONS, PATCH");
        //response.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS, DELETE");

        response.setHeader("Access-Control-Allow-Credentials", "true");

        response.setHeader("Access-Control-Max-Age", "3600");

        response.setHeader("Access-Control-Allow-Headers", "Origin, Content-Type, Accept, X-Requested-With");
        //response.setHeader("Access-Control-Allow-Headers", "Content-Type, Accept, X-Requested-With, Authorization");
        //response.setHeader("Access-Control-Allow-Headers", "X-Requested-With, X-Auth-Token");

        //response.setHeader("Access-Control-Expose-Headers", "Location");

        chain.doFilter(req, res);
    }

    @Override
    public void init(FilterConfig config) throws ServletException { }

    @Override
    public void destroy() {}

}
*/
