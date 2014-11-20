package edu.illinois.ncsa.isda.softwareserver.polyglot;
import edu.illinois.ncsa.isda.softwareserver.*;
import edu.illinois.ncsa.isda.softwareserver.SoftwareServerAuxiliary.*;
import edu.illinois.ncsa.isda.softwareserver.polyglot.PolyglotAuxiliary.*;
import kgm.utility.*;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * A class that coordinates the use of several software server clients via I/O-graphs
 * to perform file format conversions.
 * @author Kenton McHenry
 */
public class PolyglotSteward extends Polyglot implements Runnable
{
	private Vector<SoftwareServerClient> ss_clients = new Vector<SoftwareServerClient>();
	private TreeSet<String> ss_client_strings = new TreeSet<String>();
	private IOGraph<Data,Application> iograph = new IOGraph<Data,Application>();
	private int application_flexibility = 0;
	private int software_server_rest_port = 8182;
	private int max_task_time = 30000;		//In milliseconds

	private boolean RUNNING = false, TCP_LISTENER_RUNNING = false;
	private ServerSocket tcp_listener_server_socket = null;
	
	/**
	 * Class constructor.
	 */
	public PolyglotSteward()
	{
		new Thread(this).start();
	}
	
	/**
	 * Class copy constructor.
	 * @param polyglot the polyglot steward to copy
	 */
	public PolyglotSteward(PolyglotSteward polyglot)
	{
		this();
		
		application_flexibility = polyglot.application_flexibility;
		
		for(int i=0; i<polyglot.ss_clients.size(); i++){
			add(new SoftwareServerClient(polyglot.ss_clients.get(i)));
		}
	}
	
	/**
	 * Get the number of software server clients being used.
	 * @return the number of software server clients
	 */
	public int size()
	{
		return ss_clients.size();
	}

	/**
	 * Add a software server client.
	 * @param ssc a software server client
	 * @return true if successfully added
	 */
	public synchronized boolean add(SoftwareServerClient ssc)
	{
		if(!ss_client_strings.contains(ssc.toString())){
			ss_client_strings.add(ssc.toString());
			ss_clients.add(ssc);
			iograph.addGraph(new IOGraph<Data,Application>(ssc));
			
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * Add a software server client.
	 * @param server the server name
	 * @param port the port number
	 * @return true if successfully added
	 */
	public boolean add(String server, int port)
	{
		if(!ss_client_strings.contains(server + ":" + port)){		//Avoid creating a SoftwareServerClient which would initiate a connection!
			return add(new SoftwareServerClient(server, port));
		}else{
			return false;
		}
	}
	
	/**
	 * Retrieve the specified software server client.
	 * @param index the index of the desired client
	 * @return the software server client
	 */
	public SoftwareServerClient get(int index)
	{
		return ss_clients.get(index);
	}
	
	/**
	 * Get the I/O-graph.
	 * @return the I/O-graph
	 */
	public IOGraph<Data,Application> getIOGraph()
	{
		return iograph;
	}
	
	/**
	 * Get the software available.
	 * @return the list of software
	 */
	public TreeSet<String> getSoftware()
	{
		return iograph.getUniqueEdgeStrings();
	}

	/**
	 * Get the outputs available.
	 * @return the list of outputs
	 */
	public TreeSet<String> getOutputs()
	{
		return iograph.getRangeStrings();
	}
	
	/**
	 * Get the outputs available for the given input type.
	 * @param input the input type
	 * @return the list of outputs
	 */
	public TreeSet<String> getOutputs(String input)
	{
		return iograph.getRangeStrings(input);
	}

	/**
	 * Get the common outputs available for the given input types.
	 * @param inputs the input types
	 * @return the list of outputs
	 */
	public TreeSet<String> getOutputs(TreeSet<String> inputs)
	{
		return iograph.getRangeIntersectionStrings(inputs);
	}
	
	/**
	 * Get the inputs available.
	 * @return the list of inputs
	 */
	public TreeSet<String> getInputs()
	{
		return iograph.getDomainStrings();
	}
	
	/**
	 * Get the inputs available for the given output type.
	 * @param output the output type
	 * @return the list of inputs
	 */
	public TreeSet<String> getInputs(String output)
	{
		return iograph.getDomainStrings(output);
	}
	
	/**
	 * Get the available inputs, outputs, and conversions.
	 * @return an IOGraph containing the information as strings
	 */
	public IOGraph<String,String> getInputOutputGraph()
	{
		return iograph.getIOGraphStrings();
	}
	
	/**
	 * Get a list of connected software servers.
	 * @return a list of connected software servers
	 */
	public Vector<String> getServers()
	{
		Vector<String> servers = new Vector<String>();
		
		for(int i=0; i<ss_clients.size(); i++){
			servers.add(ss_clients.get(i).toString());
		}
		
		return servers;
	}

	/**
	 * Get a string only version of this IOGraph encoded with host machines.
	 * @return a string version of this IOGraph
	 */
  public IOGraph<String,String> getDistributedInputOutputGraph()
  {   
  	IOGraph<String,String> iograph_strings = new IOGraph<String,String>();
  	Vector<Data> vertices = iograph.getVertices();
  	Vector<Vector<Application>> edges = iograph.getEdges();
  	Vector<Vector<Integer>> adjacency_list = iograph.getAdjacencyList();
  	String string;
  	
  	for(int i=0; i<vertices.size(); i++){
  		iograph_strings.addVertex(vertices.get(i).toString());
  	}
  	
  	for(int i=0; i<adjacency_list.size(); i++){
  		for(int j=0; j<adjacency_list.get(i).size(); j++){
  			string = edges.get(i).get(j).toString();
  			string += " [" + edges.get(i).get(j).ssc.getServer() + "]";
  			iograph_strings.addEdge(vertices.get(i).toString(), vertices.get(adjacency_list.get(i).get(j)).toString(), string);
  		}
  	}
  	
  	return iograph_strings;
  }

	/**
	 * Set the flexibility when choosing an application where 0=none (default), 1=allow parallel applications with
	 * the same name (different software servers), 2=allow all parallel applications.  The higher the value the more 
	 * parallelism can be taken advantage of.
	 * @param value the flexibility value
	 */
	public void setApplicationFlexibility(int value)
	{
		application_flexibility = value;
	}

	/**
	 * Set the software server port.
	 * @param value the port
	 */
	public void setSoftwareServerRESTPort(int value)
	{
		software_server_rest_port = value;
	}

	/**
	 * Set the maximum time to wait for a task to complete (used with the Software Server REST interface).
	 * @param value the time to wait in milliseconds
	 */
	public void setMaxTaskTime(int value)
	{
		max_task_time = value;
	}

	/**
	 * Convert a files format.
	 * @param input_file_data the input file data
	 * @param conversions the conversions to perform
	 * @return the output file data
	 */
	public FileData convert(FileData input_file_data, Vector<Conversion<Data,Application>> conversions)
	{
		FileData output_file_data = null;
		Task task = null;
		Application application;
		Vector<Application> application_options = null;
		FileData input, output;
		Data data_last, data_next;
		SoftwareServerClient ssc = null;
		String tmps;
				
		if(conversions != null){
			data_last = input_file_data;
			
			for(int i=0; i<conversions.size(); i++){
				application = conversions.get(i).edge;
				input = (FileData)conversions.get(i).input;
				output = (FileData)conversions.get(i).output;
				data_next = new CachedFileData(input_file_data, output.getFormat());
	
				//Attempt to avoid busy software servers
				if(application_flexibility > 0 && application.ssc.isBusy()){
					tmps = application.ssc.toString();
					
					if(application_flexibility == 1){
						application_options = iograph.getParallelEdges(input, output, application);
					}else if(application_flexibility == 2){
						application_options = iograph.getParallelEdges(input, output, null);
					}
					
					for(int j=0; j<application_options.size(); j++){
						application = application_options.get(j);
						if(!application.ssc.isBusy()) break;
					}
					
					if(false){
						if(!application.ssc.toString().equals(tmps)){
							System.out.println("[Steward]: " + tmps + " is busy, switching to " + application.ssc.toString());
						}else{
							System.out.println("[Steward]: " + tmps + " is busy, remaining with " + application.ssc.toString());
						}
					}
				}
				
				if(application.ssc == ssc){
					task.addSubtasks(application.toString(), data_last, data_next);
				}else{
					if(task != null){		//Execute task list and retrieve result before proceeding
						//task.print();
						data_last = task.execute();
						
						if(data_last instanceof CachedFileData){
							System.out.println("[Steward]: Retrieving intermediary result " + data_last.toString() + " from " + ssc.toString());
							data_last = ssc.retrieveData((CachedFileData)data_last);
						}
					}
					
					ssc = application.ssc;
					ssc.requestNewSession();	//Request a new session to avoid using files with similar names (e.g. weights tool having a failed conversion after a good conversion on the same type)
					task = new Task(ssc, application.toString(), data_last, data_next);
				}
				
				data_last = data_next;
			}
			
			//task.print();
			output_file_data = task.execute(null);
		}
		
		return output_file_data;
	}
	
	/**
	 * Convert a files format.
	 * @param input_file_data the input file data
	 * @param output_type the name of the output type
	 * @return the output file data
	 */
	public FileData convert(FileData input_file_data, String output_type)
	{
		Vector<Conversion<Data,Application>> conversions;
		FileData output_file_data;
		
		conversions = iograph.getShortestConversionPath(input_file_data.getFormat(), output_type, false);
		
		if(false){
			for(int i=0; i<conversions.size(); i++){
				System.out.print(conversions.get(i).input + " -> ");
				System.out.print(conversions.get(i).edge + " -> ");
				System.out.println(conversions.get(i).output);
			}
		}
		
		output_file_data = convert(input_file_data, conversions);
		
		return output_file_data;
	}
	
	/**
	 * Convert a files format.
	 * @param input_filename the absolute name of the input file
	 * @param output_path the output path
	 * @param conversions the conversions to perform
	 */
	public void convert(String input_filename, String output_path, Vector<Conversion<Data,Application>> conversions)
	{
		FileData input_file_data;
		FileData output_file_data;
		
		input_filename = Utility.unixPath(input_filename);
		output_path = Utility.unixPath(output_path);		
		
		input_file_data = new FileData(input_filename, true);
		output_file_data = convert(input_file_data, conversions);
		
		if(output_file_data != null){
			output_file_data.save(output_path, null);
		}
	}
	
	/**
	 * Convert a files format.
	 * @param input_filename the absolute name of the input file
	 * @param output_path the output path
	 * @param output_type the name of the output type
	 * @return the output file name (if changed, null otherwise)
	 */
	public String convert(String input_filename, String output_path, String output_type)
	{				
		FileData input_file_data;
		FileData output_file_data;	
		
		input_filename = Utility.unixPath(input_filename);
		output_path = Utility.unixPath(output_path);		
		
		input_file_data = new FileData(input_filename, true);
		output_file_data = convert(input_file_data, output_type);
		output_file_data.save(output_path, null);
		
		return null;
	}
	
	/**
	 * Convert a file's format using the Software Server's REST interface.  Allows intermediary results to be available (i.e. hosted) on the Software Servers.
	 * @param input_filename the absolute name of the input file
	 * @param output_path the output path
	 * @param output_type the name of the output type
	 */
	public void convertOverREST(String input_filename, String output_path, String output_type)
	{
		Vector<Conversion<Data,Application>> conversions = iograph.getShortestConversionPath(Utility.getFilenameExtension(input_filename), output_type, false);
		Application application;
		Vector<Application> application_options = null;
		FileData input, output;
		String file, tmps;
		long t0, dt;
				
		if(conversions != null){
			file = input_filename;
			
			for(int i=0; i<conversions.size(); i++){
				application = conversions.get(i).edge;
				input = (FileData)conversions.get(i).input;
				output = (FileData)conversions.get(i).output;
	
				//Attempt to avoid busy software servers
				if(application_flexibility > 0 && application.ssc.isBusy()){
					tmps = application.ssc.toString();
					
					if(application_flexibility == 1){
						application_options = iograph.getParallelEdges(input, output, application);
					}else if(application_flexibility == 2){
						application_options = iograph.getParallelEdges(input, output, null);
					}
					
					for(int j=0; j<application_options.size(); j++){
						application = application_options.get(j);
						if(!application.ssc.isBusy()) break;
					}
					
					if(false){
						if(!application.ssc.toString().equals(tmps)){
							System.out.println("[Steward]: " + tmps + " is busy, switching to " + application.ssc.toString());
						}else{
							System.out.println("[Steward]: " + tmps + " is busy, remaining with " + application.ssc.toString());
						}
					}
				}
			
				//Execute task
				String url = "http://" + application.ssc.toString() + ":" + String.valueOf(software_server_rest_port) + "/software/" + application.toString() + "/convert/" + output.getFormat() + "/";
				
				System.out.println("\n Working file - " + file);
				System.out.println(" Software Server REST Call - " + url + "...");

				if(file.startsWith("http://")){
					file = Utility.readURL(url + Utility.urlEncode(file), "text/plain");
				}else{
					file = Utility.postFile(url, file, "text/plain");
				}

				//Wait for result
				t0 = System.currentTimeMillis();
				
				while(!Utility.existsURL(file)){
					Utility.pause(1000);

					//Break if wait exceeds maximum wait time
					dt = System.currentTimeMillis() - t0;
					
					if(dt > max_task_time){
						System.out.println(" Warning: maximum task wait time exceeded!");
						break;					
					}
				}
			}
			
			System.out.println("\n Output file - " + file);
			
			//Download output file
			Utility.downloadFile(output_path, Utility.getFilenameName(input_filename), file);
		}
	}

  /**
	 * Close all software server client connections and stop the PolyglotSteward thread.
	 */
	public void close()
	{
		waitOnPending();
		
		for(int i=0; i<ss_clients.size(); i++){
			ss_clients.get(i).close();
		}
		
		//Code that used to be in a separate stop() method
		RUNNING = false;
		
		//Close the TCP listener socket
		try{
			tcp_listener_server_socket.close();
		}catch(IOException e) {e.printStackTrace();}
		
		waitUntilStopped();
	}
	
  /**
   * Wait until the steward's threads have stopped.
   */
  public void waitUntilStopped()
  {
  	while(RUNNING || TCP_LISTENER_RUNNING){
  		Utility.pause(500);
  	}
  }

	/**
	 * Start listening for SoftwareServers.
	 * @param port the port to listen to
	 */
	public void listen(int port)
	{
		final int port_final = port;
		
		//Listen over TCP
		if(true){
			new Thread(){
				public void run(){
					Socket client_socket = null;
					String sr_server;
					int sr_port;
					
					TCP_LISTENER_RUNNING = true;
					
					try{
						tcp_listener_server_socket = new ServerSocket(port_final);
					}catch(Exception e) {e.printStackTrace();}
					
			  	//Begin accepting connections
			  	System.out.println("[Steward]: Listening for Software Servers...");
					
					while(TCP_LISTENER_RUNNING){
						try{
							//Wait for a connection
							client_socket = tcp_listener_server_socket.accept();
							
							//Handle this connection
							sr_server = client_socket.getInetAddress().getCanonicalHostName();

							if(sr_server.equals("localhost")){
								sr_server = InetAddress.getLocalHost().getCanonicalHostName();
							}

							sr_port = (Integer)Utility.readObject(client_socket.getInputStream());
							
							if(add(sr_server, sr_port)){
								System.out.println("[Steward]: Found Software Server over TCP - " + sr_server + ":" + sr_port);
							}
						}catch(SocketException e){
							TCP_LISTENER_RUNNING = false;
						}catch(Exception e) {e.printStackTrace();}
					}					
				}
			}.start();
		}
		
		//Listen over UDP
		if(true){
			new Thread(){
				public void run(){
					String sr_server;
					int sr_port = 50000;
					byte[] buffer = new byte[10];
					DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
					
					try{
						MulticastSocket multicast_socket = new MulticastSocket(port_final);
						multicast_socket.joinGroup(InetAddress.getByName("225.4.5.6"));
						
						while(RUNNING){
							multicast_socket.receive(packet);
							sr_server = packet.getAddress().toString().substring(1);
							
							if(add(sr_server, sr_port)){
								System.out.println("[Steward]: Found Software Server over UDP - " + sr_server + ":" + sr_port);
							}
							
							Utility.pause(100);
						}
						
						//Close the socket
						//multicast_socket.leaveGroup(InetAddress.getByName("225.4.5.6"));
						//multicast_socket.close();						
					}catch(Exception e) {e.printStackTrace();}
				}
			}.start();
		}
	}
	
	/**
	 * Clean up lost connections.
	 */
	public void run()
	{
		boolean DROPPED_CONNECTION;
		
		RUNNING = true;
		
		while(RUNNING){
			DROPPED_CONNECTION = false;
			int i = 0;
			
			synchronized(this){
				while(i < ss_clients.size()){
					if(!ss_clients.get(i).isAlive()){
						System.out.println("[Steward]: Lost Software Server - " + ss_clients.get(i).toString());
						ss_client_strings.remove(ss_clients.get(i).toString());
						ss_clients.remove(i);
						DROPPED_CONNECTION = true;
					}else{
						i++;
					}
				}
				
				//Rebuild I/O-graph
				if(DROPPED_CONNECTION){
					iograph.clear();
	
					for(i=0; i<ss_clients.size(); i++){
						iograph.addGraph(new IOGraph<Data,Application>(ss_clients.get(i)));
					}
				}
			}
			
			Utility.pause(1000);
		}
	}

	/**
	 * Command line polyglot interface.
	 * @param args command line arguments
	 */
	public static void main(String[] args)
	{
		PolyglotSteward polyglot = new PolyglotSteward();
		String class_path = Utility.unixPath(System.getProperty("java.class.path"));
		String[] paths = class_path.split(";");
		String conf_filename = "PolyglotSteward.conf";
		String input_filename = "";
		String output_path = "./";
		String output_extension = "";
		int count = 0;
		
		//Find class path for the configuration file
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
	        	if(key.equals("SoftwareServer")){
	        		tmpi = value.lastIndexOf(':');
	        		
	        		if(tmpi != -1){
	        			server = value.substring(0, tmpi);
	        			port = Integer.valueOf(value.substring(tmpi+1));
	        			polyglot.add(server, port);
	        		}
	        	}
	        }
	      }
	    }
	    
	    ins.close();
	  }catch(Exception e) {e.printStackTrace();}
	  
    //Set some test arguments if none provided
    if(args.length == 0){	
    	args = new String[]{"C:/Kenton/Data/Temp/PolyglotDemo/heart.wrl", "ply", "C:/Kenton/Data/Temp/"};		//stl, ply
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
		polyglot.convertLater(input_filename, output_path, output_extension);
		polyglot.close();
	}
}
