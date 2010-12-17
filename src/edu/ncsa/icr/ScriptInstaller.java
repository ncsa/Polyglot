package edu.ncsa.icr;
import edu.ncsa.utility.*;
import java.io.File;
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
	 * @return the script file name
	 */
	private static String getScriptFileName(String script)
	{
		String filename = null;
		int dash, dot;
		
		script = Utility.getFilename(script);
		dash = script.lastIndexOf('-');
		dot = script.lastIndexOf('.');
		
		if(dash >= 0 && dot >= 0){
			filename = script.substring(0, dash) + script.substring(dot);
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
	 * @return a list of URL's to scripts for locally installed software
	 */
	public static Vector<String> getSystemScripts(TreeSet<String> methods, String csr_script_url)
	{
		String os = System.getProperty("os.name");
		String arch = System.getProperty("os.arch");
		TreeSet<String> local_software = new TreeSet<String>();
		TreeSet<String> scripted_software = new TreeSet<String>();
		Vector<String> local_scripted_software = new Vector<String>();
		Vector<String> scripts = new Vector<String>();
		TreeMap<String,String> newest_scripts = new TreeMap<String,String>();
		Process p;
		Scanner p_output, scanner;
		String method, line, software, result;
		String filename;
		int timestamp;
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
			
			//Get scripts
			software = "";
			
			for(int i=0; i<local_scripted_software.size(); i++){
				if(software.isEmpty()){
					software = local_scripted_software.get(i);
				}else{
					software += ", " + local_scripted_software.get(i);
				}
			}
				
			result = Utility.readURL(Utility.urlEncode(csr_script_url + "get_scripts.php?software=" + software));
			scanner = new Scanner(result);
			
			while(scanner.hasNextLine()){
				line = scanner.nextLine().trim();
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
				filename = getScriptFileName(scripts.get(i));
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
	 * The script installer main.
	 * @param args the command line arguments
	 */
	public static void main(String args[])
	{		
		String csr_url = "http://isda.ncsa.uiuc.edu/NARA/CSR/";
		String csr_script_url = "http://isda.ncsa.uiuc.edu/~kmchenry/tmp/CSRDebug/";
		String download_path = "scripts/csr/";
		TreeSet<String> methods = new TreeSet<String>();
		Vector<String> scripts;
		String script;
		boolean NO_CONFIG = false;
		
		//Debug arguments
		if(true && args.length == 0){
			args = new String[]{"-method", "reg"};
		}
		
		//Process command line arguments
		for(int i=0; i<args.length; i++){
			if(args[i].equals("-?")){
				System.out.println("Usage: ScriptInstaller [options]");
				System.out.println();
				System.out.println("Options: ");
				System.out.println("  -?: display this help");
				System.out.println("  -method x: set the method to use to determine what software is installed (wmic, reg)");
				System.out.println("  -noconfig: just download scripts and do not configure them");
				System.out.println();
				System.exit(0);
			}else if(args[i].equals("-method")){
				methods.add(args[++i]);
			}else if(args[i].equals("-noconfig")){
				NO_CONFIG = true;
			}
		}
		
		//Download scripts for this system
		scripts = getSystemScripts(methods, csr_script_url);
		
		if(!Utility.exists(download_path)){
			new File(download_path).mkdir();
		}	
			
		System.out.println("\nDownloading scripts:\n");
		
		for(int i=0; i<scripts.size(); i++){
			script = Utility.getFilename(getScriptFileName(scripts.get(i)));
			System.out.println("  " + script);
			
			Utility.downloadFile(download_path, Utility.getFilenameName(script), csr_url + scripts.get(i));
			scripts.set(i, download_path + script);
		}
		
		//Configure downloaded scripts
		if(!NO_CONFIG){
			ScriptDebugger debugger = new ScriptDebugger("ScriptDebugger.ini");
			
			for(int i=0; i<scripts.size(); i++){
				debugger.configureScript(scripts.get(i));
			}
		}
	}
}