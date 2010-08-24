package edu.ncsa.icr;
import edu.ncsa.icr.ICRAuxiliary.*;
import edu.ncsa.icr.polyglot.PolyglotClient;
import edu.ncsa.utility.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.*;

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
	private AtomicInteger pending_asynchronous_calls = new AtomicInteger();
	
	/**
	 * Class constructor.
	 * @param server the name of the ICR server
	 * @param port the port used for the connection
	 */
	public ICRClient(String server, int port)
	{				
		this.server = server;
		this.port = port;
		
		try{
			socket = new Socket(server, port);
			ins = socket.getInputStream();
			outs = socket.getOutputStream();
			
			//Get session and applications
			session = (Integer)Utility.readObject(ins);
			applications = (Vector<Application>)Utility.readObject(ins);
			
			//Set client entry in applications
			for(int i=0; i<applications.size(); i++){
				applications.get(i).icr = this;
			}
			
			//System.out.println("Connected (session=" + session + ")...\n");
			//Application.print(applications);	
		}catch(Exception e){
			System.out.println("Unable to connect to ICR server: " + server);
		}
	}
	
	/**
	 * Class copy constructor.
	 * @param icr an ICR client session to copy
	 */
	public ICRClient(ICRClient icr)
	{
		this(icr.server, icr.port);
	}

	/**
	 * Return a string representation of this object.
	 * @return a string representation of this object
	 */
	public String toString()
	{
		return server + ":" + port;
	}

	/**
	 * Get the server this client is connected to.
	 * @return the ICR server
	 */
	public String getServer()
	{
		return server;
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
	 * @param application_string the application string representation (can be null)
	 * @param operation_name the operation name
	 * @param input_data input data (can be null)
	 * @param output_data output data (can be null)
	 * @return the index of the application and operation (null if none found)
	 */
	public Pair<Integer,Integer> getOperation(String application_string, String operation_name, Data input_data, Data output_data)
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
		CachedFileData cached_file_data = null;
		
		try{
			Utility.writeObject(outs, "send");
			Utility.writeObject(outs, file_data);
			cached_file_data = (CachedFileData)Utility.readObject(ins);
		}catch(Exception e) {e.printStackTrace();}
		
		return cached_file_data;
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

		pending_asynchronous_calls.incrementAndGet();
		
		new Thread(){
			public void run(){
				async_object_final.set(sendData(file_data_final));
				pending_asynchronous_calls.decrementAndGet();
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
		FileData file_data = null;
		
		try{
			Utility.writeObject(outs, "retrieve");
			Utility.writeObject(outs, cached_file_data);
			file_data = (FileData)Utility.readObject(ins);
		}catch(Exception e) {e.printStackTrace();}
		
		return file_data;
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
						
		pending_asynchronous_calls.incrementAndGet();

		new Thread(){
			public void run(){
				async_object_final.set(retrieveData(cached_file_data_final));
				pending_asynchronous_calls.decrementAndGet();
			}
		}.start();
		
		return async_object;
	}
	
	/**
	 * Execute tasks on the ICR server.
	 * @param tasks a list of tasks to execute (note, all input file data should be cached already!)
	 * @return server response (0=success)
	 */
	public synchronized int executeTasks(Vector<Task> tasks)
	{		
		Integer response = null;
		
		try{
			Utility.writeObject(outs, "execute");
			Utility.writeObject(outs, tasks);
			response = (Integer)Utility.readObject(ins);
		}catch(Exception e) {e.printStackTrace();}
		
		return response;
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
		
		pending_asynchronous_calls.incrementAndGet();

		new Thread(){
			public void run(){
				async_object_final.set(executeTasks(tasks_final));
				pending_asynchronous_calls.decrementAndGet();
			}
		}.start();
		
		return async_object;
	}
	
	/**
	 * Request a new session.
	 */
	public synchronized void requestNewSession()
	{
		try{
			Utility.writeObject(outs, "new_session");
			session = (Integer)Utility.readObject(ins);
		}catch(Exception e) {e.printStackTrace();}
	}
	
	/**
	 * Check if the ICR Server is busy executing another task.
	 * @return true if the ICR Server is currently busy
	 */
	public synchronized boolean isBusy()
	{
		Boolean BUSY = true;
		
		try{
			Utility.writeObject(outs, "is_busy");
			BUSY = (Boolean)Utility.readObject(ins);
		}catch(Exception e) {e.printStackTrace();}
		
		return BUSY;
	}
	
	/**
	 * Check if connection is alive.
	 * @return true if currently connected
	 */
	public synchronized boolean isAlive()
	{
		try{
			Utility.writeObject(outs, "ping");
		}catch(Exception e){
			return false;
		}
		
		return true;
	}

	/**
	 * Wait for all pending asynchronous calls to complete.
	 */
	public void waitOnPending()
	{
		while(pending_asynchronous_calls.get() > 0){
			Utility.pause(500);
		}
	}

	/**
	 * Close the connection to the ICR server.
	 */
	public synchronized void close()
	{
		if(outs != null && ins != null){
			waitOnPending();

			try{
				Utility.writeObject(outs, "close");
				Utility.readObject(ins);	//Wait for response
			}catch(Exception e) {e.printStackTrace();}
		}
	}
	
	/**
	 * Debug tests for an ICRClient.
	 * @param icr an ICR client
	 */
	public static void debug(ICRClient icr)
	{
		String debug_input_path = "C:/Kenton/Data/NARA/DataSets/PolyglotDemo/";
		String debug_output_path = "C:/Kenton/Data/Temp/";
		
		//Test sending a file
		if(false){	
			FileData file_data0 = new FileData(debug_input_path + "heart.wrl", true);
			CachedFileData cached_file_data0 = icr.sendData(file_data0);
			System.out.println("Cached data for " + cached_file_data0.getName() + "." + cached_file_data0.getFormat());
		}
		
		//Test retrieving a file
		if(false){
			FileData file_data0 = new FileData(debug_input_path + "heart.wrl", true);
			CachedFileData cached_file_data0 = icr.sendData(file_data0);
			FileData file_data1 = icr.retrieveData(cached_file_data0);
			file_data1.save(debug_output_path, null);
		}
		
		//Test tasks execution
		if(false){
			FileData file_data0 = new FileData(debug_input_path + "heart.wrl", true);
			CachedFileData cached_file_data0 = icr.sendData(file_data0);
			CachedFileData cached_file_data1 = new CachedFileData(cached_file_data0, "stp");		//stl, stp
			
			Vector<Task> tasks = (new TaskList(icr, null, cached_file_data0, cached_file_data1)).getTasks();
			icr.executeTasks(tasks);
						
			FileData file_data1 = icr.retrieveData(cached_file_data1);
			file_data1.save(debug_output_path, null);
		}
		
		//Test asynchronous usage
		if(false){
			FileData file_data0 = new FileData(debug_input_path + "heart.wrl", true);
			AsynchronousObject<CachedFileData> cached_file_data0 = icr.sendDataLater(file_data0);
			CachedFileData cached_file_data1 = new CachedFileData(file_data0, "stl");		//stl, stp
			
			Vector<Task> tasks = (new TaskList(icr, null, cached_file_data0.get(), cached_file_data1)).getTasks();
			AsynchronousObject<Integer> response = icr.executeTasksLater(tasks);
						
			response.waitUntilAvailable();
			AsynchronousObject<FileData> file_data1 = icr.retrieveDataLater(cached_file_data1);
			file_data1.get().save(debug_output_path, null);
		}

		//Test user specified tasks execution
		if(false){
			TaskList tasks = new TaskList(icr);
			tasks.add("A3DReviewer", "open", debug_input_path + "heart.wrl", "");
			tasks.add("A3DReviewer", "export", "", "heart.stp");
			tasks.print();
			tasks.execute(debug_output_path);
		}
		
		//Test user specified tasks execution
		if(true){
			TaskList tasks = new TaskList(icr);
			tasks.add("Blender", "convert", debug_input_path + "heart.wrl", "heart.stl");
			tasks.add("A3DReviewer", "open", "heart.stl", "");
			tasks.add("A3DReviewer", "export", "", "heart.stp");
			tasks.print();
			tasks.execute(debug_output_path);
		}
	}
	
	/**
	 * Startup an ICR client command prompt.
	 * @param args command line arguments
	 */
	public static void main(String args[])
	{
		boolean DEBUG = false;
		TaskList tasks = null;
		Console console;
		String cwd = Utility.unixPath(System.getProperty("user.dir")) + "/";
		String line, alias, operation, input, output;
		String server = "localhost";
		int port = 30;
				
		//Read in *.ini file
	  try{
	    BufferedReader ins = new BufferedReader(new FileReader("ICRClient.ini"));
	    String key, value;
	    int tmpi;
	    
	    while((line=ins.readLine()) != null){
	      if(line.contains("=")){
	        key = line.substring(0, line.indexOf('='));
	        value = line.substring(line.indexOf('=')+1);
	        
	        if(key.charAt(0) != '#'){
	        	if(key.equals("ICRServer")){
	          	tmpi = value.lastIndexOf(':');
	        		
	        		if(tmpi != -1){
	        			server = InetAddress.getByName(value.substring(0, tmpi)).getHostAddress();
	        			port = Integer.valueOf(value.substring(tmpi+1));
	        		}
	          }else if(key.equals("DefaultPath")){
	          	cwd = value + "/";
	          }
	        }
	      }
	    }
	    
	    ins.close();
	  }catch(Exception e) {e.printStackTrace();}
	  
	  //Parse command line arguments
    for(int i=0; i<args.length; i++){
      if(args[i].charAt(0) == '-'){
      	if(args[i].equals("-dbg")){
      		DEBUG = true;
      	}
      }
    }
    
	  //Establish connection
		ICRClient icr = new ICRClient(server, port);
		
		//Process ICR requests
		if(DEBUG){
			debug(icr);
		}else{
			console = System.console();
			
			if(console != null){				
				while(true){
					line = console.readLine("\nicr> ");
	
					if(line.equals("pwd")){
						System.out.println(cwd);
					}else if(line.equals("ls")){
						File[] cwd_files = new File(cwd).listFiles();
						
						for(int i=0; i<cwd_files.length; i++){
							System.out.println(cwd_files[i].getName());
						}
					}else if(line.equals("cd ..")){
						cwd = Utility.pathDotDot(cwd) + "/";
					}else if(line.startsWith("cd ")){
						cwd += line.substring(3) + "/";
					}else if(line.startsWith("send ")){
						icr.sendData(new FileData(cwd + line.substring(5), true));
					}else if(line.startsWith("retrieve ")){
						FileData file_data = icr.retrieveData(new CachedFileData(line.substring(9)));
						file_data.save(cwd, null);
					}else if(line.equals("help")){
						System.out.println();
						Application.print(icr.getApplications());
					}else if(line.equals("tasks")){
						tasks = new TaskList(icr);
						
						while(true){
							line = console.readLine("task " + (tasks.size()+1) + "> ");
							
							if(line.equals("end")){
								tasks.execute(cwd);
								break;
							}else{
								String[] strings = line.split(" ");
								alias = strings[0];
								operation = strings[1];
								input = strings[2];
								output = strings[3];
								
								if(input.startsWith("./")){
									input = cwd + input.substring(2);
								}
								
								tasks.add(alias, operation, input, output);
							}
						}
					}else if(line.equals("quit") || line.equals("exit")){
						break;
					}
				}
			}else{
				System.out.println("No console available!");
			}
		}
		
		//Close connection
		icr.close();
	}
}