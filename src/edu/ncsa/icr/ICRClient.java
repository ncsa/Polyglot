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
			
			if(true){
				System.out.println("Connected (session=" + session + ")...\n");
				Application.print(applications);
			}			
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
	 * Synchronously send file data to the ICR server.
	 * @param file_data the file data to send
	 * @return a cached version of the given file data (i.e. a pointer to the data on the server)
	 */
	public CachedFileData sendDataAndWait(FileData file_data)
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
	public CachedFileData sendData(FileData file_data)
	{
		CachedFileData cached_file_data = new CachedFileData();
		final FileData file_data_final = file_data;
		final CachedFileData cached_file_data_final = cached_file_data;
		
		new Thread(){
			public void run(){
				cached_file_data_final.assign(sendDataAndWait(file_data_final));
			}
		}.start();
		
		return cached_file_data;
	}
	
	/**
	 * Synchronously retrieve cached file data from the ICR server.
	 * @param cached_file_data the cached file data to retrieve
	 * @return the actual file data from the server
	 */
	public FileData retrieveDataAndWait(CachedFileData cached_file_data)
	{
		cached_file_data.waitUntilValid();	//In case filled asynchronously!
		
		Utility.writeObject(outs, "retrieve");
		Utility.writeObject(outs, cached_file_data);
		return (FileData)Utility.readObject(ins);
	}
	
	/**
	 * Synchronously retrieve cached file data from the server.
	 * @param cached_file_data the cached file data to retrieve
	 * @return the file data
	 */
	public FileData retrieveData(CachedFileData cached_file_data)
	{
		FileData file_data = new FileData();
		final CachedFileData cached_file_data_final = cached_file_data;
		final FileData file_data_final = file_data;
		
		new Thread(){
			public void run(){
				cached_file_data_final.waitUntilValid();	//In case filled asynchronously!
				file_data_final.assign(retrieveDataAndWait(cached_file_data_final));
			}
		}.start();
		
		return file_data;
	}
	
	/**
	 * Find a suitable operation given the desired application, operation, and data.
	 * @param application_alias the application alias (can be null)
	 * @param operation_name the operation name
	 * @param input_file_data input file data (can be null)
	 * @param output_file_data output file data (can be null)
	 * @return the index of the application and operation (null if none found)
	 */
	public Pair<Integer,Integer> findOperation(String application_alias, String operation_name, FileData input_file_data, FileData output_file_data)
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
									if(((FileData)data).format.equals(input_file_data.format)){
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
									if(((FileData)data).format.equals(output_file_data.format)){
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
	void printOperation(Pair<Integer,Integer> apop)
	{
		if(apop != null){
			System.out.println("Application: " + applications.get(apop.first).alias);
			System.out.println("Operation: " + applications.get(apop.first).operations.get(apop.second).name);
		}else{
			System.out.println("No operation found!");
		}
	}
	
	/**
	 * Synchronously request an operation from the ICR server.
	 * @param application the index of the application to use
	 * @param operation the index of the operation to perform
	 * @param input_file_data the input file data to perform the operation on (can be null)
	 * @param output_file_data the output file data (can be null, used for specifying output formats)
	 * @return the resulting cached file data after the operation
	 */
	public CachedFileData requestOperationAndWait(Integer application, Integer operation, FileData input_file_data, FileData output_file_data)
	{
		Data data;
		
		if(input_file_data != null){
			input_file_data.waitUntilValid();	//In case filled asynchronously!
			
			//Work only with server cached data
			if(!(input_file_data instanceof CachedFileData)){
				input_file_data = sendDataAndWait((FileData)input_file_data);
			}
		}
		
		Utility.writeObject(outs, "operation");
		Utility.writeObject(outs, application);
		Utility.writeObject(outs, operation);
		Utility.writeObject(outs, new Boolean(input_file_data != null));
		if(input_file_data != null) Utility.writeObject(outs, input_file_data);
		Utility.writeObject(outs, new Boolean(output_file_data != null));
		if(output_file_data != null) Utility.writeObject(outs, output_file_data);

		data = (Data)Utility.readObject(ins);
		
		if(data instanceof CachedFileData){
			return (CachedFileData)data;
		}else{
			return null;
		}
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
		
		//Test sending a file synchronously
		if(false){	
			FileData file_data0 = new FileData(icr.data_path + "heart.wrl", true);
			CachedFileData cached_file_data0 = icr.sendDataAndWait(file_data0);
			System.out.println("Cached data\n" + cached_file_data0.toString());
		}
		
		//Test sending a file asynchronously
		if(false){
			FileData file_data0 = new FileData(icr.data_path + "heart.wrl", true);
			CachedFileData cached_file_data0 = icr.sendData(file_data0);
			
			cached_file_data0.waitUntilValid();
			System.out.println("Cached data\n" + cached_file_data0.toString());
		}
		
		//Test retrieving a file synchronously
		if(false){
			FileData file_data0 = new FileData(icr.data_path + "heart.wrl", true);
			CachedFileData cached_file_data0 = icr.sendDataAndWait(file_data0);
			FileData file_data1 = icr.retrieveDataAndWait(cached_file_data0);
			file_data1.save(icr.temp_path, null);
		}
		
		//Test retrieving a file asynchronously
		if(false){
			FileData file_data0 = new FileData(icr.data_path + "heart.wrl", true);
			CachedFileData cached_file_data0 = icr.sendDataAndWait(file_data0);
			FileData file_data1 = icr.retrieveData(cached_file_data0);
			
			file_data1.waitUntilValid();
			file_data1.save(icr.temp_path, null);
		}
		
		//Test sending and retrieving a file asynchronously
		if(false){
			FileData file_data0 = new FileData(icr.data_path + "heart.wrl", true);
			CachedFileData cached_file_data0 = icr.sendData(file_data0);
			FileData file_data1 = icr.retrieveData(cached_file_data0);
			
			file_data1.waitUntilValid();
			file_data1.save(icr.temp_path, null);
		}
		
		//Test synchronous convert operation request
		if(false){
			FileData file_data0 = new FileData(icr.data_path + "heart.wrl", true);
			FileData output_format = FileData.newFormat("stl");
			CachedFileData cached_file_data0 = icr.sendDataAndWait(file_data0);
			Pair<Integer,Integer> apop = icr.findOperation(null, "convert", cached_file_data0, output_format);
			icr.printOperation(apop);
			
			CachedFileData cached_file_data1 = icr.requestOperationAndWait(apop.first, apop.second, cached_file_data0, output_format);
			FileData file_data1 = icr.retrieveDataAndWait(cached_file_data1);
			file_data1.save(icr.temp_path, null);
		}
		
		//Test synchronous open/save operation request
		if(true){
			FileData file_data0 = new FileData(icr.data_path + "heart.wrl", true);
			FileData output_format = FileData.newFormat("stl");
			CachedFileData cached_file_data0 = icr.sendDataAndWait(file_data0);
			Pair<Integer,Integer> apop = icr.findOperation(null, "open", cached_file_data0, null);
			
			icr.printOperation(apop);
			icr.requestOperationAndWait(apop.first, apop.second, cached_file_data0, null);
			//FileData file_data1 = icr.retrieveDataAndWait(cached_file_data1);
			//file_data1.save(icr.temp_path, null);
		}
		
		icr.close();
	}
}