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
      HttpURLConnection.setFollowRedirects(false);
      HttpURLConnection connection = (HttpURLConnection)new URL(url).openConnection();
      connection.setRequestMethod("HEAD");
 
      return (connection.getResponseCode() == HttpURLConnection.HTTP_OK || connection.getResponseCode() == HttpURLConnection.HTTP_NO_CONTENT);
    }catch(Exception e){
      e.printStackTrace();
      return false;
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
      
      if(url.endsWith(".gz")){
      	ins = new BufferedReader(new InputStreamReader(new GZIPInputStream(conn.getInputStream())));
      }else{
      	ins = new BufferedReader(new InputStreamReader(conn.getInputStream()));
      }
     
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
}
