package org.conceptoriented.sc.rest;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
public class ScControllerTest {

	@Autowired
	private WebApplicationContext ctx;

	private MockMvc mockMvc;
	protected MockHttpSession mockSession;
	
	@Before
	public void setUp() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(ctx).build();
		String sessionId = UUID.randomUUID().toString();
		this.mockSession = new MockHttpSession(ctx.getServletContext(), "123456789");
	}

	@Test
	public void noParamPing() throws Exception {

		// Some examples: https://github.com/spring-projects/spring-test-mvc/blob/master/src/test/java/org/springframework/test/web/server/samples/standalone/resultmatchers/JsonPathAssertionTests.java
		
		MvcResult result = this.mockMvc.perform(get("/api/v1/ping").session(mockSession))
				.andDo(print())
				.andExpect(status().isOk())
				//.andExpect(content().string("StreamCommandr"))
				//.andExpect(jsonPath("$.content").value("Expected Value")),
				//.andExpect(jsonPath(composerByName, "Robert Schumann").exists())
				//.andExpect(jsonPath("$.composers[2]").exists())
				.andReturn();
		
		String content = result.getResponse().getContentAsString();

		assertEquals("StreamCommandr", content);
	}


}
