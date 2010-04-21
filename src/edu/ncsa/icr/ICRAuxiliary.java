package edu.ncsa.icr;
import edu.ncsa.utility.*;
import java.util.*;
import java.io.*;

/**
 * Helper classes for the ICR package.
 * @author Kenton McHenry
 */
public class ICRAuxiliary
{
	/**
	 * An container for some kind of data.
	 */
	public static class Data implements Serializable, Comparable
	{
		public static final long serialVersionUID = 1L;

		/**
  	 * Compare this object to another object.
  	 * @param object the object to compare to
  	 * @return 0 if the same, -1 if less, and 1 if greater
  	 */
  	public int compareTo(Object object)
  	{
  		if(this == object){
  			return 0;
  		}else{
  			return -1;
  		}
  	}
	}
	
	/**
	 * A buffered file.
	 */
	public static class FileData extends Data implements Serializable, Comparable
	{
		public static final long serialVersionUID = 1L;
		private String absolute_name;
		private String name;
		private String format;
		private byte[] data;
		
		public FileData() {}
		
		/**
		 * Class constructor.
		 * @param absolute_name the absolute name of the file
		 * @param LOAD true if the data should be loaded into memory
		 */
		public FileData(String absolute_name, boolean LOAD)
		{
			this.absolute_name = absolute_name;
			name = Utility.getFilenameName(absolute_name);
			format = Utility.getFilenameExtension(absolute_name);
			if(LOAD) load(null);
		}
		
  	/**
  	 * Create a new FileData instance representing this format.
  	 * @param format the format extension
  	 */
  	public static FileData newFormat(String format)
  	{
  		FileData data = new FileData();
  		data.format = format;
  		
  		return data;
  	}
  	
  	/**
		 * Get a string representation of this instance.
		 */
		public String toString()
		{
			if(name == null){
				return format;
			}else{
				return name + "." + format;
			}
		}

		/**
  	 * Compare this object to another object.
  	 * @param object the object to compare to
  	 * @return 0 if the same, -1 if less, and 1 if greater
  	 */
  	public int compareTo(Object object)
  	{
  		if(this == object){
  			return 0;
  		}else if(object instanceof FileData){
  			return format.compareTo(((FileData)object).format);
  		}else{
  			return -1;
  		}
  	}
  	
  	/**
  	 * Determine if this instance is empty or not.
  	 * @return true if this instance doesn't represent any data
  	 */
  	public boolean isEmpty()
  	{
  		return absolute_name == null || absolute_name.isEmpty();
  	}
  	
  	/**
  	 * Get the files absolute name.
  	 * @return the absolute file name
  	 */
  	public String getAbsoluteName()
  	{
  		return absolute_name;
  	}
  	
		/**
  	 * Get the files name.
  	 * @return the file name
  	 */
  	public String getName()
  	{
  		return name;
  	}
  	
  	/**
  	 * Get the files format.
  	 * @return the file format
  	 */
  	public String getFormat()
  	{
  		return format;
  	}
  	
  	/**
  	 * Get the files data.
  	 * @return the file data
  	 */
  	public byte[] getData()
  	{
  		return data;
  	}
  	
  	/**
		 * Load the file data into this structure.
		 * @param path the path to load from
		 */
		public void load(String path)
		{
			String filename = absolute_name;
			if(path != null) filename = path + name + "." + format;
			if(filename != null) data = Utility.loadToBytes(filename);
		}
		
  	/**
  	 * Clear stored data.
  	 */
  	public void unload()
  	{
  		data = null;
  	}

		/**
  	 * Save the file data to a file.
  	 * @param path the path to save to
  	 * @param filename the name of the file (can be null)
  	 */
  	public void save(String path, String filename)
  	{
      if(filename == null) filename = name + "." + format;
      Utility.save(path + filename, data);
  	}
  	
  	/**
  	 * Cache the file data to a file.
  	 * @param session the session_id responsible for this file
  	 * @param path the path to cache to
  	 * @return a pointer to the cached file
  	 */
  	public CachedFileData cache(int session, String path)
  	{
  		return new CachedFileData(this, session, path);
  	}
	}
	
	/**
	 * A pointer to a file.
	 */
	public static class CachedFileData extends Data implements Serializable, Comparable
	{		
		public static final long serialVersionUID = 1L;
		private String name;
		private String format;

		public CachedFileData() {}
		
		/**
		 * Class constructor.
		 * @param absolute_name the absolute name of the file
		 */
		public CachedFileData(String absolute_name)
		{
			name = Utility.getFilenameName(absolute_name);
			format = Utility.getFilenameExtension(absolute_name);
		}
		
		/**
		 * Class copy constructor.
		 * @param cached_file_data the data to copy
		 * @param format the new format of this data
		 */
		public CachedFileData(CachedFileData cached_file_data, String format)
		{			
			name = cached_file_data.name;
			this.format = format;
		}

		/**
		 * Class copy constructor.
		 * @param file_data the data to copy
		 * @param format the new format of this data
		 */
		public CachedFileData(FileData file_data, String format)
		{			
			name = file_data.name;
			this.format = format;
		}

		/**
		 * Class copy constructor.
		 * @param file_data the file data to cache
		 * @param session the session_id responsible for this file
		 * @param cache_path the path to the cached file
		 */
		public CachedFileData(FileData file_data, int session, String cache_path)
		{			
			name = file_data.name;
			format = file_data.format;
			
  		file_data.save(cache_path, getCacheFilename(session));
		}
		
  	/**
		 * Get a string representation of this instance.
		 */
		public String toString()
		{
			if(name == null){
				return format;
			}else{
				return name + "." + format;
			}
		}

		/**
  	 * Compare this object to another object.
  	 * @param object the object to compare to
  	 * @return 0 if the same, -1 if less, and 1 if greater
  	 */
  	public int compareTo(Object object)
  	{
  		if(this == object){
  			return 0;
  		}else if(object instanceof CachedFileData){
  			return format.compareTo(((CachedFileData)object).format);
  		}else{
  			return -1;
  		}
  	}
  	
  	/**
  	 * Get the files name.
  	 * @return the file name
  	 */
  	public String getName()
  	{
  		return name;
  	}
  	
  	/**
  	 * Get the files format.
  	 * @return the file format
  	 */
  	public String getFormat()
  	{
  		return format;
  	}
  	
		/**
		 * Get the name of the cached file.
		 * @param session the session id
		 * @return the name of the cached file
		 */
		public String getCacheName(int session)
		{
			return session + "_" + name;
		}
		
		/**
		 * Get the file name of the cached file.
		 * @param session the session id
		 * @return the file name of the cached file
		 */
		public String getCacheFilename(int session)
		{
			return getCacheName(session) + "." + format;
		}
		
		/**
		 * Check if the file exists in the cache.
		 * @param session the session id
		 * @param cache_path the path to the cache directory
		 * @return true if the file exists within the cache
		 */
		public boolean exists(int session, String cache_path)
		{
			return Utility.exists(cache_path + getCacheFilename(session));
		}
		
		/**
		 * Retrieve the file data from the cache.
		 * @param session the session id
		 * @param cache_path the path to the cache directory
		 * @return the file data
		 */
		public FileData uncache(int session, String cache_path)
		{
			FileData file_data = null;
			String filename = cache_path + getCacheFilename(session);
			
			if(Utility.exists(filename)){
				file_data = new FileData(filename, true);
				file_data.name = name;	//Remove session id
			}
			
			return file_data;
		}
	}
	
  /**
   * A structure to store information about applications.
   */
  public static class Application implements Serializable, Comparable
  {
		public static final long serialVersionUID = 1L;
    public String name = "";
    public String alias = "";
    public Vector<Operation> operations = new Vector<Operation>();
    public Operation monitor_operation = null;
    public Operation exit_operation = null;
    public Operation kill_operation = null;
  	public ICRClient icr = null;		//An application belongs to an ICR server (set by the client)!

    /**
     * Class constructor.
     * @param name the name of the application
     * @param alias the alias of the application
     */
    public Application(String name, String alias)
    {
    	this.name = name;
      this.alias = alias;
    }
    
    /**
		 * Get a string representation of this instance.
		 */
		public String toString()
		{
			return name;
		}
		
		/**
  	 * Compare this object to another object.
  	 * @param object the object to compare to
  	 * @return 0 if the same, -1 if less, and 1 if greater
  	 */
  	public int compareTo(Object object)
  	{
  		if(this == object){
  			return 0;
  		}else if(object instanceof Application){
  			return ((Application)object).toString().compareTo(toString());
  		}else{
  			return -1;
  		}
  	}

		/**
     * Add an operation to this application.
     * @param operation the operation to add
     */
    public void add(Operation operation)
    {
    	operation.application = this;
    	operations.add(operation);
    	
    	if(operation.name.equals("monitor")){
    		monitor_operation = operation;
    	}else if(operation.name.equals("exit")){
    		exit_operation = operation;
    	}else if(operation.name.equals("kill")){
    		kill_operation = operation;
    	}
    }
    
  	/**
     * Display information on available applications.
     * @param applications the list of applications available and their supported operations
     */
    public static void print(Vector<Application> applications)
    {
    	Application application;
    	
    	for(int i=0; i<applications.size(); i++){
    		application = applications.get(i);
    		System.out.println("Applicaton: " + application.name);
    		System.out.println("Alias: " + application.alias);
    		Operation.print(application.operations);
    		System.out.println();
    	}
    }
  }
  
  /**
   * A structure representing an operation an application supports (think of mathematical functions).
   */
  public static class Operation implements Serializable
  {
		public static final long serialVersionUID = 1L;
  	public Application application;		//An operation belongs to an application!
  	public String name;
  	public Vector<Data> inputs = new Vector<Data>();
  	public Vector<Data> outputs = new Vector<Data>();
  	public String script;
  	
  	/**
  	 * Class constructor.
  	 * @param name the name of the operation
  	 */
  	public Operation(String name)
  	{
  		this.name = name;
  	}
  	
    /**
		 * Get a string representation of this instance.
		 */
		public String toString()
		{
			return application.toString();
		}
  	
  	/**
  	 * Get the executable for the operation.
  	 * @return the name of the executable
  	 */
  	public String getScript()
  	{
			if(script.endsWith(".ahk")){
				return script.substring(0, script.lastIndexOf('.')) + ".exe";
			}
			
			return script;
  	}
  	
  	/**
  	 * Run the operation script with no arguments and wait until it completes.
  	 */
  	public void runScriptAndWait()
  	{
      try{
        Process process = Runtime.getRuntime().exec(getScript());
        process.waitFor();
      }catch(Exception e) {}
  	}
  	
  	/**
  	 * Run the operation script with no arguments.
  	 */
  	public void runScript()
  	{
      try{
        Runtime.getRuntime().exec(getScript());
      }catch(Exception e) {}
  	}
  	
    /**
     * Display operation information.
     * @param operations a list of operations
     */
    public static void print(Vector<Operation> operations)
    {
    	Operation operation;
    	Data data;
    	
  		for(int i=0; i<operations.size(); i++){
  			operation = operations.get(i);
  			System.out.println("Operation: " + operation.name + " (" + operation.script + ")");
  			System.out.print("  inputs:");
  			
  			for(int j=0; j<operation.inputs.size(); j++){
  				data = operation.inputs.get(j);
  				
  				if(data instanceof FileData){
  					System.out.print(" " + ((FileData)data).getFormat());
  				}
  			}
  			
  			System.out.println();
  			System.out.print("  outputs:");
  			
  			for(int j=0; j<operation.outputs.size(); j++){
  				data = operation.outputs.get(j);
  				
  				if(data instanceof FileData){
  					System.out.print(" " + ((FileData)data).getFormat());
  				}
  			}
  			
  			System.out.println();
  		}
    }
  }
  
  /**
   * A structure representing an application task.
   */
  public static class Task implements Serializable
  {
		public static final long serialVersionUID = 1L;
  	public int application;		//Use indices for security purposes, we don't want arbitrary script execution!
  	public int operation;
  	public Data input_data;
  	public Data output_data;
  	
  	/**
  	 * Class constructor.
  	 * @param application the application index
  	 * @param operation the operation index
  	 * @param input_data the input data
  	 * @param output_data the output data
  	 */
  	public Task(int application, int operation, Data input_data, Data output_data)
  	{
  		this.application = application;
  		this.operation = operation;
  		this.input_data = input_data;
  		this.output_data = output_data;
  	}
  }
}