package edu.ncsa.icr;
import edu.ncsa.utility.*;
import java.io.*;
import java.util.*;

/**
 * Benchmarks for software servers.
 * @author Kenton McHenry
 */
public class Benchmarks
{
	/**
	 * Run a benchmark.
	 * @param args command line arguments
	 */
	public static void main(String args[])
	{		
		String software_server = "http://localhost:8182/";
		String data_path = "./";
		String temp_path = "./";
		Vector<String> outputs;
		Vector<String> inputs = new Vector<String>();
		Vector<Double> times = new Vector<Double>();
		String application = null, task = null, output, input, url, output_url;
		Random random = new Random();
		long benchmark_start, wait_start;
		long t0, t1;
		int max_wait_time = 30000;
		int count, successes, tmpi;
		
		//Load *.ini file
	  try{
	    BufferedReader ins = new BufferedReader(new FileReader("Benchmarks.ini"));
	    String line, key, value;
	    
	    while((line=ins.readLine()) != null){
	      if(line.contains("=")){
	        key = line.substring(0, line.indexOf('='));
	        value = line.substring(line.indexOf('=')+1);
	        
	        if(key.charAt(0) != '#'){
	          if(key.equals("SoftwareServer")){
	          	software_server = value + "/";
	          }else if(key.equals("Application")){
	        		application = value;
	          }else if(key.equals("Task")){
	        		task = value;
	          }else if(key.equals("DataPath")){
	        		data_path = value + "/";
	          }else if(key.equals("TempPath")){
	        		temp_path = value + "/";
	          }else if(key.equals("MaxWaitTime")){
	        		max_wait_time = Integer.valueOf(value);
	          }
	        }
	      }
	    }
	    
	    ins.close();
	  }catch(Exception e) {e.printStackTrace();}
	  
	  //Get list of output formats
		outputs = DistributedSoftwareServerRestlet.getEndpointValues(software_server + "software/" + application + "/" + task + "/");
	  
	  //Build a list of input files
	  File folder = new File(data_path);
	  
	  for(File file : folder.listFiles()){
	  	if(!file.isDirectory()){
	  		inputs.add(Utility.unixPath(file.getAbsolutePath()));
	  	}
	  }
	  
	  //Create a "tmp" folder
		if(!Utility.exists(temp_path + "tmp/")) new File(temp_path + "tmp/").mkdir();

	  //Hit the software server
		benchmark_start = System.currentTimeMillis();
	  count = 0;
	  successes = 0;
	  
	  while(true){
	  	count++;
	  	output = outputs.get(Math.abs(random.nextInt())%outputs.size());
	  	input = inputs.get(Math.abs(random.nextInt())%inputs.size());
	  	url = software_server + "software/" + application + "/" + task + "/" + output + "/";
	  	System.out.println(count + ": " + url + "[" + Utility.getFilename(input) + "]");

	  	t0 = System.currentTimeMillis();
	  	output_url = Utility.postFile(url, input, "text/plain");
	  	System.out.print("-> " + output_url + " ");
	  	
	  	//Wait for result
	  	wait_start = System.currentTimeMillis();
	  	tmpi = 0;
	  	
	  	while(!Utility.existsURL(output_url)){
	  		if(System.currentTimeMillis()-wait_start > max_wait_time) break;
	  		if(tmpi%10 == 0) System.out.print(".");
	  		tmpi++;
	  	}
	  	
	  	t1 = System.currentTimeMillis(); 
	  	times.add((t1-t0)/1000.0);
	  	
	  	//Check result
	  	if(Utility.existsURL(output_url)) Utility.downloadFile(temp_path + "tmp/", output_url);
	  	
	  	if(ScriptDebugger.exists(temp_path + "tmp/" + Utility.getFilename(output_url))){
	  		successes++;
	  	}
	  	
	  	//Display statistics
	  	double hours = (System.currentTimeMillis()-benchmark_start)/3600000.0;
	  	double operations_per_hour = count / hours;
	  	double success_rate = 100.0 * successes/(double)count;
	  	double wait_mean = Utility.mean(times);
	  	double wait_std = Utility.std(times, wait_mean);
	  	
	  	int whole_hours = (int)Math.floor(hours);
	  	int whole_minutes = (int)Math.floor(60.0*(hours-whole_hours));
	  	int seconds = (int)Math.round(3600.0*(hours-whole_hours-(whole_minutes/60.0)));
	  	
	  	String message = "";
	  	message += "[time: " + Utility.toString(whole_hours, 2) + ":" + Utility.toString(whole_minutes, 2) + ":" + Utility.toString(seconds, 2);
	  	message += ", tasks/hour: " + Utility.round(operations_per_hour, 2);
	  	message += ", success rate: " + Utility.round(success_rate, 2) + "%";
	  	message += ", average wait: " + Utility.round(wait_mean, 2) + " s (" + Utility.round(wait_std, 2) + " s)";
	  	message += "]";
	  	
	  	Utility.println(temp_path + "log.txt", message);
	  	System.out.println("\n" + message + "\n");
	  }
	}
}