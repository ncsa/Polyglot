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
   * Execute the given command.
   * @param command the command
   * @param max_runtime the maximum allowed time to run (in milli-seconds, -1 indicates forever)
   * @param HANDLE_OUTPUT true if the process output should be handled
   * @param SHOW_OUTPUT true if the process output should be shown
   * @return true if the operation completed within the given time frame
   */
  public static boolean executeAndWait(String command, int max_runtime, boolean HANDLE_OUTPUT, boolean SHOW_OUTPUT)
  {
    Process process;
    TimedProcess timed_process;
    boolean COMPLETE = false;

    if(!command.isEmpty()){
      try{
        process = Runtime.getRuntime().exec(command);

        if(max_runtime >= 0){
          timed_process = new TimedProcess(process, HANDLE_OUTPUT, SHOW_OUTPUT);
          COMPLETE = timed_process.waitFor(max_runtime); System.out.println();
        }else{
          if(HANDLE_OUTPUT){
            SoftwareServerUtility.handleProcessOutput(process, SHOW_OUTPUT);
          }else{
            process.waitFor();
          }

          COMPLETE = true;
        }
      }catch(Exception e) {e.printStackTrace();}
    }

    return COMPLETE;
  }

  /**
   * Execute the given command.
   * @param command the command
   * @param max_runtime the maximum allowed time to run (in milli-seconds, -1 indicates forever)
   * @return true if the operation completed within the given time frame
   */
  public static boolean executeAndWait(String command, int max_runtime)
  {
    return executeAndWait(command, max_runtime, false, false);
  }

  /**
   * Handle the output of a process.
   * @param process the process
   * @param SHOW_OUTPUT true if the output should be printed
   */
  public static void handleProcessOutput(Process process, boolean SHOW_OUTPUT)
  {
    String line = null;

		/*
    DataInputStream ins = new DataInputStream(process.getInputStream());

    try{
      while((line = ins.readLine()) != null){
        if(SHOW_OUTPUT) System.out.println(line);
      }
    }catch(Exception e) {e.printStackTrace();}
		*/

		BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
		BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

		try{
			//Read output
			while((line = stdInput.readLine()) != null){
    		if(SHOW_OUTPUT) System.out.println(line);
			}

			//Read errors
			while((line = stdError.readLine()) != null){
				if(SHOW_OUTPUT) System.out.println(line);
			}
    }catch(Exception e) {e.printStackTrace();}
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
}
