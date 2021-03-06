package edu.illinois.ncsa.isda.softwareserver.polyglot;
import edu.illinois.ncsa.isda.softwareserver.SoftwareServerAuxiliary.*;
import kgm.utility.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import javax.swing.JFrame;

public class PolyglotServer implements Runnable
{
	private ServerSocket server_socket;
	private int port;
	private PolyglotSteward polyglot = new PolyglotSteward();
	private int steward_port = -1;
	private LinkedList<String> clients = new LinkedList<String>();
	private AtomicInteger session_counter = new AtomicInteger();
	private boolean POLYGLOT_WEB_INTERFACE = false;	
	private boolean POLYGLOT_MONITOR = false;
	private boolean RUNNING;

	/**
	 * Class constructor.
	 * @param filename the file name of a configuration file
	 */
	public PolyglotServer(String filename)
	{		
		if(filename != null) loadConfiguration(filename);
		if(steward_port >= 0) polyglot.listen(steward_port);
		
		try{
			server_socket = new ServerSocket(port);
		}catch(Exception e) {e.printStackTrace();}
		
		new Thread(this).start();
							
		Utility.pause(5000);		//Wait a bit for software server connections
		
		if(POLYGLOT_WEB_INTERFACE) new PolyglotWebInterface("PolyglotWebInterface.conf", false);
		
		if(POLYGLOT_MONITOR){
			JFrame frame = new PolyglotMonitor("localhost", port).createFrame();
	  	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		}
	}
	
	/**
	 * Initialize based on parameters within the given configuration file.
	 * @param filename the file name of the configuration file
	 */
	public void loadConfiguration(String filename)
	{
	  try{
	    BufferedReader ins = new BufferedReader(new FileReader(filename));
	    String line, key, value;
	    String server;
	    int port, tmpi;
	    
	    while((line=ins.readLine()) != null){
	      if(line.contains("=")){
	        key = line.substring(0, line.indexOf('='));
	        value = line.substring(line.indexOf('=')+1);
	        
	        if(key.charAt(0) != '#'){
	        	if(key.equals("Port")){
	        		this.port = Integer.valueOf(value);
	        	}else if(key.equals("StewardPort")){
	          	steward_port = Integer.valueOf(value);
	          }else if(key.equals("SoftwareServer")){
          		tmpi = value.lastIndexOf(':');
	        		
	        		if(tmpi != -1){
	        			server = value.substring(0, tmpi);
	        			port = Integer.valueOf(value.substring(tmpi+1));
	        			polyglot.add(server, port);
	        		}
	          }else if(key.equals("PolyglotWebInterface")){
	          	POLYGLOT_WEB_INTERFACE = Boolean.valueOf(value);
	          }else if(key.equals("PolyglotMonitor")){
	          	POLYGLOT_MONITOR = Boolean.valueOf(value);
	          }
	        }
	      }
	    }
	    
	    ins.close();
	  }catch(Exception e){}
	}
	
  /**
   * Process Polyglot requests.
   */
  public void run()
  {				
  	Socket client_socket = null;
  	
  	//Begin accepting connections
  	System.out.println("\nPolyglot Server is running...\n");
		RUNNING = true;
		
		while(RUNNING){
			try{			
				//Wait for a connection
				client_socket = server_socket.accept();
				
				//Spawn a thread to handle this connection
				final Socket client_socket_final = client_socket;
				
				new Thread(){
					public void run(){
						serveConnection(session_counter.incrementAndGet(), client_socket_final);
					}
				}.start();
			}catch(SocketException e){
				RUNNING = false;
			}catch(Exception e) {e.printStackTrace();}
		}
		
		System.out.println("... Polyglot Server is exiting.");
  }
  
  /**
   * Process requests from the given connection.
   * @param session the session id for this connection
   * @param client_socket the connection to serve
   */
	public void serveConnection(int session, Socket client_socket)
	{			
		String message;
		FileData input_file_data;
		FileData output_file_data;
		String input_type;
		String output_type;
		TreeSet<String> input_types;
		TreeSet<String> output_types;
		IOGraph<String,String> iograph;
		String host = client_socket.getInetAddress().getHostName();
				
		System.out.println("[" + host + "](" + session + "): connection established");

		synchronized(clients){
			clients.add(host);
		}

		try{
			InputStream ins = client_socket.getInputStream();
			OutputStream outs = client_socket.getOutputStream();
		
			//Send session ID
			Utility.writeObject(outs, session);
	  	
			//Process requests
			while(client_socket.isConnected()){
				message = (String)Utility.readObject(ins);
				
				if(message.equals("all_outputs")){
					output_types = polyglot.getOutputs();
					
					Utility.writeObject(outs, output_types);
					System.out.println("[" + host + "](" + session + "): sending all output types");
				}else if(message.equals("outputs")){
					input_type = (String)Utility.readObject(ins);
					output_types = polyglot.getOutputs(input_type);
					
					Utility.writeObject(outs, output_types);
					System.out.println("[" + host + "](" + session + "): sending output types");
				}else if(message.equals("common_outputs")){
					input_types = (TreeSet<String>)Utility.readObject(ins);
					output_types = polyglot.getOutputs(input_types);
					
					Utility.writeObject(outs, output_types);
					System.out.println("[" + host + "](" + session + "): sending common output types");
				}else if(message.equals("input_output_graph")){
					iograph = polyglot.getInputOutputGraph();
					
					Utility.writeObject(outs, iograph);
					System.out.println("[" + host + "](" + session + "): sending input/output graph");
				}else if(message.equals("distributed_input_output_graph")){
					iograph = polyglot.getDistributedInputOutputGraph();
					
					Utility.writeObject(outs, iograph);
					System.out.println("[" + host + "](" + session + "): sending distributed input/output graph");
				}else if(message.equals("servers")){
					Utility.writeObject(outs, polyglot.getServers());
				}else if(message.equals("clients")){
					Utility.writeObject(outs, new Vector<String>(clients));
				}else if(message.equals("convert")){
					input_file_data = (FileData)Utility.readObject(ins);
					System.out.println("[" + host + "](" + session + "): received file " + input_file_data.getName() + "." + input_file_data.getFormat());
					
					output_type = (String)Utility.readObject(ins);
					System.out.print("[" + host + "](" + session + "): converting " + input_file_data.getName() + "." + input_file_data.getFormat() + " to " + input_file_data.getName() + "." + output_type + "...");
					output_file_data = polyglot.convert(input_file_data, output_type);
					
					if(output_file_data != null){
						System.out.println("\t[succeeded]");
					}else{
						System.out.println("\t[failed]");
					}
					
					Utility.writeObject(outs, output_file_data);
					if(output_file_data != null) System.out.println("[" + host + "](" + session + "): sent file " + output_file_data.getName() + "." + output_file_data.getFormat());
				}else if(message.equals("close")){
					Utility.writeObject(outs, "bye");
					System.out.println("[" + host + "](" + session + "): closing connection");
					break;
				}else{
					System.out.println("[" + host + "](" + session + "): unrecognized request, terminating connection!");
					break;
				}
				
				Utility.pause(500);
			}
		}catch(Exception e){
			System.out.println("[" + host + "](" + session + "): connection lost!");
			//e.printStackTrace();
		}
		
		synchronized(clients){
			clients.remove(host);
		}
	}
	
  /**
   * Wait until the servers main thread stops.
   */
  public void waitUntilStopped()
  {
  	while(RUNNING){
  		Utility.pause(500);
  	}
  }
  
	/**
	 * Stop the Polyglot server.
	 */
	public void stop()
	{
		try{
			server_socket.close();
		}catch(Exception e) {e.printStackTrace();}
		
		waitUntilStopped();
		
		polyglot.close();
	}
  
	/**
	 * A main for the Polyglot service.
	 * @param args command line arguments
	 */
	public static void main(String args[])
	{
		PolyglotServer server = new PolyglotServer("PolyglotServer.conf");
	}
}