package org.conceptoriented.sc.rest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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

@CrossOrigin(/*origins = "http://dc.conceptoriented.com", */maxAge = 3600)
@RestController
@RequestMapping("/api")
public class ScRestService {
	
	//
	// Configure CORS. It will set http access control headers in response so that the browser can either accept or reject them 
	//
	private static final String crossOrigins = "*";
	//private static final String crossOrigins = "http://localhost:8080";
	//private static final String crossOrigins = "http://dc.conceptoriented.com:80";
	//private static final String crossOrigins = "http://13.68.111.155:80"; // datacommandr.eastus2.cloudapp.azure.com
	
	private static final Logger LOG = LoggerFactory.getLogger(ScRestService.class.getName());

	private final Repository repository;
    @Autowired
    public ScRestService(Repository repository) {
        this.repository = repository;
    }
	
	//
	// Test
	//

	@CrossOrigin(origins = crossOrigins)
	@RequestMapping(method = RequestMethod.GET, value = "/ping", produces = "text/plain")
	public ResponseEntity<String> ping(HttpServletRequest request /*HttpSession session*/) {
		HttpSession session = request.getSession();
		LOG.debug("GET/ping: {}", "SUCCESS");
		return ResponseEntity.ok("DataCommandr");
	}

	//
	// Account, user, session, authentication
	//

	@CrossOrigin(origins = crossOrigins)
	@RequestMapping(value = "/account", method = RequestMethod.GET) // Get account object given credentials
	public ResponseEntity<String> getAccount(HttpSession session) {
		//
		// We need to get credentials (user id) and then find an account object for this user
		//
		Account acc = repository.getAccountForSession(session); // Use session as user id

		if(acc == null) { // No account for this session/user (first time access)
			// Create a new account and associate it with this session (the old account if any will be garbage collected)
			acc = new Account(repository, "test@host.com");
			acc.setSession(session.getId());
			repository.addAccount(acc);
			LOG.info("Method: {}, ACCOUNT CREATED. Account: {}", "GET/login", acc.getId());

			Schema schema1 = Repository.buildSampleSchema1("My Schema");
			schema1.translate();
			repository.addSchema(acc, schema1);

			Schema schema2 = Repository.buildSampleSchema2("Sales");
			schema2.translate();
			repository.addSchema(acc, schema2);

		}
		
		return ResponseEntity.ok( acc.toJson() );
	}

	//
	// Schemas and their elements
	//

	@CrossOrigin(origins = crossOrigins)
	@RequestMapping(value = "/schemas", method = RequestMethod.GET) // Get all schemas (of an account)
	public ResponseEntity<String> /* with List<Schema> */ getSchemas(HttpSession session) {
		Account acc = repository.getAccountForSession(session);
		if(acc == null) {
			LOG.error("Method: {}, NOT_FOUND_IDENTITY, Session: {}", "GET/schemas", session.getId());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(DcError.error(DcErrorCode.NOT_FOUND_IDENTITY, "Session: "+session));
		}
		LOG.debug("Method: {}, Account: {}", "GET/schemas", acc.getId());

		List<Schema> schemas = repository.getSchemasForAccount(acc.getId());

		String jelems = "";
		for(Schema elem : schemas) {
			String jelem = elem.toJson();
			jelems += jelem + ", ";
		}
		if(jelems.length() > 2) {
			jelems = jelems.substring(0, jelems.length()-2);
		}
		return ResponseEntity.ok( "{\"data\": [" + jelems + "]}" );
	}
	@CrossOrigin(origins = crossOrigins)
	@RequestMapping(value = "/schemas", method = RequestMethod.POST, produces = "application/json") // Create one (or several) schemas
	public ResponseEntity<String> /* of List<Schema> */ createSchemas(HttpSession session, @RequestBody String body) { 
		Account acc = repository.getAccountForSession(session);
		if(acc == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(DcError.error(DcErrorCode.NOT_FOUND_IDENTITY, "Session: "+session));
		}
		LOG.debug("Method: {}, Account: {}", "POST/schemas", acc.getId());

		Schema schema = null;
		try {
			schema = Schema.fromJson(body); // Main operation
			acc.schemaCreateCount++;
		}
		catch(DcError e) {
			LOG.error("DcError",  e);
			return ResponseEntity.ok(e.toJson());
		}
		catch(Exception e) {
			LOG.error("Exception",  e);
			return ResponseEntity.ok(DcError.error(DcErrorCode.GENERAL, "Error creating schema.", e.getMessage()));
		}
		if(schema == null) return ResponseEntity.ok(DcError.error(DcErrorCode.GENERAL, "Error creating schema.", ""));

		repository.addSchema(acc, schema);

		return ResponseEntity.ok( schema.toJson() );
	}

	// Operations with one schema

	@CrossOrigin(origins = crossOrigins)
	@RequestMapping(value = "/schemas/{id}", method = RequestMethod.GET, produces = "application/json") // Get one schema with the specified id
	public ResponseEntity<String> /* of Schema */ getSchema(HttpSession session, @PathVariable String id) { 
		Account acc = repository.getAccountForSession(session);
		if(acc == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(DcError.error(DcErrorCode.NOT_FOUND_IDENTITY, "Session: "+session));
		}
		LOG.debug("Method: {}/{}, Account: {}", "GET/schemas", id, acc.getId());

		Schema schema = repository.getSchema(UUID.fromString(id));
		if(schema == null) return ResponseEntity.ok(DcError.error(DcErrorCode.GENERAL, "Schema not found.", ""));

		return ResponseEntity.ok( schema.toJson() );
	}
	@CrossOrigin(origins = crossOrigins)
	@RequestMapping(value = "/schemas/{id}", method = RequestMethod.PUT, produces = "application/json") // Update an existing schema
	public ResponseEntity<String> /* of Schema */ updateSchema(HttpSession session, @PathVariable String id, @RequestBody String body) { 
		Account acc = repository.getAccountForSession(session);
		if(acc == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(DcError.error(DcErrorCode.NOT_FOUND_IDENTITY, "Session: "+session));
		}
		LOG.debug("Method: {}/{}, Account: {}", "PUT/schemas", id, acc.getId());

		Schema schema = repository.getSchema(UUID.fromString(id));
		if(schema == null) return ResponseEntity.ok(DcError.error(DcErrorCode.GENERAL, "Schema not found.", ""));


		try {
			schema.updateFromJson(body); // Main operation
			acc.schemaUpdateCount++;
		}
		catch(DcError e) {
			LOG.error("DcError",  e);
			return ResponseEntity.ok(e.toJson());
		}
		catch(Exception e) {
			LOG.error("Exception",  e);
			return ResponseEntity.ok(DcError.error(DcErrorCode.GENERAL, "Error updating schema.", e.getMessage()));
		}

		return ResponseEntity.ok( schema.toJson() );
	}
	@CrossOrigin(origins = crossOrigins)
	@RequestMapping(value = "/schemas/{id}", method = RequestMethod.DELETE, produces = "application/json") // Delete the specified schema (and all its elements)
	public ResponseEntity<String> deleteSchema(HttpSession session, @PathVariable String id) { 
		Account acc = repository.getAccountForSession(session);
		if(acc == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(DcError.error(DcErrorCode.NOT_FOUND_IDENTITY, "Session: "+session));
		}
		LOG.debug("Method: {}/{}, Account: {}", "DELETE/schemas", id, acc.getId());

		Schema schema = repository.getSchema(UUID.fromString(id));
		if(schema == null) return ResponseEntity.ok(DcError.error(DcErrorCode.GENERAL, "Schema not found.", ""));
		
		try {
			repository.deleteSchema(schema); // Main operation
			acc.schemaDeleteCount++;
		}
		catch(Exception e) {
			LOG.error("Exception",  e);
			return ResponseEntity.ok(DcError.error(DcErrorCode.GENERAL, "Error deleting schema.", e.getMessage()));
		}

		return ResponseEntity.ok(null);
	}
	
	// Tables of one schema
	
	@CrossOrigin(origins = crossOrigins)
	@RequestMapping(value = "/schemas/{id}/tables", method = RequestMethod.GET, produces = "application/json") // Read all tables in the schema
	public ResponseEntity<String> /* of List<Table> */ getTables(HttpSession session, @PathVariable String id, HttpServletRequest req) {
		Account acc = repository.getAccountForSession(session);
		if(acc == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(DcError.error(DcErrorCode.NOT_FOUND_IDENTITY, "Session: "+session));
		}
		LOG.debug("Method: {}, Account: {}", "GET/schemas/id/tables", acc.getId());

		Schema schema = repository.getSchema(UUID.fromString(id));
		if(schema == null) return ResponseEntity.ok(DcError.error(DcErrorCode.GENERAL, "Schema not found.", ""));

		String jelems = "";
		for(Table elem : schema.getTables()) {
			String jelem = elem.toJson();
			jelems += jelem + ", ";
		}
		if(jelems.length() > 2) {
			jelems = jelems.substring(0, jelems.length()-2);
		}
		return ResponseEntity.ok( "{\"data\": [" + jelems + "]}" );
	}
	@CrossOrigin(origins = crossOrigins)
	@RequestMapping(value = "/schemas/{id}/tables", method = RequestMethod.POST, produces = "application/json") // Create one (or several) tables. Return 201 Status Code and (optionally) the newly created id.
	public ResponseEntity<String> /* of List<Table> */ createTables(HttpSession session, @PathVariable String id, @RequestBody String body) {
		Account acc = repository.getAccountForSession(session);
		if(acc == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(DcError.error(DcErrorCode.NOT_FOUND_IDENTITY, "Session: "+session));
		}
		LOG.debug("Method: {}, Account: {}", "POST/schemas/id/tables", acc.getId());

		Schema schema = repository.getSchema(UUID.fromString(id));
		if(schema == null) return ResponseEntity.ok(DcError.error(DcErrorCode.GENERAL, "Schema not found.", ""));

		Table table = null;
		try {
			table = schema.createTableFromJson(body); // Main operation
			acc.tableCreateCount++;
		}
		catch(DcError e) {
			LOG.error("DcError",  e);
			return ResponseEntity.ok(e.toJson());
		}
		catch(Exception e) {
			LOG.error("Exception",  e);
			return ResponseEntity.ok(DcError.error(DcErrorCode.GENERAL, "Error creating table.", e.getMessage()));
		}

		return ResponseEntity.ok( table.toJson() );
	}
	
	// Columns of one schema 

	@CrossOrigin(origins = crossOrigins)
	@RequestMapping(value = "/schemas/{id}/columns", method = RequestMethod.GET, produces = "application/json") // Read all columns in the schema
	public ResponseEntity<String> /* with List<Column> */ getColumns(HttpSession session, @PathVariable String id) {
		Account acc = repository.getAccountForSession(session);
		if(acc == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(DcError.error(DcErrorCode.NOT_FOUND_IDENTITY, "Session: "+session));
		}
		LOG.debug("Method: {}, Account: {}", "GET/schemas/id/columns", acc.getId());

		Schema schema = repository.getSchema(UUID.fromString(id));
		if(schema == null) return ResponseEntity.ok(DcError.error(DcErrorCode.GENERAL, "Schema not found.", ""));

		String jelems = "";
		for(Column elem : schema.getColumns()) {
			String jelem = elem.toJson();
			jelems += jelem + ", ";
		}
		if(jelems.length() > 2) {
			jelems = jelems.substring(0, jelems.length()-2);
		}
		return ResponseEntity.ok( "{\"data\": [" + jelems + "]}" );
	}
	@CrossOrigin(origins = crossOrigins)
	@RequestMapping(value = "/schemas/{id}/columns", method = RequestMethod.POST, produces = "application/json") // Create one (or several) several columns
	public ResponseEntity<String> /* of List<Column> */ createColumns(HttpSession session, @PathVariable String id, @RequestBody String body) { 
		Account acc = repository.getAccountForSession(session);
		if(acc == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(DcError.error(DcErrorCode.NOT_FOUND_IDENTITY, "Session: "+session));
		}
		LOG.debug("Method: {}, Account: {}", "POST/schemas/id/columns", acc.getId());

		Schema schema = repository.getSchema(UUID.fromString(id));
		if(schema == null) return ResponseEntity.ok(DcError.error(DcErrorCode.GENERAL, "Schema not found.", ""));

		Column column = null;
		try {
			column = schema.createColumnFromJson(body); // Main operation
			schema.translate();
			acc.columnCreateCount++;
		}
		catch(DcError e) {
			LOG.error("DcError",  e);
			return ResponseEntity.ok(e.toJson());
		}
		catch(Exception e) {
			LOG.error("Exception",  e);
			return ResponseEntity.ok(DcError.error(DcErrorCode.GENERAL, "Error creating column.", e.getMessage()));
		}
		
		return ResponseEntity.ok( column.toJson() );
	}
	@CrossOrigin(origins = crossOrigins)
	@RequestMapping(value = "/schemas/{id}/columns/statuses", method = RequestMethod.GET, produces = "application/json") // Read status of all columns in the schema
	public ResponseEntity<String> /* with List<DcError> */ getStatuses(HttpSession session, @PathVariable String id) {
		Account acc = repository.getAccountForSession(session);
		if(acc == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(DcError.error(DcErrorCode.NOT_FOUND_IDENTITY, "Session: "+session));
		}
		LOG.debug("Method: {}, Account: {}", "GET/schemas/id/columns/statuses", acc.getId());

		Schema schema = repository.getSchema(UUID.fromString(id));
		if(schema == null) return ResponseEntity.ok(DcError.error(DcErrorCode.GENERAL, "Schema not found.", ""));

		String jelems = "";
		for(Column elem : schema.getColumns()) {
			String jelem = elem.getStatus().toJson();
			jelems += "\"" + elem.getId() + "\"" + ": " + jelem + ", ";
		}
		if(jelems.length() > 2) {
			jelems = jelems.substring(0, jelems.length()-2);
		}
		return ResponseEntity.ok( "{" + jelems + "}" );
	}
	
	// Operations of one schema 

	@CrossOrigin(origins = crossOrigins)
	@RequestMapping(value = "/schemas/{id}/evaluate", method = RequestMethod.GET, produces = "application/json") // Evaluate data in the schema
	public ResponseEntity<String> /* DcError */ evaluate(HttpSession session, @PathVariable String id) {
		Account acc = repository.getAccountForSession(session);
		if(acc == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(DcError.error(DcErrorCode.NOT_FOUND_IDENTITY, "Session: "+session));
		}
		LOG.debug("Method: {}, Account: {}", "GET/schemas/id/evaluate", acc.getId());

		Schema schema = repository.getSchema(UUID.fromString(id));
		if(schema == null) return ResponseEntity.ok(DcError.error(DcErrorCode.GENERAL, "Schema not found.", ""));

		try {
			schema.getTables().forEach(x -> x.markCleanAsNew()); // Mark all columns in the schema as new (dirty, non-evaluated)
			schema.translate();
			schema.evaluate(); // Evaluate
			acc.schemaEvaluateCount++;
		}
		catch(Exception e) {
			LOG.error("Exception",  e);
			return ResponseEntity.ok(DcError.error(DcErrorCode.GENERAL, "Error evaluating data.", e.getMessage()));
		}

		return ResponseEntity.ok( "{ \"data\": [] }" );
	}

	//
	// Tables
	//

	@CrossOrigin(origins = crossOrigins)
	@RequestMapping(value = "/tables/{id}", method = RequestMethod.GET, produces = "application/json") // Read one table with the specified id
	public ResponseEntity<String> /* of Table */ getTable(HttpSession session, @PathVariable String id) { 
		Account acc = repository.getAccountForSession(session);
		if(acc == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(DcError.error(DcErrorCode.NOT_FOUND_IDENTITY, "Session: "+session));
		}
		LOG.debug("Method: {}/{}, Account: {}", "GET/tables", id, acc.getId());

		Table table = repository.getTable(acc.getId(), UUID.fromString(id));
		if(table == null) return ResponseEntity.ok(DcError.error(DcErrorCode.GENERAL, "Table not found.", ""));

		return ResponseEntity.ok( table.toJson() );
	}
	@CrossOrigin(origins = crossOrigins)
	@RequestMapping(value = "/tables/{id}", method = RequestMethod.PUT, produces = "application/json") // Update an existing table
	public ResponseEntity<String> /* of Table */ updateTable(HttpSession session, @PathVariable String id, @RequestBody String body) { 
		Account acc = repository.getAccountForSession(session);
		if(acc == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(DcError.error(DcErrorCode.NOT_FOUND_IDENTITY, "Session: "+session));
		}
		LOG.debug("Method: {}/{}, Account: {}", "PUT/tables", id, acc.getId());

		Table table = repository.getTable(acc.getId(), UUID.fromString(id));
		if(table == null) return ResponseEntity.ok(DcError.error(DcErrorCode.GENERAL, "Table not found.", ""));

		Schema schema = table.getSchema();

		try {
			schema.updateTableFromJson(body); // Main operation
			acc.tableUpdateCount++;
		}
		catch(DcError e) {
			LOG.error("DcError",  e);
			return ResponseEntity.ok(e.toJson());
		}
		catch(Exception e) {
			LOG.error("Exception",  e);
			return ResponseEntity.ok(DcError.error(DcErrorCode.GENERAL, "Error updating table.", e.getMessage()));
		}

		return ResponseEntity.ok( table.toJson() );
	}
	@CrossOrigin(origins = crossOrigins)
	@RequestMapping(value = "/tables/{id}", method = RequestMethod.DELETE, produces = "application/json") // Delete the specified table (and its columns)
	public ResponseEntity<String> deleteTable(HttpSession session, @PathVariable String id) { 
		Account acc = repository.getAccountForSession(session);
		if(acc == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(DcError.error(DcErrorCode.NOT_FOUND_IDENTITY, "Session: "+session));
		}
		LOG.debug("Method: {}/{}, Account: {}", "DELETE/tables", id, acc.getId());

		Table table = repository.getTable(acc.getId(), UUID.fromString(id));
		if(table == null) return ResponseEntity.ok(DcError.error(DcErrorCode.GENERAL, "Table not found.", ""));

		Schema schema = table.getSchema();

		try {
			schema.deleteTable(id); // Main operation
			acc.tableDeleteCount++;
		}
		catch(Exception e) {
			LOG.error("Exception",  e);
			return ResponseEntity.ok(DcError.error(DcErrorCode.GENERAL, "Error deleting table.", e.getMessage()));
		}

		return ResponseEntity.ok(null);
	}

	// Records from one table

	@CrossOrigin(origins = crossOrigins)
	@RequestMapping(value = "/tables/{id}/data/json", method = RequestMethod.GET, produces = "application/json") // Read records from one table with the specified id
	public ResponseEntity<String> /* of List<Records> */ getRecordsJson(HttpSession session, @PathVariable String id) { 
		Account acc = repository.getAccountForSession(session);
		if(acc == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(DcError.error(DcErrorCode.NOT_FOUND_IDENTITY, "Session: "+session));
		}
		LOG.debug("Method: {}, Account: {}", "GET/tables/id/data/json", acc.getId());

		Table table = repository.getTable(acc.getId(), UUID.fromString(id));
		if(table == null) return ResponseEntity.ok(DcError.error(DcErrorCode.GENERAL, "Table not found.", ""));

		Range range = null; // All records
		String data = "";
		for(Record record : table.read(range)) {
			String data_elem = record.toJson();
			data += data_elem + ", ";
		}
		if(data.length() > 2) {
			data = data.substring(0, data.length()-2);
		}
		return ResponseEntity.ok( "{\"data\": [" + data + "]}" );
	}
	@CrossOrigin(origins = crossOrigins)
	@RequestMapping(value = "/tables/{id}/data/csv", method = RequestMethod.GET, produces = "application/json") // Read records from one table with the specified id
	public ResponseEntity<String> /* of List<Records> */ getRecordsCsv(HttpSession session, @PathVariable String id) { 
		Account acc = repository.getAccountForSession(session);
		if(acc == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(DcError.error(DcErrorCode.NOT_FOUND_IDENTITY, "Session: "+session));
		}
		LOG.debug("Method: {}, Account: {}", "GET/tables/id/data/csv", acc.getId());

		Table table = repository.getTable(acc.getId(), UUID.fromString(id));
		if(table == null) return ResponseEntity.ok(DcError.error(DcErrorCode.GENERAL, "Table not found.", ""));

		Range range = null; // All records
		List<String> columns = table.getSchema().getColumns(table.getName()).stream().map(x -> x.getName()).collect(Collectors.<String>toList());
		String header = "";
		for(String col : columns) {
			header += col + ",";
		}
		if(header.length() >= 1) header = header.substring(0, header.length()-1);
		header += "\n";

		String data = "";
		for(Record record : table.read(range)) {
			String data_elem = record.toCsv(columns);
			data += data_elem + "\n";
		}
		return ResponseEntity.ok( "" + header +  data + "" );
	}
	@CrossOrigin(origins = crossOrigins)
	@RequestMapping(value = "/tables/{id}/data/json", method = RequestMethod.POST, produces = "application/json") // Create records in a table with a given id
	public ResponseEntity<String> /* of List<Records> */ writeRecordsJson(HttpSession session, @PathVariable String id, @RequestBody String body) {
		Account acc = repository.getAccountForSession(session);
		if(acc == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(DcError.error(DcErrorCode.NOT_FOUND_IDENTITY, "Session: "+session));
		}
		LOG.debug("Method: {}, Account: {}", "POST/tables/id/data/json", acc.getId());

		Table table = repository.getTable(acc.getId(), UUID.fromString(id));
		if(table == null) return ResponseEntity.ok(DcError.error(DcErrorCode.GENERAL, "Table not found.", ""));

		try {
			List<Record> records = Record.fromJsonList(body);
			table.append(records, null);
			acc.tableUploadCount++;
		}
		catch(Exception e) {
			LOG.error("Exception",  e);
			return ResponseEntity.ok(DcError.error(DcErrorCode.GENERAL, "Error appending data.", e.getMessage()));
		}
		
		return ResponseEntity.ok("{}");
	}
	@CrossOrigin(origins = crossOrigins)
	@RequestMapping(value = "/tables/{id}/data/csv", method = RequestMethod.POST, produces = "application/json") // Create records in a table with a given id
	public ResponseEntity<String> /* of List<Records> */ writeRecordsCsv(HttpSession session, @PathVariable String id, @RequestBody String body) {
		Account acc = repository.getAccountForSession(session);
		if(acc == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(DcError.error(DcErrorCode.NOT_FOUND_IDENTITY, "Session: "+session));
		}
		LOG.debug("Method: {}, Account: {}", "POST/tables/id/data/csv", acc.getId());

		Table table = repository.getTable(acc.getId(), UUID.fromString(id));
		if(table == null) return ResponseEntity.ok(DcError.error(DcErrorCode.GENERAL, "Table not found.", ""));

		try {
			List<Record> records = Record.fromCsvList(body);
			table.append(records, null);
			acc.tableUploadCount++;
		}
		catch(Exception e) {
			LOG.error("Exception",  e);
			return ResponseEntity.ok(DcError.error(DcErrorCode.GENERAL, "Error appending data.", e.getMessage()));
		}
		
		return ResponseEntity.ok("{}");
	}


	@CrossOrigin(origins = crossOrigins)
	@RequestMapping(value = "/tables/{id}/data", method = RequestMethod.DELETE, produces = "application/json") // Delete data from the specified table
	public ResponseEntity<String> deleteRecords(HttpSession session, @PathVariable String id) { 
		Account acc = repository.getAccountForSession(session);
		if(acc == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(DcError.error(DcErrorCode.NOT_FOUND_IDENTITY, "Session: "+session));
		}
		LOG.debug("Method: {}, Account: {}", "DELETE/tables/id/data", acc.getId());

		Table table = repository.getTable(acc.getId(), UUID.fromString(id));
		if(table == null) return ResponseEntity.ok(DcError.error(DcErrorCode.GENERAL, "Table not found.", ""));

		try {
			table.remove(); // Main operation
			acc.tableEmptyCount++;
		}
		catch(Exception e) {
			LOG.error("Exception",  e);
			return ResponseEntity.ok(DcError.error(DcErrorCode.GENERAL, "Error deleting data.", e.getMessage()));
		}

		return ResponseEntity.ok(null);
	}

	//
	// Columns
	//
	
	@CrossOrigin(origins = crossOrigins)
	@RequestMapping(value = "/columns/{id}", method = RequestMethod.GET, produces = "application/json") // Read one column with the specified id
	public ResponseEntity<String> /* of Column */ getColumn(HttpSession session, @PathVariable String id) { 
		Account acc = repository.getAccountForSession(session);
		if(acc == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(DcError.error(DcErrorCode.NOT_FOUND_IDENTITY, "Session: "+session));
		}
		LOG.debug("Method: {}/{}, Account: {}", "GET/columns", id, acc.getId());

		Column column = repository.getColumn(acc.getId(), UUID.fromString(id));
		if(column == null) return ResponseEntity.ok(DcError.error(DcErrorCode.GENERAL, "Column not found.", ""));

		return ResponseEntity.ok( column.toJson() );
	}
	@CrossOrigin(origins = crossOrigins)
	@RequestMapping(value = "/columns/{id}", method = RequestMethod.PUT, produces = "application/json") // Update an existing column
	public ResponseEntity<String> /* of Column */ updateColumn(HttpSession session, @PathVariable String id, @RequestBody String body) { 
		Account acc = repository.getAccountForSession(session);
		if(acc == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(DcError.error(DcErrorCode.NOT_FOUND_IDENTITY, "Session: "+session));
		}
		LOG.debug("Method: {}/{}, Account: {}", "PUT/columns", id, acc.getId());

		Column column = repository.getColumn(acc.getId(), UUID.fromString(id));
		if(column == null) return ResponseEntity.ok(DcError.error(DcErrorCode.GENERAL, "Column not found.", ""));

		Schema schema = column.getSchema();

		try {
			schema.updateColumnFromJson(body); // Main operation
			schema.translate();
			acc.columnUpdateCount++;
		}
		catch(DcError e) {
			LOG.error("DcError",  e);
			return ResponseEntity.ok(e.toJson());
		}
		catch(Exception e) {
			LOG.error("Exception",  e);
			return ResponseEntity.ok(DcError.error(DcErrorCode.GENERAL, "Error updating column.", e.getMessage()));
		}

		return ResponseEntity.ok( column.toJson() );
	}
	@CrossOrigin(origins = crossOrigins)
	@RequestMapping(value = "/columns/{id}", method = RequestMethod.DELETE, produces = "application/json") // Delete the specified table (and its columns)
	public ResponseEntity<String> deleteColumn(HttpSession session, @PathVariable String id) { 
		Account acc = repository.getAccountForSession(session);
		if(acc == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(DcError.error(DcErrorCode.NOT_FOUND_IDENTITY, "Session: "+session));
		}
		LOG.debug("Method: {}/{}, Account: {}", "DELETE/columns", id, acc.getId());

		Column column = repository.getColumn(acc.getId(), UUID.fromString(id));
		if(column == null) return ResponseEntity.ok(DcError.error(DcErrorCode.GENERAL, "Column not found.", ""));

		Schema schema = column.getSchema();

		try {
			schema.deleteColumn(id); // Main operation
			schema.translate();
			acc.columnDeleteCount++;
		}
		catch(Exception e) {
			LOG.error("Exception",  e);
			return ResponseEntity.ok(DcError.error(DcErrorCode.GENERAL, "Error deleting column.", e.getMessage()));
		}

		return ResponseEntity.ok(null);
	}

	//
	// Assets. They have description (like name/id and type) and contents (file itself)
	//

	// Many assets

	@CrossOrigin(origins = crossOrigins)
	@RequestMapping(value = "/assets", method = RequestMethod.GET, produces = "application/json") // Read all assets
	public ResponseEntity<String> /* with List<Asset> */ getAssets(HttpSession session) { 
		Account acc = repository.getAccountForSession(session);
		if(acc == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(DcError.error(DcErrorCode.NOT_FOUND_IDENTITY, "Session: "+session));
		}
		LOG.debug("Method: {}, Account: {}", "GET/assets", acc.getId());

		List<Asset> assets = repository.getAssetsForAccount(acc.getId());
		String jelems = "";
		for(Asset elem : assets) {
			String jelem = "{}"; // elem.toJson();
			jelems += jelem + ", ";
		}
		if(jelems.length() > 2) {
			jelems = jelems.substring(0, jelems.length()-2);
		}
		return ResponseEntity.ok( "{\"data\": [" + jelems + "]}" );
	}
	@CrossOrigin(origins = crossOrigins)
	@RequestMapping(value = "/assets", method = RequestMethod.POST, produces = "application/json") // Create one (or several) assets
	public ResponseEntity<String> /* of List<Asset> */ createAssets(HttpSession session, @RequestParam("file") MultipartFile file) { 
		Account acc = repository.getAccountForSession(session);
		if(acc == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(DcError.error(DcErrorCode.NOT_FOUND_IDENTITY, "Session: "+session));
		}
		LOG.debug("Method: {}, Account: {}", "POST/assets", acc.getId());

		List<Asset> assets = repository.getAssetsForAccount(acc.getId());
		if(file.isEmpty()) {
			return ResponseEntity.ok("{}"); // Error file is empty file.getOriginalFilename()
		}

		byte[] data = new byte[(int)file.getSize()];
		try {
			file.getInputStream().read(data);
			return ResponseEntity.ok("{}");
		} 
		catch (IOException e) {
			LOG.error("Exception",  e);
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

		return ResponseEntity.ok("{}");
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
GET    /users       - Return a list of all users (you may not want to make this publicly available)
GET    /users/:id   - Return the user with that id
POST   /users      - Create a new user. Return a 201 Status Code and the newly created id (if you want). HttpStatus.CREATED
PUT    /users/:id   - Update the user with that id. 
DELETE /users/:id  - Delete the user with that id.


session expired, 400 (safest, facebook), 401 (Unauthorized, not relevant) with custom error JSON response (facebook):
{
  "error": {
    "message": "Error validating access token: Session has expired on Jul 17, 2014 9:00am. The current time is Jul 17, 2014 9:07am.",
    "type": "OAuthException",
    "code": 190,
    "error_subcode": 463
  }
}

OR
https://developers.google.com/doubleclick-search/v2/standard-error-responses
{
 "error": {
  "errors": [
   {
    "domain": "global",
    "reason": "invalidParameter",
    "message": "Invalid string value: 'asdf'. Allowed values: [mostpopular]",
    "locationType": "parameter",
    "location": "chart"
   }
  ],
  "code": 400,
  "message": "Invalid string value: 'asdf'. Allowed values: [mostpopular]"
 }
}
 

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
