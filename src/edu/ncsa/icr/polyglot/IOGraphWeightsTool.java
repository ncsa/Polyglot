package edu.ncsa.icr.polyglot;
import edu.ncsa.icr.polyglot.PolyglotAuxiliary.*;
import edu.ncsa.icr.ICRAuxiliary.*;
import edu.ncsa.utility.*;
import edu.illinois.ncsa.versus.engine.impl.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.tree.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * A program to facilitate in the running of polyglot tests to fill in I/O-Graph edge weights.
 * @author Kenton McHenry
 */
public class IOGraphWeightsTool extends JPanel implements ActionListener, TreeSelectionListener, Runnable
{ 
	private PolyglotSteward polyglot = new PolyglotSteward();
  private IOGraph<Data,Application> iograph;
  private int steward_port = -1;

  private int window_width = 700;
  private int window_height = 800;
  private int top_height = (int)Math.round(window_height * 0.75);
  private int bottom_height = window_height - top_height;
  private int file_panel_width = (int)Math.round(window_width * 0.45);
  private int job_panel_width = window_width - file_panel_width;
  private int output_panel_width = (int)Math.round(window_width * 0.75);
  private int button_panel_width = window_width - output_panel_width;
  private int component_width = 140;
  private int component_height = 25;
  private int component_x = 5;
  private int component_y = component_height;
  private int gap = 4;
  
  private JFrame frame;
  private JSplitPane splitpane_tb;
  private JSplitPane splitpane_tlr;
  private JSplitPane splitpane_blr;
  private JPanel file_panel;
  private HTMLPanel job_panel;
  private JPanel button_panel;  
  private HTMLPanel output_panel; 
  private JScrollPane scrollpane = null;
  private JTree tree;
  private JButton new_button;
  private JButton run_button;
  private JButton measure_button;
  private JButton view_button;
  private JButton log_button;
  private JMenuBar menubar;
  private JMenuItem item_OPEN;
  private JMenuItem item_QUIT;
  private JMenuItem item_ABOUT;
    
  private FilenameFilter filename_filter;
  private Vector<FileInfo> file_info = new Vector<FileInfo>();
  private LinkedList<FileInfo> working_set = new LinkedList<FileInfo>();
  private Vector<Pair<String,String>> jobs = new Vector<Pair<String,String>>();	//Made up of two polyglot jobs (to and from a format)
  private Vector<Integer> job_status = new Vector<Integer>();
  private int working_set_size = 0;
  private String test_root = "";
  private String test = null;
  private boolean RUNNING_CONVERSIONS = false;
  private boolean MEASURING_QUALITY = false;
  
  private String test_path = "./";
  private int retry_level = 0;	//0=none, 1=all, 2=partials, 3=failures  
  private Boolean THREADED = false;
	private boolean POLYGLOT_MONITOR = false;

  private String data_path = "./";
  private String adapter = null;
  private String extractor = null;
  private String measure = null;
  private String weight_function = "x";
  private Double invalid_value = null;
  private String extension = "";
  
  /**
   * Class constructor.
   * @param filename the INI filename to use
   */
  public IOGraphWeightsTool(String filename)
  {
    setBackground(Color.white);
    setSize(window_width, window_height);
    
    frame = new JFrame();
    frame.setSize(window_width, window_height + 35);
    
    file_panel = new JPanel();
    file_panel.setBackground(this.getBackground());
    file_panel.setSize(file_panel_width, top_height);
    file_panel.setLayout(new BorderLayout());
    
    job_panel = new HTMLPanel();
    job_panel.setPreferredSize(new Dimension(job_panel_width, top_height));
    job_panel.setLeftOffset(10);
    
    output_panel = new HTMLPanel();
    output_panel.setPreferredSize(new Dimension(output_panel_width, bottom_height-5));
    output_panel.setTopLeftOffset(10, 10);
    output_panel.setText("<i><b><u>Session started</u>:</b> " + (new Date()).toString() + "</i>");
    
    button_panel = new JPanel();
    button_panel.setBackground(this.getBackground());
    button_panel.setPreferredSize(new Dimension(button_panel_width-10, bottom_height-5));
    button_panel.setLayout(null);
    
    new_button = new JButton("New Test");
    new_button.addActionListener(this);
    new_button.setSize(component_width, component_height);
    new_button.setLocation(component_x, component_y);
    component_y += 1.1*component_height;

    run_button = new JButton("Run Conversions");
    run_button.addActionListener(this);
    run_button.setSize(component_width, component_height);
    run_button.setLocation(component_x, component_y);
    component_y += 1.1*component_height; 
    
    measure_button = new JButton("Measure Quality");
    measure_button.addActionListener(this);
    measure_button.setSize(component_width, component_height);
    measure_button.setLocation(component_x, component_y);
    component_y += 1.1*component_height;
    
    view_button = new JButton("View Results");
    view_button.addActionListener(this);
    view_button.setSize(component_width, component_height);
    view_button.setLocation(component_x, component_y);
    component_y += 1.1*component_height;
    
    log_button = new JButton("Write Log");
    log_button.addActionListener(this);
    log_button.setSize(component_width, component_height);
    log_button.setLocation(component_x, component_y);
    
    button_panel.add(new_button);
    button_panel.add(run_button);
    button_panel.add(measure_button);
    button_panel.add(view_button);
    button_panel.add(log_button);

    splitpane_tlr = new JSplitPane();
    splitpane_tlr.setBorder(new EmptyBorder(0, 0, 0, 0));
    splitpane_tlr.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
    splitpane_tlr.setDividerSize(0);
    splitpane_tlr.setLeftComponent(file_panel);
    splitpane_tlr.setRightComponent(job_panel);
    
    splitpane_blr = new JSplitPane();
    splitpane_blr.setBorder(new EmptyBorder(0, 0, 0, 0));
    splitpane_blr.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
    splitpane_blr.setDividerSize(0);
    splitpane_blr.setLeftComponent(button_panel);
    splitpane_blr.setRightComponent(output_panel);
    
    splitpane_tb = new JSplitPane();  
    splitpane_tb.setBorder(new EmptyBorder(0, 0, 0, 0));
    splitpane_tb.setOrientation(JSplitPane.VERTICAL_SPLIT);
    splitpane_tb.setDividerSize(1);
    splitpane_tb.setTopComponent(splitpane_tlr);
    splitpane_tb.setBottomComponent(splitpane_blr);
    
    add(splitpane_tb);
     
    //Setup menus
    menubar = new JMenuBar();
    JMenu menu;
    
    menu = new JMenu("File");
    item_OPEN = new JMenuItem("Open"); item_OPEN.addActionListener(this); menu.add(item_OPEN);
    item_QUIT = new JMenuItem("Quit"); item_QUIT.addActionListener(this); menu.add(item_QUIT);
    menubar.add(menu);
    menu = new JMenu("Help");
    item_ABOUT = new JMenuItem("About"); item_ABOUT.addActionListener(this); menu.add(item_ABOUT);
    menubar.add(menu);
    
    updateUI();
    revalidate();
    
    //Miscallaneous
    filename_filter = new FilenameFilter(){
      public boolean accept(File dir, String name){
        return !name.startsWith(".");
      }
    };
    
    //Load data
    loadINI(filename);
    if(steward_port >= 0) Utility.pause(5000);	//Wait a bit for software server connections
    iograph = polyglot.getIOGraph();
    loadFolder(data_path, extension);
    
		if(POLYGLOT_MONITOR) new PolyglotMonitor(polyglot).createFrame();
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
          	}else if(key.equals("SoftwareServer")){
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
	          }else if(key.equals("RetryLevel")){
	          	retry_level = Integer.valueOf(value);
	          }else if(key.equals("Threaded")){
	          	THREADED = Boolean.valueOf(value);
	          	polyglot.setApplicationFlexibility(1);
	          }else if(key.equals("PolyglotMonitor")){
	          	POLYGLOT_MONITOR = Boolean.valueOf(value);
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
	  DefaultMutableTreeNode root = new DefaultMutableTreeNode("Files");
	  DefaultMutableTreeNode child;    
	  String filename;
	  String ext;
	  int tmpi;
	  
	  test = null;
	  test_root = Utility.split(path, '/', false).lastElement() + "_" + extension;
	  working_set.clear();
	  
	  //Load model files in this folder
	  Stack<File> stk = new Stack<File>();
	  stk.push(new File(path));
	  
	  while(!stk.empty()){
	    File folder = stk.pop();
	    File[] folder_files = folder.listFiles(filename_filter);
	    
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
	    child = new DefaultMutableTreeNode(file_info.get(i));
	    root.add(child);
	  }
	  
	  working_set_size = working_set.size();
	  
	  //Update the tree
	  if(scrollpane != null) file_panel.remove(scrollpane);
	  
	  tree = new JTree(root);
	  tree.addTreeSelectionListener(this);
	  tree.setBackground(file_panel.getBackground());
	
	  scrollpane = new JScrollPane(tree);
	  scrollpane.setBackground(file_panel.getBackground());
	  scrollpane.setBorder(new EmptyBorder(0, 0, 0, 0));
	  scrollpane.setLocation(12, gap);
	  scrollpane.setPreferredSize(new Dimension(window_width-10, top_height-2*gap));
	  file_panel.add(scrollpane);
	  
	  if(frame != null) frame.setTitle("I/O-Graph Weights Tool [" + path + "] - " + file_info.size() + " files");
	  
	  tree.revalidate();
	  updateUI();
	  
	  //Search for the newest previous test
	  File folder = new File(test_path);
	  File[] folder_files = folder.listFiles(filename_filter);
	  long max_ts = 0;
	  long tmpl;
	  
	  if(folder_files != null) {
	    for(int i=0; i<folder_files.length; i++){
	      if(folder_files[i].isDirectory()){
	        filename = folder_files[i].getName();
	        tmpi = filename.lastIndexOf('.');
	          
	        if(tmpi >= 0){
	          ext = filename.substring(tmpi + 1);
	          filename = filename.substring(0, tmpi);
	          
	          if(filename.equals(test_root)){
	            tmpl = Long.valueOf(ext);
	            if(tmpl > max_ts){
	              max_ts = tmpl;
	              test = test_root + "." + ext;
	            }
	          }
	        }
	      }
	    }
	  }
	      
	  //Display information
	  output_panel.putText("<br><br><b>Data path: </b>" + path);
	  output_panel.putText("<br><b>Extension: </b>" + extension);
	  
	  if(test == null){
	    output_panel.putText("<br><b>Test: <font color=red>None Found!</font></b>");
	  }else{
	    output_panel.putText("<br><b>Test: </b>" + test);
	  }
	  
	  output_panel.flush();
	  
	  //Using any found tests, load/update jobs!
	  loadJobs();
	  
	  //Display currently selected number of files
	  output_panel.addText("<br><b>Files: </b>" + working_set_size);
	}

	/**
	 * Use IOGraph to determine jobs but check if an old test exists as well and update
	 * it if necessary!
	 */
	public void loadJobs()
	{
	  TreeSet<String> range = iograph.getRangeStrings(extension);
	  Vector<String> paths_a2b;
	  Vector<String> paths_b2a;
	  String output_format;
	  
	  jobs.clear();
	  
	  for(Iterator<String> itr=range.iterator(); itr.hasNext();){
	  	output_format = itr.next();
	    paths_a2b = iograph.getShortestConversionPathStrings(extension, output_format);
	    paths_b2a = iograph.getShortestConversionPathStrings(output_format, extension);
	    
	    for(int i=0; i<paths_a2b.size(); i++){
	      for(int j=0; j<paths_b2a.size(); j++){
	        jobs.add(new Pair<String,String>(paths_a2b.get(i), paths_b2a.get(j)));
	      }
	    }
	  }
	  
	  //Order the jobs if a test was run earlier
	  if(test != null){
	    if(Utility.exists(test_path + test + "/jobs.txt")){
	      Vector<Pair<String,String>> jobs_prev = new Vector<Pair<String,String>>();
	      TreeSet<String> set = new TreeSet<String>();
	      Scanner sc;
	      String line, job_in, job_out;
	      int job_id;
	      
	      try{
	        //Read in the previous jobs
	        sc = new Scanner(new File(test_path + test + "/jobs.txt"));
	        
	        while(sc.hasNext()){
	          line = sc.nextLine();
	          
	          if(!line.isEmpty()){
	            if(line.charAt(0) == '#'){
	              job_id = Integer.valueOf(line.substring(1));
	              
	              //Read in an individual job
	              job_in = "";
	              
	              while(sc.hasNext()){
	                line = sc.nextLine();
	                
	                if(line.charAt(0)=='+' || line.isEmpty()){
	                  break;
	                }else{
	                  job_in += line + "\n";
	                }
	              }
	              
	              job_out = "";
	              
	              if(line.charAt(0) == '+'){
	                while(sc.hasNext()){
	                  line = sc.nextLine();
	                  
	                  if(line.isEmpty()){
	                    break;
	                  }else{
	                    job_out += line + "\n";
	                  }
	                }
	              }
	              
	              //Note, order we read off is order of folder indices 
	              jobs_prev.add(new Pair<String,String>(job_in, job_out));
	              set.add(job_in + job_out);
	              
	              //System.out.println(job_id + ": \n" + job_in + "\n" + job_out);
	            }
	          }
	        }
	        
	        sc.close();
	
	        //Cross reference the current jobs with the previous ones
	        Vector<Pair<String,String>> jobs_new = new Vector<Pair<String,String>>();
	        
	        for(int i=0; i<jobs.size(); i++){
	          if(!set.contains(jobs.get(i).first + jobs.get(i).second)){
	            jobs_new.add(jobs.get(i));
	          }
	        }
	        
	        output_panel.addText("<br><b>New Jobs: </b>" + jobs_new.size());
	        
	        //Update the jobs
	        jobs = jobs_prev;
	        
	        for(int i=0; i<jobs_new.size(); i++){
	          jobs.add(jobs_new.get(i));
	        }
	      }catch(Exception exception) {}
	    }
	  }
	  
	  //Set job status
	  job_status.clear();
	  
	  for(int i=0; i<jobs.size(); i++){
	    job_status.add(-1);  
	  }
	  
	  if(test != null){
	    for(int i=0; i<job_status.size(); i++){
	      if(Utility.exists(test_path + test + "/" + (i+1))){
	        job_status.set(i, jobFolderStatus(i+1));
	      }
	    }
	  }
	  
	  displayJobs();
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
	 * Display tests jobs in the jobs panel.  Note, this method delivers the status
	 * of the test as well (in the event that it is partially complete.  The method will
	 * examine the test folder and indicate which jobs are started, completed, or partially
	 * completed.
	 */
	public void displayJobs()
	{
	  String color = "ffffff";
	  
	  job_panel.clear();
	  job_panel.putText("<table border=0>");
	  
	  for(int i=0; i<jobs.size(); i++){
	    switch (job_status.get(i)) {
	      case -1: color = "ffffff"; break; //No status
	      case 0: color = "b7cee4"; break;  //Running
	      case 1: color = "b7e4b7"; break;  //Complete
	      case 2: color = "e4e4a0"; break;  //Partially completed
	      case 3: color = "e4b7b7"; break;  //Failed
	    }
	    
	    job_panel.putText("<tr>");
	    job_panel.putText("<td bgcolor=#" + color + "><b>" + (i+1) + "</b></td>");
	    job_panel.putText("<td width=" + job_panel_width + " bgcolor=#" + color + ">");
	    job_panel.putText("<table border=1 style=\"border-style: outset;\"><tr><td border=0 width=" + job_panel_width + ">");
	    
	    if(false){
	      job_panel.putText("<pre>" + jobs.get(i).first + "<hr>" + jobs.get(i).second + "</pre>");
	    }else{
	      job_panel.putText(HTMLEncodeJob(jobs.get(i).first));
	      job_panel.putText("<font size=-2><br></font>");
	      job_panel.putText(HTMLEncodeJob(jobs.get(i).second));
	    }
	    
	    job_panel.putText("</td></tr></table>");
	    job_panel.putText("</td></tr>");
	  }
	  
	  job_panel.putText("</table>");
	  job_panel.flush();
	  job_panel.setPreviousScrollPosition();
	}

	/**
	 * Check the contents of a job folder and return its status.
	 * @param job_id the ID of the job folder
	 * @return 1 if all ground truth files accounted for, 2 if partially completed, and 3 if empty
	 */
	public int jobFolderStatus(int job_id)
	{
	  File folder = new File(test_path + test + "/" + Integer.toString(job_id));
	  File[] folder_files = folder.listFiles(filename_filter);
	  TreeSet<String> set = new TreeSet<String>();
	  int total = 0;
	  int found = 0;
	  
	  if(folder_files != null){
	    for(int i=0; i<folder_files.length; i++){
	      set.add(folder_files[i].getName());
	    }
	  }
	  
	  folder = new File(test_path + test + "/0");
	  folder_files = folder.listFiles(filename_filter);
	  
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
	 * Given the comparison result from a job create the corresponding entries for the output file.
	 * @param job the job
	 * @param result the result of the comparison
	 * @return the entries for the output file
	 */
	public String getOutputEntries(Pair<String,String> job, Double result)
	{
	  Vector<String> lines;
		String application, input, output;
		String line, string = "";
		int tmpi;
		
		lines = Utility.split(job.first, '\n', false);
		
		for(int i=0; i<lines.size(); i++){
			line = lines.get(i);
	  	tmpi = line.lastIndexOf(' ');
	  	output = line.substring(tmpi+1);
	  	line = line.substring(0, tmpi);
	  	tmpi = line.lastIndexOf(' ');
	  	input = line.substring(tmpi+1);
	  	application = line.substring(0, tmpi);
			
			string += application + " " + input + " " + output + " " + result + "\n";
		}
		
		lines = Utility.split(job.second, '\n', false);
		
		for(int i=0; i<lines.size(); i++){
			line = lines.get(i);
	  	tmpi = line.lastIndexOf(' ');
	  	output = line.substring(tmpi+1);
	  	line = line.substring(0, tmpi);
	  	tmpi = line.lastIndexOf(' ');
	  	input = line.substring(tmpi+1);
	  	application = line.substring(0, tmpi);
			
			string += application + " " + input + " " + output + " " + result + "\n";
		}
		
		return string;
	}

	/**
   * Set the size of this panel.
   * @param width the desired width
   * @param height the desired height
   */
  public void setSize(int width, int height)
  {
    if(width != window_width || height != window_height){
      super.setSize(width, height);
      window_width = width;
      window_height = height;
      top_height = (int)Math.round(window_height * 0.75);
      bottom_height = window_height - top_height;
      file_panel_width = (int)Math.round(window_width * 0.45);
      job_panel_width = window_width - file_panel_width;
      output_panel_width = (int)Math.round(window_width * 0.75);
      button_panel_width = window_width - output_panel_width;
      
      job_panel.setPreferredSize(new Dimension(job_panel_width, top_height));
      file_panel.setPreferredSize(new Dimension(file_panel_width-10, top_height));
      scrollpane.setPreferredSize(new Dimension(window_width-10, top_height-2*gap));
      button_panel.setPreferredSize(new Dimension(button_panel_width-10, bottom_height-5));
      output_panel.setPreferredSize(new Dimension(output_panel_width, bottom_height-5));
      splitpane_tlr.setDividerLocation(file_panel_width);
      splitpane_tlr.updateUI();
      splitpane_blr.setDividerLocation(button_panel_width);
      splitpane_blr.updateUI();
      splitpane_tb.setDividerLocation(top_height);
      splitpane_tb.updateUI();
      updateUI();
      
      //displayJobs();
    }
  }
  
  /**
   * Paint this panel to the given graphics context
   * @param g the graphics context to draw to
   */
  public void paint(Graphics g)
  {
    super.paint(g);
    setSize(this.getWidth(), this.getHeight());
  }
  
  /**
   * Handle action events from menus and buttons.
   * @param e the action event
   */
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
  
        output_panel.addText("<br><br><b>New test created: </b>" + test);
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
	      String log_filename;
	      
	      log_filename = "log.";
	      log_filename += new SimpleDateFormat("yyyyMMddhhmmss").format(new Date());
	      log_filename += ".html";
	      
	      Utility.save(test_path + test + "/" + log_filename, output_panel.getText());
      }
    }
  }
  
  /**
   * Handle selection events from the side JTree.
   * @param e the tree selection event
   */
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
  
  /**
	 * Run the current test.
	 */
	public void run()
	{	    
		long t0, t1;
	  double dt = 0;
	  
		if(RUNNING_CONVERSIONS){
	    output_panel.addText("<br><br><b><font color=blue>Executing conversions...</font></b>");
	    
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
	    File[] folder_files = folder.listFiles(filename_filter);
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
	    
	    output_panel.addText("<br><i>Ground truth files: " + (count_old+count_new) + " (" + count_new + " new)</i><br>");
	    
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
		        displayJobs();
		        
		        //Set job sub-folders and create if they don't exist
		        job_path = test_path + test + "/" + (i+1) + "/";
		        new File(job_path).mkdir();
		        
		        tmp_path = test_path + test + "/" + (i+1) + "/tmp/";
		        new File(tmp_path).mkdir();
		        	 
		        //Convert files to target format
		        output_panel.addText("<br><b>Performing Job-" + (i+1) + "A </b> (" + working_set.size() + " files) ");
		        
		        for(Iterator<FileInfo> itr=working_set.iterator(); itr.hasNext();){
	          	conversions = iograph.getConversions(jobs.get(i).first);
	          	polyglot.convert(test_path + test + "/0/" + itr.next().filename, tmp_path, conversions);
	          	output_panel.addText(".");
		        }
		        
		        //Convert files back to source format
		        folder = new File(tmp_path);
		        folder_files = folder.listFiles(filename_filter);
		        
		        output_panel.addText("<br><b>Performing Job-" + (i+1) + "B </b> (" + folder_files.length + " files) ");
		        
		        if(folder_files != null){
		          for(int j=0; j<folder_files.length; j++){
		          	conversions = iograph.getConversions(jobs.get(i).second);
		          	polyglot.convert(folder_files[j].getAbsolutePath(), job_path, conversions);
		          	output_panel.addText(".");
		          }
		        } 
		        
		        //Display results
		        folder = new File(job_path);
		        folder_files = folder.listFiles(filename_filter);
		        
		        output_panel.addText("<br><b>Completed Job-" + (i+1) + "</b> (" + (folder_files.length-1) + " files)<br>");
		        job_status.set(i, jobFolderStatus(i+1));
		      }
		    }
			}else{
		    Vector<Thread> threads = new Vector<Thread>();
		    Thread thread;
		    boolean ACTIVE_THREADS = false;
		    boolean REFRESH;
		    
		    for(int i=0; i<jobs.size(); i++){
		      //-1=No status, 0=Running, 1=Complete, 2=Partially completed, 3=Failed
		      if(job_status.get(i)==-1 || retry_level==1 || (job_status.get(i)==2 && retry_level==2) || (job_status.get(i)==3 && retry_level==3)){
		        job_status.set(i, 0);
		        displayJobs();
		        
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
	  		        output_panel.addText("<br><b>Performing Job-" + (i_final+1) + " </b> (" + working_set.size() + " files)");

	  		        for(Iterator<FileInfo> itr=working_set.iterator(); itr.hasNext();){
	  	          	conversions = thread_iograph.getConversions(jobs.get(i_final).first);
	  	          	thread_polyglot.convert(test_path + test + "/0/" + itr.next().filename, tmp_path_final, conversions);
	  		        }
	  		        
	  		        //Convert files back to source format
	  		        File folder = new File(tmp_path_final);
	  		        File[] folder_files = folder.listFiles(filename_filter);
	  		        	  		        
	  		        if(folder_files != null){
	  		          for(int j=0; j<folder_files.length; j++){
	  		          	conversions = thread_iograph.getConversions(jobs.get(i_final).second);
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
		    	REFRESH = false;
		    	
		    	for(int i=0; i<threads.size(); i++){
		    		if(threads.get(i) != null){	    			
		    			if(!threads.get(i).isAlive()){
		    				threads.set(i, null);
				        output_panel.addText("<br><b>Completed Job-" + (i+1) + "</b>");
				        job_status.set(i, jobFolderStatus(i+1));
				        REFRESH = true;
		    			}else{
		    				ACTIVE_THREADS = true;
		    			}
		    		}
		    	}
		    	
	        if(REFRESH) displayJobs();
	        Utility.pause(500);
		    }
		    
		    output_panel.addText("<br>");			
			}
	    
	    //Set end time
	    t1 = (new Date()).getTime();
	    dt = (t1-t0) / 1000.0;
	          	    
	    displayJobs();
	    output_panel.addText("<br><b><font color=blue>Test completed in " + dt + " seconds.</font></b><br>");
	    RUNNING_CONVERSIONS = false;
	  }else if(MEASURING_QUALITY){
	  	String path0 = test_path + test + "/0/";
	  	String pathi;
	  	String output_data = "";
	  	String output_filename;
	  	File folder;
	  	File[] folder_files;
	  	String filename0, filenamei;
	  	Double result;
	  	
	    output_panel.addText("<br><br><b><font color=blue>Beginning measurments...</font></b><br><br>");

	    //Compare files
	    folder = new File(path0);
	    folder_files = folder.listFiles(filename_filter);
	    
	    if(folder_files != null){
		    //Set starting time
		    t0 = (new Date()).getTime();	    	
	    	
	      for(int i=0; i<folder_files.length; i++){
	    		output_panel.addText("<b>" + (i+1) + ") </b><i>" + folder_files[i].getName() + "</i>: <b>");
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
	        				
	        				System.out.println("Result: " + result);
		  	        	output_data += getOutputEntries(jobs.get(j), result);
		            	output_panel.addText(".");
	        			}else{	//Output file doesn't exist
	          			output_data += getOutputEntries(jobs.get(j), null);
	        				output_panel.addText("x");
	        			}
	        		}else{		//Failed jobs
	        			output_data += getOutputEntries(jobs.get(j), null);
	      				output_panel.addText("x");
	        		}
	        	}
	      	}catch(Exception e){
	      		output_panel.addText("<font color=red>failed!</font>");
	      		e.printStackTrace();
	      	}
	      	
	    		output_panel.addText("</b><br>");
	      }
	      
		    //Set end time
		    t1 = (new Date()).getTime();
		    dt = (t1-t0) / 1000.0;
		          	    	      
	    	//Set the filename and save results
	      output_filename = test_root + "_" + extractor;
	      output_filename += new SimpleDateFormat("yyyyMMddhhmmss").format(new Date());
	      output_filename += ".txt";
	    	
	      Utility.save(test_path + test + "/" + output_filename, output_data);
	    }
	    
	  	output_panel.addText("<br><b><font color=blue>Measurments completed in " + dt + " seconds.</font></b><br>");  	
	  	
	  	MEASURING_QUALITY = false;
	  }
	}

	/**
   * Start the I/O-Graph weights tool.
   * @param args not used
   */
  public static void main(String args[])
  {
    IOGraphWeightsTool iograph_wt = new IOGraphWeightsTool("IOGraphWeightsTool.ini");
    JFrame frame = iograph_wt.frame;
    frame.setJMenuBar(iograph_wt.menubar);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.add(iograph_wt);
    frame.setVisible(true);
  }
}