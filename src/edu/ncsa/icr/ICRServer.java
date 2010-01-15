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
	private String data_path = "";
	private String temp_path = "";
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
          
          //Create a new operation
          
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
          operation = new Operation(operation_name);
 
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
   * Display information on available applications.
   */
  public void printApplications()
  {
  	Application application;
  	Operation operation;
  	Data data;
  	
  	for(int i=0; i<applications.size(); i++){
  		application = applications.get(i);
  		System.out.println("Applicaton: " + application.name);
  		System.out.println("Alias: " + application.alias);
  		
  		for(int j=0; j<application.operations.size(); j++){
  			operation = application.operations.get(j);
  			System.out.println("Operation: " + operation.name + "(" + operation.script + ")");
  			System.out.print("  inputs:");
  			
  			for(int k=0; k<operation.inputs.size(); k++){
  				data = operation.inputs.get(k);
  				
  				if(data instanceof FileData){
  					System.out.print(" " + ((FileData)data).getFormat());
  				}
  			}
  			
  			System.out.println();
  			System.out.print("  outputs:");
  			
  			for(int k=0; k<operation.outputs.size(); k++){
  				data = operation.outputs.get(k);
  				
  				if(data instanceof FileData){
  					System.out.print(" " + ((FileData)data).getFormat());
  				}
  			}
  			
  			System.out.println();
  		}
  		
  		System.out.println();
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
   * @param operation the operation
   * @param data the data
   * @return the resulting data
   */
  public synchronized Data execute(Operation operation, Data data)
  {
  	Process process;
  	TimedProcess timed_process;
  	boolean COMPLETE;
  	
  	//process = Runtime.getRuntime().exec(operation.script + " \"" + source + "\" \"" + target + "\" \"" + tmp_path + "\"");
  	
  	//timed_process = new TimedProcess(process);    
    //COMPLETE = timed_process.waitFor(max_operation_time);
    
    return null;
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
          	if(key.equals("DataPath")){
          		data_path = value + "/";
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
    
		//printApplications();
  }
  
  /**
   * Process ICR requests.
   */
  public void run()
  {				
  	Socket client_socket;
  	
  	System.out.println("\nICR Server is running...");
		RUNNING = true;
		
		while(RUNNING){
			//Wait for a connection
			try{
				client_socket = server_socket.accept();
			}catch(Exception e) {e.printStackTrace();}
			
			//Spawn a thread to handle this connection
		}  	
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