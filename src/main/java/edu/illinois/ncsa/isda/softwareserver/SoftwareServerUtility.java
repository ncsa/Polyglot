package edu.illinois.ncsa.isda.softwareserver;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import java.security.*;

/**
 * Utility functions.
 * @author Kenton McHenry
 */
public class SoftwareServerUtility
{
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
   *  @param filename the file to append to
   *  @param string the text to append to the file
   */
  public static void print(String filename, String string)
  {
    try{
      BufferedWriter outs = new BufferedWriter(new FileWriter(filename, true));
      outs.write(string);
      outs.close();
    }catch(Exception e) {e.printStackTrace();}
  }

  /**
   * Append a line to a file.
   *  @param filename the file to append to
   *  @param string the line to append to the file
   */
  public static void println(String filename, String string)
  {
    try{
      BufferedWriter outs = new BufferedWriter(new FileWriter(filename, true));
      outs.write(string);
      outs.newLine();
      outs.close();
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
