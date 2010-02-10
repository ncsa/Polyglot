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
public class PolyglotSteward extends Polyglot
{
	private Vector<ICRClient> icr_clients = new Vector<ICRClient>();
	private IOGraph<Data,Application> iograph = new IOGraph<Data,Application>();
	
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
	 * @param input_filename the absolute name of the input file
	 * @param output_path the output path
	 * @param output_type the name of the output type
	 */
	public void convert(String input_filename, String output_path, String output_type)
	{
		String input_path = Utility.getFilenamePath(input_filename);
		String input_type = Utility.getFilenameExtension(input_filename);
		Vector<Conversion<Data,Application>> conversions = iograph.getShortestConversionPath(input_type, output_type, false);
		TaskList task_list = null;
		Application application;
		FileData input, output;
		FileData input_file_data;
		Data data_last, data_next;
		ICRClient icr = null;
		
		if(conversions != null){
			input_file_data = new FileData(input_filename, true);
			data_last = input_file_data;
			
			for(int i=0; i<conversions.size(); i++){
				application = conversions.get(i).edge;
				input = (FileData)conversions.get(i).input;
				output = (FileData)conversions.get(i).output;
				data_next = new CachedFileData(input_file_data, output.getFormat());

				if(application.icr == icr){
					task_list.add(application.toString(), data_last, data_next);
				}else{
					if(task_list != null){		//Execute task list and retrieve result before proceeding
						//task_list.print();
						data_last = task_list.execute();
					}
					
					icr = application.icr;
					task_list = new TaskList(icr, application.toString(), data_last, data_next);
				}
				
				data_last = data_next;
			}
			
			//task_list.print();
			task_list.execute(output_path);
		}
	}
	
	/**
	 * Get the outputs available for the given input type.
	 * @param string a string representing the input type
	 */
	public TreeSet<String> getOutputs(String string)
	{
		return iograph.getRangeStrings(string);
	}
	
	/**
	 * Get the common outputs available for the given input types.
	 * @param set a set of strings representing the input types
	 */
	public TreeSet<String> getOutputs(TreeSet<String> set)
	{
		return iograph.getRangeIntersectionStrings(set);
	}
	
	/**
	 * Close all ICR client connections.
	 */
	public void close()
	{
		waitOnPending();
		
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
		PolyglotSteward polyglot = new PolyglotSteward();
		String class_path = Utility.unixPath(System.getProperty("java.class.path"));
		String[] paths = class_path.split(";");
		String ini_filename = "PolyglotSteward.ini";
		String input_filename = "";
		String output_path = "./";
		String output_extension = "";
		int count = 0;
		
		//Find class path for *.ini file
		for(int i=0; i<paths.length; i++){
			if(!paths[i].endsWith(".jar")){
				if(Utility.exists(paths[i] + "/" + ini_filename)){
					ini_filename = paths[i] + "/" + ini_filename;
					break;
				}else if(Utility.exists(Utility.pathDotDot(paths[i]) + "/" + ini_filename)){
					ini_filename = Utility.pathDotDot(paths[i]) + "/" + ini_filename;
					break;
				}
			}
		}
	  
		//Load parameters from a *.ini file
	  try{
	    BufferedReader ins = new BufferedReader(new FileReader(ini_filename));
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