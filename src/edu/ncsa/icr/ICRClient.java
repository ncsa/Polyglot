package edu.ncsa.icr;
import edu.ncsa.icr.ICRAuxiliary.*;
import java.io.*;
import java.net.*;

/**
 * An Imposed Code Reuse client interface.
 * @author Kenton McHenry
 */
public class ICRClient
{
	private Socket socket;
	private String server;
	private int port;
	
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
		}catch(Exception e) {e.printStackTrace();}
	}
	
	/**
	 * Retrieve a list of aliases for applications running on the ICR server.
	 * @return a list of application aliases
	 */
	public String[] retrieveApplications()
	{
		return null;
	}
	
	/**
	 * Retrieve a list of operations supported by the given application alias on the ICR server.
	 * @param application the application to retrieve operations for
	 * @return a list of operations supported by the given application
	 */
	public String[] retrieveOperations(String application)
	{
		return null;
	}
	
	/**
	 * Retrieve a list of allowed inputs for a given application/operation.
	 * @param application the application alias
	 * @param operation the operation
	 * @return a list of inputs accepted
	 */
	public Data[] retrieveInputs(String application, String operation)
	{
		return null;
	}
	
	/**
	 * Retrieve a list of allowed outputs for a given application/operation.
	 * @param application the application alias
	 * @param operation the operation
	 * @return a list of outputs provided
	 */
	public Data[] retrieveOutputs(String application, String operation)
	{
		return null;
	}
	
	/**
	 * Synchronously send data to the ICR server.
	 * @param data the data to send
	 * @return a cached version of the given data (i.e. a pointer to the data on the server)
	 */
	public Data sendDataAndWait(Data data)
	{		
		return null;
	}
	
	/**
	 * Asynchronously send data to a server.
	 * @param data the data to send
	 * @return a cached version of the given data (i.e. a pointer to the data on the server)
	 */
	public Data sendData(Data data)
	{
		final Data data_final = data;
		
		new Runnable(){
			public void run(){
				sendDataAndWait(data_final);
			}
		};
		
		return null;
	}
	
	/**
	 * Retrieve cached data on the ICR server.
	 * @param cached_data the cached data to retrieve
	 * @return the actual data from the server
	 */
	public Data retrieveData(Data cached_data)
	{
		Data data = null;
		
		return data;
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
		
		new Runnable(){
			public void run(){
				requestOperationAndWait(application_final, operation_final, data_final);
			}
		};
		
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
          	if(key.equals("Server")){
            	server = InetAddress.getByName(value).getHostAddress();
          	}else if(key.equals("Port")){
          		port = Integer.valueOf(value);
            }
          }
        }
      }
      
      ins.close();
    }catch(Exception e){}
  }
  
	/**
	 * A main for debug purposes.
	 * @param args command line arguments
	 */
	public static void main(String args[])
	{
		ICRClient icr = new ICRClient("ICRClient.ini");
		String[] applications = icr.retrieveApplications();
		String[][] operations = new String[applications.length][];
		
		for(int i=0; i<applications.length; i++){
			operations[i] = icr.retrieveOperations(applications[i]);
		}
	}
}