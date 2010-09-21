package edu.ncsa.icr;
import edu.ncsa.icr.SoftwareReuseAuxiliary.*;
import edu.ncsa.utility.*;
import java.io.*;
import java.util.*;

/**
 * A tool for debugging ICR scripts.
 * @author Kenton McHenry
 */
public class SoftwareReuseScriptDebugger
{
	/**
	 * Check if the specified file exists.
	 * @param filename the name of the file to check
	 * @return true file exists
	 */
	public static boolean exists(String filename)
	{
		File file = new File(filename);
		
		if(file.exists() && file.length() > 0){
			return true;
		}
		
		return false;
	}
	
	/**
	 * Check the conversions claimed by the given script.
	 * @param script_filename the script filename
	 * @param data_path a path to test data
	 */
	public static void checkConversions(String script_filename, String data_path)
	{
		Script script;
		Script monitor_script = null;
		Script kill_script = null;
		Vector<Script> input_scripts, output_scripts;
		TreeMap<String,String> test_files = new TreeMap<String,String>();
		Iterator<String> input_itr, output_itr;
		String temp_path = data_path + "debug/";
		String input_type, output_type;
		String input_file, output_file;
		String filename, extension;
		int input_script_index;
		int max_operation_time = 30000;
		
		if(!Utility.exists(script_filename)){
			System.out.println("Script doesn't exist!");
		}else if(!Utility.exists(data_path)){
			System.out.println("Data directory doesn't exist!");
		}else{
  		script = new Script(script_filename, null);
			temp_path = data_path + "debug/";
			
			if(!Utility.exists(temp_path)){
				new File(temp_path).mkdir();
			}
			
			//Read in test files
			File folder = new File(data_path);
			File[] folder_files = folder.listFiles();
			
			for(int i=0; i<folder_files.length; i++){
				if(!folder_files[i].isDirectory()){
					filename = folder_files[i].getName();
					
					if(filename.charAt(0) != '.'){
						test_files.put(Utility.getFilenameExtension(filename), filename);
					}
				}
			}
			
			//Display test file map
			System.out.println("\nTest files:");
			
			for(Iterator<String> itr=test_files.keySet().iterator(); itr.hasNext();){
				extension = itr.next();
				System.out.println("  " + extension + " -> " + test_files.get(extension));
			}
			
			System.out.println();
			
			//Check for helper scripts
			if(Utility.exists(script.getOperationScriptname("monitor"))){
				monitor_script = new Script(script.getOperationScriptname("monitor"), null);
				monitor_script.execute();
			}
			
			if(Utility.exists(script.getOperationScriptname("kill"))){
				kill_script = new Script(script.getOperationScriptname("kill"), null);
			}
			
			//Test the script
			if(script.operation.equals("convert")){
				System.out.println("Testing convert script \"" + script.filename + "\":");
				input_itr = script.inputs.iterator();
			
				while(input_itr.hasNext()){
					input_type = input_itr.next();
					
					if(test_files.containsKey(input_type)){
						input_file = data_path + test_files.get(input_type);
						output_itr = script.outputs.iterator();
						
						while(output_itr.hasNext()){
							output_type = output_itr.next();
							output_file = temp_path + Utility.getFilenameName(input_file) + "." + output_type;
							System.out.print("  " + input_type + "->" + output_type + ": \n");

							System.out.print("    converting file");
							script.executeAndWait(input_file, output_file, temp_path, max_operation_time);
							
							if(exists(output_file)){
								System.out.println("    [success]");
								
								//Hide the output file to avoid errors with future tests
								new File(output_file).renameTo(new File(temp_path + "." + Utility.getFilenameName(output_file) + "." + System.currentTimeMillis() + "." + output_type));		
							}else{
								System.out.println("    [failure]");
							}
						}
					}
				}
			}else if(script.operation.equals("open") || script.operation.equals("import")){
				System.out.println("Testing input script \"" + script.filename + "\":");
				output_scripts = script.getAssociatedOutputScripts();

				if(!output_scripts.isEmpty()){
					input_itr = script.inputs.iterator();
				
					while(input_itr.hasNext()){
						input_type = input_itr.next();
						
						if(test_files.containsKey(input_type)){
							System.out.print("  " + input_type + ": \n");

							output_type = output_scripts.firstElement().outputs.first();
							input_file = data_path + test_files.get(input_type);
							output_file = temp_path + Utility.getFilenameName(input_file) + "." + output_type;
													
							System.out.print("    loading file");
							script.executeAndWait(input_file, null, temp_path, max_operation_time);
							
							System.out.print("    generating output");
							output_scripts.firstElement().executeAndWait(null, output_file, temp_path, max_operation_time);
							
							if(exists(output_file)){
								System.out.println("    [success]");
								
								//Hide the output file to avoid errors with future tests
								new File(output_file).renameTo(new File(temp_path + "." + Utility.getFilenameName(output_file) + "." + System.currentTimeMillis() + "." + output_type));		
							}else{
								System.out.println("    [failure]");
							}
						}
					}
				}else{
					System.out.println("  No associated output scripts found!");
				}
			}else if(script.operation.equals("save") || script.operation.equals("export")){
				System.out.println("Testing output script \"" + script.filename + "\":");
				input_scripts = script.getAssociatedInputScripts();
				input_script_index = -1;
				
				if(!input_scripts.isEmpty()){	
					input_type = null;

					//Find an input script with an available input test file
					for(int i=0; i<input_scripts.size(); i++){
						input_itr = input_scripts.get(i).inputs.iterator();
						
						while(input_itr.hasNext()){
							input_type = input_itr.next();
							
							if(test_files.containsKey(input_type)){
								input_script_index = i;
								break;
							}
						}
					}
					
					if(input_script_index >= 0){
						input_file = data_path + test_files.get(input_type);
						output_itr = script.outputs.iterator();
					
						while(output_itr.hasNext()){
							output_type = output_itr.next();
							output_file = temp_path + Utility.getFilenameName(input_file) + "." + output_type;
							System.out.print("  " + output_type + ": \n");
													
							System.out.print("    loading file");
							input_scripts.get(input_script_index).executeAndWait(input_file, null, temp_path, max_operation_time);
							
							System.out.print("    generating output");
							script.executeAndWait(null, output_file, temp_path, max_operation_time);

							if(exists(output_file)){
								System.out.println("    [success]");
								
								//Hide the output file to avoid errors with future tests
								new File(output_file).renameTo(new File(temp_path + "." + Utility.getFilenameName(output_file) + "." + System.currentTimeMillis() + "." + output_type));		
							}else{
								System.out.println("    [failure]");
							}
						}
					}else{
						System.out.println("  No matching test files were found for associated input scripts!");
					}
				}else{
					System.out.println("  No associated input scripts found!");
				}
			}
		}
	}	
	
	/**
	 * Configure the given ICR script(s) to run on the current machine.
	 * @param script_filename the script filename
	 * @param search_path the search path to use when searching for executables
	 */
	public static void configureScript(String script_filename, Vector<String> search_path)
	{
		String script_path, script_extension;
		String script_output_path;
		Vector<String> script_filenames = new Vector<String>();
		Vector<String> matches;
		Scanner scanner;
		String line, executable, buffer;
		boolean SAVE;
		int tmpi;
		
		script_path = Utility.getFilenamePath(script_filename);
		script_extension = Utility.getFilenameExtension(script_filename);
		
		//Expand wildcard if present
		if(script_filename.contains("*")){
			File[] files = new File(script_path).listFiles();
			
			for(int i=0; i<files.length; i++){
				if(files[i].getName().endsWith("." + script_extension)){
					script_filenames.add(files[i].getName());
				}
			}
		}else{
			script_filenames.add(Utility.getFilename(script_filename));
		}
		
		//Create an output directory
		script_output_path = script_path.substring(0, script_path.length()-1) + "-configured/";
		
		if(!Utility.exists(script_output_path)){
			new File(script_output_path).mkdir();
		}
					
		//Process each script
		System.out.println();
		
		for(int i=0; i<script_filenames.size(); i++){
			script_filename = script_filenames.get(i);
			System.out.println("Configuring script \"" + script_path + script_filename + "\":");
			
			try{
				scanner = new Scanner(new File(script_path + script_filename));
				buffer = "";
				SAVE = true;
				
				while(scanner.hasNextLine()){
					line = scanner.nextLine();
					
					if(script_extension.equals("ahk")){
						if(line.startsWith("Run") || line.startsWith("RunWait")){
							//Remove command
							tmpi = line.indexOf(',');
							buffer += line.substring(0,tmpi) + ", ";
							line = line.substring(tmpi+1).trim();
							
							//Get executable
							if(line.startsWith("\"")){
								line = line.substring(1);
								tmpi = line.indexOf("\"");
								executable = line.substring(0, tmpi);
								line = line.substring(tmpi+1).trim();
							}else{
								tmpi = line.indexOf(' ');
								executable = line.substring(0, tmpi);
								line = line.substring(tmpi+1).trim();
							}
							
							System.out.print("  checking for " + executable + "...");
							
							if(Utility.exists(executable)){
								System.out.println(" yes");
								buffer += "\"" + executable + "\" " + line;
							}else{
								System.out.println(" no");
								
								//Find options
								matches = new Vector<String>();
								
								for(int j=0; j<search_path.size(); j++){
									System.out.println("  searching: " + search_path.get(j));
									matches.addAll(Utility.search(search_path.get(j), Utility.getFilename(Utility.unixPath(executable))));
								}
								
								//Display matches to user for selection
								if(!matches.isEmpty()){
									System.out.println("  found " + matches.size() + " matches:");

									for(int j=0; j<matches.size(); j++){
										System.out.println("    [" + (j+1) + "] " + matches.get(j));
									}
									
									tmpi = Integer.valueOf(System.console().readLine("  enter choice: "))-1;
									
									if(tmpi < 0){
										System.out.println("  no matches found!");
										SAVE = false;
										break;
									}else{
										buffer += "\"" + matches.get(tmpi) + "\" " + line;
									}
								}else{
									System.out.println("  no matches found!");
									SAVE = false;
									break;
								}
							}
						}else{
							buffer += line + "\n";
						}
					}
				}
				
				if(SAVE){
					System.out.println("  saving...");
					Utility.save(script_output_path + script_filename, buffer);
				}
			}catch(Exception e) {e.printStackTrace();}
		}
	}
	
	/**
	 * Debug an ICR script.
	 * @param args the input arguments
	 */
	public static void main(String args[])
	{
		String data_path = "./";
		Vector<String> search_path = new Vector<String>();
		String script_filename;
		
		//Test arguments
		if(true && args.length == 0){
			args = new String[]{"scripts/ahk/A3DReviewer_open.ahk"};
			//args = new String[]{"scripts/ahk/A3DReviewer_export.ahk"};
			//args = new String[]{"scripts/ahk/Blender_convert.ahk"};
		}
		
		//Read in *.ini file
	  try{
	    BufferedReader ins = new BufferedReader(new FileReader("SoftwareReuseScriptDebugger.ini"));
	    Scanner scanner;
	    String line, key, value;
	    
	    while((line=ins.readLine()) != null){
	      if(line.contains("=")){
	        key = line.substring(0, line.indexOf('='));
	        value = line.substring(line.indexOf('=')+1);
	        
	        if(key.charAt(0) != '#'){
	        	if(key.equals("DataPath")){
	      			data_path = value + "/";
	          }else if(key.equals("SearchPath")){
	          	scanner = new Scanner(value);
	          	scanner.useDelimiter(";");
	          	
	          	while(scanner.hasNext()){
	          		search_path.add(scanner.next().trim());
	          	}
	          }
	        }
	      }
	    }
	    
	    ins.close();
	  }catch(Exception e) {e.printStackTrace();}

	  //Display configuration
	  if(false){
		  System.out.println();
		  System.out.println("Data path: " + data_path);
		  System.out.print("Search path: ");
		  
		  for(int i=0; i<search_path.size(); i++){
		  	if(i > 0) System.out.print(";");
		  	System.out.print(search_path.get(i));
		  }
		  
		  System.out.println();
	  }
	  
		//Check arguments
		if(args.length < 1){
			System.out.println("Usage: SoftwareReuseScriptDebugger [-config] script_name");
			System.exit(1);
		}else{
			if(args[0].equals("-config")){
				configureScript(args[1], search_path);
			}else{
				checkConversions(args[0], data_path);
			}
		}
	}
}