package org.conceptoriented.sc.rest;

import org.springframework.stereotype.Service;

import com.google.common.io.Files;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
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
	
	protected Duration accountTimeout = Duration.ofHours(3); // Maximum inactivity time after last access
	protected Duration accountAge = Duration.ofHours(12); // Maximum existence time after being created
	protected Instant lastCheck = Instant.now();

	public void pruneAccounts() { // Delete all expired accounts

		Instant now = Instant.now();
		if(Duration.between(lastCheck, now).getSeconds() < 10) return; // Do not prune too frequently
		List<Account> toDelete = new ArrayList<Account>();
		for(Account acc : accounts) {
			if(Duration.between(acc.getAccessTime(), now).getSeconds() > accountTimeout.getSeconds()) { // Not accessed long time
				acc.setDeleted();
				toDelete.add(acc);
			}
		}

		String fileName = "repo.log";
		try (FileWriter fw = new FileWriter(fileName, true);
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter out = new PrintWriter(bw)) {

			// Write all pruned accounts to file
			for(Account acc : toDelete) {
				String jacc = acc.toJson();
				out.print(jacc);
				out.println(", ");
			}
		} catch (IOException e) {
			;
		} finally {
			for(Account acc : toDelete) {
				accounts.remove(acc);
			}
		}
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

	public static Schema buildSampleSchema1(String name) {
		if(name == null || name.isEmpty()) name = "My Schema";

		Schema schema = new Schema(name);
		
		//
		// Table
		//
		Table t1 = schema.createTable("My Table");
		t1.setMaxLength(10);

		// Columns
		Column c;
		c = schema.createColumn("A", "My Table", "Double");
		c = schema.createColumn("B", "My Table", "Double");
		c.setFormula("[A] + 1.0");
		c = schema.createColumn("C", "My Table", "Double");
		c.setFormula("[A] + [B]");
        //String d13 = "{ `class`:`org.conceptoriented.sc.core.SUM`, `dependencies`:[`A`,`B`] }";
		//c13.setDescriptor(d13.replace('`', '"'));
		c = schema.createColumn("AA", "My Table", "Double");
		c.setFormula("[A]");
		c.setAccuformula("out + [E] * 10.0");
		c.setAccutable("My Table 2");
		c.setAccupath("[GG]");
		
		// Data
        Record r = new Record();

        r.set("A", 11.11); r.set("B", 22.22);
        t1.append(r);

        r.set("A", 33.33); r.set("B", 44.44);
        t1.append(r);

        r.set("A", 55.55); r.set("B", 66.66);
        t1.append(r);

		//
		// Table 2
		//
		Table t2 = schema.createTable("My Table 2");
		t2.setMaxLength(20);

		// Columns
		Column c21 = schema.createColumn("D", "My Table 2", "Double");
		Column c22 = schema.createColumn("E", "My Table 2", "Double");
		c22.setFormula("[D] * 2.0");
		Column c23 = schema.createColumn("GG", "My Table 2", "My Table");
		c23.setFormula("{ [A]=[D] }");
		
		// Data
        r = new Record();

        r.set("D", 11.11);
        t2.append(r);

        r.set("D", 22.22);
        t2.append(r);

		return schema;
	}

	public static Schema buildSampleSchema2(String name) {
		if(name == null || name.isEmpty()) name = "Sales";
		
		Schema schema = new Schema(name);

		String path = "src/test/resources/example1";
		if (!new File(path).exists())
			path = "samples/example1";
		if(!new File(path).exists())
			return schema;

		// Tables

		String path1 = path + "/OrderItems.csv";
        Table table1 = schema.createFromCsv(path1, true);

        String path2 = path + "/Products.csv";
        Table table2 = schema.createFromCsv(path2, true);

        Table table3 = schema.createTable("Orders");
        schema.createColumn("ID", "Orders", "Double");

		// Columns

        Column c11 = schema.createColumn("Product", "OrderItems", "Products");
        c11.setKind(DcColumnKind.LINK);
		c11.setFormula("{ [ID] = [Product ID] }");
        Column c12 = schema.createColumn("Order", "OrderItems", "Orders");
        c12.setKind(DcColumnKind.LINK);
		c12.setFormula("{ [ID] = [Order ID] }");

		Column c;
		c = schema.createColumn("Total Amount", "Products", "Double");
        c.setKind(DcColumnKind.ACCU);
		c.setFormula("0.0");
		c.setAccuformula("out + [Quantity] * [Unit Price]");
		c.setAccutable("OrderItems");
		c.setAccupath("[Product]");

		c = schema.createColumn("Total Amount", "Orders", "Double");
        c.setKind(DcColumnKind.ACCU);
		c.setFormula("0.0");
		c.setAccuformula("out + [Quantity] * [Unit Price]");
		c.setAccutable("OrderItems");
		c.setAccupath("[Order]");

		return schema;
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

	private Instant deletionTime;
	public Instant getDeletionTime() {
		return deletionTime;
	}
	public void setDeleted() {
		this.deletionTime = Instant.now();
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
	
	//
	// Statistics
	//
	public int schemaCreateCount = 0;
	public int schemaUpdateCount = 0;
	public int schemaDeleteCount = 0;
	public int schemaEvaluateCount = 0;

	public int tableCreateCount = 0;
	public int tableUpdateCount = 0;
	public int tableDeleteCount = 0;

	public int tableUploadCount = 0;
	public int tableEvaluateCount = 0;
	public int tableEmptyCount = 0;

	public int columnCreateCount = 0;
	public int columnUpdateCount = 0;
	public int columnDeleteCount = 0;

	public String toJson() {
		// Trick to avoid backslashing double quotes: use backticks and then replace it at the end 
		String jid = "`id`: `" + this.getId() + "`";

		String jname = "`name`: `" + this.getName() + "`";
		
		String jcreation_time = "`creationTime`: `" + this.getCreationTime() + "`";
		String jaccess_time = "`accessTime`: `" + this.getAccessTime() + "`";
		String jchange_time = "`changeTime`: `" + this.getChangeTime() + "`";
		String jdeletion_time = "`deletionTime`: `" + this.getDeletionTime() + "`";

		String jsession = "`session`: `" + this.getSession() + "`";

		String json = jid + ", " + jname + ", " + jcreation_time + ", " + jaccess_time + ", " + jchange_time + ", " + jdeletion_time + ", " + jsession;

		//
		// Statistics
		//
		String jschemastats = "`schemaCreateCount`: " + this.schemaCreateCount + ", `schemaUpdateCount`: " + this.schemaUpdateCount + ", `schemaDeleteCount`: " + this.schemaDeleteCount + ", `schemaEvaluateCount`: " + this.schemaEvaluateCount;
		String jtablestats = "`tableCreateCount`: " + this.tableCreateCount + ", `tableUpdateCount`: " + this.tableUpdateCount + ", `tableDeleteCount`: " + this.tableDeleteCount + ", `tableUploadCount`: " + this.tableUploadCount + ", `tableEvaluateCount`: " + this.tableEvaluateCount + ", `tableEmptyCount`: " + this.tableEmptyCount;
		String jcolumntstats = "`columnCreateCount`: " + this.columnCreateCount + ", `columnUpdateCount`: " + this.columnUpdateCount + ", `columnDeleteCount`: " + this.columnDeleteCount;
		
		String jstats = jschemastats + ", " + jtablestats + ", " + jcolumntstats;

		return ("{" + json + ", " + jstats + "}").replace('`', '"');
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
