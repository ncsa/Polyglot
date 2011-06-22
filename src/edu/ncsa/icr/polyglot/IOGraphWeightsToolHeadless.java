package edu.ncsa.icr.polyglot;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import edu.ncsa.icr.ICRAuxiliary.Application;
import edu.ncsa.icr.ICRAuxiliary.Data;
import edu.ncsa.icr.polyglot.PolyglotAuxiliary.Conversion;
import edu.ncsa.icr.polyglot.PolyglotAuxiliary.FileInfo;
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

	private Map<String, List<FileInfo>> working_set = new HashMap<String, List<FileInfo>>();
	private Map<String, Vector<Pair<Vector<Conversion<Data,Application>>,Vector<Conversion<Data,Application>>>>> jobs = new HashMap<String,Vector<Pair<Vector<Conversion<Data,Application>>,Vector<Conversion<Data,Application>>>>>();
	private Map<String, Vector<Integer>> job_status = new HashMap<String,Vector<Integer>>();
	private Vector<ConversionResult> results = new Vector<ConversionResult>();
	private String test = null;

	private String test_path = "./";
	private int retry_level = 0; // 0=none, 1=all, 2=partials, 3=failures

	private String csr_url = null;
	private String user = null;
	private String password = null;

	private int software_threads = 0;

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
							software_threads = Integer.parseInt(value);
						}else if(key.equals("TestPath")){
							test_path = Utility.unixPath(value) + "/";
							new File(test_path).mkdirs();
						}else if(key.equals("RetryLevel")){
							retry_level = Integer.valueOf(value);
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
			
			if (software_threads == 0) {
				software_threads = servers;
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
			working_set.put(extension, new ArrayList<PolyglotAuxiliary.FileInfo>());
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
							working_set.get(extension).add(new FileInfo(folder_files[i].getAbsolutePath()));
							count++;
						}
					}
				}
			}
		}

		// Display information
		System.out.println("--------------------------------------------------");
		System.out.println("Data path : " + path);
		System.out.println("Extension : " + extension);
		System.out.println("Files     : " + count);
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
				displayJob(ext, i);
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
	public void displayJob(String ext, int idx)
	{
		String path = getConversionString(jobs.get(ext).get(idx).first) + " => " + getConversionString(jobs.get(ext).get(idx).second);
		System.out.print("Job " + ext + "." + (idx + 1) + " : ");
		switch(job_status.get(ext).get(idx)){
		case -1:
			System.out.println("Status   : No status " + path);
			break;
		case 0:
			System.out.println("Status   : Running   " + path);
			break;
		case 1:
			System.out.println("Status   : Complete  " + path);
			break;
		case 2:
			System.out.println("Status   : Partial   " + path);
			break;
		case 3:
			System.out.println("Status   : Failed    " + path);
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

	/**
	 * Given the comparison result from a job create entries in the result list
	 * 
	 * @param job
	 *          the job
	 * @param result
	 *          the result of the comparison
	 * @return the entries for the output file
	 */
	public void addResult(
			Pair<Vector<Conversion<Data,Application>>,Vector<Conversion<Data,Application>>> job,
			double result, MeasureInfo measure)
	{
		for(Conversion<Data,Application> conversion:job.first){
			ConversionResult cr = new ConversionResult();
			cr.software = conversion.edge.alias;
			cr.input_format = conversion.input.toString();
			cr.output_format = conversion.output.toString();
			cr.measure_id = measure.measure_id;
			cr.result = result;
			results.add(cr);
		}

		for(Conversion<Data,Application> conversion:job.second){
			ConversionResult cr = new ConversionResult();
			cr.software = conversion.edge.alias;
			cr.input_format = conversion.input.toString();
			cr.output_format = conversion.output.toString();
			;
			cr.measure_id = measure.measure_id;
			cr.result = result;
			results.add(cr);
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
	public void runConversions()
	{
		long t0, t1;
		double dt = 0;

		System.out.println();
		System.out.println("--------------------------------------------------");
		System.out.println("Executing conversions...");
		
		for(String ext : jobs.keySet()) {
			// Update ground truth directory
			String basefolder = test_path + test + "/" + ext + "/";
			String foldername = basefolder + "0";
			File folder = new File(foldername);
			File[] folder_files = folder.listFiles();
			TreeSet<String> set = new TreeSet<String>();
			FileInfo fi;
			int count_old = 0;
			int count_new = 0;
	
			if(folder_files != null){
				for(int i = 0; i < folder_files.length; i++){
					set.add(folder_files[i].getName());
					count_old++;
				}
			}
	
			for(Iterator<FileInfo> itr = working_set.get(ext).iterator(); itr.hasNext();){
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
	
			// Set starting time
			t0 = (new Date()).getTime();
	
			// push all jobs on a thread
			ExecutorService exec = Executors.newFixedThreadPool(software_threads);
			for(int i = 0; i < jobs.get(ext).size(); i++){
				// -1=No status, 0=Running, 1=Complete, 2=Partially completed, 3=Failed
				if(job_status.get(ext).get(i) == -1 || retry_level == 1 || (job_status.get(ext).get(i) == 2 && retry_level == 2) || (job_status.get(ext).get(i) == 3 && retry_level == 3)){
					job_status.get(ext).set(i, 0);

					exec.execute(new ConversionJob(ext, i, basefolder + (i + 1) + "/tmp/", basefolder + (i + 1) + "/"));
					try{
						Thread.sleep(100);
					}catch(InterruptedException e){
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}

			// wait for everything to finish
			exec.shutdown();
			try{
				exec.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
			}catch(InterruptedException e){
				e.printStackTrace();
			}
			
			// Set end time
			t1 = (new Date()).getTime();
			dt = (t1 - t0) / 1000.0;
		}
		
		// displayJobs();
		System.out.println("--------------------------------------------------");
		System.out.println("Conversions completed in " + dt + " seconds.");
	}
	
	public void runMeasurements()
	{
		System.out.println();
		System.out.println("--------------------------------------------------");
		System.out.println("Beginning measurments...");

		// load measures
		List<MeasureInfo> measures = new ArrayList<MeasureInfo>();
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
		
		// clear results
		results.clear();

		// Set starting time
		long l = System.currentTimeMillis();

		// compare all jobs
		List<ComparisonJob> comparisonJobs = new ArrayList<ComparisonJob>();
		
		for(String ext : jobs.keySet()) {
			String base = test_path + test + "/" + ext + "/";
			String path0 = base + "0/";
			File folder;
			File[] folder_files;
	
			// Compare files
			folder = new File(path0);
			folder_files = folder.listFiles();
	
			if(folder_files != null){
				for(int i = 0; i < folder_files.length; i++){
					// System.out.println("<b>" + (i+1) + ") </b><i>" +
					// folder_files[i].getName() + "</i>: <b>");
					File file1 = new File(path0 + folder_files[i].getName());
					
					for(int j = 0; j < jobs.get(ext).size(); j++) {
						File file2 = new File(base + (j + 1) + "/"	+ folder_files[i].getName());
						for(MeasureInfo mi:measures){
							ComparisonJob cj = new ComparisonJob(ext, j, file1, file2, mi);
							cj.submitJob();
							comparisonJobs.add(cj);
						}
					}
				}
			}
		}
		
		while(comparisonJobs.size() > 0) {
			ComparisonJob cj = comparisonJobs.get(0);
			if (cj.jobFinished()) {
				cj.storeResult();
			} else {
				comparisonJobs.add(cj);
				if (comparisonJobs.size() == 1) {
					try{
						Thread.sleep(100);
					}catch(InterruptedException e){
						e.printStackTrace();
					}
				}
			}
		}

		System.out.println("--------------------------------------------------");
		System.out.println("Measurments completed in " + ((System.currentTimeMillis() -l ) / 1000) + " seconds.");
	}

	public void printResults()
	{
		Collections.sort(results);

		System.out.println();
		System.out.println("--------------------------------------------------");
		System.out.println("Results");
		for(ConversionResult cr:results){
			String s[] = cr.software.split("-");
			String software;
			if(s.length > 1){
				software = s[1];
			}else{
				software = s[0];
			}
			System.out.println(software + "\t" + cr.input_format + "\t" + cr.output_format + "\t" + cr.result);
		}
	}

	public void saveResults()
	{
		String output_data = "";
		for(ConversionResult cr:results){
			output_data += cr.toString() + "\n";
		}

		// Set the filename and save results
		String output_filename = new SimpleDateFormat("yyyyMMddhhmmss").format(new Date());
		output_filename += ".txt";

		Utility.save(test_path + test + "/" + output_filename, output_data);
		System.out.println();
		System.out.println("--------------------------------------------------");
		System.out.println("Saved as " + output_filename);
	}

	public void uploadResults()
	{
		for(ConversionResult cr:results){
			try{
				String url = csr_url + "php/add/add_conversion_measure.php";
				url += "?software=" + cr.software.split("-")[0];
				url += "&measure=" + cr.measure_id;
				url += "&input=" + cr.input_format;
				url += "&output=" + cr.output_format;
				url += "&result=" + cr.result;
				url += "&email=" + URLEncoder.encode(user, "UTF8");

				BufferedReader br = new BufferedReader(new InputStreamReader(new URL(
						url).openStream()));
				if(!br.readLine().equals("1")){
					System.out.println("Error submitting : " + url);
				}
				br.close();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}

	class MeasureInfo
	{
		public String measure_id;
		public String measure;
		public String versus_adapter;
		public String versus_extractor;
		public String versus_measure;
	}
	
	class ComparisonJob {
		private int idx;
		private String ext;
		private File file1;
		private File file2;
		private MeasureInfo mi;
		private String id;
		private boolean finished = false;
		private double value;

		public ComparisonJob(String ext, int idx, File file1, File file2, MeasureInfo mi) {
			this.ext = ext;
			this.idx = idx;			
			this.file1 = file1;
			this.file2 = file2;
			this.mi = mi;
		}
		
		public void submitJob() {
			if(job_status.get(ext).get(idx) == 1 || job_status.get(ext).get(idx) == 2){
				if(file2.exists()){
					try{
						// upload the files
						String upload1 = VersusServiceCompare.uploadFile(versus_url, file1);
						String upload2 = VersusServiceCompare.uploadFile(versus_url, file2);

						// compare files
						id = VersusServiceCompare.compare(versus_url, upload1, upload2, mi.versus_adapter, mi.versus_extractor, mi.versus_measure);
						
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
					if (!"N/A".equals(result)) {
						finished = true;
						try {
							value = Double.parseDouble(result);							
						} catch (NumberFormatException e) {
							value = Double.NaN;
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
		
		public void storeResult() {
			if (!jobFinished()) {
				log.error("Job is not finished.");
				return;
			}
			addResult(jobs.get(ext).get(idx), getResult(), mi);
		}		
	}

	class ConversionJob implements Runnable {
		private int idx;
		private String ext;
		private String tmp_path;
		private String job_path;

		public ConversionJob(String ext, int idx, String tmp_path, String job_path) {
			this.ext = ext;
			this.idx = idx;			
			this.tmp_path = tmp_path;
			this.job_path = job_path;
			
			new File(tmp_path).mkdirs();
			new File(job_path).mkdirs();
		}
		
		public void run() {
			try {
				// convert one way
				for(Iterator<FileInfo> itr = working_set.get(ext).iterator(); itr.hasNext();){
				  polyglot.convert(test_path + test + "/" + ext + "/0/" + itr.next().filename, tmp_path, jobs.get(ext).get(idx).first);
				}
	
				// Convert files back to source format
				File folder = new File(tmp_path);
				File[] folder_files = folder.listFiles();
	
				if(folder_files != null){
					for(int j = 0; j < folder_files.length; j++){
					  polyglot.convert(folder_files[j].getAbsolutePath(), job_path, jobs.get(ext).get(idx).second);
					}
				}
				
			} catch (Throwable thr) {
				thr.printStackTrace();
			}
			
			// done with conversion
      job_status.get(ext).set(idx, jobFolderStatus(idx+1));
      displayJob(ext, idx);
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
		IOGraphWeightsToolHeadless iograph_wt = new IOGraphWeightsToolHeadless(
				"IOGraphWeightsToolHeadless.conf");

		// check params
		if((iograph_wt.csr_url == null) || (iograph_wt.user == null) || (iograph_wt.versus_url == null)){
			System.err.println("Need to specify at least CSR, User and Versus in IOGraphWeightsToolHeadless.conf");
			System.exit(-1);
		}

		// load all test folders
		for(int i=0; i<args.length; i+=2) {
			iograph_wt.loadFolder(args[i], args[i+1]);
		}
		
	  //Using any found tests, load/update jobs!
		iograph_wt.loadJobs();
	  
		// create test cases first
		iograph_wt.createTests();

		// run conversions
		iograph_wt.runConversions();

		// compute measurements
		iograph_wt.runMeasurements();

		// normalize weights

		// print/upload results
		iograph_wt.printResults();
		iograph_wt.uploadResults();

		// all done
		System.exit(0);
	}
}