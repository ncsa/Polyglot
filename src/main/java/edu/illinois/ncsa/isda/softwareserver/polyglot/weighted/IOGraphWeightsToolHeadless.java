package edu.illinois.ncsa.isda.softwareserver.polyglot.weighted;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;
import edu.illinois.ncsa.isda.softwareserver.SoftwareServerAuxiliary.Application;
import edu.illinois.ncsa.isda.softwareserver.SoftwareServerAuxiliary.Data;
import edu.illinois.ncsa.isda.softwareserver.polyglot.IOGraph;
import edu.illinois.ncsa.isda.softwareserver.polyglot.PolyglotAuxiliary;
import edu.illinois.ncsa.isda.softwareserver.polyglot.PolyglotAuxiliary.Conversion;
import edu.illinois.ncsa.isda.softwareserver.polyglot.PolyglotAuxiliary.FileInformation;
import kgm.utility.*;

/**
 * A program to facilitate in the running of polyglot tests to fill in I/O-Graph
 * edge weights.
 * 
 * @author Kenton McHenry
 */
public class IOGraphWeightsToolHeadless
{
	private static Log log = LogFactory.getLog(IOGraphWeightsToolHeadless.class);
	
	private PolyglotStewardThreaded polyglot = new PolyglotStewardThreaded();
	private IOGraph<Data,Application> iograph;
	List<MeasureInfo> measures = new ArrayList<MeasureInfo>();

	private Map<String, List<FileInformation>> working_set = new HashMap<String, List<FileInformation>>();
	private Map<String, Vector<Pair<Vector<Conversion<Data,Application>>,Vector<Conversion<Data,Application>>>>> jobs = new HashMap<String,Vector<Pair<Vector<Conversion<Data,Application>>,Vector<Conversion<Data,Application>>>>>();
	private Map<String, Vector<Integer>> job_status = new HashMap<String,Vector<Integer>>();
	private String test = null;

	private String test_path = "./";

	private String csr_url = null;
	private String user = null;
	private String password = null;

	private int softwareThreads = 0;
	private int compareThreads = 1;
	
	private Integer comparisonID = 0;

	private String versus_url = null;

	/**
	 * Class constructor.
	 * 
	 * @param filename
	 *          the configuration file to use
	 */
	public IOGraphWeightsToolHeadless(String filename)
	{
		// Load data
		loadConfiguration(filename);
		
		// initialize iograph
		polyglot.setApplicationFlexibility(1);
		iograph = polyglot.getIOGraph();
		
		// initialize measures
		try{
			URL m_url = new URL(csr_url + "php/search/get_measures.php");
			BufferedReader br = new BufferedReader(new InputStreamReader(m_url.openStream()));
			String line;
			while((line = br.readLine()) != null){
				if(line.endsWith("<br>")){
					line = line.substring(0, line.length() - 4);
				}
				String[] pieces = line.split("\t");
				if(pieces.length != 5){
					continue;
				}
				if(pieces[2].equals("")){
					System.err.println("Ignoring : " + pieces[1] + " could not be used with versus, missing adapter.");
					continue;
				}
				if(pieces[3].equals("")){
					System.err.println("Ignoring : " + pieces[1] + " could not be used with versus, missing extractor.");
					continue;
				}
				if(pieces[4].equals("")){
					System.err.println("Ignoring : " + pieces[1] + " could not be used with versus, missing measure.");
					continue;
				}
				MeasureInfo mi = new MeasureInfo();
				mi.measure_id = pieces[0];
				mi.measure = pieces[1];
				mi.versus_adapter = pieces[2];
				mi.versus_extractor = pieces[3];
				mi.versus_measure = pieces[4];
				measures.add(mi);
			}
			br.close();
		}catch(Exception e1){
			e1.printStackTrace();
		}
	}

	/**
	 * Load a configuration file.
	 * 
	 * @param filename
	 *          the name of the configuration file
	 */
	public void loadConfiguration(String filename)
	{
		try{
			BufferedReader ins = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
			String line;
			String key;
			String value;
			String server;
			int port, tmpi;
			int servers = 0;

			while((line = ins.readLine()) != null){
				if(line.contains("=")){
					key = line.substring(0, line.indexOf('='));
					value = line.substring(line.indexOf('=') + 1);

					if(key.charAt(0) != '#' && key.charAt(0) != ';'){
						if(key.equals("SoftwareServer")){
							tmpi = value.lastIndexOf(':');

							if(tmpi != -1){
								server = value.substring(0, tmpi);
								port = Integer.valueOf(value.substring(tmpi + 1));
								polyglot.add(server, port);
								servers++;
							}
						}else if(key.equals("SoftwareThreads")){
							softwareThreads = Integer.parseInt(value);
						}else if(key.equals("TestPath")){
							test_path = Utility.unixPath(value) + "/";
							new File(test_path).mkdirs();
						}else if(key.equals("CSR")){
							csr_url = value;
							if(!csr_url.endsWith("/")){
								csr_url += "/";
							}
						}else if(key.equals("Versus")){
							versus_url = value;
							if(!versus_url.endsWith("/")){
								versus_url += "/";
							}
						}else if(key.equals("Password")){
							password = value;
						}else if(key.equals("User")){
							user = value;
						}
					}
				}
			}
			
			if (softwareThreads == 0) {
				softwareThreads = servers;
			}

			ins.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	/**
	 * Load all files of the given extension beneath the given folder.
	 * 
	 * @param path
	 *          the folder to search
	 * @param extension
	 *          the extension of the files
	 */
	public void loadFolder(String path, String extension)
	{
		String filename;
		String ext;
		int tmpi;
		int count = 0;

		if (!working_set.containsKey(extension)) {
			working_set.put(extension, new ArrayList<PolyglotAuxiliary.FileInformation>());
		}
		
		// Load model files in this folder
		Stack<File> stk = new Stack<File>();
		stk.push(new File(path));

		while(!stk.empty()){
			File folder = stk.pop();
			File[] folder_files = folder.listFiles();

			if(folder_files != null){
				for(int i = 0; i < folder_files.length; i++){
					if(folder_files[i].isDirectory()) {
						stk.push(folder_files[i]); 
						continue;
					}
					
					// Check file extension
					filename = folder_files[i].getName();
					tmpi = filename.lastIndexOf('.');

					if(tmpi >= 0){
						ext = filename.substring(tmpi + 1);

						if(ext.equals("gz") || ext.equals("zip")){
							String tmp = filename.substring(0, tmpi);
							tmpi = tmp.lastIndexOf('.');
							if(tmpi >= 0) ext = tmp.substring(tmpi + 1);
						}

						if(ext.equalsIgnoreCase(extension)){
							working_set.get(extension).add(new FileInformation(folder_files[i].getAbsolutePath()));
							count++;
						}
					}
				}
			}
		}
		
		int conversion = 0;
		TreeSet<String> range = iograph.getRangeStrings(extension);
		Vector<Vector<Conversion<Data,Application>>> paths_a2b;
		Vector<Vector<Conversion<Data,Application>>> paths_b2a;
		String output_format;
	
		for(Iterator<String> itr = range.iterator(); itr.hasNext();){
			output_format = itr.next();
			paths_a2b = iograph.getShortestConversionPaths(extension, output_format);
			paths_b2a = iograph.getShortestConversionPaths(output_format, extension);
			conversion += (paths_a2b.size() * paths_b2a.size());
		}

		// Display information
		System.out.println("--------------------------------------------------");
		System.out.println("Data path   : " + path);
		System.out.println("Extension   : " + extension);
		System.out.println("Files       : " + count);
		System.out.println("Conversions : " + conversion);
	}

	/**
	 * Use IOGraph to determine jobs but check if an old test exists as well and
	 * update it if necessary!
	 */
	public void loadJobs()
	{
		jobs.clear();
		job_status.clear();

		System.out.println("--------------------------------------------------");
		System.out.println("List of jobs :");

		for (String ext : working_set.keySet()) {
			Vector<Pair<Vector<Conversion<Data,Application>>,Vector<Conversion<Data,Application>>>> extJob =  new Vector<Pair<Vector<Conversion<Data,Application>>,Vector<Conversion<Data,Application>>>>();
			Vector<Integer> extStatus = new Vector<Integer>();
			jobs.put(ext, extJob);
			job_status.put(ext, extStatus);
			TreeSet<String> range = iograph.getRangeStrings(ext);
			Vector<Vector<Conversion<Data,Application>>> paths_a2b;
			Vector<Vector<Conversion<Data,Application>>> paths_b2a;
			String output_format;
		
			for(Iterator<String> itr = range.iterator(); itr.hasNext();){
				output_format = itr.next();
	
				paths_a2b = iograph.getShortestConversionPaths(ext, output_format);
				paths_b2a = iograph.getShortestConversionPaths(output_format, ext);
	
				for(int i = 0; i < paths_a2b.size(); i++){
					for(int j = 0; j < paths_b2a.size(); j++){
						extJob.add(new Pair<Vector<Conversion<Data,Application>>,Vector<Conversion<Data,Application>>>(paths_a2b.get(i), paths_b2a.get(j)));
						extStatus.add(-1);
					}
				}
				
				// FIXME
				//break;
			}
			
			for(int i = 0; i < extJob.size(); i++){
				displayConversionJob(ext, i);
			}
		}

//		if(test != null){
//			for(int i = 0; i < job_status.size(); i++){
//				if(Utility.exists(test_path + test + "/" + (i + 1))){
//					job_status.set(i, jobFolderStatus(i + 1));
//				}
//			}
//		}
	}

	/**
	 * Display information about a specific job.
	 */
	public void displayConversionJob(String ext, int idx)
	{
		String path = getConversionString(jobs.get(ext).get(idx).first) + " => " + getConversionString(jobs.get(ext).get(idx).second);
		System.out.print("Job " + ext + "." + (idx + 1) + " : ");
		switch(job_status.get(ext).get(idx)){
		case -1:
			System.out.println("Conversion   : No status " + path);
			break;
		case 0:
			System.out.println("Conversion   : Running   " + path);
			break;
		case 1:
			System.out.println("Conversion   : Complete  " + path);
			break;
		case 2:
			System.out.println("Conversion   : Partial   " + path);
			break;
		case 3:
			System.out.println("Conversion   : Failed    " + path);
			break;
		}
	}

	private String getConversionString(Vector<Conversion<Data,Application>> conversion)
	{
		String task = "";
		for(int j = 0; j < conversion.size(); j++){
			task += "[" + conversion.get(j).edge.alias + " ";
			task += conversion.get(j).input.toString() + " ";
			task += conversion.get(j).output.toString() + "] ";
		}
		return task;
	}


	/**
	 * Check the contents of a job folder and return its status.
	 * 
	 * @param job_id
	 *          the ID of the job folder
	 * @return 1 if all ground truth files accounted for, 2 if partially
	 *         completed, and 3 if empty
	 */
	public int jobFolderStatus(int job_id)
	{
		File folder = new File(test_path + test + "/" + Integer.toString(job_id));
		File[] folder_files = folder.listFiles();
		TreeSet<String> set = new TreeSet<String>();
		int total = 0;
		int found = 0;

		if(folder_files != null){
			for(int i = 0; i < folder_files.length; i++){
				set.add(folder_files[i].getName());
			}
		}

		folder = new File(test_path + test + "/0");
		folder_files = folder.listFiles();

		if(folder_files != null){
			for(int i = 0; i < folder_files.length; i++){
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

	public void createTests()
	{
		test = new SimpleDateFormat("yyyyMMddhhmmss").format(new Date());

		for(String ext : jobs.keySet()) {
			new File(test_path + test + "/" + ext + "/0").mkdirs();			
		}
		
		System.out.println("--------------------------------------------------");
		System.out.println("New test created: " + test);
	}

	/**
	 * Run the current test.
	 */
	public void runTests()
	{
		double t0 = System.currentTimeMillis();
		
		System.out.println();
		System.out.println("--------------------------------------------------");
		System.out.println("Executing conversions and comparisons...");
		
		ExecutorService conv = Executors.newFixedThreadPool(softwareThreads);
		ExecutorService comp = Executors.newFixedThreadPool(compareThreads);

		for(String ext : jobs.keySet()) {
			// Update ground truth directory
			String basefolder = test_path + test + "/" + ext + "/";
			String foldername = basefolder + "0";
			File folder = new File(foldername);
			File[] folder_files = folder.listFiles();
			TreeSet<String> set = new TreeSet<String>();
			FileInformation fi;
			int count_old = 0;
			int count_new = 0;
	
			if(folder_files != null){
				for(int i = 0; i < folder_files.length; i++){
					set.add(folder_files[i].getName());
					count_old++;
				}
			}
	
			for(Iterator<FileInformation> itr = working_set.get(ext).iterator(); itr.hasNext();){
				fi = itr.next();
	
				if(!set.contains(fi.filename)){
					Utility.copyFile(fi.absolutename, foldername + "/" + fi.filename);
					count_new++;
				}
			}
	
			// System.out.println("--------------------------------------------------");
			// System.out.println("Ground truth files: " + (count_old+count_new) + " ("
			// + count_new + " new)");
	
			// Update job status based on a possibly modified ground truth directory
			for(int i = 0; i < job_status.size(); i++){
				if(Utility.exists(basefolder + (i + 1))){
					job_status.get(ext).set(i, jobFolderStatus(i + 1));
				}
			}
	
			// push all jobs on a thread		
			//ExecutorService conv = Executors.newFixedThreadPool(softwareThreads);
			for(int i = 0; i < jobs.get(ext).size(); i++){
				// -1=No status, 0=Running, 1=Complete, 2=Partially completed, 3=Failed
				if(job_status.get(ext).get(i) != 1) {
					job_status.get(ext).set(i, 0);
					conv.execute(new ConversionJob(ext, i, basefolder + (i + 1) + "/tmp/", basefolder + (i + 1) + "/", comp));
				}
			}
		}

		// wait for everything to finish
		conv.shutdown();
		try{
			conv.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
		}catch(InterruptedException e){
			e.printStackTrace();
		}
		comp.shutdown();
		try{
			comp.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
		}catch(InterruptedException e){
			e.printStackTrace();
		}

		// displayJobs();
		t0 = (System.currentTimeMillis() - t0) / 1000.0;
		System.out.println("--------------------------------------------------");
		System.out.println("Completed in " + t0  + " seconds.");
	}
	
	class MeasureInfo
	{
		public String measure_id;
		public String measure;
		public String versus_adapter;
		public String versus_extractor;

		public String versus_measure;
		
		public String toString() {
			return measure;
		}
	}
	
	class ComparisonJob implements Runnable {
		private int idx;
		private String ext;
		private File file1;
		private File file2;
		private MeasureInfo mi;
		private String id;
		private boolean finished = false;
		private double value;
		private int compid;

		public ComparisonJob(String ext, int idx, File file1, File file2, MeasureInfo mi, int compid) {
			this.ext = ext;
			this.idx = idx;			
			this.file1 = file1;
			this.file2 = file2;
			this.mi = mi;
			this.compid = compid;
		}
		
		public void run() {			
			logVersus(compid, "Start " + mi.measure);
			submitJob();

			while(!jobFinished()) {
				try{
					Thread.sleep(100);
				}catch(InterruptedException e){
					e.printStackTrace();
				}
  		}
  		
			logVersus(compid, "Finished");
			System.out.print("Job " + ext + "." + (idx + 1) + " : ");
			System.out.println("Comparison   : Complete  " + mi + " " + getResult());

			for(Conversion<Data,Application> conversion : jobs.get(ext).get(idx).first){
				uploadResult(conversion, mi, getResult());
			}

			for(Conversion<Data,Application> conversion : jobs.get(ext).get(idx).second){
				uploadResult(conversion, mi, getResult());
			}
		}
	
		private void uploadResult(Conversion<Data,Application> conversion, MeasureInfo mi, double value) {
			String url = csr_url + "php/add/add_conversion_measure.php";
			try{
				url += "?software=" + conversion.edge.alias.split("-")[0];
				url += "&measure=" + mi.measure_id;
				url += "&input=" + conversion.input.toString();
				url += "&output=" + conversion.output.toString();
				url += "&result=" + value;
				url += "&email=" + URLEncoder.encode(user, "UTF8");
	
				BufferedReader br = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
				if(!br.readLine().equals("1")){
					System.out.println("Error submitting : " + url);
				}
				br.close();
			}catch(Exception e){
				System.out.println(url);
				e.printStackTrace();
			}
		}
		
		public void submitJob() {
			if(job_status.get(ext).get(idx) == 1 || job_status.get(ext).get(idx) == 2){
				if(file2.exists()){
					try{
						// upload the files
						String upload1 = VersusServiceCompare.uploadFile(versus_url, file1);
	  				logVersus(compid, "Upload File 1");
						
						String upload2 = VersusServiceCompare.uploadFile(versus_url, file2);
	  				logVersus(compid, "Upload File 2");

						// compare files
						id = VersusServiceCompare.compare(versus_url, upload1, upload2, mi.versus_adapter, mi.versus_extractor, mi.versus_measure);
	  				logVersus(compid, "Compare Submitted [" + id + "]");

					}catch(Exception e){
						value = 10000;
						finished = true;
						log.info("Comparison failed.", e);
					}
				}else{
					value = 20000;
					finished = true;
				}
			}else{
				value = 30000;
				finished = true;
			}			
		}
		
		public boolean jobFinished() {
			if (!finished) {
				try {
					String result = VersusServiceCompare.checkComparisonASync(id);
					if ("FAILED".equals(result)) {
						finished = true;
						value = 10000;						
					} else if (!"N/A".equals(result)) {
						finished = true;
						try {
							value = Double.parseDouble(result);
							// FIXME this should never happen if values are normalized
							if (value < 0) {
								value = Math.abs(value);
							}
							if (value >= 10000) {
								value = 9999;
							}
						} catch (NumberFormatException e) {
							System.out.println(result + " is not a valid number.");
							value = 10000;
						}
					}
				} catch (Exception e) {
					log.info("Could not get job status.",e);
				}
			}
			return finished;
		}
		
		public double getResult() {
			if (!jobFinished()) {
				return Double.NaN;				
			}
			return value;
		}
	}
	
	private static Map<Integer, Date> timestamps = new HashMap<Integer,Date>();
	private static PrintStream ps = null;
	private static DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	
	private static void logVersus(int id, String msg) {
		synchronized(timestamps) {
			Date d = new Date();
			Date p = timestamps.get(id);
			long elapsed = 0;
			if (p != null) {
				elapsed = d.getTime() - p.getTime();
			}
			timestamps.put(id, d);
			if (ps == null) {
				try{
					ps = new PrintStream("versus.log");
				}catch(FileNotFoundException e){
					ps = System.err;
				}
			}
			ps.println(String.format("%d\t%s\t%d\t%s", id, df.format(d), elapsed, msg));
			ps.flush();
		}
	}


	class ConversionJob implements Runnable {
		private int idx;
		private String ext;
		private String tmp_path;
		private String job_path;
		private ExecutorService exec;

		public ConversionJob(String ext, int idx, String tmp_path, String job_path, ExecutorService exec) {
			this.ext = ext;
			this.idx = idx;			
			this.tmp_path = tmp_path;
			this.job_path = job_path;
			this.exec = exec;
			
			new File(tmp_path).mkdirs();
			new File(job_path).mkdirs();
		}
		
		public void run() {
			String base = test_path + test + "/" + ext + "/";
			String path0 = base + "0/";

			String path = getConversionString(jobs.get(ext).get(idx).first) + " => " + getConversionString(jobs.get(ext).get(idx).second);
			System.out.print("Job " + ext + "." + (idx + 1) + " : ");
			System.out.println("Conversion   : Running   " + path);
				
			try {				
				for(Iterator<FileInformation> itr = working_set.get(ext).iterator(); itr.hasNext();){
					Vector<Conversion<Data,Application>> conversion = new Vector<PolyglotAuxiliary.Conversion<Data,Application>>();
					conversion.addAll(jobs.get(ext).get(idx).first);
					conversion.addAll(jobs.get(ext).get(idx).second);
					polyglot.convert(path0 + itr.next().filename, job_path, conversion);
				}
//				
//				// convert one way
//				for(Iterator<FileInfo> itr = working_set.get(ext).iterator(); itr.hasNext();){
//				  polyglot.convert(path0 + itr.next().filename, tmp_path, jobs.get(ext).get(idx).first);
//				}
//	
//				// Convert files back to source format
//				File folder = new File(tmp_path);
//				File[] folder_files = folder.listFiles();
//	
//				if(folder_files != null){
//					for(int j = 0; j < folder_files.length; j++){
//					  polyglot.convert(folder_files[j].getAbsolutePath(), job_path, jobs.get(ext).get(idx).second);
//					}
//				}			
			} catch (Throwable thr) {
				thr.printStackTrace();
			}
			
			// done with conversion
      job_status.get(ext).set(idx, jobFolderStatus(idx+1));
      displayConversionJob(ext, idx);
           
      
			// compare all conversions 		
			for(Iterator<FileInformation> itr = working_set.get(ext).iterator(); itr.hasNext();){
				String filename = itr.next().filename;
				for(MeasureInfo mi : measures) {
					File file1 = new File(path0 + filename);
					File file2 = new File(job_path + filename);
					int id;
					synchronized(comparisonID){
						id = comparisonID;
						comparisonID++;
					}
		      exec.execute(new ComparisonJob(ext, idx, file1, file2, mi, id));
				}				
			}
		}
	}


	class ConversionResult implements Comparable<ConversionResult>
	{
		public String software;
		public String input_format;
		public String output_format;
		public String measure_id;
		public double result;
		
		public int compareTo(ConversionResult o)
		{
			if(!software.equals(o.software)){ return software.compareTo(o.software); }
			if(!input_format.equals(o.input_format)){ return input_format.compareTo(o.input_format); }
			if(!output_format.equals(o.output_format)){ return output_format.compareTo(o.output_format); }
			return Double.compare(result, o.result);
		}

		@Override
		public String toString()
		{
			if(result >= 0){
				return software + "\t" + input_format + "\t" + output_format + "\t" + result;
			}else{
				return software + "\t" + input_format + "\t" + output_format + "\t0";
			}
		}
	}
	
	/**
	 * Start the I/O-Graph weights tool.
	 * 
	 * @param args
	 *          not used
	 */
	public static void main(String args[])
	{
		IOGraphWeightsToolHeadless iograph_wt = new IOGraphWeightsToolHeadless("IOGraphWeightsToolHeadless.conf");

		// check params
		if((iograph_wt.csr_url == null) || (iograph_wt.user == null) || (iograph_wt.versus_url == null)){
			System.err.println("Need to specify at least CSR, User and Versus in IOGraphWeightsToolHeadless.conf");
			System.exit(-1);
		}
		
		BasicConfigurator.configure();
		org.apache.log4j.Logger.getLogger("org.restlet").setLevel(org.apache.log4j.Level.WARN);
		Logger.getLogger("org.restlet").setLevel(Level.WARNING);
		
		// load all test folders
		for(int i=0; i<args.length; i+=2) {
			iograph_wt.loadFolder(args[i], args[i+1]);
		}
		
	  //Using any found tests, load/update jobs!
		iograph_wt.loadJobs();
	  
		// create test cases first
		iograph_wt.createTests();

		// run conversions
		iograph_wt.runTests();

		// all done
		System.exit(0);
	}
}