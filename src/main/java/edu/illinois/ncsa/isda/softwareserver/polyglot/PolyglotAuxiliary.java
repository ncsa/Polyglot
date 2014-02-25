package edu.illinois.ncsa.isda.softwareserver.polyglot;
import kgm.utility.*;
import java.io.*;
import java.util.*;

/**
 * Helper classes for the polyglot package.
 * @author Kenton McHenry
 */
public class PolyglotAuxiliary
{
	/**
	 * A simple structure to store a single conversion task.
	 */
	public static class Conversion<V extends Comparable,E>
	{
		public V input;
		public V output;
		public E edge;
		
		public Conversion() {}
		
		/**
		 * Class constructor.
		 * @param input the input vertex
		 * @param output the output vertex
		 * @param edge the edge
		 */
		public Conversion(V input, V output, E edge)
		{
			this.input = input;
			this.output = output;
			this.edge = edge;
		}
	}
	
	/**
	 * A convenient structure for holding vertex information when displaying an IOGraph.
	 */
	public static class Point2D implements Comparable
	{
	  public String text;
	  public int x;
	  public int y;
	    
	  /**
	   * Class constructor.
	   * @param text the text to display with this vertex
	   */
	  public Point2D(String text)
	  {
	    this.text = text;
	    x = 0;
	    y = 0;
	  }

		/**
		 * Compare this object to another object.
		 * @param object the object to compare to
		 * @return 0 if the same, -1 if less, and 1 if greater
		 */
		public int compareTo(Object object)
		{
			if(this == object){
				return 0;
			}else if(object instanceof Point2D){
				return text.compareTo(((Point2D)object).text);
			}else{
				return -1;
			}
		}
		
	  /**
	   * Create vertices from a vector names.
	   * @param strings the vector of names
	   * @return the vector of vertices
	   */
	  public static Vector<Point2D> getVertices(Vector<String> strings)
	  {
	  	Vector<Point2D> vertices = new Vector<Point2D>();
	  	
	  	for(int i=0; i<strings.size(); i++){
	      vertices.add(new Point2D(strings.get(i)));
	  	}
	  	
	  	return vertices;
	  }
	}
	
	/**
	 * A structure to hold file information.
	 */
	public static class FileInformation
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
	  public FileInformation(String absolutename)
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
	
	/**
	 * A structure to store information about Polyglot requests.
	 */
	public static class RequestInformation
	{
		public String address;
		public String filename;
		public long filesize;
		public String input;
		public String output;
		public long start_time, end_time;
		public boolean success;

		/**
		 * Class constructor.
		 * @param address the client address
		 * @param filename the filename
		 * @param output the desired output format
		 */
		public RequestInformation(String address, String filename, String output)
		{
			this.address = address;
			this.filename = Utility.getFilename(filename);
			filesize = new File(filename).length();
			input = Utility.getFilenameExtension(filename);
			this.output = output;
			start_time = System.currentTimeMillis();
		}
		
		/**
		 * Set the end time of the request along with whether or not the conversion was successful or not
		 * @param success true if the conversion request was successful
		 */
		public void setEndOfRequest(boolean success)
		{
			end_time = System.currentTimeMillis();
			this.success = success;
		}
		
		/**
		 * Return a string representation of the Polyglot request information.
		 */
		public String toString()
		{
			return address + ", " + filename + ", " + filesize + ", " + input + ", " + output + ", " + start_time + ", " + end_time + ", " + success;
		}
	}
}