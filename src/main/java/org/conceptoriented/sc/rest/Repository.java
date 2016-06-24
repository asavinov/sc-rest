package org.conceptoriented.sc.rest;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import org.conceptoriented.sc.core.*;

@Service
public class Repository  {
	public String name = "My repository";
	
	public Map<String, Space> spaces = new HashMap<String, Space>();

	public Space createSampleSpace() {
		Space space = new Space("My Space");
		
		Table t1 = space.createTable("Table 1");
		space.createColumn("Column 11", "Table 1", "Double");
		space.createColumn("Column 12", "Table 1", "String");
		
		space.createTable("Table 2");
		space.createColumn("Column 21", "Table 1", "Double");
		space.createColumn("Column 22", "Table 1", "String");

		return space;
	}
	
	public Repository() {
		spaces.put("sample", createSampleSpace());
	}

}
