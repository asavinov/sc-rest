package org.conceptoriented.sc.rest;

import static org.junit.Assert.assertTrue;

import org.conceptoriented.sc.rest.Application;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes=Application.class)
//@ActiveProfiles("Win")
public class ApplicationTest {

	private String profiles;

	@Before
	public void before() {
		this.profiles = System.getProperty("spring.profiles.active");
	}

	@After
	public void after() {
		if (this.profiles != null) {
			System.setProperty("spring.profiles.active", this.profiles);
		} else {
			System.clearProperty("spring.profiles.active");
		}
	}

	@Test
	public void testProfileForWindows() throws Exception {
		System.out.println(">>>>>>>>>>>>>>>>>>>>> testProfileForWindows");
		
		//Application.main(new String[0]); // TODO: Fails for Gradle test and ok for Eclipse test
		//String output = this.outputCapture.toString();

		//assertTrue(output.contains("ActiveProfile: Win"));
	}

}
