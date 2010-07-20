package edu.ncsa.icr.polyglot;
import edu.ncsa.icr.ICRAuxiliary.*;
import edu.ncsa.utility.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.*;

public class PolyglotServer implements Runnable
{
	private ServerSocket server_socket;
	private int port;
	private PolyglotSteward polyglot = new PolyglotSteward();
	private int steward_port = -1;
	private AtomicInteger session_counter = new AtomicInteger();
	private boolean RUNNING;

	/**
	 * Class constructor.
	 * @param filename the file name of an initialization file
	 */
	public PolyglotServer(String filename)
	{		
		if(filename != null) loadINI(filename);
		if(steward_port >= 0) polyglot.listen(steward_port);
		
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
	    String server;
	    int port, tmpi;
	    
	    while((line=ins.readLine()) != null){
	      if(line.contains("=")){
	        key = line.substring(0, line.indexOf('='));
	        value = line.substring(line.indexOf('=')+1);
	        
	        if(key.charAt(0) != '#'){
	        	if(key.equals("Port")){
	        		this.port = Integer.valueOf(value);
	          }else if(key.equals("ICRServer")){
          		tmpi = value.lastIndexOf(':');
	        		
	        		if(tmpi != -1){
	        			server = value.substring(0, tmpi);
	        			port = Integer.valueOf(value.substring(tmpi+1));
	        			polyglot.add(server, port);
	        		}
	          }else if(key.equals("StewardPort")){
	          	steward_port = Integer.valueOf(value);
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
			//Wait for a connection
			try{
				client_socket = server_socket.accept();
			}catch(Exception e) {e.printStackTrace();}
						
			//Spawn a thread to handle this connection
			final Socket client_socket_final = client_socket;
			
			new Thread(){
				public void run(){
					serveConnection(session_counter.incrementAndGet(), client_socket_final);
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
		FileData input_file_data;
		FileData output_file_data;
		String input_type;
		String output_type;
		TreeSet<String> input_types;
		TreeSet<String> output_types;
		IOGraph<String,String> iograph;
		
		System.out.println("Session " + session + ": connection established...");

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
					System.out.println("Session " + session + ": sending all output types");
				}else if(message.equals("outputs")){
					input_type = (String)Utility.readObject(ins);
					output_types = polyglot.getOutputs(input_type);
					
					Utility.writeObject(outs, output_types);
					System.out.println("Session " + session + ": sending output types");
				}else if(message.equals("common_outputs")){
					input_types = (TreeSet<String>)Utility.readObject(ins);
					output_types = polyglot.getOutputs(input_types);
					
					Utility.writeObject(outs, output_types);
					System.out.println("Session " + session + ": sending common output types");
				}else if(message.equals("input_output_graph")){
					iograph = polyglot.getInputOutputGraph();
					
					Utility.writeObject(outs, iograph);
					System.out.println("Session " + session + ": sending input/output graph");
				}else if(message.equals("convert")){
					input_file_data = (FileData)Utility.readObject(ins);
					output_type = (String)Utility.readObject(ins);
					output_file_data = polyglot.convert(input_file_data, output_type);
					
					Utility.writeObject(outs, output_file_data);
					System.out.println("Session " + session + ": converted " + input_file_data.getName() + "." + input_file_data.getFormat() + " to " + output_file_data.getName() + "." + output_file_data.getFormat());
				}else if(message.equals("close")){
					Utility.writeObject(outs, "bye");
					System.out.println("Session " + session + ": closing connection!\n");
					break;
				}else{
					System.out.println("Session " + session + ": unrecognized request, terminating connection!\n");
					break;
				}
				
				Utility.pause(500);
			}
		}catch(Exception e){
			System.out.println("Session " + session + ": connection lost!\n");
		}
	}
  
	/**
	 * A main for the Polyglot service.
	 * @param args command line arguments
	 */
	public static void main(String args[])
	{
		PolyglotServer server = new PolyglotServer("PolyglotServer.ini");
	}
}