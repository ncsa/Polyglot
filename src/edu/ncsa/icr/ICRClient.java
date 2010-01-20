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
	 * Synchronously request an operation from the ICR server.
	 * @param application the application performing the operation
	 * @param operation the operation to perform
	 * @param data the data to perform the operation on
	 * @return the resulting data after the operation
	 */
	public Data requestOperationAndWait(String application, String operation, Data data)
	{
		return null;
	}
	
	/**
	 * Asynchronously request an operation from the ICR server.
	 * @param application the application performing the operation
	 * @param operation the operation to perform
	 * @param data the data to perform the operation on
	 * @return the resulting data after the operation
	 */
	public Data requestOperation(String application, String operation, Data data)
	{
		final String application_final = application;
		final String operation_final = operation;
		final Data data_final = data;
		
		new Thread(){
			public void run(){
				requestOperationAndWait(application_final, operation_final, data_final);
			}
		}.start();
		
		return null;
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
			FileData data0 = new FileData(icr.data_path + "heart.wrl", true);
			CachedFileData data0_cached = icr.sendDataAndWait(data0);
			System.out.println("Cached data\n" + data0_cached.toString());
		}
		
		//Test sending a file asynchronously
		if(false){
			FileData data0 = new FileData(icr.data_path + "heart.wrl", true);
			CachedFileData data0_cached = icr.sendData(data0);
			
			data0_cached.waitUntilValid();
			System.out.println("Cached data\n" + data0_cached.toString());
		}
		
		//Test retrieving a file synchronously
		if(false){
			FileData data0 = new FileData(icr.data_path + "heart.wrl", true);
			CachedFileData data0_cached = icr.sendDataAndWait(data0);
			FileData data1 = icr.retrieveDataAndWait(data0_cached);
			data1.save(icr.temp_path, null);
		}
		
		//Test retrieving a file asynchronously
		if(false){
			FileData data0 = new FileData(icr.data_path + "heart.wrl", true);
			CachedFileData data0_cached = icr.sendDataAndWait(data0);
			FileData data1 = icr.retrieveData(data0_cached);
			
			data1.waitUntilValid();
			data1.save(icr.temp_path, null);
		}
		
		//Test sending and retrieving a file asynchronously
		if(true){
			FileData data0 = new FileData(icr.data_path + "heart.wrl", true);
			CachedFileData data0_cached = icr.sendData(data0);
			FileData data1 = icr.retrieveData(data0_cached);
			
			data1.waitUntilValid();
			data1.save(icr.temp_path, null);
		}
		
		icr.close();
	}
}