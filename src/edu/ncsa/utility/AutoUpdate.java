package edu.ncsa.utility;
import java.io.*;
import java.text.*;
import java.util.*;

/**
 * A class that performs automatic updates of files from a specified URL.
 * @author Kenton McHenry
 */
public class AutoUpdate
{
	String temp_path = null;
	TreeSet<String> excluded_types = new TreeSet<String>();
	TreeSet<String> windows_excluded_types = new TreeSet<String>();
	TreeSet<String> unix_excluded_types = new TreeSet<String>();
	TreeSet<String> excluded_folders = new TreeSet<String>();
	
	/**
	 * Class constructor.
	 * @param filename the name of the *.ini file containing the information needed for the update
	 */
	public AutoUpdate(String filename)
	{			
		Scanner scanner1, scanner2;
		String url = null;
		Vector<String> files = new Vector<String>();
		String string;
		
		//Read *.ini file
		try{
			scanner1 = new Scanner(new File(filename));
			String line;
			
			while(scanner1.hasNextLine()){
				line = scanner1.nextLine();
				
				if(line.charAt(0) == '!'){		//Read file/folder exclusions
					if(line.startsWith("!win:")){
						scanner2 = new Scanner(line.substring(5).trim());
						scanner2.useDelimiter("[\\s,]+");
	          
	          while(scanner2.hasNext()){
	          	windows_excluded_types.add(scanner2.next());
	          }
					}else if(line.startsWith("!unix:")){
						scanner2 = new Scanner(line.substring(6).trim());
						scanner2.useDelimiter("[\\s,]+");
	          
	          while(scanner2.hasNext()){
	          	unix_excluded_types.add(scanner2.next());
	          }
					}else{
						scanner2 = new Scanner(line.substring(1));
						scanner2.useDelimiter("[\\s,]+");
	          
	          while(scanner2.hasNext()){
	          	string = scanner2.next();
	          	
	          	if(string.endsWith("/")){
	          		excluded_folders.add(string);
	          	}else{
	          		excluded_types.add(string);
	          	}
	          }
					}
				}else{
					if(url == null){						//The URL to download from
						url = line;
					}else{											//Individual files at the above URL to download
						files.add(line);
					}
				}
			}
		}catch(Exception e) {e.printStackTrace();}
		
		//Make a temp directory    
    temp_path = ".update_" + new SimpleDateFormat("yyyyMMddhhmmss").format(new Date()) + "/";    
		new File(temp_path).mkdir();
		
		//Download files to temp directory
		String url_filename = Utility.getFilename(url);
		
		if(!url_filename.isEmpty()){	//If a file is directly specified just download that
			Utility.downloadFile(temp_path, null, url);
			
			if(Utility.getFilenameExtension(url_filename).equals("zip")){
				Utility.unzip(temp_path, temp_path + url_filename);
			}
		}else{												//Download each listed file individually
			for(int i=0; i<files.size(); i++){
				Utility.downloadFile(temp_path, null, url + files.get(i));
			}
		}
		
		//Display downloaded files
		File[] temp_files = new File(temp_path).listFiles();
		
		System.out.println("\nDownloaded:\n");
		
		for(int i=0; i<temp_files.length; i++){
			System.out.println("  " + temp_files[i].getName() + " (" + Utility.getBytes((int)temp_files[i].length()) + ")");
		}
	}
	
	/**
	 * Apply the update.
	 */
	public void apply()
	{
		System.out.println("\nApplying update...\n");
		apply(temp_path);
	}
	
	/**
	 * Apply the update to the given path.
	 * @param update_path the path to update
	 */
	private void apply(String update_path)
	{
	  File[] files = new File(update_path).listFiles();
		String os = System.getProperty("os.name");
	  String path, filename, extension;
	  boolean WINDOWS = os.contains("Windows");
	  boolean UNIX = os.contains("Linux") || os.contains("Mac");
	  int tmpi;
	  
	  for(int i=0; i<files.length; i++){
  		filename = Utility.unixPath(files[i].getAbsolutePath());
  		tmpi = filename.lastIndexOf(temp_path);	  	
  		
  		if(tmpi >= 0){
  			filename = filename.substring(tmpi + temp_path.length());
		  	path = Utility.getFilenamePath(filename);
		  	
		  	if(excluded_folders.contains(path)){
					System.out.println(" !" + filename);
		  	}else{
	  			if(!files[i].isDirectory()){
	  				extension = Utility.getFilenameExtension(filename);
	
	  				if(WINDOWS && windows_excluded_types.contains(extension)){
	  					System.out.println(" !" + filename);
	  				}else if(UNIX && unix_excluded_types.contains(extension)){
	  					System.out.println(" !" + filename);
	  				}else if(Utility.exists(filename)){
		  				if(excluded_types.contains(extension)){
		  					System.out.println(" !" + filename);
		  					
		  					if(!Utility.getMD5Checksum(filename).equals(Utility.getMD5Checksum(temp_path + filename))){
		  						Utility.copyFile(temp_path + filename, filename + ".new");
		  					}
		  				}else{
				  			System.out.println("  " + filename);
				  			Utility.copyFile(temp_path + filename, filename);
		  				}
	  				}else{
	  					System.out.println(" +" + filename);
			  			Utility.copyFile(temp_path + filename, filename);
	  				}
			  	}else{
			  		new File(filename).mkdir();
			  		apply(temp_path + filename);
			  	}
  			}
	  	}
	  }
	}
	
	/**
	 * Download and apply updates to a package.
	 * @param args command line arguments
	 */
	public static void main(String[] args)
	{
		AutoUpdate au = new AutoUpdate("AutoUpdate.ini");
		au.apply();
	}
}