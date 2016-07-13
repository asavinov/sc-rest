package org.conceptoriented.sc.rest;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import org.conceptoriented.sc.core.*;
import org.conceptoriented.sc.rest.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
@IntegrationTest("server.port:0")
@ActiveProfiles("Win")
public class ScRestServiceTest {

	@Value("${local.server.port}")
	private int port;

	private URL base;
	private RestTemplate template;

	@Before
	public void setUp() throws Exception {
		this.base = new URL("http://localhost:" + port + "/");
		template = new TestRestTemplate();
	}

	@Test
	public void testRestExecute() throws Exception {
		System.out.println(">>>>>>>>>>>>>>>>>>>>> testRestExecute");

		String result = template.getForObject(base.toString() + "/api/ping", String.class);

		assertEquals("StreamCommandr", result);

		result = template.getForObject(base.toString() + "ping", String.class);
	}

	@Test
	public void testClassLoader() throws Exception {
		System.out.println(">>>>>>>>>>>>>>>>>>>>> testClassLoader");
		
		String fileName = "EvaluatorB.jar";
		
		// Read custom jar
		InputStream stream;
		stream = getClass().getClassLoader().getResourceAsStream(fileName);
        //stream = resource.getInputStream();
        byte[] fileData = org.springframework.util.StreamUtils.copyToByteArray(stream);

        //File file = org.springframework.util.ResourceUtils.getFile("application.properties");
        //fileData = Files.readAllBytes(file.toPath());
		

		// Add custom jar file to the account
        Asset ass = new Asset();
        ass.setData(fileData);
        ass.setName(fileName);

		Repository repo = new Repository();
        Account acc = repo.getAccountForName("test@host.com");
        acc.setClassLoader(new UdfClassLoader(acc));
        repo.addAsset(acc, ass);
		
        // Define a column which uses a custom class
        Space space = repo.getSpacesForAccount(acc.getId()).get(0);
        
        Column column = space.getColumn("Table 1", "Column 11");
        String descr = "{ `class`:`org.conceptoriented.sc.core.EvaluatorB`, `dependencies`:[`Column 11`,`Column 12`] }";
        column.setDescriptor(descr.replace('`', '"'));
        
        // Evaluate space - it has to load the class dynamically
        column.evaluate();
        
	}

}
