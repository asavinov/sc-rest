package org.conceptoriented.sc.rest;

import org.springframework.stereotype.Service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.conceptoriented.sc.core.*;

@Service
public class Repository  {
	public String name = "My repository";

	// It is where we store various assets including UDFs
	protected String udfDir; 
	protected File classDir;
	
	//
	// All existing accounts. An account contains spaces, assets and parameters like name.
	//
	protected List<Account> accounts = new ArrayList<Account>();

	public Account getAccountForName(String name) { // An account must have unique name
		return null;
	}
	public Account getAccountForSession(UUID id) { // One session per account
		return null;
	}

	//
	// All existing spaces belonging to different users
	//
	protected Map<String, Space> spaces = new HashMap<String, Space>();

	public Space getSpaceForName(String name) {
		return spaces.get(name);
	}

	//
	// All existing assets belonging to different users
	//
	protected List<Asset> assets = new ArrayList<Asset>();

	public List<Asset> getAssetsForAccount(UUID id) {
		return null;
	}

	public Repository() {
		udfDir = "C:/temp/classes/"; // It is common for all space but can contain subfolders for individual spaces
		classDir = new File(udfDir);

		//
		// Create sample repository
		//

		// Account
		Account account = new Account();
		account.setName("test@host.com");
		accounts.add(account);

		URL[] classUrl = new URL[1];
		try {
			// We might also add more specific folder for this space only (like subfolder with space id)
			classUrl[0] = classDir.toURI().toURL();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		URLClassLoader classLoader = new URLClassLoader(classUrl);
		account.setClassLoader(classLoader);

		// Space
		Space space = new Space("My Space");
		space.setClassLoader(account.getClassLoader()); // Space will use its account loader which knows how to load from assets
		spaces.put("sample", space);
		
		// Schema
		Table t1 = space.createTable("Table 1");
		space.createColumn("Column 11", "Table 1", "Double");
		space.createColumn("Column 12", "Table 1", "String");
		
		space.createTable("Table 2");
		space.createColumn("Column 21", "Table 2", "Integer");
		space.createColumn("Column 22", "Table 2", "String");
	}

}

class Account {

	private final UUID id;
	public UUID getId() {
		return id;
	}

	// Session id (only one session per account)
	private String session;
	public String getSession() {
		return session;
	}
	public void setSession(String session) {
		this.session = session;
	}

	// User/account name
	private String name;
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	// This class loader knows how to get classes/instances from the assets of this account. 
	// It is injected into each space of this user so that custom columns can be evaluated.
	private ClassLoader classLoader; 
	public ClassLoader getClassLoader() {
		return classLoader;
	}
	public void setClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}
	
	public Account() {
		this.id = UUID.randomUUID();
		
		// Instantiate an account-specific class loader
		classLoader = ClassLoader.getSystemClassLoader();
	}

}

class Asset {

	private final UUID id;
	public UUID getId() {
		return id;
	}

	// Asset name (file name, without path)
	private String name;
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	// Owner
	private Account account;
	public Account getAccount() {
		return account;
	}
	public void setAccount(Account account) {
		this.account = account;
	}
	
	 public Asset() {
		this.id = UUID.randomUUID();
	}

}
