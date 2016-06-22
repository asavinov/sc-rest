package org.conceptoriented.sc.rest;

import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ScRestService {

	private static final Logger LOG = Logger.getLogger(ScRestService.class.getName());

	@RequestMapping(method = RequestMethod.GET, value = "/ping", produces = "text/plain")
	public String ping() {
		LOG.info("SUCCESS");
		return "StreamCommandr";
	}

}
