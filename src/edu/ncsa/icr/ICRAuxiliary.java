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
			String string = "";
			
			if(name != null) string += name + ".";
			string += format;
						
			return string;
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
  			return getFormat().compareTo(((FileData)object).getFormat());
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
     * Display operation information.
     * @param operations a list of operations
     */
    public static void print(Vector<Operation> operations)
    {
    	Operation operation;
    	Data data;
    	
  		for(int i=0; i<operations.size(); i++){
  			operation = operations.get(i);
  			System.out.println("Operation: " + operation.name + " (" + Utility.getFilename(operation.script) + ")");
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
  
  /**
   * A structure representing a wrapper script.
   */
  public static class Script
  {
  	public String filename;
  	public String path;
  	public String name;
  	public String type;
  	public String alias;
  	public String operation;
  	public String application; 
  	public TreeSet<String> types = new TreeSet<String>();
  	public TreeSet<String> inputs = new TreeSet<String>();
  	public TreeSet<String> outputs = new TreeSet<String>();
  	
  	/**
  	 * Class constructor.
  	 * @param filename the script file name
  	 * @param comment_head the preceding sequence of characters indicating a commented line (can be null)
  	 */
  	public Script(String filename, String comment_head)
  	{
  		Scanner scanner;  		
  		String line;
			
  		this.filename = Utility.unixPath(filename);
  		path = Utility.getFilenamePath(filename);
      name = Utility.getFilenameName(filename);
      type = Utility.getFilenameExtension(filename);
    	
      //Set comment syntax if not set already
      if(comment_head == null){
      	if(type.equals("ahk")){
      		comment_head = ";";
      	}else{
      		System.out.println("Warning: Unknown comment style for script of type: " + type + "!");
      		comment_head = "#";
      	}
      }
      
    	//Examine script name
      String[] tokens = name.split("_");
      alias = tokens[0];
      operation = tokens[1];
    
      if(tokens.length > 2){
        if(operation.equals("open") || operation.equals("import")){
          inputs.add(tokens[2]);
        }else if(operation.equals("save") || operation.equals("export")){
          outputs.add(tokens[2]);
        }else if(operation.equals("convert")){
          inputs.add(tokens[2]);
          outputs.add(tokens[3]);
        }
      }
      
      //Examine script header
      try{
        BufferedReader ins = new BufferedReader(new FileReader(filename));
        
        //Get application pretty name
        line = ins.readLine();
        application = line.substring(comment_head.length());  //Remove comment characters
        
        //Remove version if present
        if(application.indexOf('(') != -1){
        	application = application.substring(0, application.indexOf('(')).trim();
        }
 
        if(!operation.equals("monitor") && !operation.equals("exit") && !operation.equals("kill")){
        	//Get content types supported by the application
        	line = ins.readLine();
          line = line.substring(comment_head.length());				//Remove comment characters
          
          scanner = new Scanner(line);
          scanner.useDelimiter("[\\s,]+");
          
          while(scanner.hasNext()){
          	types.add(scanner.next());
          }         	
        
          //Extract supported file formats
          if(inputs.isEmpty() && outputs.isEmpty()){
            line = ins.readLine();
            line = line.substring(comment_head.length());     //Remove comment characters
            
            if(operation.equals("open") || operation.equals("import")){
            	inputs = parseFormatList(line);
            }else if(operation.equals("save") || operation.equals("export")){
            	outputs = parseFormatList(line);
            }else if(operation.equals("convert")){
            	inputs = parseFormatList(line);
              
              //Convert is a binary operation thus we must read in outputs as well
              line = ins.readLine();
              line = line.substring(comment_head.length());		//Remove comment characters
              outputs = parseFormatList(line);
            }
          }
        }
        
        ins.close();
      }catch(Exception e) {e.printStackTrace();}
  	}
  	
  	/**
  	 * Parse a line from a script header containing a format list
  	 * @param line the line containing the format list
  	 * @return the formats parsed from this list
  	 */
  	public TreeSet<String> parseFormatList(String line)
  	{
  		TreeSet<String> formats = new TreeSet<String>();
  		TreeSet<String> options = new TreeSet<String>();
  		String format, format_string, option_string;
  		Scanner scanner;
  		int tmpi = line.indexOf('(');
      
      if(tmpi == -1){
      	format_string = line;
      	option_string = null;
      }else{
      	format_string = line.substring(0, tmpi).trim();
      	option_string = line.substring(tmpi+1, line.length()-1).trim();
      }
      
      //Read in options
      if(option_string != null){
	      scanner = new Scanner(option_string);
	      scanner.useDelimiter("[\\s,]+");
	      
	      while(scanner.hasNext()){
	        options.add(scanner.next());
	      }
      }
      
      //Read in formats (applying options if present)
      scanner = new Scanner(format_string);
      scanner.useDelimiter("[\\s,]+");
      
      while(scanner.hasNext()){
      	format = scanner.next();
      	
      	if(options.isEmpty()){
      		formats.add(format);
      	}else{
      		for(Iterator<String> itr=options.iterator(); itr.hasNext();){
      			formats.add(format + ";" + itr.next());
      		}
      	}
      }
    
  		return formats;
  	}
  	
  	/**
  	 * Return the name of an associated script for the given operation.
  	 * @param operation the desired operation
  	 * @return the name of the script for this operation
  	 */
  	public String getOperationScriptname(String operation)
  	{
  		return path + alias + "_" + operation + "." + type;
  	}
  	
  	/**
  	 * Search this scripts path for input scripts associated with this application.
  	 * @return the list of scripts found
  	 */
  	public Vector<Script> getAssociatedInputScripts()
  	{
  		Vector<Script> scripts = new Vector<Script>();
			File folder = new File(path);
			File[] folder_files = folder.listFiles();
			String[] tokens;
			String filename, name, alias, operation;
			
			for(int i=0; i<folder_files.length; i++){
				if(!folder_files[i].isDirectory() && folder_files[i].getName().charAt(0) != '.' && folder_files[i].getName().endsWith(type)){
					filename = Utility.unixPath(folder_files[i].getAbsolutePath());
					
					if(!filename.equals(this.filename)){
						name = Utility.getFilenameName(filename);
						
			    	//Examine script name
			      tokens = name.split("_");
			      alias = tokens[0];
			      operation = tokens[1];
			      
			      if(alias.equals(this.alias)){
			      	if(operation.equals("open") || operation.equals("import")){
			      		scripts.add(new Script(filename, ";"));
			      	}
			      }
					}
				}
			}
			
			return scripts;
  	}
  	
  	/**
  	 * Search this scripts path for output scripts associated with this application.
  	 * @return the list of scripts found
  	 */
  	public Vector<Script> getAssociatedOutputScripts()
  	{
  		Vector<Script> scripts = new Vector<Script>();
			File folder = new File(path);
			File[] folder_files = folder.listFiles();
			String[] tokens;
			String filename, name, alias, operation;
			
			for(int i=0; i<folder_files.length; i++){
				if(!folder_files[i].isDirectory() && folder_files[i].getName().charAt(0) != '.' && folder_files[i].getName().endsWith(type)){
					filename = Utility.unixPath(folder_files[i].getAbsolutePath());

					if(!filename.equals(this.filename)){
						name = Utility.getFilenameName(filename);
						
			    	//Examine script name
			      tokens = name.split("_");
			      alias = tokens[0];
			      operation = tokens[1];
			      
			      if(alias.equals(this.alias)){
			      	if(operation.equals("save") || operation.equals("export")){
			      		scripts.add(new Script(filename, ";"));
			      	}
			      }
					}
				}
			}
			
			return scripts;
  	}
  	
  	/**
  	 * Get the operation performed by the given script.
  	 * @param script the script filename
  	 * @return the operation performed by the script
  	 */
  	public static String getOperation(String script)
  	{
      String name = Utility.getFilenameName(script);
      String[] tokens = name.split("_");
      String operation = tokens[1];
      
  		return operation;
  	}
  	
  	/**
  	 * Get the execution command for the given script.
  	 * @param script the absolute filename of the script
  	 * @return the execution command
  	 */
  	public static String getCommand(String script)
  	{
			if(script.endsWith(".ahk")){
				return script.substring(0, script.lastIndexOf('.')) + ".exe";
			}else if(script.endsWith(".applescript")){
				return "osascript " + script;
			}
			
			return script;
  	}
  	
  	/**
  	 * Get the execution command for the given script.
  	 * @param script the absolute filename of the script
  	 * @param source the first argument to pass to the script
  	 * @param target the second argument to pass to the script
  	 * @param temp_path the third argument to pass to the script
  	 * @return the execution command
  	 */
  	public static String getCommand(String script, String source, String target, String temp_path)
  	{
  		String command = getCommand(script);
  		String operation = getOperation(script);
  		String type = Utility.getFilenameExtension(script);
  		boolean WINDOWS_PATHS = false;
  		boolean QUOTED_PATHS = false;
  		
  		if(temp_path != null){		//Ensure this path produces uniquely named files!
  			temp_path += System.currentTimeMillis() + "_";	
  		}
  		
  		if(type.equals("ahk")){		//*.ahk scripts are likely running on Windows
  			WINDOWS_PATHS = true;
  			QUOTED_PATHS = true;
  		}
  		
  		if(WINDOWS_PATHS){
  			if(source != null) source = Utility.windowsPath(source);
				if(target != null) target = Utility.windowsPath(target);
				if(temp_path != null) temp_path = Utility.windowsPath(temp_path);
  		}else{
	  		if(source != null) source = Utility.unixPath(source);
				if(target != null) target = Utility.unixPath(target);
				if(temp_path != null) temp_path = Utility.unixPath(temp_path);    			
  		}
  		
  		if(QUOTED_PATHS){
  			if(source != null) source = "\"" + source + "\"";
  			if(target != null) target = "\"" + target + "\"";
  			if(temp_path != null) temp_path = "\"" + temp_path + "\"";
  		}
  		
	  	if(operation.equals("convert")){
	  		command += " " + source + " " + target + " " + temp_path;
	  	}else if(operation.equals("open") || operation.equals("import")){
	  		command += " " + source;
	  	}else if(operation.equals("save") || operation.equals("export")){
	  		command += " " + target;
	  	}
	  	
	  	return command;
  	}
  	
  	/**
  	 * Execute a script.
  	 * @param script the absolute filename of the script
  	 */
  	public static void execute(String script)
		{
		  try{
		    Runtime.getRuntime().exec(Script.getCommand(script));
		  }catch(Exception e) {}
		}
  	
  	/**
  	 * Execute this script.
  	 * @param command the command executing the script
  	 * @param max_operation_time the maximum allowed time to run (in milli-seconds, -1 indicates forever)
  	 * @return true if the operation completed within the given time frame
  	 */
  	public static boolean executeAndWait(String command, int max_operation_time)
  	{
  		Process process;
  		TimedProcess timed_process;
  		boolean COMPLETE = false;

	  	if(!command.isEmpty()){
		  	try{
			  	process = Runtime.getRuntime().exec(command);
			  	
			  	if(max_operation_time >= 0){
					  timed_process = new TimedProcess(process);    
					  COMPLETE = timed_process.waitFor(max_operation_time); System.out.println();
			  	}else{
		        process.waitFor();
		        COMPLETE = true;
			  	}
		  	}catch(Exception e) {e.printStackTrace();}
	  	}
	  	
	  	return COMPLETE;
  	}
  	
  	/**
		 * Execute a script.
		 * @param script the absolute filename of the script
		 */
		public static void executeAndWait(String script)
		{
		  executeAndWait(Script.getCommand(script), -1);
		}

		/**
  	 * Execute this script.
  	 * @param source the first argument to pass to the script
  	 * @param target the second argument to pass to the script
  	 * @param temp_path the third argument to pass to the script
  	 * @param max_operation_time the maximum allowed time to run (in milli-seconds)
  	 * @return true if the operation completed within the given time frame
  	 */
  	public boolean executeAndWait(String source, String target, String temp_path, int max_operation_time)
  	{
  		String command = getCommand(filename, source, target, temp_path);
  		
  		return executeAndWait(command, max_operation_time);
  	}
  	
  	/**
  	 * Execute this script.
  	 */
  	public void executeAndWait()
  	{
  		executeAndWait(null, null, null, 10000);
  	}
  }
}