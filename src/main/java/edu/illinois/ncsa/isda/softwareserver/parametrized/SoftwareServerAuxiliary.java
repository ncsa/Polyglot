package edu.illinois.ncsa.isda.softwareserver.parametrized;

import com.google.gson.Gson;

import java.util.*;
import java.io.*;
import kgm.utility.*;

/**
 * Helper classes for the Software Server package.
 * @author Kenton McHenry
 */
public class SoftwareServerAuxiliary
{
	/**
	 * An container for some kind of data.
	 */
	public static class Data implements Serializable, Comparable
	{
		public static final long serialVersionUID = 1L;

  	/**
		 * Get a string representation of this instance.
		 * @return the string representation of this instance
		 */
		public String toString()
		{
			return "null";
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
  		}else{
  			return -1;
  		}
  	}
	}
	
	/**
	 * Data representing a file with a path, a name, and a format (possibly also buffered in memory).
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
		 * @return the string representation of this instance
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
  	 * Cache the file data.
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
	 * A pointer to a file stored in an externally defined cache directory.  This structure
	 * stores only the name and format of a file without a path, thus only indicating the possible
	 * existence of such file in some cache directory.  Think of this as a variable or place holder
	 * for files.
	 */
	public static class CachedFileData extends Data implements Serializable, Comparable
	{		
		public static final long serialVersionUID = 1L;
		private String name;
		private String format;

		public CachedFileData() {}
		
		/**
		 * Class constructor.
		 * @param filename the name of the file
		 */
		public CachedFileData(String filename)
		{
			name = Utility.getFilenameName(filename);
			format = Utility.getFilenameExtension(filename);
		}
		
		/**
		 * Class constructor.
		 * @param filename the name of the file
		 * @param format the new format of this data
		 */
		public CachedFileData(String filename, String format)
		{
			name=filename;
			if(filename!=null)
				name = Utility.getFilenameName(filename);
			this.format = format;
		}
		
		/**
		 * Class constructor.
		 * @param file_data the data to initialize with
		 * @param format the format of this data
		 */
		public CachedFileData(FileData file_data, String format)
		{			
			name = file_data.name;
			this.format = format;
		}

		/**
		 * Class constructor.
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
		 * Get a string representation of this instance.
		 * @return the string representation of this instance
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
		public TreeSet<String> types = new TreeSet<String>(); //domains
		public TreeSet<String> inputs = new TreeSet<String>();
		public TreeSet<String> outputs = new TreeSet<String>();
		public TreeSet<String> executables = new TreeSet<String>();
		public HashMap<String,OperationParameters> inparameters = new HashMap<String,OperationParameters>();
		public HashMap<String,OperationParameters> outparameters =  new HashMap<String,OperationParameters>();
		public Vector<SoftwareParameters> swparameters = new Vector<SoftwareParameters>();
		
		/**
		 * Class constructor.
		 * @param filename the script file name
		 * @param comment_head the preceding sequence of characters indicating a commented line (can be null)
		 */
		public Script(String filename, String comment_head)
		{
			String line;
			int tmpi;
			
			this.filename = Utility.unixPath(filename);
			path = Utility.getFilenamePath(this.filename);
	    name = Utility.getFilenameName(this.filename);
	    type = Utility.getFilenameExtension(this.filename);
	    
	    //Set comment syntax if not set already
	    if(comment_head == null){
	    	if(type.equals("ahk")){
	    		comment_head = ";";
	    	}else if(type.equals("applescript")){
		    	comment_head = "--";
	    	}else if(type.equals("sikuli")){
		    	comment_head = "#";
	    	}else if(type.equals("py")){
		    	comment_head = "#";
	    	}else if(type.equals("sh")){
		    	comment_head = "#";
	    	}else{
	    		System.out.println("Warning: Unknown comment style for script of type: " + type + "!");
	    		comment_head = "#";
	    	}
	    }
	    
	  	//Examine script name
	    String[] tokens = name.split("_");
	    alias = tokens[0];
	    
	    if(tokens.length > 1){
	    	operation = tokens[1].toLowerCase();
	    }else{
	    	operation = "";
	    	System.out.println("Warning: script \"" + filename + "\" violates naming convention (unknown operation)!");
	    }
	  
	    if(tokens.length > 2){
	      if(operation.equals("open") || operation.equals("import")){
	        inputs.add(tokens[2].toLowerCase());
	      }else if(operation.equals("save") || operation.equals("export")){
	        outputs.add(tokens[2].toLowerCase());
	      }else if(operation.equals("convert")&& (tokens.length == 3)){
	      	outputs.add(tokens[2].toLowerCase());
	      }else if(tokens.length > 3){
	        inputs.add(tokens[2].toLowerCase());
	        outputs.add(tokens[3].toLowerCase());
	      }
	    }
	    if(!processNewHeader())
	    	processOldHeader(comment_head);
	    
	    try{
	      BufferedReader ins = new BufferedReader(new FileReader(filename));
	      if(type.equals("ahk")){
	      	while((line=ins.readLine()) != null){
						if(line.trim().startsWith("Run") || line.trim().startsWith("RunWait")){
							//Remove command
							tmpi = line.indexOf(',');
							line = line.substring(tmpi+1).trim();
							
							//Get executable							
							if(line.startsWith("\"")){								
								line = line.substring(1);
								tmpi = line.indexOf("\"");
								executables.add(line.substring(0, tmpi));
							}else{
								executables.add(line);
							}
						}
					}
	      }
	      
	      ins.close();
	    }catch(Exception e) {e.printStackTrace();}

	    
		}

		public boolean processNewHeader(){
			String header=this.path+this.name+".json";
//			System.out.println(header);
			File headerFile = new File(header);
			if(!headerFile.exists())
				return false;
	    try{
//	    	System.out.println(header);
	      BufferedReader ins = new BufferedReader(new FileReader(header));
//	      StringBuilder sb = new StringBuilder();
//	      while((line=ins.readLine())!= null){
//	          sb.append(line);
//	      }
//	      json=sb.toString();
//	      ScriptHeader sh = new Gson().fromJson(json,ScriptHeader.class);
	      ScriptHeader sh = new Gson().fromJson(ins,ScriptHeader.class);
	      ins.close();
	      
	      application=sh.software;
	      if(!operation.equals("exit") && !operation.equals("kill") && !operation.equals("monitor")){
	      	for(String str:sh.domains){
	      		types.add(str);
	      	}
	      	for(FormatParameters fp:sh.inputFormats){
	      		inputs.add(fp.format);
	      		inparameters.put(fp.format, new OperationParameters(operation,fp.paramSelections));
	      	}
	      	for(FormatParameters fp:sh.outputFormats){
	      		outputs.add(fp.format);
	      		outparameters.put(fp.format, new OperationParameters(operation,fp.paramSelections));
	      	}
		      swparameters=sh.softwareParameters;

	      }	      
	    }catch(Exception e) {e.printStackTrace(); return false;}
	    return true;
		}
		
		public void processOldHeader(String comment_head){
			Scanner scanner;  		
			String line;
			
	    //Examine script header
	    try{
		    //Check for scripts within sub-directories
		    if(new File(filename).isDirectory()){
		    	if(type.equals("sikuli")){
		    		filename = this.filename + "/" + name + ".py"; 
			    }else{
		    		System.out.println("Warning: Unknown script type: " + type + "!");
		    	}
		    }
		    
	      BufferedReader ins = new BufferedReader(new FileReader(filename));
	      
	      //Get application pretty name
	      line = ins.readLine();
	      if(line.startsWith("#!")) line = ins.readLine();			//Skip first line if it contains a shell/interpreter (unix scripts)
	      application = line.substring(comment_head.length());  //Remove comment characters
	      
	      //Remove version if present
	      if(application.indexOf('(') != -1){
	      	application = application.substring(0, application.indexOf('(')).trim();
	      }
	
	      if(!operation.equals("exit") && !operation.equals("kill") && !operation.equals("monitor")){
	      	//Get content types supported by the application
	      	line = ins.readLine();
	      	
	      	if(line != null && line.startsWith(comment_head)){
		        line = line.substring(comment_head.length());				//Remove comment characters
		        scanner = new Scanner(line);
		        scanner.useDelimiter("[\\s,]+");
		        
		        while(scanner.hasNext()){
		        	types.add(scanner.next().toLowerCase());
		        }         	
		      
		        //Extract supported file formats
		        if(inputs.isEmpty() && outputs.isEmpty()){
		          line = ins.readLine();
		          
		          if(line != null && line.startsWith(comment_head)){
			          line = line.substring(comment_head.length());     //Remove comment characters
			          
			          if(operation.equals("open") || operation.equals("import")){
			          	inputs = parseFormatList(line);
			          }else if(operation.equals("save") || operation.equals("export")){
			          	outputs = parseFormatList(line);
			          }else{																						//If we have gotten this far this must by a binary operation
			          	inputs = parseFormatList(line);
			            
			            line = ins.readLine();
			            line = line.substring(comment_head.length());		//Remove comment characters
			            outputs = parseFormatList(line);
			          }
		          }
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
	    
	    //Read in options (TODO: utilize this or get rid of it!)
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
	    	format = scanner.next().toLowerCase();
	    	
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
					filename = path + folder_files[i].getName();
					
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
					filename = path + folder_files[i].getName();
	
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
		 * Get the executables called by the script.
		 * @return a set of found executables
		 */
		public TreeSet<String> getExecutables()
		{
			return executables;
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
				//return script.substring(0, script.lastIndexOf('.')) + ".exe";
				return "AutoHotKey " + script;
			}else if(script.endsWith(".applescript")){
				return "osascript " + script;
			}else if(script.endsWith(".sikuli")){
				String os = System.getProperty("os.name");

				if(os.contains("Windows")){
					return "sikuli-ide.bat -s -r " + script + " --args ";
				}else{
					return "sikuli-ide.sh -s -r " + script + " --args ";
				}
			}else if(script.endsWith(".py")){
				return "python " + script;
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
			String os = System.getProperty("os.name");
			String command = getCommand(script);
			String operation = getOperation(script);
			String type = Utility.getFilenameExtension(script);
			boolean WINDOWS_PATHS = false;
			boolean QUOTED_PATHS = false;
			
			if(temp_path != null){		//Ensure this path produces uniquely named files!
				temp_path += System.currentTimeMillis() + "_";	
			}
			
			//if(type.equals("ahk")){		//*.ahk scripts are likely running on Windows
			if(os.contains("Windows")){
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
		public static void executeAndWait(String script)
		{
		  Utility.executeAndWait(Script.getCommand(script), -1);
		}
	
		/**
		 * Execute this script.
		 * @param source the first argument to pass to the script
		 * @param target the second argument to pass to the script
		 * @param temp_path the third argument to pass to the script
		 * @param max_runtime the maximum allowed time to run (in milli-seconds)
		 * @param HANDLE_OUTPUT true if the process output should be handled
		 * @return true if the operation completed within the given time frame
		 */
		public boolean executeAndWait(String source, String target, String temp_path, int max_runtime, boolean HANDLE_OUTPUT)
		{
			String command = getCommand(filename, source, target, temp_path);
			
			return Utility.executeAndWait(command, max_runtime, HANDLE_OUTPUT, false);
		}
		
		/**
		 * Execute this script.
		 * @param source the first argument to pass to the script
		 * @param target the second argument to pass to the script
		 * @param temp_path the third argument to pass to the script
		 * @param max_runtime the maximum allowed time to run (in milli-seconds)
		 * @return true if the operation completed within the given time frame
		 */
		public boolean executeAndWait(String source, String target, String temp_path, int max_runtime)
		{
			String command = getCommand(filename, source, target, temp_path);
			
			return Utility.executeAndWait(command, max_runtime);
		}
	
		/**
		 * Execute this script.
		 */
		public void executeAndWait()
		{
			executeAndWait(null, null, null, 10000);
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
		 */
		public void execute()
		{
		  execute(filename);
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
    public String icon = null;
    public Vector<Operation> operations = new Vector<Operation>();
    public Operation monitor_operation = null;
    public Operation exit_operation = null;
    public Operation kill_operation = null;
    public TreeSet<String> executables = new TreeSet<String>();
  	public SoftwareServerClient icr = null;		//An application belongs to an software reuse server (set by the client)!
  	
  	
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
     * Class constructor.
     * @param script a scripted operation within an application
     */
    public Application(Script script)
    {
    	this.name = script.application;
      this.alias = script.alias;
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
     * Add an operation to this application.
     * @param script a scripted operation for this application
     */
    public void add(Script script)
    {
    	add(new Operation(script));
    	
    	if(!script.operation.equals("monitor") && !script.operation.equals("exit") && !script.operation.equals("kill")){
    		executables.addAll(script.getExecutables());
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
//  	public HashMap<String,Pair<OperationParameters,OperationParameters>> conv_parameters = new HashMap<String,Pair<OperationParameters,OperationParameters>>();
  	public HashMap<String,OperationParameters> in_parameters = new HashMap<String,OperationParameters>();
		public HashMap<String,OperationParameters> out_parameters =  new HashMap<String,OperationParameters>();
		public Vector<SoftwareParameters> swparameters = new Vector<SoftwareParameters>();
  	
  	/**
  	 * Class constructor.
  	 * @param name the name of the operation
  	 */
  	public Operation(String name)
  	{
  		this.name = name;
  	}
  	
  	/**
  	 * Class constructor.
  	 * @param script the script to use for this operation
  	 */
  	public Operation(Script script)
  	{
      name = script.operation;
      
      for(Iterator<String> itr=script.inputs.iterator(); itr.hasNext();){
      	inputs.add(FileData.newFormat(itr.next()));
      }
      
      for(Iterator<String> itr=script.outputs.iterator(); itr.hasNext();){
      	outputs.add(FileData.newFormat(itr.next()));
      }
//      if(!script.inparameters.isEmpty() && !script.outparameters.isEmpty()){
//	      for(String in:script.inparameters.keySet())
//	        for(String out:script.outparameters.keySet())
//	        	this.conv_parameters.put(in+" "+out, new Pair<OperationParameters,OperationParameters>(script.inparameters.get(in),script.outparameters.get(out)));
//      }
      this.in_parameters=script.inparameters;
      this.out_parameters=script.outparameters;
      this.swparameters=script.swparameters;
      this.script = script.filename;
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
   * A structure representing a a sub-task consisting of an application, operation, input, and output.
   */
  public static class Subtask implements Serializable
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
  	public Subtask(int application, int operation, Data input_data, Data output_data)
  	{
  		this.application = application;
  		this.operation = operation;
  		this.input_data = input_data;
  		this.output_data = output_data;
  	}
  }

	/**
	 * A task consisting of a sequence of application and operations on data.
	 */
	public static class Task
	{
		private SoftwareServerClient icr = null;
		private Vector<Application> applications = null;
		private Vector<Subtask> task = new Vector<Subtask>();
		private TreeMap<String,FileData> files = new TreeMap<String,FileData>();
		private TreeMap<String,CachedFileData> cached_files = new TreeMap<String,CachedFileData>();
		
		/**
		 * Class constructor.
		 * @param icr the software reuse client we will create tasks for
		 */
		public Task(SoftwareServerClient icr)
		{
			this.icr = icr;
			applications = icr.getApplications();
		}
		
		/**
		 * Class constructor, create a sequence of tasks that will allow an application to go from the input to the output format.
		 * @param icr the software reuse client we will create tasks for
		 * @param application_string an applications string representation (can be null)
		 * @param input_data the input file
		 * @param output_data the output file
		 */
		public Task(SoftwareServerClient icr, String application_string, Data input_data, Data output_data)
		{
			this(icr);
			addSubtasks(application_string, input_data, output_data);
		}
		
		/**
		 * Class constructor.
		 * @param applications the applications from the software reuse client we will create tasks for
		 */
		public Task(Vector<Application> applications)
		{
			this.applications = applications;
		}
		
		/**
		 * Get the number of tasks in the list.
		 * @return the number of tasks
		 */
		public int size()
		{
			return task.size();
		}
		
		/**
		 * Print information about the given tasks.
		 */
		public void print()
		{
			Subtask subtask;
			
			for(int i=0; i<task.size(); i++){
				subtask = task.get(i);
				
				System.out.println("Application: " + applications.get(subtask.application).alias);
				System.out.println("Operation: " + applications.get(subtask.application).operations.get(subtask.operation).name);
				System.out.println();
			}
		}
	
		/**
		 * Get the associated software reuse client.
		 * @return the associated software reuse client
		 */
		public SoftwareServerClient getSoftwareReuseClient()
		{
			return icr;
		}
		
		/**
		 * Get a task from the list.
		 * @param index the index of the desired task
		 * @return the task at the given index
		 */
		public Subtask get(int index)
		{
			return task.get(index);
		}
	
		/**
		 * Get the vector of subtasks.
		 * @return the vector of subtasks
		 */
		public Vector<Subtask> getSubtasks()
		{
			cache();
			return task;
		}
		
		/**
		 * Get the string associated with the given application alias
		 * @param alias an application alias
		 * @return the application string
		 */
		public String getApplicationString(String alias)
		{
			for(int i=0; i<applications.size(); i++){
				if(applications.get(i).alias.equals(alias)){
					return applications.get(i).toString();
				}
			}
			
			return null;
		}
		
		/**
		 * Find a suitable application/operation given the desired application, operation, and data.
		 * @param application_string the application string representation (can be null)
		 * @param operation_name the operation name
		 * @param input_data input data (can be null)
		 * @param output_data output data (can be null)
		 * @return the index of the application and operation (null if none found)
		 */
		public Pair<Integer,Integer> getApplicationOperation(String application_string, String operation_name, Data input_data, Data output_data)
		{
			Application application;
			Operation operation;
			Data data;
			boolean FOUND_INPUT, FOUND_OUTPUT;
			
			for(int i=0; i<applications.size(); i++){
				application = applications.get(i);
				
				if(application_string == null || application.toString().equals(application_string)){
					for(int j=0; j<application.operations.size(); j++){
						operation = application.operations.get(j);
						
						if(operation.name.equals(operation_name)){
							FOUND_INPUT = input_data == null;
							
							if(!FOUND_INPUT){		//Check for a matching input
								for(int k=0; k<operation.inputs.size(); k++){
									data = operation.inputs.get(k);
									
									if(data instanceof FileData){		//FileData
										if((input_data instanceof FileData && ((FileData)data).getFormat().equals(((FileData)input_data).getFormat())) ||
											 (input_data instanceof CachedFileData && ((FileData)data).getFormat().equals(((CachedFileData)input_data).getFormat()))){
											FOUND_INPUT = true;
											break;
										}
									}
								}
							}
							
							FOUND_OUTPUT = output_data == null;
							
							if(!FOUND_OUTPUT){		//Check for a matching output
								for(int k=0; k<operation.outputs.size(); k++){
									data = operation.outputs.get(k);
									
									if(data instanceof FileData){		//FileData
										if((output_data instanceof FileData && ((FileData)data).getFormat().equals(((FileData)output_data).getFormat())) ||
											 (output_data instanceof CachedFileData && ((FileData)data).getFormat().equals(((CachedFileData)output_data).getFormat()))){
											FOUND_OUTPUT = true;
											break;
										}
									}
								}
							}
													
							if(FOUND_INPUT && FOUND_OUTPUT){
								return new Pair<Integer,Integer>(i,j);
							}
						}
					}
				}
			}
			
			return null;
		}

		/**
		 * Print information about a given application/operation.
		 * @param apop a pair containing the index of an application and an operation
		 */
		public void printApplicationOperation(Pair<Integer,Integer> apop)
		{
			if(apop != null){
				System.out.println("Application: " + applications.get(apop.first).alias);
				System.out.println("Operation: " + applications.get(apop.first).operations.get(apop.second).name);
			}else{
				System.out.println("No operation found!");
			}
		}
	
		/**
		 * Add a subtask to the list.
		 * @param subtask the subtask to add
		 */
		public void add(Subtask subtask)
		{
			task.add(subtask);
		}
		
		/**
		 * Create a new subtask for the given application and operation names.
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
				add(new Subtask(application_index, operation_index, input_data, output_data));
			}
		}
	
		/**
		 * Create a new subtask for the given application, operation, file, and format names.
		 * @param application_alias the application alias
		 * @param operation_name the operation name
		 * @param input_filename the input file name
		 * @param output_filename the output file name
		 */
		public void add(String application_alias, String operation_name, String input_filename, String output_filename)
		{
			Data input_file_data = new Data();
			Data output_file_data = new Data();
					
			if(!input_filename.isEmpty()){
				if(input_filename.contains("/") || input_filename.contains("\\")){	//Local file
					input_file_data = files.get(input_filename);
		
					if(input_file_data == null){
						input_file_data = new FileData(input_filename, true);
						files.put(input_filename, (FileData)input_file_data);
					}
				}else{																															//Cached file
					input_file_data = new CachedFileData(input_filename);
				}
			}
			
			if(!output_filename.isEmpty()){
				output_file_data = new CachedFileData(output_filename);
			}
	
			add(application_alias, operation_name, input_file_data, output_file_data);
		}
		
		/**
		 * Merge this task list with another.
		 * @param task another task (must have same software reuse client!)
		 */
		public void add(Task task)
		{
			if(icr == task.icr){
				for(int i=0; i<task.size(); i++){
					add(task.get(i));
				}
			}
		}

		/**
		 * Add a sequence of subtasks that will allow an application to go from the input to the output format.
		 * @param application_string an applications string representation (can be null)
		 * @param task_string the task to perform (null assumed to be conversion)
		 * @param input_data the input file
		 * @param output_data the output file
		 */
		public void addSubtasks(String application_string, String task_string, Data input_data, Data output_data)
		{
			Pair<Integer,Integer> apop = null, apop0, apop1, apop2;
			
			//Attempt a direct conversion operation
			if(task_string == null || task_string.isEmpty() || task_string.equals("convert")){
				apop = getApplicationOperation(application_string, "convert", input_data, output_data);
			}else{
				apop = getApplicationOperation(application_string, task_string, input_data, output_data);
			}
			
			if(apop != null){
				task.add(new Subtask(apop.first, apop.second, input_data, output_data));
			}else{	//Attempt two part open/import -> save/export
				apop0 = getApplicationOperation(application_string, "open", input_data, null);
				if(apop0 == null) apop0 = getApplicationOperation(application_string, "import", input_data, null);
				
				if(apop0 != null){
					apop1 = getApplicationOperation(applications.get(apop0.first).toString(), "save", null, output_data);
					if(apop1 == null) apop1 = getApplicationOperation(applications.get(apop0.first).toString(), "export", null, output_data);
		
					if(apop1 != null){
						if(task_string == null || task_string.isEmpty() || task_string.equals("convert")){
							task.add(new Subtask(apop0.first, apop0.second, input_data, null));
							task.add(new Subtask(apop1.first, apop1.second, input_data, output_data));
						}else{
							apop2 = getApplicationOperation(applications.get(apop0.first).toString(), task_string, null, null);
							
							if(apop2 != null){
								task.add(new Subtask(apop0.first, apop0.second, input_data, null));
								task.add(new Subtask(apop2.first, apop2.second, input_data, null));
								task.add(new Subtask(apop1.first, apop1.second, input_data, output_data));
							}
						}
					}
				}
			}
		}
		
		/**
		 * Add a sequence of subtasks that will allow an application to go from the input to the output format.
		 * @param application_string an applications string representation (can be null)
		 * @param input_data the input file
		 * @param output_data the output file
		 */
		public void addSubtasks(String application_string, Data input_data, Data output_data)
		{
			addSubtasks(application_string, null, input_data, output_data);
		}
	
		/**
		 * Cache all task input data if not already.
		 */
		public void cache()
		{
			Data data;
			FileData file_data;
			CachedFileData cached_file_data;
			
			for(int i=0; i<task.size(); i++){
				data = task.get(i).input_data;
				
				if(data instanceof FileData){
					file_data = (FileData)data;
					cached_file_data = cached_files.get(file_data.getAbsoluteName());
					
					if(cached_file_data == null){
						cached_file_data = icr.sendData(file_data);
						cached_files.put(file_data.getAbsoluteName(), cached_file_data);
					}
					
					task.get(i).input_data = cached_file_data;
				}
			}
		}
		
		/**
		 * Execute the this task list and return the result of the last task.
		 * @return the data resulting from the last task (will be cached!)
		 */
		public Data execute()
		{
			icr.executeTasks(getSubtasks());
			return task.lastElement().output_data;
		}
		
		/**
		 * Execute the this task list and save the result of the last task to the specified path.
		 * @param output_path the path to save results into
		 * @return the result of the last task
		 */
		public FileData execute(String output_path)
		{
			Data data = execute();
			CachedFileData cached_file_data;
			FileData file_data = null;
			
			if(data instanceof CachedFileData){
				cached_file_data = (CachedFileData)data;
				file_data = icr.retrieveData(cached_file_data);
				
				if(output_path != null){
					if(file_data != null){
						file_data.save(output_path, null);
					}else{
						System.out.println("Output was null!");
					}
				}
			}
			
			return file_data;
		}
		
		@Override
		public String toString()
		{
			StringBuilder sb = new StringBuilder();
			sb.append("SERVER : ");
			sb.append(icr.toString());
			sb.append("\n");
			
			
			for(int i=0; i<task.size(); i++){
				sb.append("  TASK ");
				sb.append(i);
				sb.append(" : ");				
				sb.append(task.get(i).application + " ");
				sb.append(task.get(i).operation + " ");
				sb.append(task.get(i).input_data + " ");
				sb.append(task.get(i).output_data + "\n");
			}
			
			return sb.toString();
		}

		/**
		 * Print the given tasks.
		 * @param task the task to print
		 */
		public static void print(Vector<Subtask> task)
		{
			for(int i=0; i<task.size(); i++){
				System.out.print(task.get(i).application + " ");
				System.out.print(task.get(i).operation + " ");
				System.out.print(task.get(i).input_data + " ");
				System.out.print(task.get(i).output_data + "\n");
			}
		}

		/**
		 * Print the given tasks.
		 * @param task the task to print
		 * @param applications the application data referenced
		 */
		public static void print(Vector<Subtask> task, Vector<Application> applications)
		{
			Application application;
			Operation operation;
			
			for(int i=0; i<task.size(); i++){
				application = applications.get(task.get(i).application);
				operation = application.operations.get(task.get(i).operation);
				
				System.out.print(application.alias + " ");
				System.out.print(operation.name + " ");
				System.out.print(task.get(i).input_data + " ");
				System.out.print(task.get(i).output_data + "\n");
			}
		}

		/**
		 * Get the tasks available for each of the given applications.
		 * @param applications a list of applications
		 * @return the tasks available for each application
		 */
		public static Vector<TreeSet<TaskInfo>> getApplicationTasks(Vector<Application> applications)//liana: added params
		{
			Vector<TreeSet<TaskInfo>> application_tasks = new Vector<TreeSet<TaskInfo>>();
			TaskInfo convert_info = null, task_info;
			Application application;
			Operation operation;
			boolean FOUND_CONVERT, FOUND_OPEN, FOUND_SAVE;
			
			for(int i=0; i<applications.size(); i++){
				application = applications.get(i);
				application_tasks.add(new TreeSet<TaskInfo>());
				FOUND_CONVERT = false;
				FOUND_OPEN = false;
				FOUND_SAVE = false;
				
				//Check for pure convert, open, and save operations
				for(int j=0; j<application.operations.size(); j++){
					operation = application.operations.get(j);
					
					if(operation.name.equals("convert")){
						FOUND_CONVERT = true;
					}else if(operation.name.equals("open") || operation.name.equals("import")){
						FOUND_OPEN = true;
					}else if(operation.name.equals("save") || operation.name.equals("export")){
						FOUND_SAVE = true;
					}
				}
								
				//Record tasks for this application
				if(FOUND_CONVERT || (FOUND_OPEN && FOUND_SAVE)){
					convert_info = new TaskInfo(application, "convert");
					application_tasks.lastElement().add(convert_info);
				}
				
				for(int j=0; j<application.operations.size(); j++){
					operation = application.operations.get(j);
					
					if(operation.name.equals("convert") || operation.name.equals("open") || operation.name.equals("save") || operation.name.equals("import") || operation.name.equals("export")){
						String str;
						if(convert_info != null){																				//Add inputs/outputs to convert task
							for(int k=0; k<operation.inputs.size(); k++){
								str=operation.inputs.get(k).toString();
								convert_info.inputs.add(str);
							}							
							for(int k=0; k<operation.outputs.size(); k++){
								str=operation.outputs.get(k).toString();
								convert_info.outputs.add(str);
							}
//							if(operation.name.equals("convert")){
//								Pair<OperationParameters,OperationParameters> val;
//								String pair;
//								for(int k=0; k<operation.inputs.size(); k++){
//									for(int m=0; m<operation.outputs.size(); m++){
//										pair=operation.inputs.get(k).toString()+" "+operation.outputs.get(m).toString();
//										System.out.println("testing pair ("+pair+")");
//										if(!operation.conv_parameters.isEmpty())
//											for(String p:operation.conv_parameters.keySet())
//												System.out.println("\t("+p+")");
//										if((convert_info.conv_parameters.get(pair)==null) && ((val=operation.conv_parameters.get(pair))!=null)){
//											convert_info.conv_parameters.put(pair, val);
////											System.out.println("Inserted to conv_parameters: "+pair);
//										}
//									}
//								}
//							}
//							else{
//								if(operation.name.equals("open") || operation.name.equals("import")){
//									for(String str2:convert_info.inputs){
//										if(operation.in_parameters.size()>0 && convert_info.in_parameters.get(str2)==null && operation.in_parameters.get(str2)!=null)
//											convert_info.in_parameters.put(str2, operation.in_parameters.get(str2));
//									}									
//								}
//								else{//operation.name.equals("export")|| operation.name.equals("save")
//									for(String str2:convert_info.outputs){
//										if(operation.out_parameters.size()>0 && convert_info.out_parameters.get(str2)==null && operation.out_parameters.get(str2)!=null)
//											convert_info.out_parameters.put(str2, operation.out_parameters.get(str2));
//									}									
//								}
//							}
//							if(!operation.swparameters.isEmpty())
//								convert_info.swparameters.addAll(operation.swparameters);
						}
					}
					else if(!operation.name.equals("exit") && !operation.name.equals("monitor") && !operation.name.equals("kill")){
						if(!operation.inputs.isEmpty() && !operation.outputs.isEmpty()){	//"Modify" script with inputs and outputs (i.e. a conversion)
							task_info = new TaskInfo(application, operation.name);
							application_tasks.lastElement().add(task_info);
							String str;
							//Record inputs/outputs
							for(int k=0; k<operation.inputs.size(); k++){
								str=operation.inputs.get(k).toString();
								task_info.inputs.add(str);
							}							
							for(int k=0; k<operation.outputs.size(); k++){
								str=operation.outputs.get(k).toString();
								task_info.outputs.add(str);
							}
//							String pair;
//							Pair<OperationParameters,OperationParameters> val;
//							for(int k=0; k<operation.inputs.size(); k++){
//								for(int m=0; m<operation.outputs.size(); m++){
//									pair=operation.inputs.get(k).toString()+" "+operation.outputs.get(k).toString();
//									if((task_info.conv_parameters.get(pair)==null) && ((val=operation.conv_parameters.get(pair))!=null)){
//										task_info.conv_parameters.put(pair, val);
//									}
//								}
//							}
//							if(!operation.swparameters.isEmpty())
//								task_info.swparameters.addAll(operation.swparameters);
						}
						else if(FOUND_OPEN && FOUND_SAVE){															//"Modify" script requiring a corresponding open/save operation
							task_info = new TaskInfo(application, operation.name);
							application_tasks.lastElement().add(task_info);							
//							String str;
							//Record inputs/outputs
//							if(!operation.swparameters.isEmpty())
//								task_info.swparameters.addAll(operation.swparameters);																	
							for(int k=0; k<application.operations.size(); k++){
								if(application.operations.get(k).name.equals("open") || application.operations.get(k).name.equals("import")){
									for(int l=0; l<application.operations.get(k).inputs.size(); l++){
										task_info.inputs.add(application.operations.get(k).inputs.get(l).toString());
//										str=application.operations.get(k).inputs.get(l).toString();
//										task_info.inputs.add(str);
//										if(task_info.in_parameters.get(str)==null)
//											task_info.in_parameters.put(str, application.operations.get(k).in_parameters.get(str));
									}
//									if(!application.operations.get(k).swparameters.isEmpty())
//										task_info.swparameters.addAll(application.operations.get(k).swparameters);									
								}
								else if(application.operations.get(k).name.equals("save") || application.operations.get(k).name.equals("export")){
									for(int l=0; l<application.operations.get(k).outputs.size(); l++){
										task_info.outputs.add(application.operations.get(k).outputs.get(l).toString());
//										str=application.operations.get(k).outputs.get(l).toString();
//										task_info.outputs.add(str);
//										task_info.out_parameters.put(str, application.operations.get(k).out_parameters.get(str));
									}
//									if(!application.operations.get(k).swparameters.isEmpty())
//										task_info.swparameters.addAll(application.operations.get(k).swparameters);																	
								}
							}
						}
					}
				}
			}
			
			return application_tasks;
		}
		
		/**
		 * Get the result of the given task (i.e. the last output).
		 * @param cache_path the cache path (assumes data resides in server cache)
		 * @param session the session id
		 * @param task the task
		 * @return the output of the task
		 */
		public static String getResult(String cache_path, int session, Vector<Subtask> task)
		{
			Data output_data;
			String result = null;
			
			for(int i=0; i<task.size(); i++){
				output_data = task.get(i).output_data;
			
				if(output_data != null && output_data instanceof CachedFileData){
	  			result = cache_path + ((CachedFileData)output_data).getCacheFilename(session);
				}
			}
			
			return result;
		}
	}
	
  /**
   * A structure storing a task name, its inputs, and its outputs.
   */
	public static class TaskInfo implements Comparable
	{
		public Application application;
		public String name;
		public TreeSet<String> inputs = new TreeSet<String>();
		public TreeSet<String> outputs = new TreeSet<String>();
//  	public HashMap<String,OperationParameters> in_parameters = new HashMap<String,OperationParameters>();
//  	public HashMap<String,OperationParameters> out_parameters =  new HashMap<String,OperationParameters>();
//		public HashMap<String,Pair<OperationParameters,OperationParameters>> conv_parameters =  new HashMap<String,Pair<OperationParameters,OperationParameters>>();
//		public Vector<SoftwareParameters> swparameters = new Vector<SoftwareParameters>();

		/**
		 * Class constructor.
		 * @param name the name of this task
		 */
		public TaskInfo(String name)
		{
			this.name = name;
		}
		
		/**
		 * Class constructor.
		 * @param application the application this task belongs to
		 * @param name the name of this task
		 */
		public TaskInfo(Application application, String name)
		{
			this.application = application;
			this.name = name;
		}
		
  	/**
		 * Get a string representation of this instance.
		 * @return the string representation of this instance
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
  		}else if(object instanceof TaskInfo){
  			return name.compareTo(((TaskInfo)object).name);
  		}else{
  			return -1;
  		}
  	}
  	
  	/**
  	 * Find the task information for the specified task name.
  	 * @param tasks the set of tasks to search in
  	 * @param task the task name to search for
  	 * @return the task information found
  	 */
  	public static TaskInfo getTask(TreeSet<TaskInfo> tasks, String task)
  	{
  		TaskInfo task_info;
  		
			for(Iterator<TaskInfo> itr=tasks.iterator(); itr.hasNext();){
				task_info = itr.next();
				
				if(task_info.name.equals(task)){
					return task_info;
				}
			}
			
			return null;
  	}
	}
	
	/**
	 * A structure storing information about a task on a remote server.
	 */
	public static class RemoteTaskInfo implements Comparable
	{
		public String application_alias;
		public String application_name;
		public String task;
		public String input;
		public String output;
		public TreeSet<String> servers = new TreeSet<String>();
		
		/**
		 * Class constructor.
		 * @param alias the application alias
		 * @param task the application task
		 * @param input the task input
		 * @param output the task output
		 */
		public RemoteTaskInfo(String alias, String task, String input, String output)
		{
			this.application_alias = alias;
			this.application_name = alias;
			this.task = task;
			this.input = input;
			this.output = output;
		}
		
  	/**
		 * Get a string representation of this instance.
		 * @return the string representation of this instance
		 */
		public String toString()
		{
			return application_alias + "/" + task + "/" + output + "/" + input;
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
  		}else if(object instanceof RemoteTaskInfo){
  			return toString().compareTo(((RemoteTaskInfo)object).toString());
  		}else{
  			return -1;
  		}
  	}
	}
	
	/**
	 * A main for debug purposes.
	 * @param args command line arguments
	 */
	public static void main(String args[])
	{
		if(true){
			Script script = new Script("scripts/ahk/A3DReviewer_open.ahk", ";");
			
			for(String executable : script.getExecutables()){
				System.out.println(Utility.getFilename(Utility.unixPath(executable)));
			}
		}
	}


	
	
		/**
	 * a wrapper for the script header
	 */
	public static class ScriptHeader{
		public String software;
		public String version;
		public Vector<String> domains;
		public Vector<SoftwareParameters> softwareParameters;
		public Vector<FormatParameters> inputFormats;
		public Vector<FormatParameters> outputFormats;
	}
	
	
	
	
	/**
	* a single operation parameter
	*/
	public static class SingleParameter{
		
		public String nameForDisplay=null;
		public String nameInSoftware=null;
		
		public boolean discreteParam=false; 
		public String discreteParamSelected=null;
		public Vector<String> possibleValues=null; //null means any value is accepted
		
		
		public boolean continuousParam=false; //user has to choose or insert a value
		public boolean range=false;
		public String minRange;
		public String maxRange;
		public String value=null;	
		
		public Vector<Vector<SingleParameter>> nestedParams; // possible side-effect steps
		
		
		public String toString() {
			return this.toString(0);
		}	
	
		
		/**
		 * display parameters selected (for debug purposes)
		 * @param level gives the nesting level of the parameters in order to print it nicely
		 */
		public String toString(int level) {
			Vector<SingleParameter> l_ps;
			String res="", line="";
			line+= "name: " + nameForDisplay;
			if (discreteParam)
				line+= " - type: discrete";
			if (continuousParam){// && range)
				line+= " - type: continuous";
			}
			res+=print(line, level);
			line="";
			int j=0;
			if (possibleValues!=null && possibleValues.size()!=0){
				for(;j<possibleValues.size() && possibleValues.get(j)!=null;j++){
					line +="possible value: "+ possibleValues.get(j);
//					if(nestedParams!=null && j<nestedParams.size() &&)
					res+=print(line, level+1);
					line="";
					if (nestedParams!=null && j<nestedParams.size()){
						l_ps = nestedParams.get(j);
						if (l_ps != null){
							if(l_ps.size()!=0)res+=print("parameters:", level+1);
							for(SingleParameter sp:l_ps){
								res+=sp.toString(level+2);
							}
						}
					}			
				}
			}
			else{
				if(range)
					res +=print("possible values: ["+minRange+","+maxRange+"]", level+1);
				else
					res +=print("possible values: ANY", level+1);
				if (nestedParams!=null && j<nestedParams.size()){
					l_ps = nestedParams.get(j);
					if (l_ps != null){
						for(SingleParameter sp:l_ps){
							res+=sp.toString(level+2);
						}
					}
				}	
			}
			return res;
	  }
	
		/**
		 * display parameters selected (for debug purposes)
		 * @param level gives the nesting level of the parameters in order to print it nicely
		 */
		public String toString_old(int level) {
			int j=0;
			SingleParameter ps;
			Vector<SingleParameter> l_ps;
			String res="", line="";
			line+= "display name: " + nameForDisplay;
			if (discreteParam)
				line+= " discrete param selected: "+discreteParamSelected;
			if (continuousParam){// && range)
				line+= " continuous param";
				if(range)
					line+="["+minRange+","+maxRange+"]";
				line+=": "+value;
			}
			res+=print(line, level);
			line="";
			if (possibleValues!=null && possibleValues.size()!=0){
				for(;j<possibleValues.size() && possibleValues.get(j)!=null;j++){
					line +="possible value: "+ possibleValues.get(j);
//					if(nestedParams!=null && j<nestedParams.size() &&)
					res+=print(line, level+1);
					line="";
					if (nestedParams!=null && j<nestedParams.size()){
						l_ps = nestedParams.get(j);
						if (l_ps != null){
							if(l_ps.size()!=0)res+=print("parameters:", level+1);
							for(SingleParameter sp:l_ps){
								res+=sp.toString(level+2);
							}
						}
					}			
				}
			}
			else{
				res +=print("possible values: ANY", level+1);
				if (nestedParams!=null && j<nestedParams.size()){
					l_ps = nestedParams.get(j);
					if (l_ps != null){
						for(SingleParameter sp:l_ps){
							res+=sp.toString(level+2);
						}
					}
				}	
			}
			return res;
	  }
		
		public static String print(String line, int level){
			String tabs="";
			while(level>0){
				tabs+="\t";
				level--;
			}
			return tabs+line+"\n";
		}
	}
	
	/**
	* holds parameter data for a specific operation to be performed
	*/
	public static class OperationParameters{
		public String operation;
		public List<SingleParameter> paramSelections;
		
		
		public OperationParameters(String operation, List<SingleParameter> paramSelections){
			this.operation=operation;
			this.paramSelections=paramSelections;
		}
		
		public OperationParameters(){
			
		}
		
		/**
		 * @return the output format
		 */
		public String getOperation(){
			return operation;
		}
		/**
		 * @return all parameters possible to be specified for the conversion 
		 */
		public List<SingleParameter> getParamSelections(){
			return paramSelections;
		}
		
		public String toString(){
			SingleParameter ps;
			String str="";
//			str+="operation: "+operation+"\n";
//			str+="parameters: \n";
			if(paramSelections==null)
				return "";
//				return str+="\tempty\n";
				
			for(Iterator<SingleParameter> itr=paramSelections.iterator(); itr.hasNext();){
				ps=itr.next();
				str+=ps.toString(1);
			}
			return str;
		}	
	}
	
	/**
	* holds parameter data for a specific format conversion
	*/
	public static class FormatParameters extends OperationParameters{
		private String format;
		
		/**
		 * @return the output format
		 */
		public String getFormat(){
			return format;
		}
		
		public String toString(){
			String str="";
			str+="\nformat: "+format+"\n";
			str+=super.toString();
			return str;
		}	
	}
	
	public static class SoftwareParameters extends OperationParameters{
		
	}
	
	public static class Parameters{
		FormatParameters in;
		FormatParameters out;
		SoftwareParameters sw;
		
		public Parameters(FormatParameters in, FormatParameters out, SoftwareParameters sw){
			this.in=in;
			this.out=out;
			this.sw=sw;
		}
	}

}