package edu.ncsa.icr;
import edu.ncsa.icr.ICRAuxiliary.*;
import edu.ncsa.utility.*;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * An Imposed Code Reuse client interface.
 * @author Kenton McHenry
 */
public class ICRClient
{
	private String server;
	private int port;
	private Socket socket;
	private InputStream ins;
	private OutputStream outs;
	private int session = -1;
	private Vector<Application> applications = new Vector<Application>();
	private String data_path = "./";
	private String temp_path = "./";

	/**
	 * Class constructor.
	 */
	public ICRClient()
	{
		this(null);
	}
	
	/**
	 * Class constructor.
	 * @param filename the name of an initialization *.ini file
	 */
	public ICRClient(String filename)
	{		
		if(filename != null) loadINI(filename);
		
		try{
			socket = new Socket(server, port);
			ins = socket.getInputStream();
			outs = socket.getOutputStream();
			
			//Get session and applications
			session = (Integer)Utility.readObject(ins);
			applications = (Vector<Application>)Utility.readObject(ins);
			
			//System.out.println("Connected (session=" + session + ")...\n");
			//Application.print(applications);	
		}catch(Exception e) {e.printStackTrace();}
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
	        	if(key.equals("Server")){
	          	server = InetAddress.getByName(value).getHostAddress();
	        	}else if(key.equals("Port")){
	        		port = Integer.valueOf(value);
	          }else if(key.equals("DataPath")){
	          	data_path = value + "/";
	          }else if(key.equals("TempPath")){
	          	temp_path = value + "/";
	          }
	        }
	      }
	    }
	    
	    ins.close();
	  }catch(Exception e){}
	}

	/**
	 * Get the list of applications available on the ICR server.
	 * @return the available applications/operations
	 */
	public Vector<Application> getApplications()
	{
		return applications;
	}

	/**
	 * Find a suitable application/operation given the desired application, operation, and data.
	 * @param application_alias the application alias (can be null)
	 * @param operation_name the operation name
	 * @param input_file_data input file data (can be null)
	 * @param output_file_data output file data (can be null)
	 * @return the index of the application and operation (null if none found)
	 */
	public Pair<Integer,Integer> getOperation(String application_alias, String operation_name, CachedFileData input_file_data, CachedFileData output_file_data)
	{
		Application application;
		Operation operation;
		Data data;
		boolean FOUND_INPUT, FOUND_OUTPUT;
		
		for(int i=0; i<applications.size(); i++){
			application = applications.get(i);
			
			if(application_alias == null || application.alias.equals(application_alias)){
				for(int j=0; j<application.operations.size(); j++){
					operation = application.operations.get(j);
					
					if(operation.name.equals(operation_name)){
						FOUND_INPUT = input_file_data == null;
						
						if(!FOUND_INPUT){		//Check for a matching input
							for(int k=0; k<operation.inputs.size(); k++){
								data = operation.inputs.get(k);
								
								if(data instanceof FileData){
									if(((FileData)data).getFormat().equals(input_file_data.getFormat())){
										FOUND_INPUT = true;
										break;
									}
								}
							}
						}
						
						FOUND_OUTPUT = output_file_data == null;
						
						if(!FOUND_OUTPUT){		//Check for a matching output
							for(int k=0; k<operation.outputs.size(); k++){
								data = operation.outputs.get(k);
								
								if(data instanceof FileData){
									if(((FileData)data).getFormat().equals(output_file_data.getFormat())){
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
	 * Print information about a given operation.
	 * @param apop a pair containing the index of an application and an operation
	 */
	public void printOperation(Pair<Integer,Integer> apop)
	{
		if(apop != null){
			System.out.println("Application: " + applications.get(apop.first).alias);
			System.out.println("Operation: " + applications.get(apop.first).operations.get(apop.second).name);
		}else{
			System.out.println("No operation found!");
		}
	}

	/**
	 * Send file data to the ICR server.
	 * @param file_data the file data to send
	 * @return a cached version of the given file data (i.e. a pointer to the data on the server)
	 */
	public synchronized CachedFileData sendData(FileData file_data)
	{							
		Utility.writeObject(outs, "send");
		Utility.writeObject(outs, file_data);
		return (CachedFileData)Utility.readObject(ins);
	}
	
	/**
	 * Asynchronously send file data to the server.
	 * @param file_data the file data to send
	 * @return a cached version of the given file data (i.e. a pointer to the data on the server)
	 */
	public AsynchronousObject<CachedFileData> sendDataLater(FileData file_data)
	{
		AsynchronousObject<CachedFileData> async_object = new AsynchronousObject<CachedFileData>();
		final AsynchronousObject<CachedFileData> async_object_final = async_object;
		final FileData file_data_final = file_data;

		new Thread(){
			public void run(){
				async_object_final.set(sendData(file_data_final));
			}
		}.start();
		
		return async_object;
	}
	
	/**
	 * Retrieve cached file data from the ICR server.
	 * @param cached_file_data the cached file data to retrieve
	 * @return the actual file data from the server
	 */
	public synchronized FileData retrieveData(CachedFileData cached_file_data)
	{
		Utility.writeObject(outs, "retrieve");
		Utility.writeObject(outs, cached_file_data);
		return (FileData)Utility.readObject(ins);
	}
	
	/**
	 * Synchronously retrieve cached file data from the server.
	 * @param cached_file_data the cached file data to retrieve
	 * @return the file data
	 */
	public AsynchronousObject<FileData> retrieveDataLater(CachedFileData cached_file_data)
	{
		AsynchronousObject<FileData> async_object = new AsynchronousObject<FileData>();
		final AsynchronousObject<FileData> async_object_final = async_object;
		final CachedFileData cached_file_data_final = cached_file_data;
						
		new Thread(){
			public void run(){
				async_object_final.set(retrieveData(cached_file_data_final));
			}
		}.start();
		
		return async_object;
	}
	
	/**
	 * Execute tasks on the ICR server.
	 * @param tasks a list of tasks to execute (note, all input file data should be cached already!)
	 * @param status, 0=success
	 */
	public synchronized int executeTasks(Vector<Task> tasks)
	{		
		Utility.writeObject(outs, "execute");
		Utility.writeObject(outs, tasks);
		return (Integer)Utility.readObject(ins);
	}
	
	/**
	 * Asynchronously execute tasks on the ICR server.
	 * @param tasks a list of tasks to execute (note, all input file data should be cached already!)
	 * @return status, 0=success
	 */
	public AsynchronousObject<Integer> executeTasksLater(Vector<Task> tasks)
	{
		AsynchronousObject<Integer> async_object = new AsynchronousObject<Integer>();
		final AsynchronousObject<Integer> async_object_final = async_object;
		final Vector<Task> tasks_final = tasks;
		
		new Thread(){
			public void run(){
				async_object_final.set(executeTasks(tasks_final));
			}
		}.start();
		
		return async_object;
	}
	
	/**
	 * Close the connection to the ICR server.
	 */
	public void close()
	{
		Utility.writeObject(outs, "close");
		Utility.readObject(ins);	//Wait for response
	}
	
  /**
	 * A main for debug purposes.
	 * @param args command line arguments
	 */
	public static void main(String args[])
	{
		ICRClient icr = new ICRClient("ICRClient.ini");
		
		//Test sending a file
		if(false){	
			FileData file_data0 = new FileData(icr.data_path + "heart.wrl", true);
			CachedFileData cached_file_data0 = icr.sendData(file_data0);
			System.out.println("Cached data for " + cached_file_data0.getName() + "." + cached_file_data0.getFormat());
		}
		
		//Test retrieving a file
		if(false){
			FileData file_data0 = new FileData(icr.data_path + "heart.wrl", true);
			CachedFileData cached_file_data0 = icr.sendData(file_data0);
			FileData file_data1 = icr.retrieveData(cached_file_data0);
			file_data1.save(icr.temp_path, null);
		}
		
		//Test tasks execution
		if(false){
			FileData file_data0 = new FileData(icr.data_path + "heart.wrl", true);
			CachedFileData cached_file_data0 = icr.sendData(file_data0);
			CachedFileData cached_file_data1 = new CachedFileData(cached_file_data0, "stl");		//stl, stp
			
			Vector<Task> tasks = (new TaskList(icr, cached_file_data0, cached_file_data1)).getTasks();
			icr.executeTasks(tasks);
						
			FileData file_data1 = icr.retrieveData(cached_file_data1);
			file_data1.save(icr.temp_path, null);
		}
		
		//Test asynchronous usage
		if(true){
			FileData file_data0 = new FileData(icr.data_path + "heart.wrl", true);
			AsynchronousObject<CachedFileData> cached_file_data0 = icr.sendDataLater(file_data0);
			CachedFileData cached_file_data1 = new CachedFileData(file_data0, "stl");		//stl, stp
			
			Vector<Task> tasks = (new TaskList(icr, (CachedFileData)cached_file_data0.get(), cached_file_data1)).getTasks();
			AsynchronousObject<Integer> response = icr.executeTasksLater(tasks);
						
			response.waitUntilAvailable();
			AsynchronousObject<FileData> file_data1 = icr.retrieveDataLater(cached_file_data1);
			((FileData)file_data1.get()).save(icr.temp_path, null);
		}

		//Test tasks execution
		if(false){
			TaskList tasks = new TaskList(icr);
			tasks.add("A3DReviewer", "open", icr.data_path + "heart.wrl", "");
			tasks.add("A3DReviewer", "export", "", "heart.stp");
			tasks.print();
			tasks.execute(icr.temp_path);
		}
		
		//Test tasks execution
		if(false){
			TaskList tasks = new TaskList(icr);
			tasks.add("Blender", "convert", icr.data_path + "heart.wrl", "heart.stl");
			tasks.add("A3DReviewer", "open", "heart.stl", "");
			tasks.add("A3DReviewer", "export", "", "heart.stp");
			tasks.print();
			tasks.execute(icr.temp_path);
		}
		
		icr.close();
	}
}