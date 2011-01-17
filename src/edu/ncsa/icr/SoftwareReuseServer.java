package edu.ncsa.icr;
import edu.ncsa.icr.SoftwareReuseAuxiliary.*;
import edu.ncsa.utility.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An Imposed Code Reuse server.
 * @author Kenton McHenry
 */
public class SoftwareReuseServer implements Runnable
{
	private ServerSocket server_socket;
	private Vector<Application> applications = new Vector<Application>();
	private int port;
	private AtomicInteger session_counter = new AtomicInteger();
	private String root_path = "./";
	private String cache_path = root_path + "Cache";
	private String temp_path = root_path + "Temp";
	private int max_operation_time = 10000; 	//In milliseconds
	private int max_operation_attempts = 1;
	private String steward_server = null;
	private int steward_port;
	private String status = "idle";
	
	private boolean ENABLE_MONITORS = false;
	private boolean STARTED_MONITORS = false;
	private boolean RUNNING;
	private boolean BUSY = false;
	
	/**
	 * Class constructor.
	 */
	public SoftwareReuseServer()
	{
		this(null);
	}
	
	/**
	 * Class constructor.
	 * @param filename the file name of an initialization file
	 */
	public SoftwareReuseServer(String filename)
	{		
		if(filename != null) loadINI(filename);
		
		try{
			server_socket = new ServerSocket(port);
		}catch(Exception e) {e.printStackTrace();}
		
		new Thread(this).start();
	}
	
	/**
	 * Initialize based on parameters within the given *.ini file.
	 * @param filename the file name of the *.ini file
	 */
	public void loadINI(String filename)
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
	        	if(key.equals("RootPath")){
	        		//root_path = value + "/";
	        		root_path = Utility.unixPath(Utility.absolutePath(value)) + "/";
	        		
	        		if(!Utility.exists(root_path)){
	        			System.out.println("Root path doesn't exist!");
	        			System.exit(1);
	        		}
	        		
	        		tmpi = 0;
	        		
	        		while(Utility.exists(root_path + "Cache" + Utility.toString(tmpi,3))){
	        			tmpi++;
	        		}
	        		
	        		cache_path = root_path + "Cache" + Utility.toString(tmpi,3) + "/";
	        		new File(cache_path).mkdir();
	        		tmpi = 0;
	        		
	        		while(Utility.exists(root_path + "Temp" + Utility.toString(tmpi,3))){
	        			tmpi++;
	        		}
	        		
	        		temp_path = root_path + "Temp" + Utility.toString(tmpi,3) + "/";
	        		new File(temp_path).mkdir();
	        	}else if(key.equals("AHKScripts")){
	          	addScriptedOperations(value + "/", "ahk", ";");
	        	}else if(key.equals("AppleScripts")){
	          	addScriptedOperations(value + "/", "applescript", "--");
	        	}else if(key.equals("ShellScripts")){
	          	addScriptedOperations(value + "/", "sh", "#");
	        	}else if(key.equals("Port")){
	        		port = Integer.valueOf(value);
	          }else if(key.equals("MaxOperationTime")){
	            max_operation_time = Integer.valueOf(value);
	          }else if(key.equals("MaxOperationAttempts")){
	            max_operation_attempts = Integer.valueOf(value);
	          }else if(key.equals("EnableMonitors")){
	          	ENABLE_MONITORS = Boolean.valueOf(value);
	          }else if(key.equals("PolyglotSteward")){
	        		tmpi = value.lastIndexOf(':');
	        		
	        		if(tmpi != -1){
	        			steward_server = value.substring(0, tmpi);
	        			steward_port = Integer.valueOf(value.substring(tmpi+1));
	        		}
	          }
	        }
	      }
	    }
	    
	    ins.close();
	  }catch(Exception e){}
	  
		//Application.print(applications);
	}

	/**
	 * Add operation scripts within the given directory.
	 * Note: all scripts must follow the text header convention
	 * @param path the path to the scripts
	 * @param extension the script file extension
	 * @param comment_head the preceding sequence of characters indicating a commented line (can be null)
	 */
  public void addScriptedOperations(String path, String extension, String comment_head)
  {    
  	TreeSet<String> alias_list = null; 
  	Script script;
    String name;
    String alias;
    String line;
    String[] tokens;
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
      	return !name.startsWith(".") && !name.startsWith("#") && name.endsWith("." + extension_final);
      }
    };    
    
    File dir = new File(path);
    File[] scripts = dir.listFiles(extension_filter);
    
    if(scripts != null){
      for(int i=0; i<scripts.length; i++){
        name = Utility.getFilenameName(scripts[i].getName());      	
        tokens = name.split("_");
        alias = tokens[0];

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
            applications.add(application);
          }
          
          //Add a new operation to the application      
          application.add(new Operation(script));
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
   * Get the utilized applications.
   * @return a list of applications being used
   */
  public Vector<Application> getApplications()
  {
  	return applications;
  }
  
  /**
   * Execute the given task.
   * @param host the host requesting this task execution
   * @param session the session id
   * @param task a list of subtasks to execute
   */
  public synchronized void executeTasks(String host, int session, Vector<Subtask> task)
  {
  	Subtask subtask;
  	Application application;
  	TreeSet<Integer> application_set = new TreeSet<Integer>();
  	Operation operation;
  	Data input_data, output_data;
  	
  	CachedFileData input_file_data, output_file_data;
  	String source, target;
  	String command = "";
  	boolean COMPLETE;
  	
  	BUSY = true;  
  	
  	//Execute each subtask
  	for(int i=0; i<task.size(); i++){
  		subtask = task.get(i);
  		application = applications.get(subtask.application); application_set.add(subtask.application);
  		operation = application.operations.get(subtask.operation);
  		input_data = subtask.input_data;
  		output_data = subtask.output_data;	
			source = null;
			target = null;
			
			status = "executing, " + application.name + ", " + operation.name + ", " + input_data.toString();
			
			//Set the source, target, and command to execute
			if(input_data != null && input_data instanceof CachedFileData){
  			input_file_data = (CachedFileData)input_data;
  			source = Utility.windowsPath(cache_path) + input_file_data.getCacheFilename(session);
			}
			
			if(output_data != null && output_data instanceof CachedFileData){
  			output_file_data = (CachedFileData)output_data;
  			target = Utility.windowsPath(cache_path) + output_file_data.getCacheFilename(session);
			}
	  	
	  	command = Script.getCommand(operation.script, source, target, Utility.windowsPath(temp_path) + session);
	  	System.out.print("[" + host + "](" + session + "): " + command + " ");
	  	
	  	//Execute the command (note: this script execution has knowledge of other scripts, e.g. monitor and kill)
	  	if(!command.isEmpty()){
	  		for(int j=0; j<max_operation_attempts; j++){
		  		COMPLETE = Script.executeAndWait(command, max_operation_time);
			  	
			    //Try again if command failed
          if(!COMPLETE && application.kill_operation != null){
            if(j < (max_operation_attempts-1)){
              System.out.println("retrying...");
            }else{
              System.out.println("killing...");
            }
            
            Script.executeAndWait(application.kill_operation.script);
          }else{
            break;
          }
	  		}
	  	}
  	}
  	
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
  }
  
  /**
   * Process ICR requests.
   */
  public void run()
  {				
  	Socket client_socket = null;
  	
  	//Display software being used
  	System.out.println("\nAvailable Software:");
  	
  	for(int i=0; i<applications.size(); i++){
  		System.out.println("  " + applications.get(i).name + " (" + applications.get(i).alias + ")");
  	}
  	
  	//Notify a Polyglot steward
  	if(steward_server != null){
  		System.out.println("\nStarting steward notification thread...");
  		
	  	new Thread(){
	  		public void run(){
	  			Socket socket;
	  			
	  			while(true){
		  			try{
		  				socket = new Socket(steward_server, steward_port);
		  				Utility.writeObject(socket.getOutputStream(), port);
		  				Utility.readObject(socket.getInputStream());		//Wait for any response before moving on to prevent re-sending
		  			}catch(Exception e) {}
		  			
		  			Utility.pause(500);
	  			}
	  		}
	  	}.start();
  	}
  	
  	//Begin accepting connections
  	System.out.println("\nSoftware reuse server is running...\n");
		RUNNING = true;
		
		while(RUNNING){
			//Wait for a connection
			try{
				client_socket = server_socket.accept();
			}catch(Exception e) {e.printStackTrace();}
						
			//Spawn a thread to handle this connection
			final Socket client_socket_final = client_socket;
			
			new Thread(){
				public void run(){
					serveConnection(session_counter.incrementAndGet(), client_socket_final);
				}
			}.start();
		}  	
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
					executeTasks(host, session, task);
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
	 * A main for the ICR service.
	 * @param args command line arguments
	 */
	public static void main(String args[])
	{
		SoftwareReuseServer server = new SoftwareReuseServer("SoftwareReuseServer.ini");

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
						
						server.executeTasks("localhost", i, task);	//Use application index as the session
						
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