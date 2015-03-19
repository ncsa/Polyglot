package edu.illinois.ncsa.isda.softwareserver;
import edu.illinois.ncsa.isda.softwareserver.SoftwareServerAuxiliary.*;
import kgm.utility.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.apache.commons.io.*;

/**
 * A Software Server.
 * @author Kenton McHenry
 */
public class SoftwareServer implements Runnable
{
	private ServerSocket server_socket;
	private Vector<Application> applications = new Vector<Application>();
	private int port;
	private AtomicInteger session_counter = new AtomicInteger();
	private String root_path = "./";
	private String cache_path = root_path + "Cache";
	private String temp_path = root_path + "Temp";
	private int max_operation_time = 10000; 	//In milliseconds
	private int task_attempts = 1;
	private String steward_server = null;
	private int steward_port;
	private String broadcast_group = null;
	private int broadcast_port;
	private String status = "idle";
	private int task_count = 0;
	private int completed_task_count = 0;
	private int kill_count = 0;
	
	private boolean NEW_TEMP_FOLDERS = true;
	private boolean SHOW_EXECUTABLES = true;
	private boolean HANDLE_OPERATION_OUTPUT = false;
	private boolean SHOW_OPERATION_OUTPUT = false;
	private boolean ENABLE_MONITORS = false;
	private boolean ATTEMPT_AUTO_KILL = false;
	private boolean ATOMIC_EXECUTION = true;
	private boolean WINDOWS = false;
	private boolean STARTED_MONITORS = false;
	private boolean RUNNING;
	private boolean BUSY = false;
	
	/**
	 * Class constructor.
	 */
	public SoftwareServer()
	{
		this(null);
	}
	
	/**
	 * Class constructor.
	 * @param filename the file name of a configuration file
	 */
	public SoftwareServer(String filename)
	{		
		if(filename != null) loadConfiguration(filename);
		WINDOWS = System.getProperty("os.name").contains("Windows");
		
		if(port >= 0){
			try{
				server_socket = new ServerSocket(port);
			}catch(Exception e) {e.printStackTrace();}
			
			new Thread(this).start();
		}
	}
	
	/**
	 * Initialize based on parameters within the given configuration file.
	 * @param filename the file name of the configuration file
	 */
	public void loadConfiguration(String filename)
	{
	  try{
	    BufferedReader ins = new BufferedReader(new FileReader(filename));
	    String line, key, value;
	    int tmpi;
	    
	    while((line=ins.readLine()) != null){
	      if(line.contains("=")){
	        key = line.substring(0, line.indexOf('='));
	        value = line.substring(line.indexOf('=')+1);
	        
	        if(key.charAt(0) != '#'){
	        	if(key.equals("NewTempFolders")){
	        		NEW_TEMP_FOLDERS = Boolean.valueOf(value);
	        	}else if(key.equals("RootPath")){
	        		//root_path = value + "/";
	        		root_path = Utility.unixPath(Utility.absolutePath(value)) + "/";
	        		
	        		if(!Utility.exists(root_path)){
	        			System.out.println("Root path doesn't exist!");
	        			System.exit(1);
	        		}
	        		
	        		if(NEW_TEMP_FOLDERS){
	        			//Find last folder and increment
		        		tmpi = 0;
		        		
		        		while(Utility.exists(root_path + "Cache" + Utility.toString(tmpi,3))){
		        			tmpi++;
		        		}
		        		
		        		cache_path = root_path + "Cache" + Utility.toString(tmpi,3) + "/";
		        		tmpi = 0;
		        		
		        		while(Utility.exists(root_path + "Temp" + Utility.toString(tmpi,3))){
		        			tmpi++;
		        		}
		        		
		        		temp_path = root_path + "Temp" + Utility.toString(tmpi,3) + "/";
	        		}else{
		        		cache_path = root_path + "Cache/";
		        		temp_path = root_path + "Temp/";
	        		}
	        		
	        		//Create new folders
	        		if(!Utility.exists(cache_path)){
	        			new File(cache_path).mkdir();
	        		}else{
	        			session_counter = new AtomicInteger(getLastSession(cache_path));
	        		}
	        		
	        		if(!Utility.exists(temp_path)) new File(temp_path).mkdir();
	        	}else if(key.equals("Scripts")){
	        		addScriptedOperations(value + "/", null);
	        	}else if(key.equals("AHKScripts")){
	          	addScriptedOperations(value + "/", "ahk");
	        	}else if(key.equals("AppleScripts")){
	          	addScriptedOperations(value + "/", "applescript");
	        	}else if(key.equals("SikuliScripts")){
	          	addScriptedOperations(value + "/", "sikuli");
	        	}else if(key.equals("PythonScripts")){
	          	addScriptedOperations(value + "/", "py");
	        	}else if(key.equals("ShellScripts")){
	          	addScriptedOperations(value + "/", "sh");
	        	}else if(key.equals("BatchScripts")){
	          	addScriptedOperations(value + "/", "bat");
	        	}else if(key.equals("RScripts")){
	          	addScriptedOperations(value + "/", "R");
	        	}else if(key.equals("Port")){
	        		port = Integer.valueOf(value);
	          }else if(key.equals("MaxOperationTime")){
	            max_operation_time = Integer.valueOf(value);
	          }else if(key.equals("TaskAttempts")){
	            task_attempts = Integer.valueOf(value);
	          }else if(key.equals("ShowExecutables")){
	            SHOW_EXECUTABLES = Boolean.valueOf(value);
	          }else if(key.equals("HandleOperationOutput")){
	            HANDLE_OPERATION_OUTPUT = Boolean.valueOf(value);
	          }else if(key.equals("ShowOperationOutput")){
	            SHOW_OPERATION_OUTPUT = Boolean.valueOf(value);
	          }else if(key.equals("EnableMonitors")){
	          	ENABLE_MONITORS = Boolean.valueOf(value);
	          }else if(key.equals("AttemptAutoKill")){
	          	ATTEMPT_AUTO_KILL = Boolean.valueOf(value);
	          }else if(key.equals("AtomicExecution")){
	          	ATOMIC_EXECUTION = Boolean.valueOf(value);
	          }else if(key.equals("PolyglotSteward")){
	        		tmpi = value.lastIndexOf(':');
	        		
	        		if(tmpi != -1){
	        			steward_server = value.substring(0, tmpi);
	        			steward_port = Integer.valueOf(value.substring(tmpi+1));
	        		}
	          }else if(key.equals("Broadcast")){
	        		tmpi = value.lastIndexOf(':');
	        		
	        		if(tmpi != -1){
	        			broadcast_group = value.substring(0, tmpi);
	        			broadcast_port = Integer.valueOf(value.substring(tmpi+1));
	        		}
	          }
	        }
	      }
	    }
	    
	    ins.close();
	  }catch(Exception e) {}
	  
		//Application.print(applications);
	  
  	//Display software being used
  	System.out.println("\nAvailable Software:");
  	
  	for(int i=0; i<applications.size(); i++){
  		System.out.println("  " + applications.get(i).name + " (" + applications.get(i).alias + ")");
  		
  		//Show associated executables
  		if(SHOW_EXECUTABLES){
  			for(String executable : applications.get(i).executables){
  				System.out.println("    " + executable);
  			}
  		}
  	}
	}

	/**
	 * Add operation scripts within the given directory.  Script info will be looked up.
	 * Note: all scripts must follow the text header convention
	 * @param path the path to the scripts
	 * @param extension the script file extension (can be null indicating any extension)
	 */
  public void addScriptedOperations(String path, String extension)
  {   
  	if(extension == null || new Script().scripttypes.isRunnable(extension)){
  		System.out.println("Adding *." + extension + " scripts.");
  		addScriptedOperations(path, extension, null);
  	}else{
  		System.out.println("Skipping *." + extension + " scripts.");
  	}
  }
  
	/**
	 * Add operation scripts within the given directory.
	 * Note: all scripts must follow the text header convention
	 * @param path the path to the scripts
	 * @param extension the script file extension (can be null indicating any extension)
	 * @param comment_head the preceding sequence of characters indicating a commented line (can be null)
	 */
  public void addScriptedOperations(String path, String extension, String comment_head)
  {    
  	TreeSet<String> alias_list = null; 
  	Script script;
    String alias;
    String line;
    Scanner scanner;
    Application application;
    final String extension_final = extension;
    
    //Read in alias list file if available
    if(Utility.exists(path + ".aliases.txt")){
    	try{
    		scanner = new Scanner(new File(path + ".aliases.txt"));
    		alias_list = new TreeSet<String>();

    		while(scanner.hasNextLine()){
    			line = scanner.nextLine();
    			
    			if(line.charAt(0) != '#'){
    				alias_list.add(line);
    			}
    		}
    	}catch(Exception e) {e.printStackTrace();}
    }
    
    //Examine scripts with the given extension
    FilenameFilter extension_filter = new FilenameFilter(){
      public boolean accept(File dir, String name){
      	return !name.startsWith(".") && !name.startsWith("#") && (extension_final == null || name.endsWith("." + extension_final));
      }
    };    
    
    File dir = new File(path);
    File[] scripts = dir.listFiles(extension_filter);
    
    if(scripts != null){
      for(int i=0; i<scripts.length; i++){
        alias = Script.getAlias(scripts[i].getName());

        if(alias_list == null || alias_list.contains(alias)){
        	//Load and parse the script's header
        	//script = new Script(scripts[i].getAbsolutePath(), comment_head);
        	script = new Script(path + scripts[i].getName(), comment_head);
        	
          //Retrieve this application if it already exists
          application = null;
          
          for(int j=0; j<applications.size(); j++){
            if(applications.get(j).name.equals(script.application)){
              application = applications.get(j);
              break;
            }
          }
          
          //If the application doesn't exist yet, create it
          if(application == null){
            application = new Application(script.application, alias);
 
            if(Utility.exists(path + alias + ".jpg")){
            	application.icon = path + alias + ".jpg";
            }
            
            applications.add(application);
          }
          
          //Add a new operation to the application      
          //application.add(new Operation(script));
          application.add(script);
      	}
      }
    }
    
    if(ENABLE_MONITORS){		//Execute all monitoring applications
	    for(int i=0; i<applications.size(); i++){
	    	application = applications.get(i);
	    	
	    	if(application.monitor_operation != null){
	    		if(!STARTED_MONITORS) System.out.println();
	    		System.out.println("Starting monitor for " + application.alias + "...");
	    		Script.execute(application.monitor_operation.script);
	    		STARTED_MONITORS = true;
	    	}
	    }
    }
  }
  
  /**
	 * Get the port the server is listening to.
	 * @return the port used
	 */
	public int getPort()
	{
		return port;
	}
	
	/**
	 * Get the servers cache path.
	 * @return the servers cache path
	 */
	public String getCachePath()
	{
		return cache_path;
	}
	
	/**
	 * Get the servers temp path.
	 * @return the servers temp path
	 */
	public String getTempPath()
	{
		return temp_path;
	}
	
	/**
	 * Get a new session number.
	 * @return a session number
	 */
	public int getSession()
	{
		return session_counter.incrementAndGet();
	}
	
	/**
   * Get the utilized applications.
   * @return a list of applications being used
   */
  public Vector<Application> getApplications()
  {
  	return applications;
  }
  
  /**
   * Get the number of tasks received since starting up.
   * @return the number of tasks recevied
   */
  public int getTaskCount()
  {
  	return task_count;
  }
  
  /**
   * Get the number of times the kill operation has been used.
   * @return the number of times the kill operation has been used
   */
  public int getKillCount()
  {
  	return kill_count;
  }
  
  /**
   * Get the number of completed tasks.
   * @return the number of completed tasks
   */
  public int getCompletedTaskCount()
  {
  	return completed_task_count;
  }
  
  /**
   * Execute the given task.
   * @param host the host requesting this task execution
   * @param session the session id
   * @param task a list of subtasks to execute
   * @return the result (i.e. the last output file)
   */
  public String executeTask(String host, int session, Vector<Subtask> task)
  {
  	Subtask subtask;
  	Application application;
  	TreeSet<Integer> application_set = new TreeSet<Integer>();
  	Operation operation;
  	Data input_data, output_data;
  	
  	CachedFileData input_file_data, output_file_data;
  	String source, target, temp;
  	String temp_target, temp_target_path;
  	String command = "";
  	String result = null;
  	boolean COMMAND_COMPLETED;
  	boolean TASK_COMPLETED = false;
  	
  	BUSY = true;  
  	task_count++;
  	
  	//Execute each subtask in the task
		for(int i=0; i<task_attempts; i++){
	  	for(int j=0; j<task.size(); j++){
	  		subtask = task.get(j);
	  		application = applications.get(subtask.application); application_set.add(subtask.application);
	  		operation = application.operations.get(subtask.operation);
	  		input_data = subtask.input_data;
	  		output_data = subtask.output_data;	
				source = null;
				target = null;
				temp_target = null;
				
				status = "executing, " + application.name + ", " + operation.name + ", " + input_data.toString();
				
				//Set the source, target, and command to execute
				if(input_data != null && input_data instanceof CachedFileData){
	  			input_file_data = (CachedFileData)input_data;
	  			source = cache_path + input_file_data.getCacheFilename(session);
	  			if(WINDOWS) source = Utility.windowsPath(source);
				}
				
				if(output_data != null && output_data instanceof CachedFileData){
	  			output_file_data = (CachedFileData)output_data;
	  			target = cache_path + output_file_data.getCacheFilename(session);
	  			result = target;
	  			
	  			//Prevent applications from complaining about overwriting files by creating a temporary output directory (if a file with the same output name exists!)
	  			temp_target = null;
	  			
	  			if(Utility.exists(target)){	
	  			  //Create a new temporary directory (important to not change name as scripts can use the name)
	  				temp_target_path = temp_path + session + "_" + System.currentTimeMillis() + "/";				
	  				if(!Utility.exists(temp_target_path)) new File(temp_target_path).mkdir();
	  				
		  			temp_target = temp_target_path + Utility.getFilename(target);
		  			if(WINDOWS) temp_target = Utility.windowsPath(temp_target);
	  			}
	  			
	  			if(WINDOWS) target = Utility.windowsPath(target);
				}
				
				//Suggest a name for a scratch space for the script to use. May be a folder, may be prepended to output files.
				temp = temp_path + session + "_";
				if(WINDOWS) temp = Utility.windowsPath(temp);
		  
		  	command = Script.getCommand(operation.script, source, temp_target != null ? temp_target : target, temp);
		  	System.out.print("[" + host + "](" + session + "): " + command + " ");
		  	
		  	//Execute the command (note: this script execution has knowledge of other scripts, e.g. monitor and kill)
		  	if(!command.isEmpty()){
		  		COMMAND_COMPLETED = SoftwareServerUtility.executeAndWait(command, max_operation_time, HANDLE_OPERATION_OUTPUT, SHOW_OPERATION_OUTPUT);
			  	
          if(!COMMAND_COMPLETED){
            if(i < (task_attempts-1)){
              System.out.println("retrying...");
            }else{
              System.out.println("killing...");
            }  
            
          	if(application.kill_operation != null){
          		kill_count++;
	            Script.executeAndWait(application.kill_operation.script);
          	}else if(ATTEMPT_AUTO_KILL && !application.executables.isEmpty()){
          		if(WINDOWS){
          			kill_count++;
          			
          			try{
          				SoftwareServerUtility.executeAndWait("taskkill /f /im " + Utility.getFilename(Utility.unixPath(application.executables.first())), -1);
          			}catch(Exception e) {e.printStackTrace();}
          		}
          	}
          	
          	break;	//Try the entire task again!
          }
		  	}
		  	
		  	//Move the output if a temporary target was used
		  	if(temp_target != null && Utility.exists(temp_target)){
					if(!Utility.isDirectory(temp_target)){
						Utility.copyFile(temp_target, target);
					}else{
						try{
							FileUtils.copyDirectory(new File(temp_target), new File(target));
						}catch(Exception e) {e.printStackTrace();}
					}
				}
		  	
		  	//If we got past the last subtask then the task is complete!
		  	if(j == task.size()-1) TASK_COMPLETED = true;
	  	}
	  	
	  	if(TASK_COMPLETED){
	  		completed_task_count++;
	  		break;
	  	}
		}
		
		//Guarantee output by creating an empty file if need be
		if(result != null && !Utility.exists(result)) Utility.touch(result);
  	
  	//Exit all used applications
  	for(Iterator<Integer> itr=application_set.iterator(); itr.hasNext();){
  		application = applications.get(itr.next());
  		
	    if(application.exit_operation != null){
				System.out.println("exiting " + application.alias + "...");
	      Script.executeAndWait(application.exit_operation.script);
	    }
  	}
  	
  	status = "idle";
  	BUSY = false;
  	
  	return result;
  }
  
  /**
	 * Execute the given task (synchronized version).
	 * @param host the host requesting this task execution
	 * @param session the session id
	 * @param task a list of subtasks to execute
	 * @return the result (i.e. the last output file)
	 */
	public synchronized String executeTaskAtomically(String host, int session, Vector<Subtask> task)
	{
		return executeTask(host, session, task);
	}

	/**
	 * Is the server busy executing a task?
	 * @return true if the server is busy
	 */
	public boolean isBusy()
	{
		return BUSY;
	}

	/**
   * Process ICR requests.
   */
  public void run()
  {				
  	Socket client_socket = null;
  	
  	//Notify a Polyglot steward
  	if(steward_server != null){
  		System.out.println("\nStarting steward notification thread...");
  		
	  	new Thread(){
	  		public void run(){
	  			Socket socket = null;
	  			
	  			while(true){
		  			try{
		  				socket = new Socket(steward_server, steward_port);
		  				Utility.writeObject(socket.getOutputStream(), port);
		  				Utility.readObject(socket.getInputStream());		//Wait for any response before moving on to prevent re-sending
		  			}catch(Exception e){
		  			}finally{
		  				try{
		  					socket.close();																//Close socket to prevent OS from running out of resources!
		  				}catch(Exception e) {};
		  			}
		  			
		  			Utility.pause(500);
	  			}
	  		}
	  	}.start();
  	}else if(broadcast_group != null){
   		System.out.println("\nStarting UDP broadcast thread...\n");

	  	new Thread(){
	  		public void run(){
		  		try{
		  			MulticastSocket multicast_socket = new MulticastSocket();
		  			DatagramPacket packet;
		  			byte[] buffer = new byte[10];
		  			
		  			packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(broadcast_group), broadcast_port);
		  			multicast_socket.setTimeToLive(10);
		  			
		  			while(true){
		  				multicast_socket.send(packet);
		  				Utility.pause(2000);
		  			}
		  			
		  			//socket.close();
		  		}catch(Exception e) {e.printStackTrace();}
	  		}
	  	}.start();
  	}
  	
  	//Begin accepting connections
  	System.out.println("\nSoftware server is running...\n");
		RUNNING = true;
		
		while(RUNNING){
			try{			
				//Wait for a connection
				client_socket = server_socket.accept();
				
				//Spawn a thread to handle this connection
				final Socket client_socket_final = client_socket;
				
				new Thread(){
					public void run(){
						serveConnection(session_counter.incrementAndGet(), client_socket_final);
					}
				}.start();
			}catch(SocketException e){
				RUNNING = false;
			}catch(IOException e) {e.printStackTrace();}
		}  	
		
		System.out.println("... Software Server is exiting.");
  }
  
  /**
   * Wait until the servers main thread is running.
   */
  public void waitUntilRunning()
  {
  	while(!RUNNING){
  		Utility.pause(500);
  	}
  }
  
  /**
   * Process requests from the given connection.
   * @param session the session id for this connection
   * @param client_socket the connection to serve
   */
	public void serveConnection(int session, Socket client_socket)
	{			
		String message;
		Data data;
		FileData file_data;
		CachedFileData cached_file_data;
		Vector<Subtask> task;
		String host = client_socket.getInetAddress().getHostName();
		
		System.out.println("[" + host + "](" + session + "): connection established");

		try{
			InputStream ins = client_socket.getInputStream();
			OutputStream outs = client_socket.getOutputStream();
		
			//Send applications
			Utility.writeObject(outs, session);
			Utility.writeObject(outs, applications);
			//System.out.println("[" + host + "](" + session + "): applications registry transfered");
	  	
			//Process requests
			while(client_socket.isConnected()){
				message = (String)Utility.readObject(ins);
				
				if(message.equals("send")){
					data = (Data)Utility.readObject(ins);
					
					if(data instanceof FileData){
						file_data = (FileData)data;
						cached_file_data = file_data.cache(session, cache_path);
						Utility.writeObject(outs, cached_file_data);
						System.out.println("[" + host + "](" + session + "): received file " + file_data.getName() + "." + file_data.getFormat());
					}
				}else if(message.equals("retrieve")){
					data = (Data)Utility.readObject(ins);
					
					if(data instanceof CachedFileData){
						cached_file_data = (CachedFileData)data;
						file_data = cached_file_data.uncache(session, cache_path);
						Utility.writeObject(outs, file_data);
						
						if(file_data != null){
							System.out.println("[" + host + "](" + session + "): sent file " + file_data.getName() + "." + file_data.getFormat());
						}else{
							System.out.println("[" + host + "](" + session + "): requested file doesn't exist!");
						}
					}
				}else if(message.equals("execute")){
					task = (Vector<Subtask>)Utility.readObject(ins);
					System.out.println("[" + host + "](" + session + "): requested task execution ...");
					
					if(ATOMIC_EXECUTION){
						executeTaskAtomically(host, session, task);
					}else{
						executeTask(host, session, task);
					}
					
					Utility.writeObject(outs, new Integer(0));
					System.out.println("[" + host + "](" + session + "): executed " + task.size() + " task(s)");
				}else if(message.equals("new_session")){
					System.out.print("[" + host + "](" + session + "): requesting new session");
					session = session_counter.incrementAndGet();
					System.out.println(" (" + session + ")");
					Utility.writeObject(outs, session);
				}else if(message.equals("status")){
					Utility.writeObject(outs, status);
				}else if(message.equals("is_busy")){
					Utility.writeObject(outs, BUSY);
				}else if(message.equals("ping")){
				}else if(message.equals("close")){
					Utility.writeObject(outs, "bye");
					System.out.println("[" + host + "](" + session + "): closing connection");
					break;
				}
				
				Utility.pause(500);
			}
		}catch(Exception e){
			System.out.println("[" + host + "](" + session + "): connection lost!");
		}
	}
	
	/**
	 * Reset task counters to zero.
	 */
	public void resetCounts()
	{
		task_count = 0;
		kill_count = 0;
		completed_task_count = 0;
	}

	/**
	 * Reboot the machine the software server is running on.
	 */
	public void rebootMachine()
	{
		if(WINDOWS){
			try{
				Runtime.getRuntime().exec("shutdown -r -t 5");
			}catch(Exception e) {e.printStackTrace();}
		}
	}
	
  /**
   * Wait until the servers main thread stops.
   */
  public void waitUntilStopped()
  {
  	while(RUNNING){
  		Utility.pause(500);
  	}
  }
	
	/**
	 * Stop the Software Server.
	 */
	public void stop()
	{
		if(port >= 0){
			try{
				server_socket.close();
			}catch(Exception e) {
				e.printStackTrace();
				
				//No need to wait in case of null pointer exception
				if(e instanceof NullPointerException) return;
			}
			
			waitUntilStopped();
		}
	}

	/**
	 * Parse the session id from the cached filename.
	 * @param filename the cached filename
	 * @return the session id
	 */
	public static int getSession(String filename)
	{
		int tmpi;
		
		filename = Utility.getFilename(filename);
		tmpi = filename.indexOf('_');
		
		if(tmpi >= 0){
			try{
				return Integer.valueOf(filename.substring(0, tmpi));
			}catch(NumberFormatException e){
				return -1;
			}
		}
		
		return -1;
	}
	
	/**
	 * Get the last session, the highest number, based on the files in the given directory.
	 * @param path the directory to examine
	 * @return the last session number
	 */
	public static int getLastSession(String path)
	{
		int last_session = 0;
		int tmpi;
  	File dir = new File(path);
  	
  	for(File file : dir.listFiles()){
  		tmpi = getSession(file.getName());
  		if(tmpi > last_session) last_session = tmpi;
  	}
  	
  	return last_session;
	}

	/**
	 * Parse the filename from the cached filename.
	 * @param filename the cached filename
	 * @return the filename
	 */
	public static String getFilename(String filename)
	{
		int tmpi;
		
		filename = Utility.getFilename(filename);
		tmpi = filename.indexOf('_');
		
		if(tmpi >= 0){
			return filename.substring(tmpi+1);
		}
		
		return null;
	}

	/**
	 * A main for the ICR service.
	 * @param args command line arguments
	 */
	public static void main(String args[])
	{
		SoftwareServer server = new SoftwareServer("SoftwareServer.conf");

		//Test arguments
		//args = new String[]{"-test", "../../Data/Temp/PolyglotDemo"};

		if(args.length > 0){
			if(args[0].equals("-test")){
				String test_path = args[1] + "/";
				TreeMap<String,String> test_files = new TreeMap<String,String>();
				File folder = new File(test_path);
				File[] folder_files = folder.listFiles();
				String filename, name, extension;
				Application application;
				Operation operation;
				int input_operation, input_extension, output_operation, output_extension;
				CachedFileData input_file, output_file;
				Vector<Subtask> task = new Vector<Subtask>();
				String results = "";
				
				server.waitUntilRunning();
	
				//Read in test files
				for(int i=0; i<folder_files.length; i++){
					filename = folder_files[i].getName();
					
					if(filename.charAt(0) != '.'){
						test_files.put(Utility.getFilenameExtension(filename), filename);
					}
				}
				
				System.out.println("Test files:");
				
				for(Iterator<String> itr=test_files.keySet().iterator(); itr.hasNext();){
					extension = itr.next();
					System.out.println("  " + extension + " -> " + test_files.get(extension));
				}
				
				System.out.println();
				
				//Perform a test for each application			
				for(int i=0; i<server.applications.size(); i++){
					application = server.applications.get(i);
					
					//Find an input and output operation
					input_operation = -1;
					input_extension = -1;
					output_operation = -1;
					output_extension = -1;
					
					for(int j=0; j<application.operations.size(); j++){
						operation = application.operations.get(j);
	
						//Check if this operation can input one of the test files
						if(input_operation == -1){
							for(int k=0; k<operation.inputs.size(); k++){
								if(test_files.keySet().contains(operation.inputs.get(k).toString())){
									input_operation = j;
									input_extension = k;
									break;
								}
							}
						}
	
						//Check if this operation can output this file into a different format
						if(output_operation == -1){
							for(int k=0; k<operation.outputs.size(); k++){
								if(input_extension != k){
									output_operation = j;
									output_extension = k;
									break;
								}
							}
						}
						
						if(input_operation != -1 && output_operation != -1) break;
					}
					
					//Create task and run test (using application index as session)
					if(input_operation != -1 && output_operation != -1){
						extension = application.operations.get(input_operation).inputs.get(input_extension).toString();
						input_file = new CachedFileData(new FileData(test_path + test_files.get(extension), true), i, server.cache_path);
											
						filename = test_files.get(extension);
						name = Utility.getFilenameName(filename);
						extension = application.operations.get(output_operation).outputs.get(output_extension).toString();
						output_file = new CachedFileData(name + "." + extension);
						
						task.clear();
						
						if(application.operations.get(input_operation).name.equals("convert")){
							results += "  " + application.toString() + " (convert";
							results += " " + application.operations.get(input_operation).inputs.get(input_extension).toString();
							results += " " + application.operations.get(output_operation).outputs.get(output_extension).toString() + ")";
													
							task.add(new Subtask(i, input_operation, input_file, output_file));
						}else{
							results += "  " + application.toString();
							results += " (" + application.operations.get(input_operation).name;
							results += " " + application.operations.get(input_operation).inputs.get(input_extension).toString();
							results += " " + application.operations.get(output_operation).name;
							results += " " + application.operations.get(output_operation).outputs.get(output_extension).toString() + ")";
							
							task.add(new Subtask(i, input_operation, input_file, new Data()));
							task.add(new Subtask(i, output_operation, new Data(), output_file));
						}
						
						server.executeTaskAtomically("localhost", i, task);	//Use application index as the session
						
						if(output_file.exists(i, server.cache_path)){
							results += " -> [OK]\n";
						}else{
							results += " -> [FAILED]\n";
						}
					}
				}
				
				System.out.println("\nResults:");
				System.out.print(results);
				System.exit(0);
			}
		}
	}
}
