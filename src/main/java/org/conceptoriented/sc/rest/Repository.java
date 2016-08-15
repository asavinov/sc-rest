package org.conceptoriented.sc.rest;

import org.springframework.stereotype.Service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.servlet.http.HttpSession;

import org.conceptoriented.sc.core.*;

@Service
public class Repository  {

	// It is where we store various assets including UDFs
	protected String udfDir; 
	protected File classDir;
	
	protected Duration accountTimeout = Duration.ofMinutes(60); // Maximum time after being accessed
	protected Duration accountAge = Duration.ofDays(1); // Maximum time after being created
	protected Instant lastCheck = Instant.now();
	public void pruneAccounts() { // Delete all expired accounts
		Instant now = Instant.now();
		if(Duration.between(lastCheck, now).getSeconds() < 10) return; // Do not prune too frequently
		List<Account> toDelete = new ArrayList<Account>();
		for(Account acc : accounts) {
			if(Duration.between(acc.getAccessTime(), now).getSeconds() > accountTimeout.getSeconds()) { // Not accessed long time
				toDelete.add(acc);
			}
		}
		for(Account acc : toDelete) accounts.remove(acc);
	}

	//
	// All existing accounts. An account contains schemas, assets and parameters like name.
	//
	protected List<Account> accounts = new ArrayList<Account>();
	
	public Account getAccount(UUID id) {
		pruneAccounts();
		Optional<Account> ret = accounts
				.stream()
				.filter(acc -> acc.getId().equals(id))
				.findAny();
        if(!ret.isPresent()) return null;
        Account acc = ret.get();
        acc.setAccessed();
    	return acc;
	}
	public Account getAccountForName(String name) { // An account must have unique name
		pruneAccounts();
		Optional<Account> ret = accounts
				.stream()
				.filter(x -> x.getName().equals(name))
				.findAny();
        if(!ret.isPresent()) return null;
        Account acc = ret.get();
        acc.setAccessed();
    	return acc;
	}
	public Account getAccountForSession(HttpSession session) { // Find an account associated with this session
		pruneAccounts();
		Optional<Account> ret = accounts
				.stream()
				.filter(x -> x.getSession().equals(session.getId()))
				.findAny();
        if(!ret.isPresent()) return null;
        Account acc = ret.get();
        acc.setAccessed();
    	return acc;
	}
	public Account addAccount(Account account) {
		accounts.add(account);
		return account;
	}
	public Account addSampleAccount() {

		// Account
		Account account = new Account(this, "test@host.com");
		accounts.add(account);

		/*
		URL[] classUrl = new URL[1];
		try {
			// We might also add more specific folder for this schema only (like subfolder with schema id)
			classUrl[0] = classDir.toURI().toURL();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		URLClassLoader classLoader = new URLClassLoader(classUrl);
		account.setClassLoader(classLoader);
		*/

		// Schema
		Schema schema = new Schema("My Schema");
		this.addSchema(account, schema); // Here schema will get class loader from its account
		
		// Schema
		Table t1 = schema.createTable("T");
		t1.setMaxLength(3);
		schema.createColumn("A", "T", "Double");
		schema.createColumn("B", "T", "Double");
		Column c13 = schema.createColumn("C", "T", "Double");
        String d13 = "{ `class`:`org.conceptoriented.sc.core.SUM`, `dependencies`:[`A`,`B`] }";
		c13.setDescriptor(d13.replace('`', '"'));
		
		schema.createTable("Table 2");
		schema.createColumn("Column 21", "Table 2", "Double");
		schema.createColumn("Column 22", "Table 2", "Double");
		
		return account;
	}

	//
	// All existing schemas belonging to different users
	//
	protected Map<Schema, Account> schemas = new HashMap<Schema, Account>();

	public Schema getSchema(UUID id) {
		Optional<Schema> ret = schemas.keySet()
				.stream()
				.filter(key -> key.getId().equals(id))
				.findAny();
        if(ret.isPresent()) return ret.get();
        else return null;
	}
	public Schema getSchemaForName(UUID id, String name) {
		Optional<Schema> ret = getSchemasForAccount(id)
				.stream()
				.filter(x -> x.getName().equals(name))
				.findAny();
        if(ret.isPresent()) return ret.get();
        else return null;
	}
	public List<Schema> getSchemasForAccount(UUID id) {
		List<Schema> ret = schemas.entrySet()
				.stream()
				.filter(entry -> entry.getValue().getId().equals(id))
				.map(Map.Entry::getKey)
				.collect(Collectors.<Schema>toList());
		return ret;
	}
	public Schema addSchema(Account account, Schema schema) {
		if(account.getClassLoader() != null) {
			// Schema will use its account loader which knows how to load classes from this account assets
			schema.setClassLoader(account.getClassLoader());
		}
		schemas.put(schema, account);
		return schema;
	}
	public void deleteSchema(Schema schema) {
		schemas.remove(schema);
	}

	public Table getTable(UUID accId, UUID id) {
		for(Schema schema : getSchemasForAccount(accId)) {
			Table table = schema.getTableById(id.toString());
			if(table != null) return table;
		}
		return null;
	}

	public Column getColumn(UUID accId, UUID id) {
		for(Schema schema : getSchemasForAccount(accId)) {
			Column column = schema.getColumnById(id.toString());
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
		udfDir = "C:/temp/classes/"; // It is common for all schema but can contain subfolders for individual schemas
		classDir = new File(udfDir);
	}

}

class Account {

	private final UUID id;
	public UUID getId() {
		return id;
	}

	private final Instant creationTime;
	public Instant getCreationTime() {
		return creationTime;
	}

	private Instant accessTime;
	public Instant getAccessTime() {
		return accessTime;
	}
	public void setAccessed() {
		this.accessTime = Instant.now();
	}

	private Instant changeTime;
	public Instant getChangeTime() {
		return changeTime;
	}
	public void setChanged() {
		this.changeTime = Instant.now();
		this.accessTime = this.changeTime;
	}

	// Session id (in future, there can be many sessions for one account)
	private String session = "";
	public String getSession() {
		return session;
	}
	public void setSession(String session) {
		this.session = session;
	}

	// User/account name
	private String name = "";
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	// This class loader knows how to get evaluator classes/instances from the assets of this account. 
	// It is injected into each schema of this user so that custom columns can be evaluated.
	private ClassLoader classLoader; 
	public ClassLoader getClassLoader() {
		return classLoader;
	}
	public void setClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}
	
	private Repository repository;

	public List<Asset> getAssets(String extension) {
		return repository.getAssetsForAccount(this.getId());
	}
	
	public Account(Repository repository) {
		this(repository, "");
	}

	public Account(Repository repository, String name) {
		this.id = UUID.randomUUID();
		this.creationTime = Instant.now();
		this.accessTime = this.creationTime;
		this.changeTime = this.creationTime;

		this.name = name;
		
		this.repository = repository;
		
		// Instantiate an account-specific class loader
		//classLoader = ClassLoader.getSystemClassLoader();
		classLoader = new UdfClassLoader(this);
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
	
	// Asset content
	private byte[] data;
	public byte[] getData() {
		return data;
	}
	public void setData(byte[] data) {
		this.data = data;
	}
	
	 public Asset() {
		this.id = UUID.randomUUID();
	}

}
