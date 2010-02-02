package edu.ncsa.icr.polyglot;
import edu.ncsa.icr.*;
import edu.ncsa.icr.ICRAuxiliary.*;
import edu.ncsa.utility.*;
import java.io.*;
import java.util.*;

/**
 * A class that coordinates the use of several ICR clients via I/O-graphs to perform file
 * format conversions.
 * @author Kenton McHenry
 */
public class PolyglotSteward implements Polyglot
{
	private Vector<ICRClient> icr_clients = new Vector<ICRClient>();
	private IOGraph<Data,Application> iograph = new IOGraph<Data,Application>();
	
	/**
	 * Class constructor.
	 * @param filename the name of a *.ini file
	 */
	public PolyglotSteward(String filename)
	{
		loadINI(filename);
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
	        	if(key.equals("ICRServer")){
	        		tmpi = value.lastIndexOf(':');
	        		
	        		if(tmpi != -1){
	        			server = value.substring(0, tmpi);
	        			port = Integer.valueOf(value.substring(tmpi+1));
	        			add(server, port);
	        		}
	        	}
	        }
	      }
	    }
	    
	    ins.close();
	  }catch(Exception e) {e.printStackTrace();}
	}
	
	/**
	 * Add an ICR client.
	 * @param icr an ICR client
	 */
	public void add(ICRClient icr)
	{
		icr_clients.add(icr);
		iograph.addGraph(new IOGraph<Data,Application>(icr));
	}
	
	/**
	 * Add an ICR client.
	 * @param server the server name
	 * @param port the port number
	 */
	public void add(String server, int port)
	{
		ICRClient icr = new ICRClient(server, port);
		add(icr);
	}
	
	/**
	 * Convert a files format.
	 * @param input_filename the name of the input file
	 * @param output_type the name of the output type
	 */
	public void convert(String input_filename, String output_type)
	{
		String input_name = Utility.getFilenameName(input_filename);
		String input_type = Utility.getFilenameExtension(input_filename);
		Vector<Conversion<Data,Application>> conversions = iograph.getShortestConversionPath(input_type, output_type, false);
		TaskList task_list;
		
		if(conversions != null){
			for(int i=0; i<conversions.size(); i++){
				task_list = conversions.get(i).getTaskList(input_name);
			}
		}
	}
	
	/**
	 * Close all ICR client connections.
	 */
	public void close()
	{
		for(int i=0; i<icr_clients.size(); i++){
			icr_clients.get(i).close();
		}
	}
	
	/**
	 * Command line polyglot interface.
	 * @param args command line arguments
	 */
	public static void main(String[] args)
	{
		PolyglotSteward polyglot = new PolyglotSteward("PolyglotSteward.ini");
		polyglot.close();
	}
}