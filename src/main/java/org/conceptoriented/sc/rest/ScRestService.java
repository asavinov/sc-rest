package org.conceptoriented.sc.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
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

@RestController
public class ScRestService {

	private static final Logger LOG = Logger.getLogger(ScRestService.class.getName());

	@RequestMapping(method = RequestMethod.GET, value = "/ping", produces = "text/plain")
	public String ping(HttpServletRequest request /*HttpSession session*/) {
		HttpSession session = request.getSession();
		LOG.info("SUCCESS");
		return "StreamCommandr";
	}

	@RequestMapping(value="/spaces")
	public List<Space> spaces() {
		
		List<Space> list = new ArrayList<Space>();
		list.add(new Space("Space 1", 25));
		list.add(new Space("Space 2", 55));
		list.add(new Space("Space 3", 45));
		
		return list;
	}

}


// Criteria:
// - Serialization is needed for two purposes: persistence and transfer to a web app 
// - Java references to other domain objects have to be transformed to uuid during serialization and then resolved during de-serialization
//   - This means that (de-)serializer need access to the whole space
// - Inheritance
// - Whole space, Table w. cols, or individual obj



// ALTERNATIVE 1: Annotate domain classes to use custom serializer 

//@JsonDeserialize(using = RoleDeserializer.class)
@JsonSerialize(using = SpaceSerializer.class)
class Space {
	public String name;
	public double age;

	public Space(String name, double age) {
		this.name = name;
		this.age = age;
	}
}

class SpaceSerializer extends JsonSerializer<Space> {
	@Override
	public void serialize(Space value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {

	    gen.writeStartObject();

	    gen.writeStringField("name", value.name);
	    gen.writeNumberField("age", value.age);
	    gen.writeStringField("sent", "today");

	    gen.writeEndObject();
	}
}

//ALTERNATIVE 2: Create a Java class which can be easily serialized by a standard serializer like ColumnJson 


//ALTERNATIVE 3: Implement special methods of the domain class which can serialized its instances
