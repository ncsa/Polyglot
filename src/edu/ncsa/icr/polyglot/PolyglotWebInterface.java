package edu.ncsa.icr.polyglot;
import edu.ncsa.utility.*;
import java.io.*;
import java.util.*;

/**
 * A background process that monitors a folder for conversion tasks.  When a folder
 * is created in the upload location and contains a file "commit" this process will
 * move the folder to the download location and execute the task dictated in the "tasks"
 * file.  When completed the daemon will create a file "complete" within the folder.
 * @author Kenton McHenry
 */
public class PolyglotWebInterface implements Runnable
{
	private Polyglot polyglot = null;
  private String path = ".";  
  private int sleep_length = 1000;  				 //How long to sleep before checking for more jobs (in milliseconds)
  private boolean VERBOSE = false;
  
  /**
   * Class constructor.
   * @param filename the INI file to load
   */
  public PolyglotWebInterface(String filename, boolean VERBOSE)
  {
  	loadINI(filename);
  	this.VERBOSE = VERBOSE;
  	
    writeOutputFormats();
    new Thread(this).start();
  }
  
  /**
   * Load an initialization file.
   * @param filename the INI file
   */
  public void loadINI(String filename)
  {
    try{
      BufferedReader ins = new BufferedReader(new FileReader(filename));
      String line;
      String key, value;
	    String server;
	    int port, tmpi;
      
      while((line=ins.readLine()) != null){
        if(line.contains("=")){
          key = line.substring(0, line.indexOf('='));
          value = line.substring(line.indexOf('=')+1);
          
          if(key.charAt(0) != '#'){
            if(key.equals("PolyglotPath")){
              path = Utility.unixPath(value) + "/";
	          }else if(key.equals("SoftwareReuseServer")){
	          	if(polyglot == null || !(polyglot instanceof PolyglotSteward)){
	          		polyglot = new PolyglotSteward();
	          	}
	          	
          		tmpi = value.lastIndexOf(':');
	        		
	        		if(tmpi != -1){
	        			server = value.substring(0, tmpi);
	        			port = Integer.valueOf(value.substring(tmpi+1));
	        			((PolyglotSteward)polyglot).add(server, port);
	        		}
	          }else if(key.equals("PolyglotServer")){
          		tmpi = value.lastIndexOf(':');
	        		
	        		if(tmpi != -1){
	        			server = value.substring(0, tmpi);
	        			port = Integer.valueOf(value.substring(tmpi+1));
	        			polyglot = new PolyglotClient(server, port);
	        		}
            }else if(key.equals("SleepLength")){
            	sleep_length = Integer.valueOf(value);              
            }
          }
        }
      }
      
      if(false){
        System.out.println("Path: " + path);
        System.out.println();
      }
      
      ins.close();
    }catch(Exception e){}
  }
  
  /**
   * Write out available output formats.
   */
  public void writeOutputFormats()
  {
    TreeSet<String> outputs = polyglot.getOutputs();
    
    try{
      BufferedWriter outs = new  BufferedWriter(new FileWriter(path + "var/output_formats.txt"));
      
      for(Iterator<String> itr=outputs.iterator(); itr.hasNext();){
        outs.write(itr.next());
        outs.newLine();
      }
      
      outs.close();
    }catch(Exception e) {e.printStackTrace();}
  }
  
  /**
   * The starting point for the thread that monitors the uploads directory for tasks.
   */
  public void run()
  {
    File dir_uploads;
    File[] sub1_uploads;
    File[] sub2_uploads;
    String folder;
    String timestamp;
    Vector<String> tasks;
    String line;
    boolean PROCESS_FOLDER = false;
    boolean MOVED_FOLDER;
           
    FilenameFilter filename_filter = new FilenameFilter(){
      public boolean accept(File dir, String name){
          return !name.startsWith(".");
      }
    };
    
    if(VERBOSE) System.out.println("\nPolyglot Web Server is running ...\n");

    while(true){
    	//Read in the uploads directory
      dir_uploads = new File(path + "uploads");
      sub1_uploads = dir_uploads.listFiles(filename_filter);
      
      if(sub1_uploads != null){		
      	//Sort folders by date
      	Arrays.sort(sub1_uploads, new Comparator<File>(){
      		public int compare(File f1, File f2){
      			return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
      		}
      	});
      	
      	//Process the folders
        for(int i=0; i<sub1_uploads.length; i++){
          if(sub1_uploads[i].isDirectory()){
            sub2_uploads = sub1_uploads[i].listFiles(filename_filter);
            folder = sub1_uploads[i].getName();
            PROCESS_FOLDER = false;
            tasks = null;
            
            //Check if this folder is ready to be processed
            for(int j=0; j<sub2_uploads.length; j++){
              if(sub2_uploads[j].getName().equals("commit")){		//Load tasks
              	tasks = new Vector<String>();
              	
                try{
                  BufferedReader ins = new BufferedReader(new FileReader(path + "uploads/" + folder + "/tasks"));
                  
                  while((line=ins.readLine()) != null){
                    tasks.add(line);
                  }
                  
                  ins.close();
                }catch(Exception e) {e.printStackTrace();}
                              	
                PROCESS_FOLDER = true;
                break;
              }
            }
            
            if(PROCESS_FOLDER){            	
              Date date = new Date();
              if(VERBOSE) System.out.println("[" + date.toString() + "]");
              if(VERBOSE) System.out.println("Starting job from: " + folder);
              
              //Move folder from uploads to downloads
              int tmpi = folder.indexOf('_');
              
              if(tmpi >= 0){  //Folder already has a timestamp, re-use it (used by IOGraphWeightsTool)
                timestamp = folder.substring(tmpi+1);
                folder = folder.substring(0, tmpi);
              }else{
                timestamp = (new Long((new Date()).getTime())).toString();
              }
              
              MOVED_FOLDER = sub1_uploads[i].renameTo(new File(path + "downloads/" + folder + "_" + timestamp));
              
              //Update jobs log (must be done after folder has been moved for web page to use it!)
              try{
                BufferedWriter outs = new BufferedWriter(new FileWriter(path + "proc/jobs.txt", true));
                outs.write(folder + " " + timestamp);
                outs.newLine();
                outs.close();
              }catch(Exception e) {e.printStackTrace();}
              
              //The folder has been examined/moved to downloads, process each file in the list
              final FilenameFilter filename_filter_final = filename_filter;
              final String timestamp_final = timestamp;
              final boolean MOVED_FOLDER_final = MOVED_FOLDER;
              final String folder_final = folder;
              final Date date_final = date;
              final Vector<String> tasks_final = tasks;
              
          		new Thread(){
          			public void run(){
          				Scanner scanner;
          				String task;
          				String input_format;
          				String output_format;
          				String filename;
          				String filename_path;
          				String filename_root;
          				String filename_ext;
          				String filename_source;
          				String filename_target;
          				
		              if(MOVED_FOLDER_final){
		                String job_log = path + "downloads/" + folder_final + "_" + timestamp_final + "/" + "log";
		                Utility.println(job_log, "Job started on " + date_final.toString() + ":\n");
		
		                for(int j=0; j<tasks_final.size(); j++){  
		                	task = tasks_final.get(j);
		                	scanner = new Scanner(task);
		                	input_format = scanner.next();
		                	output_format = scanner.next();
		                	
		                  //Apply task to each valid input file
		                  File dir_downloads = new File(path + "downloads/" + folder_final + "_" + timestamp_final);
		                  File[] sub1_downloads = dir_downloads.listFiles(filename_filter_final);
		                  
		                  if(sub1_downloads != null){
		                    for(int k=0; k<sub1_downloads.length; k++){
		                      filename = sub1_downloads[k].getName();
		                      filename_path = path + "downloads/" + folder_final + "_" + timestamp_final + "/";
		                      filename_root = Utility.getFilenameName(filename);
		                      filename_ext = Utility.getFilenameExtension(filename);
		                                            
		                      if(!filename_ext.isEmpty() && (filename_ext.equals(input_format) || input_format.equals("*"))){
		                        filename_source = filename_path + filename;
		                        filename_target = filename_path + filename_root + "." + output_format;
		                        
		                        //Only convert if output doesn't already exist
		                        if(!Utility.exists(filename_target)){
		                        	if(VERBOSE) System.out.println("Converting: " + filename + " to " + output_format + "...");
				                      Utility.println(job_log, "Converting: " + filename + " to " + output_format + "...");

		                        	polyglot.convert(filename_source, filename_path, output_format);
		                        
		                        	if(Utility.exists(filename_target)){
		                        		if(VERBOSE) System.out.println("Conversion: " + filename + " to " + output_format + " [Success]");
		                        		Utility.println(job_log, "Conversion: " + filename + " to " + output_format + " [Success]");
		                        	}else{
		                        		if(VERBOSE) System.out.println("Conversion: " + filename + " to " + output_format + " [Failed]");
		                        		Utility.println(job_log, "Conversion: " + filename + " to " + output_format + " [Failed]");
		                        	}
		                        }
		                      }
		                    }
		                  }
		                }
		
		                Utility.println(job_log, "\nJob finished on " + new Date().toString() + ".");
		                
		                //Create file "complete", indicating the job is finished
		                try{
		                  BufferedWriter outs = new BufferedWriter(new FileWriter(path + "downloads/" + folder_final + "_" + timestamp_final + "/" + "complete"));
		                  outs.close();
		                }catch(Exception e) {e.printStackTrace();}
		              }
		              
		              if(VERBOSE) System.out.println("Ending job from: " + folder_final + "\n");
            		}
            	}.start();
            
              break;    //Reset sub1_uploads (i.e. reload the uploads directory)
            }
          }
        }
      }
      
      Utility.pause(sleep_length);	//Sleep for a bit to prevent spinning
    }
  }
  
  /**
   * A main to start the polyglot web server.
   * @param args not used
   */
  public static void main(String[] args)
  {
    new PolyglotWebInterface("PolyglotWebInterface.ini", true);
  }
}