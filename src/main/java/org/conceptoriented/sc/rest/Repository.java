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

import org.apache.commons.lang3.tuple.Pair;
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
		if(Duration.between(this.lastCheck, now).getSeconds() < 10) return; // Do not prune too frequently
		List<Account> toDelete = new ArrayList<Account>();
		for(Account acc : this.accounts) {
			if(Duration.between(acc.getAccessTime(), now).getSeconds() > this.accountTimeout.getSeconds()) { // Not accessed long time
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
				this.accounts.remove(acc);
			}
		}
	}

	//
	// All existing accounts. An account contains schemas, assets and parameters like name.
	//
	protected List<Account> accounts = new ArrayList<Account>();
	
	public Account getAccount(UUID id) {
		pruneAccounts();
		Account acc = this.accounts
			.stream()
			.filter(x -> x.getId().equals(id))
			.findAny()
			.orElse(null);
		if(acc == null) return null;
        acc.setAccessed();
    	return acc;
	}
	public Account getAccountForName(String name) { // An account must have unique name
		pruneAccounts();
		Account acc = this.accounts
			.stream()
			.filter(x -> x.getName().equalsIgnoreCase(name))
			.findAny()
			.orElse(null);
		if(acc == null) return null;
        acc.setAccessed();
    	return acc;
	}
	public Account getAccountForSession(HttpSession session) { // Find an account associated with this session
		pruneAccounts();
		Account acc = this.accounts
				.stream()
				.filter(x -> x.getSession().equals(session.getId()))
				.findAny()
				.orElse(null);
        if(acc == null) return null;
        acc.setAccessed();
    	return acc;
	}
	public Account addAccount(Account account) {
		this.accounts.add(account);
		return account;
	}

	//
	// All existing schemas belonging to different accounts
	//
	protected List<Schema> schemas = new ArrayList<Schema>();

	protected List<Pair<Schema, Account>> saRelationship = new ArrayList<Pair<Schema, Account>>();

	public Schema getSchema(UUID id) {
		return this.schemas
			.stream()
			.filter(x -> x.getId().equals(id))
			.findAny()
			.orElse(null);
	}
	public Schema getSchemaForName(UUID id, String name) {
		return getSchemasForAccount(id)
			.stream()
			.filter(x -> x.getName().equalsIgnoreCase(name))
			.findAny()
			.orElse(null);
	}
	public List<Schema> getSchemasForAccount(UUID id) {
		return this.saRelationship
			.stream()
			.filter(x -> x.getRight().getId().equals(id))
			.map(x -> x.getLeft())
			.collect(Collectors.<Schema>toList());
	}
	public Schema addSchema(Account account, Schema schema) {
		if(account.getClassLoader() != null) {
			// Schema will use its account loader which knows how to load classes from this account assets
			schema.setClassLoader(account.getClassLoader());
		}
		this.schemas.add(schema);
		this.saRelationship.add(Pair.of(schema, account));
		return schema;
	}
	public void deleteSchema(Schema schema) {
		this.schemas.remove(schema);
		this.saRelationship.removeIf(x -> x.getLeft().getId().equals(schema.getId()));
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
		return this.assets.keySet()
			.stream()
			.filter(key -> key.getId().equals(id))
			.findAny()
			.orElse(null);
	}
	public List<Asset> getAssetsForAccount(UUID id) {
		return this.assets.entrySet()
				.stream()
				.filter(entry -> entry.getValue().getId().equals(id))
				.map(Map.Entry::getKey)
				.collect(Collectors.<Asset>toList());
	}
	public Asset addAsset(Account account, Asset asset) {
		this.assets.put(asset, account);
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

		//
		// Columns
		//
		Column c;
		c = schema.createColumn("My Table", "A", "Double");

		c = schema.createColumn("My Table", "B", "Double");
		c.setKind(DcColumnKind.CALC);
        c.setDefinitionCalc(new ColumnDefinitionCalc("[A] + 1.0", ExpressionKind.EXP4J));

		c = schema.createColumn("My Table", "C", "Double");
		c.setKind(DcColumnKind.CALC);
        c.setDefinitionCalc(new ColumnDefinitionCalc("[A] + [B]", ExpressionKind.EXP4J));

		c = schema.createColumn("My Table", "AA", "Double");
		c.setKind(DcColumnKind.ACCU);
        c.setDefinitionAccu(new ColumnDefinitionAccu("[A]", "[out] + [E] * 10.0", null, "My Table 2", "[GG]", ExpressionKind.EXP4J));
		
		//
		// Data
		//
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
		Column c21 = schema.createColumn("My Table 2", "D", "Double");
		Column c22 = schema.createColumn("My Table 2", "E", "Double");
		c22.setKind(DcColumnKind.CALC);
		c22.setDefinitionCalc(new ColumnDefinitionCalc("[D] * 2.0", ExpressionKind.EXP4J));

		Column c23 = schema.createColumn("My Table 2", "GG", "My Table");
		c23.setKind(DcColumnKind.LINK);
		c23.setDefinitionLink(new ColumnDefinitionLink("{ [A]=[D] }", ExpressionKind.EXP4J));
		
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

		//
		// Tables
		//

		String path1 = path + "/OrderItems.csv";
        Table table1 = schema.createFromCsvFile(path1, true);

        String path2 = path + "/Products.csv";
        Table table2 = schema.createFromCsvFile(path2, true);

        Table table3 = schema.createTable("Orders");
        schema.createColumn("Orders", "ID", "Double");

        //
		// Columns
        //

        Column c11 = schema.createColumn("OrderItems", "Product", "Products");
        c11.setKind(DcColumnKind.LINK);
        c11.setDefinitionLink(new ColumnDefinitionLink("{ [ID] = [Product ID] }", ExpressionKind.EXP4J));

		Column c12 = schema.createColumn("OrderItems", "Order", "Orders");
        c12.setKind(DcColumnKind.LINK);
        c12.setDefinitionLink(new ColumnDefinitionLink("{ [ID] = [Order ID] }", ExpressionKind.EXP4J));

		Column c;
		c = schema.createColumn("Products", "Total Amount", "Double");
        c.setKind(DcColumnKind.ACCU);
        c.setDefinitionAccu(new ColumnDefinitionAccu("0.0", "[out] + [Quantity] * [Unit Price]", null, "OrderItems", "[Product]", ExpressionKind.EXP4J));

		c = schema.createColumn("Orders", "Total Amount", "Double");
        c.setKind(DcColumnKind.ACCU);
        c.setDefinitionAccu(new ColumnDefinitionAccu("0.0", "[out] + [Quantity] * [Unit Price]", null, "OrderItems", "[Order]", ExpressionKind.EXP4J));

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
		return this.repository.getAssetsForAccount(this.getId());
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
