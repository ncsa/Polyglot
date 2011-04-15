package edu.ncsa.icr;
import edu.ncsa.icr.ICRAuxiliary.*;
import edu.ncsa.image.ImageUtility;
import edu.ncsa.utility.*;
import java.io.*;
import java.util.*;

/**
 * A program to setup a software reuse server on the local machine.
 * @author Kenton McHenry
 */
public class ScriptInstaller
{	
	private static boolean DEBUG = false;
	
	/**
	 * Parse out the file name given a script name including a path and time stamp.
	 * @param script the full script name with path and time stamp
	 * @param SOFTWARE_KEYS set to true if software keys have been appended to the given script name
	 * @return the script file name
	 */
	private static String getScriptFileName(String script, boolean SOFTWARE_KEYS)
	{
		String filename = "";
		int dash, dot;
		
		if(SOFTWARE_KEYS){
			String[] parts = script.split("/");
			
			if(parts.length == 3){
				filename = parts[0] + "-";
			}
		}

		script = Utility.getFilename(script);
		dash = script.lastIndexOf('-');
		dot = script.lastIndexOf('.');
		
		if(dash >= 0 && dot >= 0){
			filename = filename + script.substring(0, dash) + script.substring(dot);
		}
		
		return filename;
	}

	/**
	 * Parse out the time stamp given a script name including a path and time stamp.
	 * @param script the full script name with path and time stamp
	 * @return the script timestamp
	 */
	private static int getScriptTimeStamp(String script)
	{
		int timestamp = -1;
		int dash, dot;
		
		script = Utility.getFilename(script);
		dash = script.lastIndexOf('-');
		dot = script.lastIndexOf('.');
		
		if(dash >= 0 && dot >= 0){
			timestamp = Integer.valueOf(script.substring(dash+1, dot));
		}
		
		return timestamp;
	}

	/**
	 * Search the CSR for scripts for applications on the local system.
	 * @param methods the methods to use for searching for local software
	 * @param csr_script_url the URL to the needed CSR query scripts
	 * @param local_software a list to hold the names of locally installed software (possibly pre-filled)
	 * @return a list of URL's to scripts for locally installed software
	 */
	public static Vector<String> getSystemScripts(TreeSet<String> methods, String csr_script_url, TreeSet<String> local_software)
	{
		String os = System.getProperty("os.name");
		String arch = System.getProperty("os.arch");
		TreeSet<String> scripted_software = new TreeSet<String>();
		Vector<String> local_scripted_software = new Vector<String>();
		Vector<Vector<String>> local_scripted_software_versions = new Vector<Vector<String>>();
		Vector<String> local_scripted_software_version = new Vector<String>();
		Vector<String> scripts = new Vector<String>();
		TreeMap<String,String> newest_scripts = new TreeMap<String,String>();
		Process p;
		Scanner p_output, scanner;
		String method, line, software, result;
		String filename;
		int timestamp;
		int tmpi;
		boolean FOUND;
		
		//Build software list
		for(Iterator<String> itr=methods.iterator(); itr.hasNext();){
			method = itr.next();

			if(os.contains("Windows") && method.equals("wmic")){				//WMIC on Windows
				try{
					p = Runtime.getRuntime().exec("wmic product get /format:csv");
					p_output = new Scanner(p.getInputStream());
					
					while(p_output.hasNextLine()){
						line = p_output.nextLine();
						
						if(!line.isEmpty()){
							//System.out.println(line);

							scanner = new Scanner(line);
							scanner.useDelimiter(",");
							scanner.next();
							scanner.next();
							
							software = scanner.next().trim();
							
							if(!software.isEmpty()){
								local_software.add(software);
							}
						}
					}
				}catch(Exception e) {e.printStackTrace();}
			}else if(os.contains("Windows") && method.equals("reg")){		//Registry on Windows
				try{
					p = Runtime.getRuntime().exec("reg query HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall /s");
					p_output = new Scanner(p.getInputStream());
					
					while(p_output.hasNextLine()){
						line = p_output.nextLine();
						
						if(!line.isEmpty() && line.contains("DisplayName")){
							//System.out.println(line);

							scanner = new Scanner(line);
							scanner.next();
							scanner.next();
							
							software = scanner.nextLine().trim();
							
							if(!software.startsWith("@")){
								local_software.add(software);
							}
						}
					}
					
					if(arch.equals("amd64")){		//Check an additional key on 64-bit systems
						p = Runtime.getRuntime().exec("reg query HKEY_LOCAL_MACHINE\\SOFTWARE\\Wow6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall /s");
						p_output = new Scanner(p.getInputStream());

						while(p_output.hasNextLine()){
							line = p_output.nextLine();
							
							if(!line.isEmpty() && line.contains("DisplayName")){
								//System.out.println(line);
	
								scanner = new Scanner(line);
								scanner.next();
								scanner.next();
								
								software = scanner.nextLine().trim();
								
								if(!software.startsWith("@")){
									local_software.add(software);
								}
							}
						}
					}
				}catch(Exception e) {e.printStackTrace();}
			}else{
				System.out.println("Method \"" + method + "\" not available on " + os);
			}
		}

		if(!local_software.isEmpty()){
			//Display found software
			if(DEBUG){
				System.out.println("Found software:\n");
				
				for(Iterator<String> itr=local_software.iterator(); itr.hasNext();){
					System.out.println("  " + itr.next());
				}
				
				System.out.println();
			}
			
			//Get list of scripted software from CSR
			result = Utility.readURL(csr_script_url + "get_scripted_software.php");
			scanner = new Scanner(result);
			
			while(scanner.hasNextLine()){
				software = scanner.nextLine().trim();
				if(software.isEmpty()) continue;
				software = software.substring(0, software.length()-4);		//Remove "<br>"
				scripted_software.add(software);
			}
			
			//Display scripted software
			if(DEBUG){
				System.out.println("Scripted software:\n");
				
				for(Iterator<String> itr=scripted_software.iterator(); itr.hasNext();){
					System.out.println("  " + itr.next());
				}
				
				System.out.println();
			}
			
			//Find local scripted software
			for(Iterator<String> itr1 = scripted_software.iterator(); itr1.hasNext();){
				software = itr1.next();
				FOUND = false;
							
				for(Iterator<String> itr2 = local_software.iterator(); itr2.hasNext();){
					if(itr2.next().toLowerCase().contains(software.toLowerCase())){
						FOUND = true;
						break;
					}
				}
				
				if(FOUND){
					local_scripted_software.add(software);
				}
			}

			//Display scripted local software
			if(DEBUG){
				System.out.println("Local scripted software:\n");
				
				for(int i=0; i<local_scripted_software.size(); i++){
					System.out.println("  " + local_scripted_software.get(i));
				}
				
				System.out.println();
			}
			
			//Get the versions available for the scripted local software
			software = "";
			
			for(int i=0; i<local_scripted_software.size(); i++){
				if(!software.isEmpty()) software += ", ";
				software += local_scripted_software.get(i);
			}
				
			result = Utility.readURL(csr_script_url + "get_versions.php?software=" + Utility.urlEncode(software));
			scanner = new Scanner(result);
			
			while(scanner.hasNextLine()){
				line = scanner.nextLine().trim();
				if(line.isEmpty()) continue;
				line = line.substring(0, line.length()-4);		//Remove "<br>"
				local_scripted_software_versions.add(Utility.split(line, ',', true, true));
			}
			
			//If more than one version ask the user
			for(int i=0; i<local_scripted_software_versions.size(); i++){
				if(!local_scripted_software_versions.get(i).isEmpty()){
					if(local_scripted_software_versions.get(i).size() == 1){
						local_scripted_software_version.add(local_scripted_software_versions.get(i).firstElement());
					}else{
						System.out.println("  found " + local_scripted_software_versions.get(i).size() + " versions of \"" + local_scripted_software.get(i) + "\":");

						for(int j=0; j<local_scripted_software_versions.get(i).size(); j++){
							System.out.println("    [" + (j+1) + "] " + local_scripted_software_versions.get(i).get(j));
						}
						
						tmpi = Integer.valueOf(System.console().readLine("  enter choice: "))-1;
						local_scripted_software_version.add(local_scripted_software_versions.get(i).get(tmpi));
					}
				}else{
					local_scripted_software_version.add(null);
				}
			}
			
			//Get the scripts
			software = "";
			
			for(int i=0; i<local_scripted_software.size(); i++){
				if(!software.isEmpty()) software += ", ";
				software += local_scripted_software.get(i);
				if(local_scripted_software_version.get(i) != null) software += "(" + local_scripted_software_version.get(i) + ")";
			}
				
			result = Utility.readURL(csr_script_url + "get_scripts.php?software=" + Utility.urlEncode(software));
			//result = Utility.readURL(csr_script_url + "get_scripts.php?software=" + Utility.urlEncode(software) + "&software_keys=true");
			scanner = new Scanner(result);
			
			while(scanner.hasNextLine()){
				line = scanner.nextLine().trim();
				if(line.isEmpty()) continue;
				line = line.substring(0, line.length()-4);		//Remove "<br>"
				scripts.add(line);
			}
			
			//Display scripts
			if(DEBUG){
				System.out.println("Scripts:\n");
				
				for(int i=0; i<scripts.size(); i++){
					System.out.println("  " + scripts.get(i));
				}
				
				System.out.println();
			}
			
			//Keep only the newest version of each script
			for(int i=0; i<scripts.size(); i++){
				filename = getScriptFileName(scripts.get(i), false);
				timestamp = getScriptTimeStamp(scripts.get(i));
				
				if(newest_scripts.get(filename) == null){
					newest_scripts.put(filename, scripts.get(i));
				}else{
					if(timestamp > getScriptTimeStamp(newest_scripts.get(filename))){
						newest_scripts.put(filename, scripts.get(i));
					}
				}
			}
			
			scripts.clear();
			
			for(Iterator<String> itr=newest_scripts.keySet().iterator(); itr.hasNext();){
				scripts.add(newest_scripts.get(itr.next()));
			}
			
			//Display newest scripts
			if(DEBUG){
				System.out.println("Newest scripts:\n");
				
				for(int i=0; i<scripts.size(); i++){
					System.out.println("  " + scripts.get(i));
				}
				
				System.out.println();
			}
		}
		
		return scripts;
	}
	
	/**
	 * Search the CSR for scripts for applications on the local system.
	 * @param methods the methods to use for searching for local software
	 * @param csr_script_url the URL to the needed CSR query scripts
	 * @return a list of URL's to scripts for locally installed software
	 */
	public static Vector<String> getSystemScripts(TreeSet<String> methods, String csr_script_url)
	{
		return getSystemScripts(methods, csr_script_url, new TreeSet<String>());
	}
	
	/**
	 * Search the CSR for test files for the given scripts.
	 * @param scripts a list of script names
	 * @param csr_script_url the URL to the needed CSR query scripts
	 * @param max_per_type the maximum number of test files per type
	 * @return a list of test files on the CSR
	 */
	public static Vector<String> getTestFiles(Vector<String> scripts, String csr_script_url, int max_per_type)
	{
		Vector<String> test_files = new Vector<String>();
		TreeSet<String> formats = new TreeSet<String>();
		TreeMap<String,Vector<String>> format_map = new TreeMap<String,Vector<String>>();
		Vector<String> files;
		Script script;
		Scanner scanner;
		String formats_string = "";
		String result, file, extension;
		
		//Build a list of needed file formats
		for(int i=0; i<scripts.size(); i++){
			script = new Script(scripts.get(i), null);
			
			//Look only at input scripts (assuming they will also test other scripts associated with an application)
			if(script.operation.equals("convert") || script.operation.equals("open") || script.operation.equals("import")){
				for(Iterator<String> itr=script.inputs.iterator(); itr.hasNext();){
					formats.add(itr.next());
				}
			}
		}
		
		for(Iterator<String> itr=formats.iterator(); itr.hasNext();){
			if(formats_string.isEmpty()){
				formats_string = itr.next();
			}else{
				formats_string += ", " + itr.next();
			}
		}
		
		//Get files from CSR
		result = Utility.readURL(csr_script_url + "get_files.php?formats=" + Utility.urlEncode(formats_string));
		scanner = new Scanner(result);
		
		while(scanner.hasNextLine()){
			file = scanner.nextLine().trim();
			file = file.substring(0, file.length()-4);		//Remove "<br>"
			extension = Utility.getFilenameExtension(file);
			
			if(format_map.get(extension) == null){
				format_map.put(extension, new Vector<String>());
			}
			
			if(format_map.get(extension).size() < max_per_type){
				format_map.get(extension).add(file);
			}
		}
		
		for(Iterator<String> itr=format_map.keySet().iterator(); itr.hasNext();){
			files = format_map.get(itr.next());
			
			for(int i=0; i<files.size(); i++){
				test_files.add(files.get(i));
			}
		}
		
		//Display newest scripts
		if(DEBUG){
			System.out.println("Test files:\n");
			
			for(int i=0; i<test_files.size(); i++){
				System.out.println("  " + test_files.get(i));
			}
			
			System.out.println();
		}
		
		return test_files;
	}

	/**
	 * The script installer main.
	 * @param args the command line arguments
	 */
	public static void main(String args[])
	{				
		String csr_script_url = "http://isda.ncsa.uiuc.edu/NARA/CSR/php/search/";
		String script_download_path = "scripts/csr/";
		String configured_script_path = script_download_path.substring(0, script_download_path.length()-1) + "-configured/";
		String data_download_path = "data/csr/";
		TreeSet<String> methods = new TreeSet<String>();
		TreeSet<String> local_software = new TreeSet<String>();
		Vector<String> scripts = new Vector<String>();
		Vector<String> scripts_configured = new Vector<String>();
		Vector<String> test_files;
		File[] files;
		String shortcut = null;
		String executable_hint = null;
		String software, filename, scriptname;
		String buffer;
		ScriptDebugger debugger = new ScriptDebugger("ScriptDebugger.conf"); debugger.setDataPath(data_download_path);
		Script script;
		int tests = 5;
		int max_operation_time = 30000;
		int tmpi;
		double success_rate;
		boolean downloaded;
		boolean SOFTWARE_KEYS = false;
		boolean NO_CONFIG = false;
		boolean TEST_SCRIPTS = false;

		//Default arguments
		if(args.length == 0){
			//args = new String[]{"-method", "reg"};
			args = new String[]{"-?"};
		}
		
		//Process command line arguments
		for(int i=0; i<args.length; i++){
			if(args[i].equals("-?")){
				System.out.println("Usage: ScriptInstaller [options] [software]");
				System.out.println();
				System.out.println("Options: ");
				System.out.println("  -?: display this help");
				System.out.println("  -shortcut file.lnk: add an application from a shortcut");
				System.out.println("  -method x: set the method to use to determine what software is installed (wmic, reg)");
				System.out.println("  -noconfig: just download scripts and do not configure them");
				System.out.println("  -test n: download relevant test data and grind the obtained scripts n times");
				System.out.println("  -wait t: the amount of time to wait for an operation to complete (in milli-seconds)");
				System.out.println("  -url u: the url of the CSR server to use");
				System.out.println("  -keys: append software keys to script filenames");
				System.out.println();
				System.exit(0);
			}else if(args[i].equals("-shortcut")){
				shortcut = args[++i];
				
				if(shortcut.charAt(0) == '"'){	//Remove quotes
					shortcut = shortcut.substring(1, shortcut.length()-1);
				}
				
				//Use the shortcut name to identify the software in the CSR
				local_software.add(Utility.getFilenameName(shortcut));	
			}else if(args[i].equals("-method")){
				methods.add(args[++i]);
			}else if(args[i].equals("-noconfig")){
				NO_CONFIG = true;
			}else if(args[i].equals("-test")){
				TEST_SCRIPTS = true;
				tests = Integer.valueOf(args[++i]);
			}else if(args[i].equals("-wait")){
				max_operation_time = Integer.valueOf(args[++i]);
			}else if(args[i].equals("-url")) {
				csr_script_url = args[++i];
			}else if(args[i].equals("-keys")){
				SOFTWARE_KEYS = true;
			}else{
				software = args[i];
				
				if(software.charAt(0) == '"'){	//Remove quotes
					software = software.substring(1, software.length()-1);
				}
				
				local_software.add(software);
			}
		}
		
		if(!csr_script_url.endsWith("php/search/")){
			if(!csr_script_url.endsWith("/")){
				csr_script_url += "/php/search/";
			}else{
				csr_script_url += "php/search/";
			}
		}
		
		//Download scripts for this system
		if(!methods.isEmpty() || !local_software.isEmpty()){
			scripts = getSystemScripts(methods, csr_script_url, local_software);
			
			if(!Utility.exists(script_download_path)){
				new File(script_download_path).mkdir();
			}	
				
			System.out.println("\nDownloading scripts:\n");
			
			for(int i=0; i<scripts.size(); i++){
				filename = Utility.getFilename(getScriptFileName(scripts.get(i), SOFTWARE_KEYS));
				System.out.print("  " + filename);
				scriptname = scripts.get(i);
				
				if(SOFTWARE_KEYS){
					tmpi = scriptname.indexOf('/');
					if(tmpi != -1) scriptname = scriptname.substring(tmpi);
				}
				
				downloaded = Utility.downloadFile(script_download_path, Utility.getFilenameName(filename), csr_script_url + "download.php?file=" + scriptname);
				
				if(downloaded){
					System.out.println();
					scripts.set(i, script_download_path + filename);
				}else{
					System.out.println("  ..failed!");
				}
			}
		}else{	//Use previously downloaded scripts
			files = new File(script_download_path).listFiles();
			
			for(int i=0; i<files.length; i++){
				if(!files[i].getName().startsWith(".")){
					scripts.add(script_download_path + files[i].getName());
				}
			}
		}
		
		//Configure downloaded scripts
		if(!NO_CONFIG){
			//Set executable hint if shortcut was given and no other software was specified
			if(shortcut != null && local_software.size() == 1){
				executable_hint = Utility.getShortcutTarget(shortcut);
			}
			
			for(int i=0; i<scripts.size(); i++){
				debugger.configureScript(scripts.get(i), executable_hint);
			}
		}
		
		//Set scripts to configured names (checking if configured script actually exists)
		for(int i=0; i<scripts.size(); i++){
			filename = configured_script_path + Utility.getFilename(scripts.get(i));
			
			if(Utility.exists(filename)){
				scripts_configured.add(filename);
			}
		}
		
		scripts = scripts_configured;
		
		//Save icon if a shortcut was given (assume no other software was specified so we can get the needed alias from any script)
		if(shortcut != null && local_software.size() == 1){
			ImageUtility.saveIcon(configured_script_path + new Script(scripts.firstElement(), null).alias + ".jpg", shortcut);
		}
		
		//Test downloaded scripts on this system
		if(TEST_SCRIPTS){
			//Download test files
			test_files = getTestFiles(scripts, csr_script_url, 2);

			if(!Utility.exists(data_download_path)){
				new File(data_download_path).mkdir();
			}	
				
			System.out.println("\nDownloading test files:\n");
			
			for(int i=0; i<test_files.size(); i++){
				filename = Utility.getFilename(test_files.get(i));
				System.out.print("  " + filename);
				
				downloaded = Utility.downloadFile(data_download_path, Utility.getFilenameName(filename), csr_script_url + "download.php?file=" + test_files.get(i));
				
				if(downloaded){
					System.out.println();
					test_files.set(i, data_download_path + filename);
				}else{
					System.out.println("  ..failed!");
				}
			}
			
			//Test downloaded and configured scripts
			buffer = "";
			
			for(int i=0; i<scripts.size(); i++){
				script = new Script(scripts.get(i), null);
				
				if(script.operation.equals("convert") || script.operation.equals("open") || script.operation.equals("import")){
					success_rate = debugger.grindScript(scripts.get(i), tests, max_operation_time);
					
					if(success_rate > 0.5){
						System.out.println("[Using \"" + script.alias + "\"]");
						buffer += script.alias + "\n";
					}else{
						System.out.println("[Dropping \"" + script.alias + "\"]");
						buffer += "#" + script.alias + "\n";
					}
				}
			}
			
			Utility.save(configured_script_path + ".aliases.txt", buffer);
		}
	}
}