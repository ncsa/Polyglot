package edu.illinois.ncsa.isda.icr;
import kgm.utility.*;
import java.io.*;
import java.text.*;
import java.util.*;

/**
 * Benchmark for software servers.
 * @author Kenton McHenry
 */
public class Benchmark
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
		String download_path = "./";
		String benchmark_name;
		String message;
		Vector<String> outputs;
		Vector<String> inputs = new Vector<String>();
		Vector<Double> times = new Vector<Double>();
		String application = null, task = null, output, input, url, output_url;
		Random random = new Random();
		long benchmark_start, wait_start;
		long t0, t1;
		int max_wait_time = 30000;
		double max_time = Double.MAX_VALUE;
  	double hours, operations_per_hour, success_rate, wait_mean, wait_std;
  	int whole_hours, whole_minutes, seconds;
  	int task_count, kill_count, completed_task_count;
		int count, successes, tmpi;
		
		//Load configuration file
	  try{
	    BufferedReader ins = new BufferedReader(new FileReader("Benchmark.conf"));
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
	          }else if(key.equals("MaxTime")){
	        		max_time = Double.valueOf(value);
	          }
	        }
	      }
	    }
	    
	    ins.close();
	  }catch(Exception e) {e.printStackTrace();}
	  
	  //Parse command line arguments
		for(int i=0; i<args.length; i++){
			if(args[i].equals("-?")){
				System.out.println("Usage: Benchmark [options] [data]");
				System.out.println();
				System.out.println("Options: ");
				System.out.println("  -?: display this help");
				System.out.println("  -a: the application to use");
				System.out.println();
				System.exit(0);
			}else if(args[i].equals("-a")){
				application = args[++i];
			}else{
				data_path = Utility.cleanUnixPath(args[i]);
			}
		}
		
		//Set benchmark name
		benchmark_name = application + "-" + Utility.split(data_path, '/', true, true).lastElement();
		
	  //Get list of output formats
		outputs = DistributedSoftwareServerRestlet.getEndpointValues(software_server + "software/" + application + "/" + task + "/");
	  
	  //Build a list of input files
	  File folder = new File(data_path);
	  
	  for(File file : folder.listFiles()){
	  	if(!file.isDirectory()){
	  		inputs.add(Utility.unixPath(file.getAbsolutePath()));
	  	}
	  }
	  
	  //Create a download folder
	  download_path = temp_path + "downloads." + benchmark_name + "/";
		if(!Utility.exists(download_path)) new File(download_path).mkdir();

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
	  	if(Utility.existsURL(output_url)) Utility.downloadFile(download_path, output_url);
	  	
	  	if(ScriptDebugger.exists(download_path + Utility.getFilename(output_url))){
	  		successes++;
	  	}
	  	
	  	//Display statistics
	  	hours = (System.currentTimeMillis()-benchmark_start)/3600000.0;
	  	operations_per_hour = count / hours;
	  	success_rate = 100.0 * successes/(double)count;
	  	wait_mean = Utility.mean(times);
	  	wait_std = Utility.std(times, wait_mean);
	  	
	  	whole_hours = (int)Math.floor(hours);
	  	whole_minutes = (int)Math.floor(60.0*(hours-whole_hours));
	  	seconds = (int)Math.round(3600.0*(hours-whole_hours-(whole_minutes/60.0)));
	  	
	  	message = "";
	  	message += "[time: " + Utility.toString(whole_hours, 2) + ":" + Utility.toString(whole_minutes, 2) + ":" + Utility.toString(seconds, 2);
	  	message += ", tasks/hour: " + Utility.round(operations_per_hour, 2);
	  	message += ", success rate: " + Utility.round(success_rate, 2) + "%";
	  	message += ", average wait: " + Utility.round(wait_mean, 2) + " s (" + Utility.round(wait_std, 2) + " s)";
	  	message += "]";
	  	
	  	Utility.println(temp_path + benchmark_name + ".txt", message);
	  	System.out.println("\n" + message + "\n");
	  	
	  	if(hours > max_time) break;
	  }
	  
	  //Add final result to shared log file
	  task_count = Integer.valueOf(Utility.readURL(software_server + "tasks"));
	  kill_count = Integer.valueOf(Utility.readURL(software_server + "kills"));
	  completed_task_count = Integer.valueOf(Utility.readURL(software_server + "completed_tasks"));

	  message = "";
	  message += new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()) + " - ";
	  message += application + " (" + data_path + "):\n";
  	message += "[time: " + Utility.toString(whole_hours, 2) + ":" + Utility.toString(whole_minutes, 2) + ":" + Utility.toString(seconds, 2);
  	message += ", tasks/hour: " + Utility.round(operations_per_hour, 2);
  	message += ", success rate: " + Utility.round(success_rate, 2) + "%";
  	message += ", average wait: " + Utility.round(wait_mean, 2) + " s (" + Utility.round(wait_std, 2) + " s)";
  	message += "]\n";
  	message += "tasks: " + task_count + ", kills: " + kill_count + ", completed tasks: " + completed_task_count + "\n";
  	
  	Utility.println(temp_path + "log.txt", message);
	}
}