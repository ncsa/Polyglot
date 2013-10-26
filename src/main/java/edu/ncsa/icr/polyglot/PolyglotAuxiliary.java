package edu.ncsa.icr.polyglot;
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
	public static class FileInfo
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
}