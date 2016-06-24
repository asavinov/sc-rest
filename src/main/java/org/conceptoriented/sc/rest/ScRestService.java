package org.conceptoriented.sc.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import org.conceptoriented.sc.core.*;

@RestController
@RequestMapping("/api/v1")
public class ScRestService {

	private static final Logger LOG = Logger.getLogger(ScRestService.class.getName());

	private final Repository repository;
    @Autowired
    public ScRestService(Repository repository) {
        this.repository = repository;
    }
	
	@RequestMapping(method = RequestMethod.GET, value = "/ping", produces = "text/plain")
	public String ping(HttpServletRequest request /*HttpSession session*/) {
		HttpSession session = request.getSession();
		LOG.info("SUCCESS");
		return "StreamCommandr";
	}

	//
	// Tables
	//

	@RequestMapping(value = "/spaces", method = RequestMethod.GET)
	public List<Some> spaces() {
		
		LOG.info("REPOSITORY: " + repository.name);
		
		List<Some> list = new ArrayList<Some>();
		list.add(new Some("Space 1", 25));
		list.add(new Some("Space 2", 55));
		list.add(new Some("Space 3", 45));
		
		return list;
	}

	//
	// Tables
	//

	// Many tables
	
	@RequestMapping(value = "/tables", method = RequestMethod.GET, produces = "application/json")
	public List<Table> getTables(HttpSession session) { // Return all tables in the space
		Space space = repository.spaces.get("sample");
		return null;
	}
	@RequestMapping(value = "/tables", method = RequestMethod.POST, produces = "application/json")
	public List<Table> createTables() { // Create several tables
		Space space = repository.spaces.get("sample");
		return null;
	}
	@RequestMapping(value = "/tables", method = RequestMethod.DELETE, produces = "application/json")
	public List<Table> deleteTables() { // Delete all tables (and their columns)
		Space space = repository.spaces.get("sample");
		return null;
	}
	@RequestMapping(value = "/tables", method = RequestMethod.PUT, produces = "application/json")
	public List<Table> updateTables() { // Update several tables (bulk update)
		Space space = repository.spaces.get("sample");
		return null;
	}

	// One table

	@RequestMapping(value = "/tables/{id}", method = RequestMethod.GET, produces = "application/json")
	public List<Table> getTable(@PathVariable String id) { // Return one table with the specified id
		Space space = repository.spaces.get("sample");
		return null;
	}
	@RequestMapping(value = "/tables/{id}", method = RequestMethod.POST, produces = "application/json")
	public List<Table> createTable() { // Not allowed to create an object with a given id (id has to be allocated by the service)
		Space space = repository.spaces.get("sample");
		return null;
	}
	@RequestMapping(value = "/tables/{id}", method = RequestMethod.DELETE, produces = "application/json")
	public List<Table> deleteTable(@PathVariable String id) { // Delete the specified table (and its columns)
		Space space = repository.spaces.get("sample");
		return null;
	}
	@RequestMapping(value = "/tables/{id}", method = RequestMethod.PUT, produces = "application/json")
	public List<Table> updateTable(@PathVariable String id) { // Update an existing table
		Space space = repository.spaces.get("sample");
		return null;
	}

	//
	// Columns
	//

}



// Criteria:
// - Serialization is needed for two purposes: persistence and transfer to a web app 
// - Java references to other domain objects have to be transformed to uuid during serialization and then resolved during de-serialization
//   - This means that (de-)serializer need access to the whole space
// - Inheritance
// - Whole space, Table w. cols, or individual obj
// - Do not return json array [] directly. Use object {"content":[]}


/*
http://blog.mwaysolutions.com/2014/06/05/10-best-practices-for-better-restful-api/

/cars GET list of cars, POST create, PUT bulk update, DELETE all cars
/cars/{id} - ret specific car, POST not allowed (405), PUT update specific car, DELETE specific car

get /api/v1/catalog/artifacts/{id}/file - Download an artifact file by id
put /api/v1/catalog/artifacts/{id} - Update an artifact by id.
post /api/v1/catalog/artifacts - Upload an artifact and attach it to an analytic catalog entry.
delete /api/v1/catalog/artifacts/{id} - Delete an artifact by id.

GET /tables/123456/columns/ - use subresources to get related elements 
GET /columns/123456/input/
GET /columns/123456/output/

GET /cars?color=red - Returns a list of red cars




Start evaluation in the space manually:
post /api/v1/analytic/execution - Execute the analytic synchronously.
post /api/v1/analytic/execution/async - Execute the analytic asynchronously.

Get results of evaluation (or another process) which could be stored in some kind of logs:
get /api/v1/analytic/execution/async/{requestId}/result - Get the analytic execution result by request id.
get /api/v1/analytic/execution/async/{requestId}/status - Get the analytic execution status by request id




Use standard HTTP status codes:
200 � OK � Eyerything is working
201 � OK � New resource has been created
204 � OK � The resource was successfully deleted

404 � Not found � There is no resource behind the URI.

Exceptions as error payload

/sc/api/v1
GET never changes state
Use plural like /columns and /tables

*/

// ALTERNATIVE 1: Annotate domain classes to use custom serializer 

//@JsonDeserialize(using = RoleDeserializer.class)
@JsonSerialize(using = SpaceSerializer.class)
class Some {
	public String name;
	public double age;

	public Some(String name, double age) {
		this.name = name;
		this.age = age;
	}
}

class SpaceSerializer extends JsonSerializer<Some> {
	@Override
	public void serialize(Some value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {

	    gen.writeStartObject();

	    gen.writeStringField("name", value.name);
	    gen.writeNumberField("age", value.age);
	    gen.writeStringField("sent", "today");

	    gen.writeEndObject();
	}
}

//ALTERNATIVE 2: Create a Java class which can be easily serialized by a standard serializer like ColumnJson 


//ALTERNATIVE 3: Implement special methods of the domain class which can serialized its instances
