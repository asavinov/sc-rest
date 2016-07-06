package org.conceptoriented.sc.rest;

import org.springframework.stereotype.Service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

import org.conceptoriented.sc.core.*;

@Service
public class Repository  {
	public String name = "My repository";

	// It is where we store various assets including UDFs
	protected String udfDir; 
	protected File classDir;
	
	public Map<String, Space> spaces = new HashMap<String, Space>();

	public Space createSampleSpace() {
		Space space = new Space("My Space");
		
		URL[] classUrl = new URL[1];
		try {
			// We might also add more specific folder for this space only (like subfolder with space id)
			classUrl[0] = classDir.toURI().toURL();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		URLClassLoader classLoader = new URLClassLoader(classUrl);
		space.setClassLoader(classLoader);
		
		Table t1 = space.createTable("Table 1");
		space.createColumn("Column 11", "Table 1", "Double");
		space.createColumn("Column 12", "Table 1", "String");
		
		space.createTable("Table 2");
		space.createColumn("Column 21", "Table 2", "Integer");
		space.createColumn("Column 22", "Table 2", "String");

		return space;
	}
	
	public Repository() {
		spaces.put("sample", createSampleSpace());

		udfDir = "C:/temp/classes/"; // It is common for all space but can contain subfolders for individual spaces
		classDir = new File(udfDir);
	}

}
