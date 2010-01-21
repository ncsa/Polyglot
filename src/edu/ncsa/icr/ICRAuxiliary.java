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
	public static class Data implements Serializable 
	{
		protected boolean valid = false;
		
		/**
		 * Check whether this piece of data is filled with valid information.
		 * Note: useful for asynchronously filling data.
		 * @return true if this data is valid
		 */
		public boolean isValid()
		{
			return valid;
		}
		
		/**
		 * Wait until this data is valid.
		 */
		public void waitUntilValid()
		{
			while(!valid){
				Utility.pause(100);
			}
		}
	}
	
	/**
	 * A buffered file.
	 */
	public static class FileData extends Data implements Serializable
	{
		protected String absolute_name;
		protected String name;
		protected String format;
		protected byte[] data;
		
		public FileData() {}
		
		/**
		 * Class constructor.
		 * @param absolute_name the absolute name of the file
		 */
		public FileData(String absolute_name, boolean LOAD)
		{
			this.absolute_name = absolute_name;
			name = Utility.getFilenameName(absolute_name);
			format = Utility.getFilenameExtension(absolute_name);
			if(LOAD) load(null);
			valid = true;
		}
		
  	/**
  	 * Create a new FileData instance representing this format.
  	 * @param format the format extension
  	 */
  	public static FileData newFormat(String format)
  	{
  		FileData data = new FileData();
  		data.format = format;
  		data.valid = true;
  		return data;
  	}
  	
		/**
		 * Assign data from another instance.
		 * @param file_data the instance to assign data from
		 */
		public void assign(FileData file_data)
		{
			absolute_name = file_data.absolute_name;
			name = file_data.name;
			format = file_data.format;
			data = file_data.data;
			valid = file_data.valid;
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
  	 * @return a pointer to the saved file
  	 */
  	public void save(String path, String filename)
  	{
  		waitUntilValid();		//In case filled asynchronously!
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
  		return new CachedFileData(session, path, this);
  	}
	}
	
	/**
	 * A pointer to a file.
	 */
	public static class CachedFileData extends FileData implements Serializable
	{
		private int session;
		private String cache_path = "./";
		
		public CachedFileData() {}
		
		/**
		 * Class constructor.
		 * @param session the session_id responsible for this file
		 * @param cache_path the path to the cached file
		 * @param file_data the file data to cache
		 */
		public CachedFileData(int session, String cache_path, FileData file_data)
		{
			this.session = session;
			this.cache_path = cache_path;
			this.absolute_name = file_data.absolute_name;
			this.name = file_data.name;
			this.format = file_data.format;
			this.data = file_data.data;
			valid = true;
			
  		save(cache_path, getCacheFilename());
  		unload();  		
		}
		
		/**
		 * Class copy constructor.
		 * @param cached_file_data the data to copy
		 * @param format the new format of this data
		 */
		public CachedFileData(CachedFileData cached_file_data, String format)
		{
			assign(cached_file_data);
			this.format = format;
		}
		
		/**
		 * Assign data from another instance.
		 * @param cached_file_data the instance to assign data from
		 */
		public void assign(CachedFileData cached_file_data)
		{
			session = cached_file_data.session;
			cache_path = cached_file_data.cache_path;
			absolute_name = cached_file_data.absolute_name;
			name = cached_file_data.name;
			format = cached_file_data.format;
			data = cached_file_data.data;
			valid = cached_file_data.valid;
		}
		
		/**
		 * Get the session id for this cached file.
		 * @return the session id
		 */
		public int getSession()
		{
			return session;
		}
		
		/**
		 * Get the path to the cache.
		 * @return the cache path
		 */
		public String getCachePath()
		{
			return cache_path;
		}
		
		/**
		 * Get the name of the cached file.
		 * @return the name of the cached file
		 */
		public String getCacheName()
		{
			return session + "_" + name;
		}
		
		/**
		 * Get the file name of the cached file.
		 * @return the file name of the cached file
		 */
		public String getCacheFilename()
		{
			return getCacheName() + "." + format;
		}
		
		/**
		 * Return a string version of this structure.
		 * @return a string version of this structures contents
		 */
		public String toString()
		{
			return "Session: " + session + "\nPath: " + cache_path + "\nFilename: " + getCacheFilename();
		}
		
		/**
		 * Retrieve the file data from the cache.
		 * @return the file data
		 */
		public FileData uncache()
		{
			FileData file_data = new FileData(cache_path + getCacheFilename(), true);
			file_data.name = name;	//Remove session id
			
			return file_data;
		}
	}
	
  /**
   * A structure to store information about applications.
   */
  public static class Application implements Serializable
  {
    public String name = "";
    public String alias = "";
    public Vector<Operation> operations = new Vector<Operation>();
    public Operation monitor_operation = null;
    public Operation exit_operation = null;
    public Operation kill_operation = null;
    
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
  	public Application application;
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
  
  /**
   * A convenient class to create a sequence of tasks.
   */
  public static class TaskList
  {
  	private ICRClient icr = null;
  	private Vector<Application> applications = null;
  	private Vector<Task> tasks = new Vector<Task>();
  	private TreeMap<String,FileData> files = new TreeMap<String,FileData>();
  	private TreeMap<String,CachedFileData> cached_files = new TreeMap<String,CachedFileData>();
  	
  	/**
  	 * Class constructor.
  	 * @param icr the ICR client we will create tasks for
  	 */
  	public TaskList(ICRClient icr)
  	{
  		this.icr = icr;
  		applications = icr.getApplications();
  	}
  	
  	/**
  	 * Class constructor, create a sequence of tasks that will allow an application to go from the input file to the output format.
  	 * @param input_file_data the input file
  	 * @param output_file_data the output format
  	 */
  	public TaskList(ICRClient icr, FileData input_file_data, FileData output_format)
  	{
  		this(icr);
  		Pair<Integer,Integer> apop, apop0, apop1;
  		
  		input_file_data.waitUntilValid();	//In case filled asynchronously!
  		
  		//Attempt a direct conversion operation
  		apop = icr.getOperation(null, "convert", input_file_data, output_format);
  		
  		if(apop != null){
  			tasks.add(new Task(apop.first, apop.second, input_file_data, output_format));
  		}else{	//Attempt two part open/import -> save/export
  			apop0 = icr.getOperation(null, "open", input_file_data, null);
  			if(apop0 == null) apop0 = icr.getOperation(null, "import", input_file_data, null);
  			
  			if(apop0 != null){
  				apop1 = icr.getOperation(applications.get(apop0.first).alias, "save", null, output_format);
  				if(apop1 == null) apop1 = icr.getOperation(applications.get(apop0.first).alias, "export", null, output_format);

  				if(apop1 != null){
  					tasks.add(new Task(apop0.first, apop0.second, input_file_data, null));
  					tasks.add(new Task(apop1.first, apop1.second, input_file_data, output_format));
  				}
  			}
  		}
  	}
  	  	
  	/**
  	 * Get the number of tasks in the list.
  	 * @return the number of tasks
  	 */
  	public int size()
  	{
  		return tasks.size();
  	}
  	
  	/**
		 * Print information about the given tasks.
		 */
		public void print()
		{
			Task task;
			
			for(int i=0; i<tasks.size(); i++){
				task = tasks.get(i);
				
				System.out.println("Application: " + applications.get(task.application).alias);
				System.out.println("Operation: " + applications.get(task.application).operations.get(task.operation).name);
				System.out.println();
			}
		}

		/**
		 * Get a task from the list.
		 * @param index the index of the desired task
		 * @return the task at the given index
		 */
		public Task get(int index)
		{
			return tasks.get(index);
		}

		/**
		 * Get the vector of tasks.
		 * @return the vector of tasks
		 */
		public Vector<Task> getTasks()
		{
			cache();
			return tasks;
		}

		/**
  	 * Add a task to the list.
  	 * @param task the task to add
  	 */
  	public void add(Task task)
  	{
  		tasks.add(task);
  	}
  	
  	/**
		 * Create a new task for the given application and operation names.
		 * @param application_alias the application alias
		 * @param operation_name the operation name
		 * @param input_data the input data for the operation
		 * @param output_data the output data for the operation
		 */
		public void add(String application_alias, String operation_name, Data input_data, Data output_data)
		{
			Application application = null;
			int application_index = -1;
			int operation_index = -1;
			
			//Find the application
			for(int i=0; i<applications.size(); i++){
				if(applications.get(i).alias.equals(application_alias)){
					application_index = i;
					application = applications.get(i);
					break;
				}
			}
			
			//Find the operation
			for(int i=0; i<application.operations.size(); i++){
				if(application.operations.get(i).name.equals(operation_name)){
					operation_index = i;
					break;
				}
			}
			
			if(application_index != -1 && operation_index != -1){
				add(new Task(application_index, operation_index, input_data, output_data));
			}
		}

		/**
		 * Create a new task for the given application, operation, file, and format names.
		 * @param application_alias the application alias
		 * @param operation_name the operation name
		 * @param input_filename the input file name
		 * @param output_format the output format name
		 */
		public void add(String application_alias, String operation_name, String input_filename, String output_format)
		{
			FileData input_file_data = files.get(input_filename);
			
			if(input_file_data == null){
				input_file_data = new FileData(input_filename, true);
				files.put(input_filename, input_file_data);
			}
			
			add(application_alias, operation_name, input_file_data, FileData.newFormat(output_format));
		}

		/**
		 * Cache all task input data if not already.
		 */
		public void cache()
		{
			Data data;
			FileData file_data;
			CachedFileData cached_file_data;
			
			for(int i=0; i<tasks.size(); i++){
				data = tasks.get(i).input_data;
				data.waitUntilValid();
				
				if(data instanceof FileData && !(data instanceof CachedFileData)){
					file_data = (FileData)data;
					cached_file_data = cached_files.get(file_data.absolute_name);
					
					if(cached_file_data == null){
						cached_file_data = icr.sendData(file_data);
						cached_files.put(file_data.absolute_name, cached_file_data);
					}
					
					tasks.get(i).input_data = cached_file_data;
				}
			}
		}
		
		/**
		 * Execute the this task list and save the result to the specified path.
		 * @param output_path the path to save results into
		 */
		public void execute(String output_path)
		{
			FileData file_data = icr.retrieveData((CachedFileData)icr.executeTasks(getTasks()));
			file_data.save(output_path, null);
		}
  }
}