package edu.ncsa.icr.polyglot;
import edu.ncsa.utility.*;
import java.io.*;
import java.nio.channels.*;
import java.util.*;

/**
 * A background process that monitors a folder for conversion tasks.  When a folder
 * is created in the upload location and contains a file "commit" this process will
 * move the folder to the download location and execute the tasks dictated in the "tasks"
 * file.  When completed the daemon will create a file "complete" within the folder.
 * @author Kenton McHenry
 */
public class PolyglotWebServer
{
  private String path = ".";  
  private Vector<String> exclusions = new Vector<String>();
  private int max_operation_time = 10000;    //In milliseconds
  private int max_operation_attempts = 2;    //Number of times to retry operation
  private int sleep_length = 1000;  				 //How long to sleep before checking for more jobs (in milliseconds)
  private FileChannel file_channel = null;
  private FileLock lock = null;
  private Boolean USE_LOCK_FILE = true;
  private Boolean USE_EXCLUSIONS = false;
  private int id = -1;
  private FilenameFilter filename_filter = null;
  
  /**
   * Class constructor.
   */
  public PolyglotWebServer()
  {
    filename_filter = new FilenameFilter(){
      public boolean accept(File dir, String name){
          return !name.startsWith(".");
      }
    };
  }
  
  /**
   * Load an initialization file.
   *  @param filename the INI file
   */
  public void loadINI(String filename)
  {
    try{
      BufferedReader ins = new BufferedReader(new FileReader(filename));
      String line;
      String key;
      String value;
      
      while((line=ins.readLine()) != null){
        if(line.contains("=")){
          key = line.substring(0, line.indexOf('='));
          value = line.substring(line.indexOf('=')+1);
          
          if(key.charAt(0) != '#'){
            if(key.equals("PolyglotPath")){
              path = value + "\\";
            }else if(key.equals("ConversionTimeout")){
              max_operation_time = Integer.valueOf(value);
            }else if(key.equals("ConversionAttempts")){
              max_operation_attempts = Integer.valueOf(value);
            }else if(key.equals("SleepLength")){
            	sleep_length = Integer.valueOf(value);              
            }
          }
        }
      }
      
      if(false){
        System.out.println("Path: " + path);
        System.out.println("Timeout: " + max_operation_time);
        System.out.println("Attempts: " + max_operation_attempts);
        System.out.println();
      }
      
      ins.close();
    }catch(Exception e){}
  }
  
  /**
   * Initialize the Polyglot service by examining AutoHotKey scripts in the AHK folder and constructing
   * the IO data required by IOGraph to suggests the tasks that this daemon will later execute.  In addition
   * this method will create a list of formats that can be converted to.
   */
  public void initialize()
  {
    TreeSet<String> set;
    String type;
    
    /*
    //Write content_types.txt
    try{
      BufferedWriter outs = new  BufferedWriter(new FileWriter(path + "var/content_types.txt"));
      Iterator<String> itr1 = content_types.keySet().iterator();
      Iterator<String> itr2;
      
      while(itr1.hasNext()){
      	type = itr1.next();
      	outs.write(type);
      	outs.newLine();
      	
      	set = content_types.get(type);
      	itr2 = set.iterator();
      	
      	while(itr2.hasNext()){
      		outs.write(itr2.next() + " ");
      	}
      	
        if(!set.isEmpty() && itr1.hasNext()) outs.newLine();
        outs.newLine();
      }
      
      outs.close();
    }catch(Exception e) {e.printStackTrace();}
    
    //Write output_formats.txt
    Object[] arr = output_formats.toArray();
    
    try{
      BufferedWriter outs = new  BufferedWriter(new FileWriter(path + "var/output_formats.txt"));
      
      for(int i=0; i<arr.length; i++){
        outs.write((String)arr[i]);
        outs.newLine();
      }
      
      outs.close();
    }catch(Exception e) {e.printStackTrace();}
    */
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
    String filename;
    String filename_root;
    String filename_ext;
    String filename_source;
    String filename_target;
    String input_format = "";
    String output_format = "";
    Vector<String> tasks = new Vector<String>();
    String line;
    boolean PROCESS_FOLDER = false;
    boolean MOVED_FOLDER;
           
    System.out.println("\nPolyglot Web Server is running ...\n");

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
            
            //Check if this folder is ready to be processed
            for(int j=0; j<sub2_uploads.length; j++){
              if(sub2_uploads[j].getName().equals("commit")){
              	PROCESS_FOLDER = true;
                
                //Load tasks
              	tasks.clear();
              	
                try{
                  BufferedReader ins = new BufferedReader(new FileReader(path + "uploads\\" + folder + "\\tasks"));
                  
                  while((line=ins.readLine()) != null){
                    tasks.add(line);
                  }
                  
                  ins.close();
                }catch(Exception e) {e.printStackTrace();}
              }
            }
            
            if(PROCESS_FOLDER){            	
              Date date = new Date();
              System.out.println("[" + date.toString() + "]");
              System.out.println("Starting job from: " + folder);
              
              //Move folder from uploads to downloads
              int tmpi = folder.indexOf('_');
              
              if(tmpi >= 0){  //Folder already has a timestamp, re-use it (used by IOGraphWeightsTool)
                timestamp = folder.substring(tmpi+1);
                folder = folder.substring(0, tmpi);
              }else{
                timestamp = (new Long((new Date()).getTime())).toString();
              }
              
              MOVED_FOLDER = sub1_uploads[i].renameTo(new File(path + "downloads\\" + folder + "_" + timestamp));
              
              //Update jobs log (must be done after folder has been moved for web page to use it!)
              try{
                BufferedWriter outs = new BufferedWriter(new FileWriter(path + "proc/jobs.txt", true));
                outs.write(folder + " " + timestamp);
                outs.newLine();
                outs.close();
              }catch(Exception e) {e.printStackTrace();}
              
              //The folder has been examined/moved to downloads, process each file in the list
              if(MOVED_FOLDER){
                String job_log = path + "downloads\\" + folder + "_" + timestamp + "\\" + "log";
                Utility.println(job_log, "Job started on " + date.toString() + ":\n");

                for(int j=0; j<tasks.size(); j++){                	
                  //Apply task to each valid input file
                  File dir_downloads = new File(path + "downloads\\" + folder + "_" + timestamp);
                  File[] sub1_downloads = dir_downloads.listFiles(filename_filter);
                  
                  if(sub1_downloads != null) {
                    for(int k=0; k<sub1_downloads.length; k++){
                      filename = sub1_downloads[k].getName();
                      tmpi = filename.lastIndexOf('.');
                      
                      if(tmpi >= 0){
	                      filename_root = filename.substring(0, tmpi);
	                      filename_ext = filename.substring(filename.lastIndexOf('.')+1, filename.length());
	                      
	                      if(filename_ext.equals(input_format)){
	                        filename_source = path + "downloads\\" + folder + "_" + timestamp + "\\" + filename;
	                        filename_target = path + "downloads\\" + folder + "_" + timestamp + "\\" + filename_root + "." + output_format;
	                        
	                        //Only convert if output doesn't already exist
	                        if(!Utility.exists(filename_target)){
	        
	                        }
	                      }
                    	}
                    }
                  }
                }

                date = new Date();
                Utility.println(job_log, "\nJob finished on " + date.toString() + ".");
                
                //Create file "complete", indicating the job is finished
                try{
                  BufferedWriter outs = new BufferedWriter(new FileWriter(path + "downloads\\" + folder + "_" + timestamp + "\\" + "complete"));
                  outs.close();
                }catch(Exception e) {e.printStackTrace();}
              }
              
              System.out.println("Ending job from: " + folder + "\n");
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
    PolyglotWebServer pws = new PolyglotWebServer();
    pws.loadINI("PolyglotWebServer.ini");
    pws.initialize();
    pws.run();
  }
}