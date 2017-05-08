package edu.illinois.ncsa.isda.softwareserver;
import kgm.utility.*;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import java.security.*;
import java.text.*;
import javax.xml.bind.DatatypeConverter;

/**
 * Utility functions.
 * @author Kenton McHenry
 */
public class SoftwareServerUtility
{

	/**
	 * Delete temporary files for a session under parent_folder
	 * 
	 * session temporary files' names start with "sessionid_"
	 * session log is ".session_" + session + ".log"
	 * 
	 * @param parent_folder folder contains session generated temporary files
	 * @param session	session id
	 */
  public static void deleteCachedFiles(String parent_folder, int session) 
  {
  	File dir = new File(parent_folder);
  	final String prefix = session+"_";
  	final String tmp_sessionlog = ".session_" + session + ".log";
  	
    File[] files = dir.listFiles(new FileFilter() {
	    public boolean accept(File file) {	    	
	    	if (file.getName().startsWith(prefix)) {
	        return true;
	      } else if (file.getName().equals(tmp_sessionlog)){
		      return true;
		    } else {
		    	return false;
		    }
	    }
    });
    
    for (File file : files) {
      Path path = Paths.get(file.getAbsolutePath());
      try {
        Files.delete(path);
      } catch (NoSuchFileException x) {
        System.err.format("%s: no such" + " file or directory%n", path);
      } catch (DirectoryNotEmptyException x) {
        System.err.format("%s not empty%n", path);
      } catch (IOException x) {
        // File permission problems are caught here.
        System.err.println(x);
      }
    }
  }
	
  /**
   * Check if the specified URL exists.
   * @param url the URL to check
   * @return true if the URL exists
   */
  @Deprecated
  public static boolean existsURL_bak(String url)
  {
    HttpURLConnection.setFollowRedirects(false);
    HttpURLConnection conn = null;
    boolean SUCCESS = false;
    
    try{
      conn = (HttpURLConnection)new URL(url).openConnection();
      conn.connect();
      
      //Try to open the stream, if it doesn't exist it will cause an exception.
      DataInputStream ins = new DataInputStream(new BufferedInputStream(conn.getInputStream()));
      ins.close();
      
      conn.disconnect();
      SUCCESS = true;
    }catch(FileNotFoundException e){
    }catch(Exception e){
      e.printStackTrace();
    }finally{
      if(conn != null) conn.disconnect();
    }
    
    return SUCCESS;
  }
  
  /**
   * Check if the specified URL exists.
   * @param url the URL to check
   * @return true if the URL exists
   */
  public static boolean existsURL(String url)
  {
    try{
      //HttpURLConnection.setFollowRedirects(false);
      HttpURLConnection.setFollowRedirects(true);
      HttpURLConnection connection = (HttpURLConnection)new URL(url).openConnection();
      connection.setRequestMethod("HEAD");

      return (connection.getResponseCode() == HttpURLConnection.HTTP_OK || connection.getResponseCode() == HttpURLConnection.HTTP_NO_CONTENT);
    }catch(Exception e){
			System.out.println("Error accessing: " + url);
      e.printStackTrace();
      return true;					//Return true to move on!
    }
  }

  /**
   * Read the contents of the specified URL and store it in a string.
   * @param url the URL to read
   * @param type the accepted content type
   * @return a string containing the URL contents
   */
  public static String readURL(String url, String type)
  {
    HttpURLConnection.setFollowRedirects(false);
    HttpURLConnection conn = null;
    BufferedReader ins;
    StringBuilder outs = new StringBuilder();
    char[] buffer = new char[1024];
    //String line;
    String contents = "";
    int tmpi;
    
    try{
      conn = (HttpURLConnection)new URL(url).openConnection();     
      if(type != null) conn.setRequestProperty("Accept", type);
      conn.connect();
      
     	ins = new BufferedReader(new InputStreamReader(conn.getInputStream()));
     
      /*
      while((line = ins.readLine()) != null){
        contents += line + "\n";
      }
      */
      
      do{
        tmpi = ins.read(buffer, 0, buffer.length);
        if(tmpi>0) outs.append(buffer, 0, tmpi);
      }while(tmpi>=0);
      
      contents = outs.toString();
 
      conn.disconnect();
    }catch(Exception e){
      e.printStackTrace();
    }finally{
      if(conn != null) conn.disconnect();
    }
    
    return contents;
  }

  /**
   * Read the contents of the specified URL and store it in a string.
   * @param url the URL to read
   * @return a string containing the URL contents
   */
  public static String readURL(String url)
  {
    return readURL(url, null);
  }

  /**
   * Post a file to a URL.
   * @param url the URL to post to
   * @param filename the name of the file to post
   * @param type the accepted content type
   * @param auth the authentication to use (e.g. user:password, null) 
   * @return a string the resulting URL contents
   */
  public static String postFile(String url, String filename, String type, String auth)
  {
    HttpURLConnection conn = null;
    OutputStream os;
    PrintWriter writer = null;
    BufferedInputStream bis;
    BufferedReader br;
    StringBuilder sb = new StringBuilder();
    String boundary = Long.toHexString(System.currentTimeMillis());
    String response = "";
    char[] char_buffer = new char[1024];
    byte[] byte_buffer = new byte[1024];
    int tmpi;

    try{
      conn = (HttpURLConnection)new URL(url).openConnection();
      conn.setDoInput(true);
      conn.setDoOutput(true);
      //conn.setRequestMethod("POST");
      conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
      if(type != null) conn.setRequestProperty("Accept", type);
			if(auth != null) conn.setRequestProperty("Authorization", "Basic " + DatatypeConverter.printBase64Binary(auth.getBytes()));
      conn.connect();

      //Upload file
      os = conn.getOutputStream();
      writer = new PrintWriter(new OutputStreamWriter(os), true);
      writer.print("--" + boundary + "\r\n");
      writer.print("Content-Disposition: form-data; name=\"file\"; filename=\"" + Utility.getFilename(filename) + "\";\r\n");
      //writer.print("Content-Type: " + URLConnection.guessContentTypeFromName(getFilename(filename)) + "\r\n");
      writer.print("Content-Type: " + Files.probeContentType((Path)Paths.get(filename)) + "\r\n");
      writer.print("Content-Transfer-Encoding: binary\r\n");
      writer.print("\r\n");
      writer.flush();

      bis = new BufferedInputStream(new FileInputStream(filename));

      do{
        tmpi = bis.read(byte_buffer, 0, byte_buffer.length);
        if(tmpi>0) os.write(byte_buffer, 0, tmpi);
      }while(tmpi>=0);

      os.flush();
      bis.close();

      writer.print("\r\n");
      writer.print("--" + boundary + "--\r\n");
      writer.flush();

      //Get response
      br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

      do{
        tmpi = br.read(char_buffer, 0, char_buffer.length);
        if(tmpi>0) sb.append(char_buffer, 0, tmpi);
      }while(tmpi>=0);

      response = sb.toString();

      conn.disconnect();
    }catch(Exception e){
      e.printStackTrace();
    }finally{
      if(writer != null) writer.close();
      if(conn != null) conn.disconnect();
    }

    return response;
  }

  /**
   * Post a file to a URL.
   * @param url the URL to post to
   * @param filename the name of the file to post
   * @return a string the resulting URL contents
   */
  public static String postFile(String url, String filename)
  {
    return postFile(url, filename, null, null);
  }

	/**
	 * Get the size of a local or remote file.
	 * @param filename the file name or URL
	 * @return the size of the file
	 */
	public static long getFileSize(String filename)
	{
		if(filename.startsWith("http")){
			HttpURLConnection conn = null;

			try{
				conn = (HttpURLConnection) new URL(filename).openConnection();
				conn.setRequestMethod("HEAD");
				conn.getInputStream();

				return conn.getContentLength();
			}catch(IOException e){
				return -1;
			}finally{
				conn.disconnect();
			}
		}else{
			return new File(filename).length();
		}
	}

	/**
	 * Get bytes in human readable form.
	 * @param bytes the number of bytes
	 * @return the human readable string representation
	 */
	public static String humanReadableBytes(long bytes)
	{
		int unit = 1024;
		
		if(bytes < unit){
			return bytes + " B";
		}else{
			int exp = (int)(Math.log(bytes)/Math.log(unit));
			return String.format("%.1f %sB", bytes/Math.pow(unit, exp), "KMGTPE".charAt(exp-1));
		}
	}

	/**
	 * Get the size of a local or remote file in a human readable form.
	 * @param filename the file name or URL
	 * @return the size of the file
	 */
	public static String getFileSizeHR(String filename)
	{
		return humanReadableBytes(getFileSize(filename));
	}

  /**
   * Execute the given command.
   * @param command the command
   * @param max_runtime the maximum allowed time to run (in milli-seconds, -1 indicates forever)
   * @param HANDLE_OUTPUT true if the process output should be handled
   * @param SHOW_OUTPUT true if the process output should be shown
   * @return the command output, null if the operation did not complete within the given time frame
   */
  public static String executeAndWait(String command, int max_runtime, boolean HANDLE_OUTPUT, boolean SHOW_OUTPUT)
  {
    Process process;
    TimedProcess timed_process;
		String output = null;
    if(!command.isEmpty()){
      try{
        process = Runtime.getRuntime().exec(command);

        if(max_runtime >= 0){
          timed_process = new TimedProcess(process, HANDLE_OUTPUT, SHOW_OUTPUT);

          if(timed_process.waitFor(max_runtime)){
						output = timed_process.getOutput();
					}
        }else{
          if(HANDLE_OUTPUT){
            output = TimedProcess.handleProcessOutput(process, SHOW_OUTPUT);
          }else{
            process.waitFor();
						output = "";
          }
        }
      }catch(Exception e) {e.printStackTrace();}
    }

    return output;
  }

  /**
   * Execute the given command.
   * @param command the command
   * @param max_runtime the maximum allowed time to run (in milli-seconds, -1 indicates forever)
   * @return the command output, null if the operation did not complete within the given time frame
   */
  public static String executeAndWait(String command, int max_runtime)
  {
    return executeAndWait(command, max_runtime, false, false);
  }

	/**
	 * Add authentication to a URL.
	 * @param url a URL
	 * @param authentication the "username:password"
	 * @return the URL with the authentication included
	 */
	public static String addAuthentication(String url, String authentication)
	{
		if(authentication != null && !authentication.isEmpty()){
			String[] strings = url.split("//");
			return strings[0] + "//" + authentication + "@" + strings[1];
		}else{
			return url;
		}
	}

	/**
	 * Set default authentication for HTTP requests.
	 * @param authentication the "username:password"
	 */
	public static void setDefaultAuthentication(String authentication)
	{
		if(authentication != null && !authentication.isEmpty()){
			String[] strings = authentication.split(":");
			final String username = strings[0];
			final String password = strings[1];

			Authenticator.setDefault(new Authenticator(){
				protected PasswordAuthentication getPasswordAuthentication(){
					return new PasswordAuthentication(username, password.toCharArray());
				}
			});
		}
	}

  /**
   * Append a string to a file.
   * @param string the text to append to the file
   * @param filename the file to append to
   */
  public static void print(String string, String filename)
  {
    try{
      BufferedWriter outs = new BufferedWriter(new FileWriter(filename, true));
      outs.write(string);
			outs.close();
    }catch(Exception e){
			e.printStackTrace();
		}
  }

  /**
   * Append a line to a file.
   * @param string the line to append to the file
   * @param filename the file to append to
   */
  public static void println(String string, String filename)
  {
    try{
      BufferedWriter outs = new BufferedWriter(new FileWriter(filename, true));
      outs.write(string);
      outs.newLine();
			outs.close();
    }catch(Exception e){
			e.printStackTrace();
		}
  }

	/**
	 * Get the current date and time (useful for logs, is thread safe).
	 * @return a string containing the current date and time
	 */
	public static String getTimeStamp()
	{
		return new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy").format(new Date(System.currentTimeMillis()));
	}

	/**
	 * Unzip a zip file into the given path.
	 * @param path the path to unzip to
	 * @param filename the name of the zip file
	 */
	public static void unzip(String path, String filename)
	{
		byte[] buffer = new byte[1024];
		int length;
		String root = null;
	
		if(Utility.getFilenameExtension(filename).equals("zip")){
			try{
				ZipFile zf = new ZipFile(filename);
				Enumeration entries = zf.entries();
				
				while(entries.hasMoreElements()){
					ZipEntry entry = (ZipEntry)entries.nextElement();
					if(root == null) root = entry.getName();
					
					if(entry.isDirectory()){
						(new File(path + entry.getName())).mkdir();
					}else{						
						InputStream ins = zf.getInputStream(entry);
						OutputStream outs = new BufferedOutputStream(new FileOutputStream(path + entry.getName()));
						
						while((length = ins.read(buffer)) >= 0){
							outs.write(buffer, 0, length);
						}
						
						ins.close();
						outs.close();
					}
				}
				
				zf.close();

				//Rename extracted files to match filename
				String new_root = Utility.getFilenameName(filename);
				if(new_root.endsWith(".checkin")) new_root = new_root.substring(0, new_root.length()-8);
				Files.move(Paths.get(path + root), Paths.get(path + new_root));
			}catch(Exception e) {e.printStackTrace();}
		}
	}
  
/**
   * Zip a file or directory.
   * @param output the output zip file
   * @param files a file
   */
  public static void zip(String output, String file)
  {
    try{
      ZipOutputStream outs = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(output)));
      zip(outs, Utility.getFilenamePath(file), Utility.getFilename(file));
      outs.close();
    }catch(Exception e) {e.printStackTrace();}
  }

  /**
   * Zip a given set of files.
   * @param output the output zip file
   * @param files a vector of files
   */
  public static void zip(String output, Vector<String> files)
  {
    try{
      ZipOutputStream outs = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(output)));

      for(int i=0; i<files.size(); i++){
        zip(outs, Utility.getFilenamePath(files.get(i)), Utility.getFilename(files.get(i)));
      }

      outs.close();
    }catch(Exception e) {e.printStackTrace();}
  }

	/**
	 * Add a file to the given zip file output stream.
	 * @param outs the zip file output stream
	 * @param path the path to the file (minus the portion of the path that should be stored in the zip file)
	 * @param filename the name of the file to add (plus the portion of the path that should be stored in the zip file)
	 */
	public static void zip(ZipOutputStream outs, String path, String filename)
	{
		File file = new File(path + filename);

		try{		
			if(file.isDirectory()){
				File[] files = file.listFiles();
				
				outs.putNextEntry(new ZipEntry(filename + "/"));
	
				for(int i=0; i<files.length; i++){
					zip(outs, path, file.getName() + "/" + files[i].getName());
				}
			}else{
				if(file.exists()){
					BufferedInputStream ins = new BufferedInputStream(new FileInputStream(file));
					byte[] buffer = new byte[1024];
					int length;
				
					outs.putNextEntry(new ZipEntry(filename));
				
					while((length = ins.read(buffer, 0, 1024)) != -1){
						outs.write(buffer, 0, length);
					}
				
					ins.close();
				}
			}
		}catch(Exception e) {e.printStackTrace();}
	}

  /**
   * A main for debug purposes.
   * @param args the command line arguments
   */
  public static void main(String args[])
  {
		if(true){		//Test timed processes
			System.out.println("Execution process...");
			String output = executeAndWait("scripts/sh/tar_convert.sh /home/kmchenry/git/polyglot/tmp/SoftwareServer/Cache/592_101_ObjectCategories.tar.gz /home/kmchenry/git/polyglot/tmp/SoftwareServer/Cache/592_101_ObjectCategories.html /home/kmchenry/git/polyglot/tmp/SoftwareServer/Temp/592_1427055418584_", 500, true, true);
			System.out.println("Finished executing (completed = " + (output != null) + ")!");
		}
  }
}
