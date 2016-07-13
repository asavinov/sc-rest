package org.conceptoriented.sc.rest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * Criteria:
 * - Load class dynamically by using the default class loader and the default class path. So the task of the user/system to put all jars to the class path.
 *   - The user jars can be uploaded dynamically to the location known to this default class loader or otherwise define in the class path, e.g., in the app config like plug-in-dir
 *   - It is important that there is one location for all external jars/classes but each space loads its evaluator classes individually and dynamically
 *   - The mechanism has to work when we manually copy/upload a jar with udf to the common system class path (maybe a special location for udfs specified in the configuration)
 * - What about dependencies and resolve?
 * - In future, we assume that columns in different spaces may have the same class name for evaluator 
 * - Also, evaluator classes from different space have to be somehow isolated   
 **/

// This class loader knows how to find UDF classes (jar or class files) provided by the user and hence treated as a user asset/resource.
// These user assets are files with special location which is specific to each user because we want to isolate multiple users and their resources.
// Currently, we support file system as a location of java code (jar and classes). 
// In future, the class loader could be able to load Java classes from other storages like databases.
// In future, we also could define a class loader from source code which will take source code as a resource and then compile it by converting into a class.

// http://www.javaworld.com/article/2077260/learn-java/learn-java-the-basics-of-java-class-loaders.html !!!
// https://examples.javacodegeeks.com/core-java/dynamic-class-loading-example/ - loading automatically
// http://www.javaworld.com/article/2071777/design-patterns/add-dynamic-java-code-to-your-application.html
// http://tutorials.jenkov.com/java-reflection/dynamic-class-loading-reloading.html - reading class from file
public class UdfClassLoader extends ClassLoader {
	
	/*
	try {
		Files.copy(file.getInputStream(), Paths.get("C:/TEMP/classes", file.getOriginalFilename()));
	} catch (IOException|RuntimeException e) {
		; // e.getMessage()
	}
	*/

	
	String classesDir = "";

	Account account;
	
	Map<String, Class> classes = new HashMap<String, Class>(); // Local cache of classes. It will be used for recursive calls.

    public UdfClassLoader(Account account) {
        this.account = account;
    }

	@Override
	public synchronized Class loadClass(String className, boolean resolve) throws ClassNotFoundException {
		
		Class clazz;

		//
		// Check if the class has been already loaded 
		// We must return the same class reference for the same name for multiple requests if we want the reference comparison to work as expcted.
		//

	    clazz = findLoadedClass(className);
	    if (clazz != null) return clazz;

		clazz = (Class) classes.get(className);
		if (clazz != null) return clazz; // The class has been already loaded so return its reference
		
		//
		// Try to load a system class from CLASSPATH first because system classes have priority over user-defined classes
		//

		try {
			clazz = super.findSystemClass(className); // Search in CLASSPATH
			return clazz;
		} catch (ClassNotFoundException e) {
			; // No system class with such a name
		}
		
		// Do we actually need this call???
		/*
		try {
			clazz = super.loadClass(className);
			return clazz;
		} catch (ClassNotFoundException e) {
			;
		}
		*/

		//
		// Try to load from our repository
		//

        try {
        	// Find bytes for the class name
            //InputStream in = getResourceAsStream(className.replace('.', '/') + ".class");
            //ByteArrayOutputStream out = new ByteArrayOutputStream();
            //StreamUtils.writeTo(in, out);
            //byte[] classData = out.toByteArray();
            //byte[] classData = org.springframework.util.StreamUtils.copyToByteArray(in);

        	byte[] classData = getClassBytesFromAssets(className.replace('.', '/') + ".class");

            // Use bytes to create a class
            clazz = defineClass(className, classData, 0, classData.length);
            if (resolve) {
                resolveClass(clazz);
            }
        } catch (Exception e) {
            clazz = super.loadClass(className, resolve);
        }
		
		/*
		byte classData[];
		classData = loadClassDataFromFile(className); 
		if (classData == null) { 
		    throw new ClassNotFoundException(); 
		}
		
		clazz = defineClass(className, classData, 0, classData.length); 

		if (resolve) { 
		    resolveClass(clazz); 
		} 
		*/
		
		// Store the class reference
		classes.put(className, clazz); 

		return clazz; 
	}
	

	@Override
	public URL getResource(String name) {
	    return null;
	}

	// http://stackoverflow.com/questions/16602668/creating-a-classloader-to-load-a-jar-file-from-a-byte-array
	//@Override
	//public byte[] getResourceAsStream(String name) {}


	// This method knows about assets and how to work with classes in assets
	protected byte[] getClassBytesFromAssets(String name) {

		List<Asset> assets = account.getAssets(".jar");
		
		for(Asset asset : assets) {
			byte[] jarBytes = asset.getData();
			byte[] classBytes = getClassBytesFromJarBytes(jarBytes, name);
			if(classBytes != null && classBytes.length > 0) return classBytes;
		}

		return null;
	}

	// Loop through all entries in the jar bytes and find the specified class
	protected byte[] getClassBytesFromJarBytes(byte[] jarBytes, String name) {
		if(jarBytes == null || jarBytes.length == 0 || name == null) return null;

		try (JarInputStream jis = new JarInputStream(new ByteArrayInputStream(jarBytes))) {
	        JarEntry entry;
	        while ((entry = jis.getNextJarEntry()) != null) {
	
	        	if (!entry.getName().equals(name)) continue;
	            
	            byte[] bytes = org.springframework.util.StreamUtils.copyToByteArray(jis);
	            
	            // Will it not be closed automatically since it is within try/catch?
	            jis.closeEntry();
	            jis.close();

	            /*
	            ByteArrayOutputStream out = new ByteArrayOutputStream();
	            org.springframework.util.StreamUtils.copy(jis, out);
	            byte[] bytes = out.toByteArray();
	            int aaa = bytes.length;
	            */

	            return bytes;
	        }
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	    return null;
	}

	
	protected byte[] loadClassDataFromFile(String name) {

		/*
		File classesDir = new File("/temp/dynacode_classes/");
		URLClassLoader loader2 = new URLClassLoader(new URL[] { classesDir.toURL() }, parentLoader);
		
		String url = "file:C:/data/projects/tutorials/web/WEB-INF/" + "classes/reflection/MyObject.class";
		URL classUrl = new URL(url);
		*/

		
		URL classUrl = null;
		try {
			classUrl = new URL("Some path");
		} catch (

		MalformedURLException e) {
			e.printStackTrace();
			return null;
		}

		try {
			URLConnection connection = classUrl.openConnection();
			InputStream input = connection.getInputStream();
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			int data = input.read();

			while (data != -1) {
				buffer.write(data);
				data = input.read();
			}

			input.close();

			byte[] classData = buffer.toByteArray();
			
			return classData;
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

}
