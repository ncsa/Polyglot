package edu.ncsa.icr;
import edu.ncsa.icr.ICRAuxiliary.*;
import edu.ncsa.utility.*;
import java.io.*;
import java.util.*;

/**
 * A tool for debugging ICR scripts.
 * @author Kenton McHenry
 */
public class ICRScriptDebugger
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
	 * Test an ICR script.
	 * @param args the input arguments
	 */
	public static void main(String args[])
	{
		Script script;
		Script monitor_script = null;
		Script kill_script = null;
		Vector<Script> input_scripts, output_scripts;
		String script_filename;
		String data_path = "./";
		String temp_path = data_path + "debug/";
		TreeMap<String,String> test_files = new TreeMap<String,String>();
		Iterator<String> input_itr, output_itr;	
		String input_type, output_type;
		String input_file, output_file;
		String filename, extension;
		int input_script_index;
		int max_operation_time = 30000;
		
		//Test arguments
		if(true && args.length == 0){
			//args = new String[]{"scripts/ahk/A3DReviewer_open.ahk", "C:/Kenton/Data/Temp/PolyglotDemo"};
			//args = new String[]{"scripts/ahk/A3DReviewer_export.ahk", "C:/Kenton/Data/Temp/PolyglotDemo"};
			args = new String[]{"scripts/ahk/Blender_convert.ahk", "C:/Kenton/Data/Temp/PolyglotDemo"};
		}

		if(args.length < 2){
			System.out.println("Usage: ICRScriptDebugger script_name data_path");
		}else{
			script_filename = args[0];
			data_path = args[1] + "/";

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
				System.out.println("Test files:");
				
				for(Iterator<String> itr=test_files.keySet().iterator(); itr.hasNext();){
					extension = itr.next();
					System.out.println("  " + extension + " -> " + test_files.get(extension));
				}
				
				System.out.println();
				
				//Check for helper scripts
				if(Utility.exists(script.getOperationScriptname("monitor"))){
					monitor_script = new Script(script.getOperationScriptname("monitor"), null);
					monitor_script.executeAndWait();
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
	}
}