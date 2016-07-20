package org.conceptoriented.sc.rest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import org.conceptoriented.sc.core.*;

@RestController
@RequestMapping("/api")
public class ScRestService {
	
	private static final String crossOrigins = "http://localhost:3000";

	private static final Logger LOG = Logger.getLogger(ScRestService.class.getName());

	private final Repository repository;
    @Autowired
    public ScRestService(Repository repository) {
        this.repository = repository;
    }
	
	//
	// Test
	//

	@RequestMapping(method = RequestMethod.GET, value = "/ping", produces = "text/plain")
	public String ping(HttpServletRequest request /*HttpSession session*/) {
		HttpSession session = request.getSession();
		LOG.info("SUCCESS");
		return "StreamCommandr";
	}

	//
	// Schemas and their elements
	//

	@CrossOrigin(origins = crossOrigins)
	@RequestMapping(value = "/schemas", method = RequestMethod.GET) // Get all schemas (of an account)
	public String /* with List<Schema> */ getSchemas(HttpSession session) {
		Account acc = repository.getAccountForName("test@host.com");
		List<Schema> schemas = repository.getSchemasForAccount(acc.getId());
		String jelems = "";
		for(Schema elem : schemas) {
			String jelem = elem.toJson();
			jelems += jelem + ", ";
		}
		if(jelems.length() > 2) {
			jelems = jelems.substring(0, jelems.length()-2);
		}
		return "{\"data\": [" + jelems + "]}";
	}
	@CrossOrigin(origins = crossOrigins)
	@RequestMapping(value = "/schemas", method = RequestMethod.POST, produces = "application/json") // Create one (or several) schemas
	public String /* of List<Schema> */ createSchemas(HttpSession session, @PathVariable String id, @RequestBody String body) { 
		Account acc = repository.getAccountForName("test@host.com");
		Schema schema = Schema.fromJson(body);
		repository.addSchema(acc, schema);
		return schema.toJson();
	}

	@CrossOrigin(origins = crossOrigins)
	@RequestMapping(value = "/schemas/{id}", method = RequestMethod.GET, produces = "application/json") // Get one schema with the specified id
	public String /* of Schema */ getSchema(HttpSession session, @PathVariable String id) { 
		Account acc = repository.getAccountForName("test@host.com");
		Schema schema = repository.getSchema(UUID.fromString(id));
		return schema.toJson();
	}
	@CrossOrigin(origins = crossOrigins)
	@RequestMapping(value = "/schemas/{id}", method = RequestMethod.PUT, produces = "application/json") // Update an existing schema
	public String /* of Schema */ updateSchema(HttpSession session, @PathVariable String id, @RequestBody String body) { 
		Account acc = repository.getAccountForName("test@host.com");
		Schema schema = repository.getSchema(UUID.fromString(id));

		return schema.toJson();
	}
	@CrossOrigin(origins = crossOrigins)
	@RequestMapping(value = "/schemas/{id}", method = RequestMethod.DELETE, produces = "application/json") // Delete the specified schema (and all its elements)
	public void deleteSchema(HttpSession session, @PathVariable String id) { 
		Account acc = repository.getAccountForName("test@host.com");
		Schema schema = repository.getSchema(UUID.fromString(id));
	}
	
	// Tables of one schema
	
	@CrossOrigin(origins = crossOrigins)
	@RequestMapping(value = "/schemas/{id}/tables", method = RequestMethod.GET, produces = "application/json") // Read all tables in the schema
	public String /* of List<Table> */ getTables(HttpSession session, @PathVariable String id) {
		Account acc = repository.getAccountForName("test@host.com");
		Schema schema = repository.getSchema(UUID.fromString(id));
		String jelems = "";
		for(Table elem : schema.getTables()) {
			String jelem = elem.toJson();
			jelems += jelem + ", ";
		}
		if(jelems.length() > 2) {
			jelems = jelems.substring(0, jelems.length()-2);
		}
		return "{\"data\": [" + jelems + "]}";
	}
	@CrossOrigin(origins = crossOrigins)
	@RequestMapping(value = "/schemas/{id}/tables", method = RequestMethod.POST, produces = "application/json") // Create one (or several) tables. Return 201 Status Code and (optionally) the newly created id.
	public String /* of List<Table> */ createTables(HttpSession session, @PathVariable String id, @RequestBody String body) {
		Account acc = repository.getAccountForName("test@host.com");
		Schema schema = repository.getSchema(UUID.fromString(id));
		Table table = schema.createTableFromJson(body);
		return table.toJson();
	}
	
	// Columns of one schema 

	@CrossOrigin(origins = crossOrigins)
	@RequestMapping(value = "/schemas/{id}/columns", method = RequestMethod.GET, produces = "application/json") // Read all columns in the schema
	public String /* with List<Column> */ getColumns(HttpSession session, @PathVariable String id) {
		Account acc = repository.getAccountForName("test@host.com");
		Schema schema = repository.getSchema(UUID.fromString(id));
		String jelems = "";
		for(Column elem : schema.getColumns()) {
			String jelem = elem.toJson();
			jelems += jelem + ", ";
		}
		if(jelems.length() > 2) {
			jelems = jelems.substring(0, jelems.length()-2);
		}
		return "{\"data\": [" + jelems + "]}";
	}
	@CrossOrigin(origins = crossOrigins)
	@RequestMapping(value = "/schemas/{id}/columns", method = RequestMethod.POST, produces = "application/json") // Create one (or several) several columns
	public String /* of List<Column> */ createColumns(HttpSession session, @PathVariable String id, @RequestBody String body) { 
		Account acc = repository.getAccountForName("test@host.com");
		Schema schema = repository.getSchema(UUID.fromString(id));
		Column column = schema.createColumnFromJson(body);
		return column.toJson();
	}
	
	//
	// Tables
	//

	@CrossOrigin(origins = crossOrigins)
	@RequestMapping(value = "/tables/{id}", method = RequestMethod.GET, produces = "application/json") // Read one table with the specified id
	public String /* of Table */ getTable(HttpSession session, @PathVariable String id) { 
		Account acc = repository.getAccountForName("test@host.com");
		Table table = repository.getTable(acc.getId(), UUID.fromString(id));
		return table.toJson();
	}
	@CrossOrigin(origins = crossOrigins)
	@RequestMapping(value = "/tables/{id}", method = RequestMethod.PUT, produces = "application/json") // Update an existing table
	public String /* of Table */ updateTable(HttpSession session, @PathVariable String id, @RequestBody String body) { 
		Account acc = repository.getAccountForName("test@host.com");
		Table table = repository.getTable(acc.getId(), UUID.fromString(id));
		Schema schema = table.getSchema();

		schema.updateTableFromJson(body);
		return table.toJson();
	}
	@CrossOrigin(origins = crossOrigins)
	@RequestMapping(value = "/tables/{id}", method = RequestMethod.DELETE, produces = "application/json") // Delete the specified table (and its columns)
	public void deleteTable(HttpSession session, @PathVariable String id) { 
		Account acc = repository.getAccountForName("test@host.com");
		Table table = repository.getTable(acc.getId(), UUID.fromString(id));
		Schema schema = table.getSchema();

		schema.deleteTable(id);
	}

	// Records from one table

	@CrossOrigin(origins = crossOrigins)
	@RequestMapping(value = "/tables/{id}/data", method = RequestMethod.GET, produces = "application/json") // Read records from one table with the specified id
	public String /* of List<Records> */ getRecords(HttpSession session, @PathVariable String id) { 
		Account acc = repository.getAccountForName("test@host.com");
		Table table = repository.getTable(acc.getId(), UUID.fromString(id));
		Range range = null; // All records
		
		String jelems = "";
		for(Record record : table.read(range)) {
			String jelem = record.toJson();
			jelems += jelem + ", ";
		}
		if(jelems.length() > 2) {
			jelems = jelems.substring(0, jelems.length()-2);
		}
		return "{\"data\": [" + jelems + "]}";
	}
	@CrossOrigin(origins = crossOrigins)
	@RequestMapping(value = "/tables/{id}/data", method = RequestMethod.POST, produces = "application/json") // Create records in a table with a given id
	public String /* of List<Records> */ createRecords(HttpSession session, @RequestBody String body, @PathVariable String id) {
		Account acc = repository.getAccountForName("test@host.com");
		Table table = repository.getTable(acc.getId(), UUID.fromString(id));

		List<Record> records = Record.fromJsonList(body);
		for(Record record : records) {
			table.push(record);
		}
		
		table.getSchema().evaluate();

		return "{}";
	}

	//
	// Columns
	//
	
	@CrossOrigin(origins = crossOrigins)
	@RequestMapping(value = "/columns/{id}", method = RequestMethod.GET, produces = "application/json") // Read one column with the specified id
	public String /* of Column */ getColumn(HttpSession session, @PathVariable String id) { 
		Account acc = repository.getAccountForName("test@host.com");
		Column column = repository.getColumn(acc.getId(), UUID.fromString(id));
		return column.toJson();
	}
	@CrossOrigin(origins = crossOrigins)
	@RequestMapping(value = "/columns/{id}", method = RequestMethod.PUT, produces = "application/json") // Update an existing column
	public String /* of Column */ updateColumn(HttpSession session, @PathVariable String id, @RequestBody String body) { 
		Account acc = repository.getAccountForName("test@host.com");
		Column column = repository.getColumn(acc.getId(), UUID.fromString(id));
		Schema schema = column.getSchema();

		schema.updateColumnFromJson(body);
		return column.toJson();
	}
	@CrossOrigin(origins = crossOrigins)
	@RequestMapping(value = "/columns/{id}", method = RequestMethod.DELETE, produces = "application/json") // Delete the specified table (and its columns)
	public void deleteColumn(HttpSession session, @PathVariable String id) { 
		Account acc = repository.getAccountForName("test@host.com");
		Table table = repository.getTable(acc.getId(), UUID.fromString(id));
		Schema schema = table.getSchema();

		schema.deleteColumn(id);
	}

	//
	// Assets. They have description (like name/id and type) and contents (file itself)
	//

	// Many assets

	@CrossOrigin(origins = crossOrigins)
	@RequestMapping(value = "/assets", method = RequestMethod.GET, produces = "application/json") // Read all assets
	public String /* with List<Asset> */ getAssets(HttpSession session) { 
		Account acc = repository.getAccountForName("test@host.com");
		List<Asset> assets = repository.getAssetsForAccount(acc.getId());

		String jelems = "";
		for(Asset elem : assets) {
			String jelem = "{}"; // elem.toJson();
			jelems += jelem + ", ";
		}
		if(jelems.length() > 2) {
			jelems = jelems.substring(0, jelems.length()-2);
		}
		return "{\"data\": [" + jelems + "]}";
	}
	@CrossOrigin(origins = crossOrigins)
	@RequestMapping(value = "/assets", method = RequestMethod.POST, produces = "application/json") // Create one (or several) assets
	public String /* of List<Asset> */ createAssets(HttpSession session, @RequestParam("file") MultipartFile file) { 
		Account acc = repository.getAccountForName("test@host.com");
		List<Asset> assets = repository.getAssetsForAccount(acc.getId());
		
		if(file.isEmpty()) {
			return "{   }"; // Error file is empty file.getOriginalFilename()
		}

		byte[] data = new byte[(int)file.getSize()];
		try {
			file.getInputStream().read(data);
			return "{}";
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		// We actually update or create only one asset per account
		Asset asset = null;
		if(assets.size() == 0) {
			asset = new Asset();
			repository.addAsset(acc, asset);
		}
		else {
			asset = assets.get(0);
		}

		asset.setName(file.getName()); // or getFileName()
		asset.setData(data);

		return "{}";
	}
	
	// One asset

}



// Criteria:
// - Serialization is needed for two purposes: persistence and transfer to a web app 
// - Java references to other domain objects have to be transformed to uuid during serialization and then resolved during de-serialization
//   - This means that (de-)serializer need access to the whole schema
// - Inheritance
// - Whole schema, Table w. cols, or individual obj
// - Do not return json array [] directly. Use object {"content":[]}


/*

FOLLOW this template (rather than many or one)
GET    /users       - Return a list of all users (you may not want to make this publically available)
GET    /users/:id   - Return the user with that id
POST   /users      - Create a new user. Return a 201 Status Code and the newly created id (if you want)
PUT    /users/:id   - Update the user with that id
DELETE /users/:id  - Delete the user with that id



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


Start evaluation in the schema manually:
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
@JsonSerialize(using = SchemaSerializer.class)
class Some {
	public String name;
	public double age;

	public Some(String name, double age) {
		this.name = name;
		this.age = age;
	}
}

class SchemaSerializer extends JsonSerializer<Some> {
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
