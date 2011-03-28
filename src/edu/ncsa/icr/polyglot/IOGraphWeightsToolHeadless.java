package edu.ncsa.icr.polyglot;
import edu.ncsa.icr.polyglot.PolyglotAuxiliary.*;
import edu.ncsa.icr.SoftwareReuseAuxiliary.*;
import edu.ncsa.utility.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * A program to facilitate in the running of polyglot tests to fill in I/O-Graph edge weights.
 * @author Kenton McHenry
 */
public class IOGraphWeightsToolHeadless implements Runnable
{ 
	private PolyglotSteward polyglot = new PolyglotSteward();
  private IOGraph<Data,Application> iograph;
  private int steward_port = -1;

  private Vector<FileInfo> file_info = new Vector<FileInfo>();
  private LinkedList<FileInfo> working_set = new LinkedList<FileInfo>();
  private Vector<Pair<Vector<Conversion<Data,Application>>,Vector<Conversion<Data,Application>>>> jobs = new Vector<Pair<Vector<Conversion<Data,Application>>,Vector<Conversion<Data,Application>>>>();	//Made up of two polyglot jobs (to and from a format)
  private Vector<Integer> job_status = new Vector<Integer>();
  private Vector<ConversionResult> results = new Vector<ConversionResult>();
  private int working_set_size = 0;
  private String test_root = "";
  private String test = null;
  private boolean RUNNING_CONVERSIONS = false;
  private boolean MEASURING_QUALITY = false;
  
  private String test_path = "./";
  private int retry_level = 0;	//0=none, 1=all, 2=partials, 3=failures  
  private Boolean THREADED = false;

  private String data_path = "./";
  private String adapter = null;
  private String extractor = null;
  private String measure = null;
  private String weight_function = "x";
  private Double invalid_value = null;
  private String extension = "";
  private String csr_script_url = null;
  
  /**
   * Class constructor.
   * @param filename the INI filename to use
   */
  public IOGraphWeightsToolHeadless(String filename)
  {
    //Load data
    loadINI(filename);
    if(steward_port >= 0) Utility.pause(5000);	//Wait a bit for software server connections
    iograph = polyglot.getIOGraph();
    loadFolder(data_path, extension);
  }
  
  /**
	 * Load an initialization file.
	 * @param filename the name of the *.ini file
	 */
	public void loadINI(String filename)
	{
	  try{
	    BufferedReader ins = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
	    String line;
	    String key;
	    String value;
	    String server;
	    int port, tmpi;
	    
	    while((line=ins.readLine()) != null){
	      if(line.contains("=")){
	        key = line.substring(0, line.indexOf('='));
	        value = line.substring(line.indexOf('=')+1);
	        
	        if(key.charAt(0) != '#' && key.charAt(0) != ';'){
          	if(key.equals("StewardPort")){
	          	steward_port = Integer.valueOf(value);
	          	
	        		if(steward_port >= 0){
	        			polyglot.listen(steward_port);
	        		}
          	}else if(key.equals("SoftwareReuseServer")){
          		tmpi = value.lastIndexOf(':');
	        		
	        		if(tmpi != -1){
	        			server = value.substring(0, tmpi);
	        			port = Integer.valueOf(value.substring(tmpi+1));
	        			polyglot.add(server, port);
	        		}
	        	}else if(key.equals("DataPath")){
	            data_path = Utility.unixPath(value) + "/";
	          }else if(key.equals("Adapter")){
	            adapter = value;
	          }else if(key.equals("Extractor")){
	            extractor = value;
	          }else if(key.equals("Measure")){
	            measure = value;
	          }else if(key.equals("WeightFunction")){
	            weight_function = value;
	          }else if(key.equals("InvalidValue")){
	          	if(value.equals("null")){
	          		invalid_value = null;
	          	}else{
	          		invalid_value = Double.valueOf(value);
	          	}
	          }else if(key.equals("Extension")){
	            extension = value;
	          }else if(key.equals("TestPath")){
	            test_path = Utility.unixPath(value) + "/";
	            new File(test_path).mkdirs();
	          }else if(key.equals("RetryLevel")){
	          	retry_level = Integer.valueOf(value);
	          }else if(key.equals("CSR")){
	          	csr_script_url = value;
	        		if (!csr_script_url.endsWith("php/add/")) {
	        			if (!csr_script_url.endsWith("/")) {
	        				csr_script_url += "/php/add/";
	        			} else {
	        				csr_script_url += "php/add/";
	        			}
	        		}
	          }else if(key.equals("Threaded")){
	          	THREADED = Boolean.valueOf(value);
	          	polyglot.setApplicationFlexibility(1);
	          }
	        }
	      }
	    }
	    
	    ins.close();
	  }catch(Exception e){
	    e.printStackTrace();
	  }
	}

	/**
	 * Load all files of the given extension beneath the given folder.
	 * @param path the folder to search
	 * @param extension the extension of the files
	 */
	public void loadFolder(String path, String extension)
	{
	  Vector<String> files = new Vector<String>();
	  String filename;
	  String ext;
	  int tmpi;
	  
	  test_root = Utility.split(path, '/', false).lastElement() + "_" + extension;
	  working_set.clear();
	  
	  //Load model files in this folder
	  Stack<File> stk = new Stack<File>();
	  stk.push(new File(path));
	  
	  while(!stk.empty()){
	    File folder = stk.pop();
	    File[] folder_files = folder.listFiles();
	    
	    if(folder_files != null) {
	      for(int i=0; i<folder_files.length; i++){
	        if(folder_files[i].isDirectory()) stk.push(folder_files[i]);  //Check this directory aswell!
	        
	        //Check file extension
	        filename = folder_files[i].getName();
	        tmpi = filename.lastIndexOf('.');
	        
	        if(tmpi >= 0){
	          ext = filename.substring(tmpi + 1);
	          
	          if(ext.equals("gz") || ext.equals("zip")){
	            String tmp = filename.substring(0, tmpi);
	            tmpi = tmp.lastIndexOf('.');              
	            if(tmpi >= 0) ext = tmp.substring(tmpi + 1);
	          }
	        
	          if(ext.equals(extension)){
	            files.add(Utility.unixPath(folder_files[i].getAbsolutePath()));
	          }
	        } 
	      }
	    }
	  }
	  
	  Collections.sort(files, new FileNameComparer());
	  
	  //Create file information list and tree
	  file_info.clear();
	  
	  for(int i=0; i<files.size(); i++){
	    file_info.add(new FileInfo(files.get(i)));
	    working_set.add(file_info.get(i));
	  }
	  
	  working_set_size = working_set.size();
	  	  
	      
	  //Display information
	  System.out.println("--------------------------------------------------");
	  System.out.println("Data path: " + path);
	  System.out.println("Extension: " + extension);
	  
	  //Using any found tests, load/update jobs!
	  loadJobs();
	  
	  //Display currently selected number of files
	  System.out.println("--------------------------------------------------");
	  System.out.println("Files: " + working_set_size);
	}

	/**
	 * Use IOGraph to determine jobs but check if an old test exists as well and update
	 * it if necessary!
	 */
	public void loadJobs()
	{
	  TreeSet<String> range = iograph.getRangeStrings(extension);
	  Vector<Vector<Conversion<Data,Application>>> paths_a2b;
	  Vector<Vector<Conversion<Data,Application>>> paths_b2a;
	  String output_format;
	  
	  jobs.clear();
	  
	  for(Iterator<String> itr=range.iterator(); itr.hasNext();){
	  	output_format = itr.next();
	  	

		  
	    paths_a2b = iograph.getShortestConversionPaths(extension, output_format);
	    paths_b2a = iograph.getShortestConversionPaths(output_format, extension);
	    
	    for(int i=0; i<paths_a2b.size(); i++){
	      for(int j=0; j<paths_b2a.size(); j++){
	        jobs.add(new Pair<Vector<Conversion<Data,Application>>,Vector<Conversion<Data,Application>>>(paths_a2b.get(i), paths_b2a.get(j)));
	      }
	    }
	  }
	  
	  // FIXME
	  jobs.setSize(5);
	  
	  //Set job status
	  job_status.clear();
	  
	  for(int i=0; i<jobs.size(); i++){
	    job_status.add(-1);  
	  }
	    
	  for(int i=0; i<jobs.size(); i++){
	  	displayJob(i);
	  }
	}

	/**
	 * see IOGraph.getShortestConversionPathStrings()
	 * @param input_format
	 * @param output_format
	 * @return
	 */
	private Vector<String> getShortestConversionPathStringsAlias(String input_format, String output_format)
	{
		Vector<Vector<Conversion<Data,Application>>> conversions = iograph.getShortestConversionPaths(input_format, output_format);
		Vector<Conversion<Data,Application>> conversion;
		Vector<String> tasks = new Vector<String>();
		String task;

		for(int i=0; i<conversions.size(); i++){
			conversion = conversions.get(i);
			task = "";
			
			for(int j=0; j<conversion.size(); j++){
				task += conversion.get(j).edge.alias + " ";
				task += conversion.get(j).input.toString() + " ";
				task += conversion.get(j).output.toString();
				task += "\n";
			}
			
			tasks.add(task);
		}
	  
	  return tasks;
	}
	/**
	 * Highlight the parts of the job via HTML encoding.
	 * @param str the text version of the job
	 * @return the HTML version of the job
	 */
	public String HTMLEncodeJob(String str)
	{
	  String str_new = "";
	  Vector<String> lines = Utility.split(str, '\n', false);
	  String line, application, input, output;
	  int tmpi;
	  
	  for(int i=0; i<lines.size(); i++){
	  	line = lines.get(i);
	  	tmpi = line.lastIndexOf(' ');
	  	output = line.substring(tmpi+1);
	  	line = line.substring(0, tmpi);
	  	tmpi = line.lastIndexOf(' ');
	  	input = line.substring(tmpi+1);
	  	application = line.substring(0, tmpi);
	  	      
	    str_new += "<b>" + application + ": </b> ";
	    
	    if(i == 0){
	      str_new += "<b><font color=red>" + input + "</font></b> ";
	    }else{
	      str_new += "<b><font color=purple>" + input + "</font></b> ";
	    }
	    
	    str_new += "&rarr; ";
	    
	    if(i == lines.size()-1){
	      str_new += "<b><font color=blue>" + output + "</font></b> ";
	    }else{
	      str_new += "<b><font color=purple>" + output + "</font></b> ";
	    }
	    
	    str_new += "<br>";
	  }
	  
	  return str_new;
	}

	/**
	 * Display information about a specific job.
	 */
	public void displayJob(int idx)
	{	  
		String path = getConversionString(jobs.get(idx).first) + " => " + getConversionString(jobs.get(idx).second);
	  System.out.print("Job " + (idx+1) + " : ");
    switch (job_status.get(idx)) {
     case -1: System.out.println("Status   : No status " + path); break;
     case  0: System.out.println("Status   : Running   " + path); break;
     case  1: System.out.println("Status   : Complete  " + path); break;
     case  2: System.out.println("Status   : Partial   " + path); break;
     case  3: System.out.println("Status   : Failed    " + path); break;
    }	    
	}

	/**
	 * Check the contents of a job folder and return its status.
	 * @param job_id the ID of the job folder
	 * @return 1 if all ground truth files accounted for, 2 if partially completed, and 3 if empty
	 */
	public int jobFolderStatus(int job_id)
	{
	  File folder = new File(test_path + test + "/" + Integer.toString(job_id));
	  File[] folder_files = folder.listFiles();
	  TreeSet<String> set = new TreeSet<String>();
	  int total = 0;
	  int found = 0;
	  
	  if(folder_files != null){
	    for(int i=0; i<folder_files.length; i++){
	      set.add(folder_files[i].getName());
	    }
	  }
	  
	  folder = new File(test_path + test + "/0");
	  folder_files = folder.listFiles();
	  
	  if(folder_files != null){
	    for(int i=0; i<folder_files.length; i++){
	      total++;
	      if(set.contains(folder_files[i].getName())) found++;
	    }
	  }
	      
	  if(found == total){
	    return 1;
	  }else if(found > 0){
	    return 2;
	  }else{
	    return 3;
	  }
	}

	/**
	 * Given the comparison result from a job create entries in the result list
	 * @param job the job
	 * @param result the result of the comparison
	 * @return the entries for the output file
	 */
	public void addResult(Pair<Vector<Conversion<Data,Application>>,Vector<Conversion<Data,Application>>> job, double result, String meassure)
	{
		for(Conversion<Data,Application> conversion : job.first) {
	  	ConversionResult cr = new ConversionResult();
	  	cr.software = conversion.edge.alias;
	  	cr.input_format = conversion.input.toString();
	  	cr.output_format = conversion.output.toString();
	  	cr.meassure = meassure;
	  	cr.result = result;
	  	results.add(cr);
		}
		
		for(Conversion<Data,Application> conversion : job.second) {
	  	ConversionResult cr = new ConversionResult();
	  	cr.software = conversion.edge.alias;
	  	cr.input_format = conversion.input.toString();
	  	cr.output_format = conversion.output.toString();;
	  	cr.meassure = meassure;
	  	cr.result = result;
	  	results.add(cr);
		}
	}
  
  /**
   * Handle action events from menus and buttons.
   * @param e the action event
  public void actionPerformed(ActionEvent e)
  {
    if(e.getSource() == item_OPEN){
      JFileChooser fc = new JFileChooser(data_path + "..");
      fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

      if(fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION){
        data_path = Utility.unixPath(fc.getCurrentDirectory().getAbsolutePath()) + "/" + fc.getSelectedFile().getName() + "/";
        loadFolder(data_path, extension);
      }
    }else if(e.getSource() == item_QUIT){
      System.exit(0);
    }else if(e.getSource() == item_ABOUT){
      JFrame about = new JFrame("About");
      about.setSize(320, 200);
      about.setLayout(null);
      HTMLPanel hp = new HTMLPanel();
      hp.setLocation(0, 0);
      hp.setSize((int)about.getSize().getWidth()-7, (int)about.getSize().getHeight()-33);
      hp.setBackground(Color.white);
      hp.setHorizontalOffset(20);
      
      String tmp = "";
      tmp += "<center><h2>I/O-Graph Weights Tool</h2></center><br>";
      tmp += "</i><br><br>";
      //tmp += "<center><img src=file:www/polyglot/images/ncsa1.gif width=50></img></center><br>";
      tmp += "<center><img src=http://gladiator.ncsa.uiuc.edu/Images/logo/ncsa_logo_sm.gif width=50></center><br>";
      hp.setText(tmp);
      
      about.add(hp);
      about.setLocation(100, 100);
      about.setVisible(true);
    }else if(e.getSource() == new_button){
      if(RUNNING_CONVERSIONS || MEASURING_QUALITY){
        output_panel.addText("<br><b><font color=red>A test is running!</font></b>");
      }else{
      	createTests();
      }
    }else if(e.getSource() == run_button || e.getSource() == measure_button){
    	if(test == null){
        output_panel.addText("<br><b><font color=red>No tests found!</font></b>");
      }else if(RUNNING_CONVERSIONS){
        output_panel.addText("<br><b><font color=red>A test is running!</font></b>");
      }else if(MEASURING_QUALITY){
        output_panel.addText("<br><b><font color=red>Results are being measured!</font></b>");
      }else{
      	if(e.getSource() == run_button){
	        RUNNING_CONVERSIONS = true;
	      }else if(e.getSource() == measure_button){
	    		MEASURING_QUALITY = true;
	      }
      	
    		(new Thread(this)).start();   	
      }
    }else if(e.getSource() == view_button){
     	if(test == null){
        output_panel.addText("<br><b><font color=red>No tests found!</font></b>");
      }else{
        FilenameFilter filename_filter = new FilenameFilter(){
          public boolean accept(File dir, String name){
            return name.startsWith(test_root + "_" + extractor);
          }
        };
        
  	    File folder = new File(test_path + test);
  	    File[] folder_files = folder.listFiles(filename_filter);
  	   	String output_filename = null;
        
  	   	//Use the newest results
  	   	for(int i=0; i<folder_files.length; i++){  	   		
  	   		if(output_filename == null || folder_files[i].getName().compareTo(output_filename) > 0){
  	   			output_filename = folder_files[i].getName();
  	   		}
  	   	}

  	   	//Set and display the I/O-graph
      	iograph.loadEdgeWeights(test_path + test + "/" + output_filename, invalid_value);
      	iograph.transformEdgeWeights(weight_function);
      	IOGraphPanel<Data,Application> iograph_panel = new IOGraphPanel<Data,Application>(iograph, 2);
      	iograph_panel.setViewEdgeQuality(true);
      	
	      JFrame frame = new JFrame("IOGraph Viewer");
	      frame.add(iograph_panel.getAuxiliaryInterfacePane());
	      frame.pack();
	      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
	      frame.setVisible(true);
      }
    }else if(e.getSource() == log_button){
     	if(test == null){
        output_panel.addText("<br><b><font color=red>No tests found!</font></b>");
      }else{ 
	      Calendar calendar = new GregorianCalendar();
	      String log_filename;
	      
	      log_filename = "log";
	      log_filename += "." +  calendar.get(Calendar.YEAR);
	      log_filename += Utility.toString((calendar.get(Calendar.MONTH) + 1), 2);
	      log_filename += Utility.toString(calendar.get(Calendar.DAY_OF_MONTH), 2);
	      log_filename += Utility.toString(calendar.get(Calendar.HOUR_OF_DAY), 2);
	      log_filename += Utility.toString(calendar.get(Calendar.MINUTE), 2);
	      log_filename += Utility.toString(calendar.get(Calendar.SECOND), 2);
	      log_filename += ".html";
	      
	      Utility.save(test_path + test + "/" + log_filename, output_panel.getText());
      }
    }
  }
     */

  /**
   * Handle selection events from the side JTree.
   * @param e the tree selection event
  public void valueChanged(TreeSelectionEvent e)
  {
    TreePath[] treepaths = e.getPaths();  //Paths that have been added or removed from the selection
    DefaultMutableTreeNode node;
    FileInfo fi;
    
    //Extract changes from JTree
    for(int i=0; i<treepaths.length; i++){
      if(treepaths[i].getPathCount() > 1){
        node = (DefaultMutableTreeNode)treepaths[i].getPathComponent(1);
        fi = (FileInfo)node.getUserObject();
        fi.FLAG = !fi.FLAG;
        
        if(fi.FLAG){
          working_set.remove(fi);
        }else{
          working_set.add(fi);
        }
      }
    }
    
    if(working_set_size != working_set.size()){
      working_set_size = working_set.size();
      output_panel.addText("<br><b>Files: </b>" + working_set_size);
    }

    repaint();
  }
     */
	
  public void createTests()
  {
    test = test_root;
    test += "." + new SimpleDateFormat("yyyyMMddhhmmss").format(new Date());
    
    new File(test_path + test).mkdir();
    new File(test_path + test + "/0").mkdir();
    
    //Reset the jobs status
    job_status.clear();
    
    for(int i=0; i<jobs.size(); i++){
      job_status.add(-1);  
    }
    
    loadJobs();

    System.out.println("--------------------------------------------------");
    System.out.println("New test created: " + test);
  }

  /**
	 * Run the current test.
	 */
	public void run()
	{	    
		long t0, t1;
	  double dt = 0;
	  
		if(RUNNING_CONVERSIONS){
	    System.out.println();
	    System.out.println("--------------------------------------------------");
	    System.out.println("Executing conversions...");
	    
	    //Backup old jobs map
	    if(Utility.exists(test_path + test + "/jobs.txt")){
	      int i = 0;
	      
	      while(Utility.exists(test_path + test + "/jobs." + i + ".txt")) i++;
	      Utility.copyFile(test_path + test + "/jobs.txt", test_path + test + "/jobs." + i + ".txt");
	    }
	    
	    //Write folder/job map
	    try{
	      FileWriter outs = new FileWriter(test_path + test + "/jobs.txt");
	      
	      for(int i=0; i<jobs.size(); i++){
	        outs.write("#" + (i+1) + "\n" + jobs.get(i).first + "+\n" + jobs.get(i).second + "\n");
	      }
	      
	      outs.close();
	    }catch(Exception exception){}
	    
	    //Update ground truth directory
	    String foldername = test_path + test + "/0";
	    File folder = new File(foldername);
	    File[] folder_files = folder.listFiles();
	    TreeSet<String> set = new TreeSet<String>();
	    FileInfo fi;
	    int count_old = 0;
	    int count_new = 0;
	    
	    if(folder_files != null){
	      for(int i=0; i<folder_files.length; i++){
	        set.add(folder_files[i].getName());
	        count_old++;
	      }
	    }
	    
	    for(Iterator<FileInfo> itr=working_set.iterator(); itr.hasNext();){
	      fi = itr.next();
	      
	      if(!set.contains(fi.filename)){
	        Utility.copyFile(fi.absolutename, foldername + "/" + fi.filename);
	        count_new++;
	      }
	    }
	    
	    //System.out.println("--------------------------------------------------");
	    //System.out.println("Ground truth files: " + (count_old+count_new) + " (" + count_new + " new)");
	    
	    //Update job status based on a possibly modified ground truth directory
	    for(int i=0; i<job_status.size(); i++){
	      if(Utility.exists(test_path + test + "/" + (i+1))){
	        job_status.set(i, jobFolderStatus(i+1));
	      }
	    }
	    
	    //Set starting time
	    t0 = (new Date()).getTime();
	  
	    //Run the test
	    if(!THREADED){
	    	Vector<Conversion<Data,Application>> conversions;
	    	String job_path, tmp_path; 
		  
		    for(int i=0; i<jobs.size(); i++){
		      //-1=No status, 0=Running, 1=Complete, 2=Partially completed, 3=Failed
		      if(job_status.get(i)==-1 || retry_level==1 || (job_status.get(i)==2 && retry_level==2) || (job_status.get(i)==3 && retry_level==3)){
		        job_status.set(i, 0);
		        
		        //Set job sub-folders and create if they don't exist
		        job_path = test_path + test + "/" + (i+1) + "/";
		        new File(job_path).mkdir();
		        
		        tmp_path = test_path + test + "/" + (i+1) + "/tmp/";
		        new File(tmp_path).mkdir();
		        	 
		        //Convert files to target format
		        //System.out.println("<br><b>Performing Job-" + (i+1) + "A </b> (" + working_set.size() + " files) ");
		        
		        for(Iterator<FileInfo> itr=working_set.iterator(); itr.hasNext();){
	          	conversions = jobs.get(i).first;
	          	polyglot.convert(test_path + test + "/0/" + itr.next().filename, tmp_path, conversions);
	          	//System.out.println(".");
		        }
		        
		        //Convert files back to source format
		        folder = new File(tmp_path);
		        folder_files = folder.listFiles();
		        
		        //System.out.println("<br><b>Performing Job-" + (i+1) + "B </b> (" + folder_files.length + " files) ");
		        
		        if(folder_files != null){
		          for(int j=0; j<folder_files.length; j++){
		          	conversions = jobs.get(i).second;
		          	polyglot.convert(folder_files[j].getAbsolutePath(), job_path, conversions);
		          	//System.out.println(".");
		          }
		        } 
		        
		        //Display results
		        folder = new File(job_path);
		        folder_files = folder.listFiles();
		        
		        //System.out.println("<br><b>Completed Job-" + (i+1) + "</b> (" + (folder_files.length-1) + " files)<br>");
		        job_status.set(i, jobFolderStatus(i+1));
		        
		        displayJob(i);
		      }
		    }
			}else{
		    Vector<Thread> threads = new Vector<Thread>();
		    Thread thread;
		    boolean ACTIVE_THREADS = false;
		    
		    for(int i=0; i<jobs.size(); i++){
		      //-1=No status, 0=Running, 1=Complete, 2=Partially completed, 3=Failed
		      if(job_status.get(i)==-1 || retry_level==1 || (job_status.get(i)==2 && retry_level==2) || (job_status.get(i)==3 && retry_level==3)){
		        job_status.set(i, 0);
		        
		        //Set job sub-folders and create if they don't exist
		        final String job_path_final = test_path + test + "/" + (i+1) + "/";
		        new File(job_path_final).mkdir();
		        
		        final String tmp_path_final = test_path + test + "/" + (i+1) + "/tmp/";
		        new File(tmp_path_final).mkdir();
		        	 
		        //Start a thread to perform the conversion
	        	final int i_final = i;
	        	
	        	thread = new Thread(){
	        		public void run()
	        		{
	        			PolyglotSteward thread_polyglot = new PolyglotSteward(polyglot);	//Create a new polyglot instance (one with unique ICR session id's!)
	        		  IOGraph<Data,Application> thread_iograph = thread_polyglot.getIOGraph();
	        			Vector<Conversion<Data,Application>> conversions;

	  		        //Convert files to target format
	  		        //System.out.println("<br><b>Performing Job-" + (i_final+1) + " </b> (" + working_set.size() + " files)");

	  		        for(Iterator<FileInfo> itr=working_set.iterator(); itr.hasNext();){
	  	          	conversions = jobs.get(i_final).first;
	  	          	thread_polyglot.convert(test_path + test + "/0/" + itr.next().filename, tmp_path_final, conversions);
	  		        }
	  		        
	  		        //Convert files back to source format
	  		        File folder = new File(tmp_path_final);
	  		        File[] folder_files = folder.listFiles();
	  		        	  		        
	  		        if(folder_files != null){
	  		          for(int j=0; j<folder_files.length; j++){
	  		          	conversions = jobs.get(i_final).second;
	  		          	thread_polyglot.convert(folder_files[j].getAbsolutePath(), job_path_final, conversions);
	  		          }
	  		        }
	  		        
	  		        thread_polyglot.close();
	        		}
	        	};
	        	
	        	thread.start();
	        	threads.add(thread);
	        	ACTIVE_THREADS = true;
		      }
		    }
		    
		    //Wait for threads to finish
		    while(ACTIVE_THREADS){
		    	ACTIVE_THREADS = false;
		    	
		    	for(int i=0; i<threads.size(); i++){
		    		if(threads.get(i) != null){	    			
		    			if(!threads.get(i).isAlive()){
		    				threads.set(i, null);
				        job_status.set(i, jobFolderStatus(i+1));
				        displayJob(i);
		    			}else{
		    				ACTIVE_THREADS = true;
		    			}
		    		}
		    	}
		    }
		    
		    //System.out.println("<br>");			
			}
	    
	    //Set end time
	    t1 = (new Date()).getTime();
	    dt = (t1-t0) / 1000.0;
	          	    
	    //displayJobs();
	    System.out.println("--------------------------------------------------");
	    System.out.println("Conversions completed in " + dt + " seconds.");
	    RUNNING_CONVERSIONS = false;
	  }else if(MEASURING_QUALITY){
	  	String path0 = test_path + test + "/0/";
	  	String pathi;
	  	File folder;
	  	File[] folder_files;
	  	String filename0, filenamei;
	  	Double result;
	  	
	    System.out.println();
	    System.out.println("--------------------------------------------------");
	    System.out.println("Beginning measurments...");

	    // clear results
		  results.clear();

	    //Compare files
	    folder = new File(path0);
	    folder_files = folder.listFiles();
	    
	    if(folder_files != null){
		    //Set starting time
		    t0 = (new Date()).getTime();	    	
	    	
	      for(int i=0; i<folder_files.length; i++){
	    		//System.out.println("<b>" + (i+1) + ") </b><i>" + folder_files[i].getName() + "</i>: <b>");
	      	filename0 = path0 + folder_files[i].getName();
	      	
	      	try{	
	        	for(int j=0; j<jobs.size(); j++){
	        		//-1=No status, 0=Running, 1=Complete, 2=Partially completed, 3=Failed
	        		if(job_status.get(j) == 1 || job_status.get(j) == 2){
	        			pathi = test_path + test + "/" + (j+1) + "/";
	        			filenamei = pathi + folder_files[i].getName();
	        			
	        			if(Utility.exists(filenamei)){
	        				if(extractor != null){
	        					result = VersusCompare.compare(filename0, filenamei, adapter, extractor, measure);
	        				}else{
	        					result = 1.0;	//Result is based on the existence of the output file
	        				}
	        				
	        				addResult(jobs.get(j), result, measure);
	        			  System.out.println("Job " + (j+1) + " Result : " + result);
		            	//System.out.println(".");
	        			}else{	//Output file doesn't exist
	        				addResult(jobs.get(j), -1.0, measure);
	        			  System.out.println("Job " + (j+1) + " Result : [no output]");
	        				//System.out.println("x");
	        			}
	        		}else{		//Failed jobs
        				addResult(jobs.get(j), -2.0, measure);
        			  System.out.println("Job " + (j+1) + " Result : [job failed]");
	      				//System.out.println("x");
	        		}
	        	}
	      	}catch(Exception e){
	      		System.out.println("TESTS FAILED!");
	      		e.printStackTrace();
	      	}
	      }
	      
		    //Set end time
		    t1 = (new Date()).getTime();
		    dt = (t1-t0) / 1000.0;
	    }
	    
	    System.out.println("--------------------------------------------------");
	  	System.out.println("Measurments completed in " + dt + " seconds.");  	
	  	
	  	MEASURING_QUALITY = false;
	  }
	}
	
	public void printResults() {
		Collections.sort(results);
		
  	String output_data = "";
  	String output_filename;

  	System.out.println();
    System.out.println("--------------------------------------------------");
    System.out.println("Results");
		for(ConversionResult cr : results) {
			System.out.println(cr.toString());
    	output_data += cr.toString() + "\n";
		}

  	//Set the filename and save results
    output_filename = test_root + "_" + extractor;
    output_filename += new SimpleDateFormat("yyyyMMddhhmmss").format(new Date());
    output_filename += ".txt";
  	
    Utility.save(test_path + test + "/" + output_filename, output_data);
	}
	
	public void uploadResults() {	
		if (csr_script_url == null) {
			return;
		}
		
  	String output_data = "";
  	String output_filename;

  	System.out.println();
    System.out.println("--------------------------------------------------");
    System.out.println("Results");
		for(ConversionResult cr : results) {
			System.out.println(cr.toString());
    	output_data += cr.toString() + "\n";
		}

  	//Set the filename and save results
    output_filename = test_root + "_" + extractor;
    output_filename += new SimpleDateFormat("yyyyMMddhhmmss").format(new Date());
    output_filename += ".txt";
  	
    Utility.save(test_path + test + "/" + output_filename, output_data);
	}
	
	private static String getConversionString(Vector<Conversion<Data,Application>> conversion) {
		String task = "";
		for(int j=0; j<conversion.size(); j++){
			task += "[" + conversion.get(j).edge.alias + " ";
			task += conversion.get(j).input.toString() + " ";
			task += conversion.get(j).output.toString() + "] ";
		}
		return task;
	}


	class ConversionResult implements Comparable<ConversionResult> {
		public String software;
		public String input_format;
		public String output_format;
		public String meassure;
		public double result;
		
		public int compareTo(ConversionResult o)
		{
			if (!software.equals(o.software)) {
				return software.compareTo(o.software);
			}
			if (!input_format.equals(o.input_format)) {
				return input_format.compareTo(o.input_format);
			}
			if (!output_format.equals(o.output_format)) {
				return output_format.compareTo(o.output_format);
			}
			return Double.compare(result, o.result);
		}
		
		@Override
		public String toString()
		{
			if (result >= 0) {
				return software + "\t" + input_format + "\t" + output_format + "\t" + result;
			} else {
				return software + "\t" + input_format + "\t" + output_format + "\t0";
			}
		}
	}
	
	/**
   * Start the I/O-Graph weights tool.
   * @param args not used
   */
  public static void main(String args[])
  {
    IOGraphWeightsToolHeadless iograph_wt = new IOGraphWeightsToolHeadless("IOGraphWeightsToolHeadless.ini");
    
    // load all test folders
    for(String dir : args) {
    	iograph_wt.loadFolder(dir, iograph_wt.extension);
    }

    // create test cases first
    iograph_wt.createTests();
    
    // run conversions
    iograph_wt.MEASURING_QUALITY = false;
    iograph_wt.RUNNING_CONVERSIONS = true;
    iograph_wt.run();
    
    // compute measurements
    iograph_wt.MEASURING_QUALITY = true;
    iograph_wt.RUNNING_CONVERSIONS = false;
    iograph_wt.run();  
    
    // normalize weights
    
    // print/upload results
    iograph_wt.printResults();
    iograph_wt.uploadResults();
    
    // all done
    System.exit(0);
  }
}