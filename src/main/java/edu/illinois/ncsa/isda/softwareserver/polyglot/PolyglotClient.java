package edu.illinois.ncsa.isda.softwareserver.polyglot;
import edu.illinois.ncsa.isda.softwareserver.SoftwareServerAuxiliary.*;
import kgm.utility.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class PolyglotClient extends Polyglot
{
	private String server;
	private int port;
	private Socket socket;
	private InputStream ins;
	private OutputStream outs;
	private int session = -1;
		
	/**
	 * Class constructor.
	 * @param server the name of the Polyglot server
	 * @param port the port used for the connection
	 */
	public PolyglotClient(String server, int port)
	{				
		this.server = server;
		this.port = port;
		
		try{
			socket = new Socket(server, port);
			ins = socket.getInputStream();
			outs = socket.getOutputStream();
			
			//Get session
			session = (Integer)Utility.readObject(ins);
			
			//System.out.println("Connected (session=" + session + ")...\n");
		}catch(Exception e){
			System.out.println("Unable to connect to Polyglot server: " + server);
		}
	}
	
	/**
	 * Get a list of connected client machines.
	 * @return a list of connected client machines
	 */
	public synchronized Vector<String> getClients()
	{
		Vector<String> clients = null;
		
		try{		
			Utility.writeObject(outs, "clients");
			clients = (Vector<String>)Utility.readObject(ins);
		}catch(Exception e) {e.printStackTrace();}
		
		return clients;
	}
	
	/**
	 * Get the software available.
	 * @return the list of software
	 */
	public TreeSet<String> getSoftware()
	{
		return null;	//TODO: not required by current applications but need to implement at some point
	}
	
	/**
	 * Get the outputs available.
	 * @return the list of outputs
	 */
	public synchronized TreeSet<String> getOutputs()
	{
		TreeSet<String> outputs = null;
		
		try{		
			Utility.writeObject(outs, "all_outputs");
			outputs = (TreeSet<String>)Utility.readObject(ins);
		}catch(Exception e) {e.printStackTrace();}
		
		return outputs;
	}
	
	/**
	 * Get the outputs available for the given input type.
	 * @param input the input type
	 * @return the list of outputs
	 */
	public synchronized TreeSet<String> getOutputs(String input)
	{
		TreeSet<String> outputs = null;
		
		try{
			Utility.writeObject(outs, "outputs");
			Utility.writeObject(outs, input);
			outputs = (TreeSet<String>)Utility.readObject(ins);
		}catch(Exception e) {e.printStackTrace();}
		
		return outputs;
	}
	
	/**
	 * Get the common outputs available for the given input types.
	 * @param inputs the input types
	 * @return the list of outputs
	 */
	public synchronized TreeSet<String> getOutputs(TreeSet<String> inputs)
	{
		TreeSet<String> outputs = null;
		
		try{
			Utility.writeObject(outs, "common_outputs");
			Utility.writeObject(outs, inputs);
			outputs = (TreeSet<String>)Utility.readObject(ins);
		}catch(Exception e) {e.printStackTrace();}
		
		return outputs;
	}
	
	/**
	 * Get the inputs available.
	 * @return the list of inputs
	 */
	public TreeSet<String> getInputs()
	{
		return null;	//TODO: not required by current applications but need to implement at some point
	}
	
	/**
	 * Get the inputs available for the given output type.
	 * @param output the output type
	 * @return the list of inputs
	 */
	public TreeSet<String> getInputs(String output)
	{
		return null;	//TODO: not required by current applications but need to implement at some point
	}
	
	/**
	 * Get the available inputs, outputs, and conversions.
	 * @return an IOGraph containing the information as strings
	 */
	public synchronized IOGraph<String,String> getInputOutputGraph()
	{
		IOGraph<String,String> iograph = null;
		
		try{		
			Utility.writeObject(outs, "input_output_graph");
			iograph = (IOGraph<String,String>)Utility.readObject(ins);
		}catch(Exception e) {e.printStackTrace();}
		
		return iograph;
	}
	
	/**
	 * Get a list of connected software reuse servers.
	 * @return a list of connected software reuse servers
	 */
	public synchronized Vector<String> getServers()
	{
		Vector<String> servers = null;
		
		try{		
			Utility.writeObject(outs, "servers");
			servers = (Vector<String>)Utility.readObject(ins);
		}catch(Exception e) {e.printStackTrace();}
		
		return servers;
	}

	/**
	 * Get a string only version of this IOGraph encoded with host machines.
	 * @return an IOGraph containing the information as strings
	 */
	public synchronized IOGraph<String,String> getDistributedInputOutputGraph()
	{
		IOGraph<String,String> iograph = null;
		
		try{		
			Utility.writeObject(outs, "distributed_input_output_graph");
			iograph = (IOGraph<String,String>)Utility.readObject(ins);
		}catch(Exception e) {e.printStackTrace();}
		
		return iograph;
	}
	
	/**
	 * Convert a files format.
	 * @param input_filename the absolute name of the input file
	 * @param output_path the output path
	 * @param output_type the name of the output type
	 * @return the output file name (if changed, null otherwise)
	 */
	public synchronized String convert(String input_filename, String output_path, String output_type)
	{
		FileData input_file_data = new FileData(input_filename, true);
		FileData output_file_data;
		
		try{		
			Utility.writeObject(outs, "convert");
			Utility.writeObject(outs, input_file_data);
			Utility.writeObject(outs, output_type);
			output_file_data = (FileData)Utility.readObject(ins);
			if(output_file_data != null) output_file_data.save(output_path, null);
		}catch(Exception e) {e.printStackTrace();}
		
		return null;
	}
	
	/**
	 * Close the connection to the Polyglot server.
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
	 * Command line polyglot interface.
	 * @param args command line arguments
	 */
	public static void main(String[] args)
	{
		PolyglotClient polyglot = null;
		String class_path = Utility.unixPath(System.getProperty("java.class.path"));
		String[] paths = class_path.split(";");
		String conf_filename = "PolyglotClient.conf";
		String input_filename = "";
		String output_path = "./";
		String output_extension = "";
		int count = 0;
		
		//Find class path for configuration file
		for(int i=0; i<paths.length; i++){
			if(!paths[i].endsWith(".jar")){
				if(Utility.exists(paths[i] + "/" + conf_filename)){
					conf_filename = paths[i] + "/" + conf_filename;
					break;
				}else if(Utility.exists(Utility.pathDotDot(paths[i]) + "/" + conf_filename)){
					conf_filename = Utility.pathDotDot(paths[i]) + "/" + conf_filename;
					break;
				}
			}
		}
			  
		//Load parameters from a configuration file
	  try{
	    BufferedReader ins = new BufferedReader(new FileReader(conf_filename));
	    String line, key, value;
	    String server;
	    int port, tmpi;
	    
	    while((line=ins.readLine()) != null){
	      if(line.contains("=")){
	        key = line.substring(0, line.indexOf('='));
	        value = line.substring(line.indexOf('=')+1);
	        
	        if(key.charAt(0) != '#'){
	        	if(key.equals("PolyglotServer")){
	        		tmpi = value.lastIndexOf(':');
	        		
	        		if(tmpi != -1){
	        			server = value.substring(0, tmpi);
	        			port = Integer.valueOf(value.substring(tmpi+1));
	        			polyglot = new PolyglotClient(server, port);
	        		}
	        	}
	        }
	      }
	    }
	    
	    ins.close();
	  }catch(Exception e) {e.printStackTrace();}
	  
    //Set some test arguments if none provided
    if(args.length == 0){	
    	args = new String[]{"C:/Kenton/Data/Temp/PolyglotDemo/hello.jpg", "gif", "C:/Kenton/Data/Temp/"};
    }
	  
	  //Parse command line arguments
    for(int i=0; i<args.length; i++){
      if(args[i].charAt(0) == '-'){
      }else{
      	if(count == 0){
      		input_filename = args[i];
      	}else if(count == 1){
      		output_extension = args[i];
      	}else if(count == 2){
      		output_path = args[i];
      	}
      	
        count++;
      }
    }
		
	  //Perform conversion
		polyglot.convert(input_filename, output_path, output_extension);
		polyglot.close();
	}
}