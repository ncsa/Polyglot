package edu.ncsa.icr.polyglot;
import edu.ncsa.utility.*;
import java.io.*;

/**
 * A structure to hold file information.
 * @author Kenton McHenry
 */
public class FileInfo
{
  public String absolutename;    //path + name + ext
  public String filename;        //name + ext
  public String path;
  public String name;
  public String ext;
  public int size;
  public boolean FLAG = false;
  
  /**
   * Class constructor to initialize the file information.
   *  @param absolutename the absolute name of the file
   */
  public FileInfo(String absolutename)
  {
    this.absolutename = absolutename;
    
    //Set filename
    int tmpi = absolutename.lastIndexOf('/');
    
    if(tmpi >= 0){
      filename = absolutename.substring(tmpi+1);
      path = absolutename.substring(0, tmpi+1);
    }else{
      filename = absolutename;
    }
    
    //Set name and extension
    tmpi = filename.indexOf('.');
    
    if(tmpi >= 0){
      name = filename.substring(0, tmpi);
      ext = filename.substring(tmpi+1);
      
      //Correct for *.gz and *.zip files
      tmpi = ext.indexOf('.');
      if(tmpi >= 0){
        ext = ext.substring(0, tmpi);
      }
    }
    
    //Obtain the file size
    File f = new File(absolutename);
    size = (int)f.length();
  }
  
  /**
   * Convert this class to a string.
   *  @return the String version of this FileInfo structure
   */
  public String toString()
  {
    String str = "";
    
    if(FLAG){
      str += "<html><s>"; 
      str += filename;
      str += "&nbsp;&nbsp;&nbsp;&nbsp;<b>(" + Utility.getBytes(size) + ")</b>";
      str += "</s></html>";
    }else{
      str += "<html>"; 
      str += filename;
      str += "&nbsp;&nbsp;&nbsp;&nbsp;<b>(" + Utility.getBytes(size) + ")</b>";
      str += "</html>";
    }
    
    return str;
  }
}