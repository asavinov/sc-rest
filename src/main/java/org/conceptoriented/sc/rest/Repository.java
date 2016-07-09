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
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.conceptoriented.sc.core.*;

@Service
public class Repository  {

	// It is where we store various assets including UDFs
	protected String udfDir; 
	protected File classDir;
	
	//
	// All existing accounts. An account contains spaces, assets and parameters like name.
	//
	protected List<Account> accounts = new ArrayList<Account>();

	public Account getAccount(UUID id) {
		Optional<Account> ret = accounts
				.stream()
				.filter(acc -> acc.getId().equals(id))
				.findAny();
        if(ret.isPresent()) return ret.get();
        else return null;
	}
	public Account getAccountForName(String name) { // An account must have unique name
		Optional<Account> ret = accounts
				.stream()
				.filter(x -> x.getName().equals(name))
				.findAny();
        if(ret.isPresent()) return ret.get();
        else return null;
	}
	public Account getAccountForSession(UUID id) { // One session per account
		return null;
	}
	public Account addAccount(Account account) {
		accounts.add(account);
		return account;
	}

	//
	// All existing spaces belonging to different users
	//
	protected Map<Space, Account> spaces = new HashMap<Space, Account>();

	public Space getSpace(UUID id) {
		Optional<Space> ret = spaces.keySet()
				.stream()
				.filter(key -> key.getId().equals(id))
				.findAny();
        if(ret.isPresent()) return ret.get();
        else return null;
	}
	public Space getSpaceForName(UUID id, String name) {
		Optional<Space> ret = getSpacesForAccount(id)
				.stream()
				.filter(x -> x.getName().equals(name))
				.findAny();
        if(ret.isPresent()) return ret.get();
        else return null;
	}
	public List<Space> getSpacesForAccount(UUID id) {
		List<Space> ret = spaces.entrySet()
				.stream()
				.filter(entry -> entry.getValue().getId().equals(id))
				.map(Map.Entry::getKey)
				.collect(Collectors.<Space>toList());
		return ret;
	}
	public Space addSpace(Account account, Space space) {
		spaces.put(space, account);
		return space;
	}

	public Table getTable(UUID accId, UUID id) {
		for(Space space : getSpacesForAccount(accId)) {
			Table table = space.getTableById(id.toString());
			if(table != null) return table;
		}
		return null;
	}

	public Column getColumn(UUID accId, UUID id) {
		for(Space space : getSpacesForAccount(accId)) {
			Column column = space.getColumnById(id.toString());
			if(column != null) return column;
		}
		return null;
	}

	//
	// All existing assets belonging to different users
	//
	protected Map<Asset, Account> assets = new HashMap<Asset, Account>();

	public Asset getAsset(UUID id) {
		Optional<Asset> ret = assets.keySet()
				.stream()
				.filter(key -> key.getId().equals(id))
				.findAny();
        if(ret.isPresent()) return ret.get();
        else return null;
	}
	public List<Asset> getAssetsForAccount(UUID id) {
		List<Asset> ret = assets.entrySet()
				.stream()
				.filter(entry -> entry.getValue().getId().equals(id))
				.map(Map.Entry::getKey)
				.collect(Collectors.<Asset>toList());
		return ret;
	}
	public Asset addAsset(Account account, Asset asset) {
		assets.put(asset, account);
		return asset;
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
		this.addSpace(account, space);
		
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
		this("");
	}

	public Account(String name) {
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
