package edu.ncsa.icr;
import edu.ncsa.icr.ICRAuxiliary.*;
import edu.ncsa.utility.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An Imposed Code Reuse server application.
 * @author Kenton McHenry
 */
public class ICRServer implements Runnable
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
	
	private boolean ENABLE_MONITORS = false;
	private boolean STARTED_MONITORS = false;
	private boolean RUNNING;
	private boolean BUSY = false;
	
	/**
	 * Class constructor.
	 */
	public ICRServer()
	{
		this(null);
	}
	
	/**
	 * Class constructor.
	 * @param filename the file name of an initialization file
	 */
	public ICRServer(String filename)
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
	        		root_path = value + "/";
	        		
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
        	script = new Script(scripts[i].getAbsolutePath(), comment_head);
        	
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
   * Execute the given list of tasks.
   * @param session the session id
   * @param tasks a list of tasks to execute
   */
  public synchronized void executeTasks(int session, Vector<Task> tasks)
  {
  	Task task;
  	Application application;
  	TreeSet<Integer> application_set = new TreeSet<Integer>();
  	Operation operation;
  	Data input_data, output_data;
  	
  	CachedFileData input_file_data, output_file_data;
  	String source, target;
  	String command = "";
  	boolean COMPLETE;
  	
  	BUSY = true;  
  	
  	//Execute each task
  	for(int i=0; i<tasks.size(); i++){
  		task = tasks.get(i);
  		application = applications.get(task.application); application_set.add(task.application);
  		operation = application.operations.get(task.operation);
  		input_data = task.input_data;
  		output_data = task.output_data;	
			source = null;
			target = null;
			
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
	  	
	  	System.out.println("Session " + session);
	  	System.out.println("command: " + command);
	  	
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
  		System.out.println("\nStarting steward notification thread...\n");
  		
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
  	System.out.println("\nICR Server is running...\n");
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
		Vector<Task> tasks;
		
		System.out.println("Session " + session + ": connection established...");

		try{
			InputStream ins = client_socket.getInputStream();
			OutputStream outs = client_socket.getOutputStream();
		
			//Send applications
			Utility.writeObject(outs, session);
			Utility.writeObject(outs, applications);
	  	
			//Process requests
			while(client_socket.isConnected()){
				message = (String)Utility.readObject(ins);
				
				if(message.equals("send")){
					data = (Data)Utility.readObject(ins);
					
					if(data instanceof FileData){
						file_data = (FileData)data;
						cached_file_data = file_data.cache(session, cache_path);
						Utility.writeObject(outs, cached_file_data);
						System.out.println("Session " + session + ": received file " + file_data.getName() + "." + file_data.getFormat());
					}
				}else if(message.equals("retrieve")){
					data = (Data)Utility.readObject(ins);
					
					if(data instanceof CachedFileData){
						cached_file_data = (CachedFileData)data;
						file_data = cached_file_data.uncache(session, cache_path);
						Utility.writeObject(outs, file_data);
						
						if(file_data != null){
							System.out.println("Session " + session + ": sent file " + file_data.getName() + "." + file_data.getFormat());
						}else{
							System.out.println("Session " + session + ": requested file doesn't exist!");
						}
					}
				}else if(message.equals("execute")){
					tasks = (Vector<Task>)Utility.readObject(ins);
					executeTasks(session, tasks);
					Utility.writeObject(outs, new Integer(0));
					System.out.println("Session " + session + ": executed " + tasks.size() + " tasks");
				}else if(message.equals("new_session")){
					session = session_counter.incrementAndGet();
					Utility.writeObject(outs, session);
				}else if(message.equals("is_busy")){
					Utility.writeObject(outs, BUSY);
				}else if(message.equals("close")){
					Utility.writeObject(outs, "bye");
					System.out.println("Session " + session + ": closing connection!\n");
					break;
				}
				
				Utility.pause(500);
			}
		}catch(Exception e){
			System.out.println("Session " + session + ": connection lost!\n");
		}
	}

	/**
	 * A main for the ICR service.
	 * @param args command line arguments
	 */
	public static void main(String args[])
	{
		ICRServer server = new ICRServer("ICRServer.ini");

		//Test arguments
		//args = new String[]{"-test", "C:/Kenton/Data/Temp/PolyglotDemo"};

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
				Vector<Task> tasks = new Vector<Task>();
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
					
					//Create tasks and run test (using application index as session)
					if(input_operation != -1 && output_operation != -1){
						extension = application.operations.get(input_operation).inputs.get(input_extension).toString();
						input_file = new CachedFileData(new FileData(test_path + test_files.get(extension), true), i, server.cache_path);
											
						filename = test_files.get(extension);
						name = Utility.getFilenameName(filename);
						extension = application.operations.get(output_operation).outputs.get(output_extension).toString();
						output_file = new CachedFileData(name + "." + extension);
						
						tasks.clear();
						
						if(application.operations.get(input_operation).name.equals("convert")){
							results += "  " + application.toString() + " (convert";
							results += " " + application.operations.get(input_operation).inputs.get(input_extension).toString();
							results += " " + application.operations.get(output_operation).outputs.get(output_extension).toString() + ")";
													
							tasks.add(new Task(i, input_operation, input_file, output_file));
						}else{
							results += "  " + application.toString();
							results += " (" + application.operations.get(input_operation).name;
							results += " " + application.operations.get(input_operation).inputs.get(input_extension).toString();
							results += " " + application.operations.get(output_operation).name;
							results += " " + application.operations.get(output_operation).outputs.get(output_extension).toString() + ")";
							
							tasks.add(new Task(i, input_operation, input_file, new Data()));
							tasks.add(new Task(i, output_operation, new Data(), output_file));
						}
						
						server.executeTasks(i, tasks);	//Use application index as the session
						
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