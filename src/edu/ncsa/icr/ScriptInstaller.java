package edu.ncsa.icr;
import edu.ncsa.utility.*;
import java.io.*;
import java.util.*;
import java.sql.*;

public class ScriptInstaller
{
	public static void main(String args[])
	{
		String os = System.getProperty("os.name");
		String arch = System.getProperty("os.arch");
		TreeSet<String> methods = new TreeSet<String>();
		TreeSet<String> software = new TreeSet<String>();
		String temp_path = Utility.exists("tmp") ? "tmp/" : "./";
		Process p;
		Scanner p_output, scanner;
		String method, line, name;
		
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
				System.out.println();
				System.exit(0);
			}else if(args[i].equals("-method")){
				methods.add(args[++i]);
			}
		}
		
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
							
							name = scanner.next().trim();
							
							if(!name.isEmpty()){
								software.add(name);
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
							
							name = scanner.nextLine().trim();
							
							if(!name.startsWith("@")){
								software.add(name);
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
								
								name = scanner.nextLine().trim();
								
								if(!name.startsWith("@")){
									software.add(name);
								}
							}
						}
					}
				}catch(Exception e) {e.printStackTrace();}
			}else{
				System.out.println("Method \"" + method + "\" not available on " + os);
			}
		}

		if(!software.isEmpty()){
			//Display found software
			if(false){
				System.out.println("Found software:\n");
				
				for(Iterator<String> itr=software.iterator(); itr.hasNext();){
					System.out.println("  " + itr.next());
				}
			}
			
			//Search CSR
	  	Connection connection = null;
	  	Statement statement;
	  	ResultSet result;
	  	String application, input_format, output_format, tmp;
	  	
			try{
	  		//Open connection to database
	  		connection = DriverManager.getConnection("jdbc:mysql://isda.ncsa.uiuc.edu/csr", "demo", "demo");
	  		
	  		//Query the database
	  		statement = connection.createStatement();
	  		statement.executeQuery("SELECT software.name, inputs.default_extension, outputs.default_extension FROM conversions, software, formats AS inputs, formats AS outputs WHERE conversions.software=software.software_id AND conversions.input_format=inputs.format_id AND conversions.output_format=outputs.format_id");
	  		result = statement.getResultSet();
	  		
	  		while(result.next()){
	  			application = result.getString("software.name");
	  			input_format = result.getString("inputs.default_extension");
	  			output_format = result.getString("outputs.default_extension");
	  			
	  			//System.out.println(application +", " + input_format + ", " + output_format);
	  		}
	  	
		  	//Close connection
	  		result.close();
	  		statement.close();
				connection.close();  		
	  	}catch(Exception e) {e.printStackTrace();}
	  	
			for(Iterator<String> itr=software.iterator(); itr.hasNext();){
				System.out.println("  " + itr.next());
			}
		}
	}
}