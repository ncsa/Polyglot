package edu.ncsa.icr;
import edu.ncsa.icr.ICRAuxiliary.*;
import edu.ncsa.utility.*;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * An Imposed Code Reuse server application.
 * @author Kenton McHenry
 */
public class ICRServer implements Runnable
{
	private ServerSocket server_socket;
	private Vector<Application> applications = new Vector<Application>();
	private int port;
	private int session_counter = 0;
	private String cache_path = "./";
	private String temp_path = "./";
	private int max_operation_time = 10000; 	//In milliseconds
	private boolean ENABLE_MONITORS = false;
	private boolean RUNNING;
	
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
	    
	    while((line=ins.readLine()) != null){
	      if(line.contains("=")){
	        key = line.substring(0, line.indexOf('='));
	        value = line.substring(line.indexOf('=')+1);
	        
	        if(key.charAt(0) != '#'){
	        	if(key.equals("CachePath")){
	        		cache_path = value + "/";
	        	}else if(key.equals("TempPath")){
	        		temp_path = value + "/";
	        	}else if(key.equals("AHKPath")){
	          	addOperationsAHK(value + "/");
	        	}else if(key.equals("Port")){
	        		port = Integer.valueOf(value);
	          }else if(key.equals("MaxOperationTime")){
	            max_operation_time = Integer.valueOf(value);
	          }else if(key.equals("EnableMonitors")){
	          	ENABLE_MONITORS = Boolean.valueOf(value);
	          }
	        }
	      }
	    }
	    
	    ins.close();
	  }catch(Exception e){}
	  
		//Application.print(applications);
	}

	/**
	 * Add operations supported by AHK scripts within the given directory.
	 * @param path the path to the AHK scripts
	 */
  public void addOperationsAHK(String path)
  {
    String filename;
    String alias;
    String application_name = "";
    String operation_name;
    Vector<String> domains = new Vector<String>();
    Vector<String> input_formats = new Vector<String>();
    Vector<String> output_formats = new Vector<String>();
    String line;
    Scanner scanner;
    Application application;
    Operation operation;
    String script;
    
    //Examine AutoHotkey scripts
    FilenameFilter ahk_filter = new FilenameFilter(){
      public boolean accept(File dir, String name){
          return !name.startsWith(".") && name.endsWith(".ahk");
      }
    };    
    
    File dir = new File(path);
    File[] scripts = dir.listFiles(ahk_filter);
    
    if(scripts != null){
      for(int i=0; i<scripts.length; i++){
        filename = Utility.getFilenameName(scripts[i].getName());
        
        if(filename.charAt(0) != '#'){		//If not "commented out" script
          domains.clear();
          input_formats.clear();
          output_formats.clear();        	
        	
        	//Examine script name
	        String[] tokens = filename.split("_");
	        alias = tokens[0];
	        operation_name = tokens[1];
          
          if(tokens.length > 2){
            if(operation_name.equals("open") || operation_name.equals("import")){
              input_formats.add(tokens[2]);
            }else if(operation_name.equals("save") || operation_name.equals("export")){
              output_formats.add(tokens[2]);
            }else if(operation_name.equals("convert")){
              input_formats.add(tokens[2]);
              output_formats.add(tokens[3]);
            }
          }
          
          //Examine script header
          try{
            BufferedReader ins = new BufferedReader(new FileReader(path + filename + ".ahk"));
            
            //Get application pretty name
            line = ins.readLine();
            application_name = line.substring(1);  //Remove semicolon
            
            //Remove version if present
            if(application_name.indexOf('(') != -1){
            	application_name = application_name.substring(0, application_name.indexOf('(')).trim();
            }
     
            if(!operation_name.equals("monitor") && !operation_name.equals("exit") && !operation_name.equals("kill")){
            	//Get content types supported by the application
            	line = ins.readLine();
              line = line.substring(1);       //Remove semicolon
              scanner = new Scanner(line);
              scanner.useDelimiter("[\\s,]+");
              
              while(scanner.hasNext()){
              	domains.add(scanner.next());
              }         	
            
              //Extract supported file formats
              if(input_formats.isEmpty() && output_formats.isEmpty()){
                line = ins.readLine();
                line = line.substring(1);       //Remove semicolon
                scanner = new Scanner(line);
                scanner.useDelimiter("[\\s,]+");
                
                if(operation_name.equals("open") || operation_name.equals("import")){
                  while(scanner.hasNext()){
                    input_formats.add(scanner.next());
                  }
                }else if(operation_name.equals("save") || operation_name.equals("export")){
                  while(scanner.hasNext()){
                    output_formats.add(scanner.next());
                  }
                }else if(operation_name.equals("convert")){
                  while(scanner.hasNext()){
                    input_formats.add(scanner.next());
                  }
                  
                  //Convert is a binary operation thus we must read in outputs as well
                  line = ins.readLine();
                  line = line.substring(1);       //Remove semicolon
                  scanner = new Scanner(line);
                  scanner.useDelimiter("[\\s,]+");
                  
                  while(scanner.hasNext()){
                    output_formats.add(scanner.next());
                  }
                }
              }
            }
            
            ins.close();
          }catch(Exception e) {e.printStackTrace();}
                    
          //Retrieve this application if it already exists
          application = null;
          
          for(int j=0; j<applications.size(); j++){
            if(applications.get(j).alias.equals(alias)){
              application = applications.get(j);
              break;
            }
          }
          
          //If the application doesn't exist yet, create it
          if(application == null){
            application = new Application(application_name, alias);
            applications.add(application);
          }
          
          //Add a new operation to the application
          operation = new Operation(application, operation_name);
          
          for(int j=0; j<input_formats.size(); j++){
          	operation.inputs.add(FileData.newFormat(input_formats.get(j)));
          }
          
          for(int j=0; j<output_formats.size(); j++){
          	operation.outputs.add(FileData.newFormat(output_formats.get(j)));
          }
          
          operation.script = path + filename + ".ahk";
          
          application.operations.add(operation);
        }
      }
    }
    
    if(ENABLE_MONITORS){		//Execute all monitoring applications
	    for(int i=0; i<applications.size(); i++){
	    	application = applications.get(i);
	    	
	    	for(int j=0; j<application.operations.size(); j++){
	    		operation = application.operations.get(j);
	    		
	    		if(operation.name.equals("monitor")){
	    			script = operation.script;
	    			
	    			if(script.endsWith(".ahk")){
	    				script = script.substring(0, script.lastIndexOf('.')) + ".exe";
	    			}
	    			
	    			System.out.println("Running monitor for " + application.alias + "...");
	    			
		        try{
		          Runtime.getRuntime().exec(script);
		        }catch(Exception e) {}
	    		}
	    	}
	    }
    }
  }
  
  /**
   * Determine if a given operation is valid with a given piece of data.
   * @param operation the operation
   * @param data the data
   * @return true if the given operation is valid with the given data
   */
  public boolean isValid(Operation operation, Data data)
  {
  	return false;
  }
  
  /**
   * Execute a given operation on the given data.
   * @param session the session id
   * @param application the application
   * @param operation the operation
   * @param input_data the input data
   * @param output_data the output data
   * @return the resulting data
   */
  public synchronized Data execute(int session, Application application, Operation operation, Data input_data, Data output_data)
  {
  	Data result = new Data();
  	CachedFileData cached_file_data;
  	FileData file_data;
  	Process process;
  	TimedProcess timed_process;
  	String script, source, target;
  	String command = "";
  	boolean COMPLETE;
  	
  	//Set the script
		script = operation.script;
		
		if(script.endsWith(".ahk")){
			script = script.substring(0, script.lastIndexOf('.')) + ".exe";
		}   		
		
		//Set the command
  	if(operation.name.equals("convert")){
  		if(input_data instanceof CachedFileData ){
  			cached_file_data = (CachedFileData)input_data;
  			file_data = (FileData)output_data;
  			
  			source = Utility.windowsPath(cached_file_data.getCachePath()) + cached_file_data.getCacheFilename();
  			target = Utility.windowsPath(cached_file_data.getCachePath()) + cached_file_data.getCacheName() + "." + file_data.getFormat();
	  		command = script + " \"" + source + "\" \"" + target + "\" \"" + Utility.windowsPath(temp_path) + session + "\"";
	  		
	  		result = new CachedFileData(cached_file_data, file_data.getFormat());
	  	}
  	}else if(operation.name.equals("open") || operation.name.equals("import")){
  		if(input_data instanceof CachedFileData ){
  			cached_file_data = (CachedFileData)input_data;
  			
  			source = Utility.windowsPath(cached_file_data.getCachePath()) + cached_file_data.getCacheFilename();
	  		command = script + " \"" + source + "\"";
	  	}
  	}
  	
  	System.out.println("Session " + session);
  	System.out.println("\tcommand: " + command);
  	
  	try{
	  	process = Runtime.getRuntime().exec(command);
	  	timed_process = new TimedProcess(process);    
	    COMPLETE = timed_process.waitFor(max_operation_time);
  	}catch(Exception e) {e.printStackTrace();}
  	
    return result;
  }
  
  /**
   * Process ICR requests.
   */
  public void run()
  {				
  	Socket client_socket = null;
  	
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
					serveConnection(session_counter++, client_socket_final);
				}
			}.start();
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
		Data data, input_data, output_data;
		FileData file_data;
		CachedFileData cached_file_data;
		Application application;
		Operation operation;
		int application_index, operation_index;
		
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
						file_data = cached_file_data.uncache();
						Utility.writeObject(outs, file_data);
						System.out.println("Session " + session + ": sent file " + file_data.getName() + "." + file_data.getFormat());
					}
				}else if(message.equals("operation")){
					application_index = (Integer)Utility.readObject(ins);
					operation_index = (Integer)Utility.readObject(ins);
					input_data = null;
					output_data = null;
					
					if((Boolean)Utility.readObject(ins)) input_data = (Data)Utility.readObject(ins);
					if((Boolean)Utility.readObject(ins)) output_data = (Data)Utility.readObject(ins);
					
					application = applications.get(application_index);
					operation = application.operations.get(operation_index);
					data = execute(session, application, operation, input_data, output_data);

					Utility.writeObject(outs, data);
					System.out.println("Session " + session + ": " + application.alias + " " + operation.name + " operation");
				}else if(message.equals("close")){
					Utility.writeObject(outs, "bye");
					System.out.println("Session " + session + ": closing connection!\n");
					break;
				}
				
				Utility.pause(500);
			}
		}catch(Exception e) {e.printStackTrace();}
	}

	/**
	 * A main for the ICR service.
	 * @param args command line arguments
	 */
	public static void main(String args[])
	{
		ICRServer server = new ICRServer("ICRServer.ini");
	}
}