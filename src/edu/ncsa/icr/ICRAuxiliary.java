package edu.ncsa.icr;
import edu.ncsa.utility.*;
import java.util.*;

/**
 * Helper classes for the ICR package.
 * @author Kenton McHenry
 */
public class ICRAuxiliary
{
	public static abstract class Data {}
	
	/**
	 * A buffered file.
	 */
	public static class FileData extends Data
	{
		private String filename;
		private String format;
		private byte[] data;
		
		public FileData() {}
		
		/**
		 * Class constructor.
		 * @param filename the name of the file
		 */
		public FileData(String filename)
		{
			this.filename = filename;
			format = Utility.getFilenameExtension(filename);
		}
		
  	/**
  	 * Create a new FileData instance representing this format.
  	 * @param format the format extension
  	 */
  	public static FileData newFormat(String format)
  	{
  		FileData data = new FileData();
  		data.format = format;
  		return data;
  	}
  	
  	/**
  	 * Get the files format.
  	 * @return the file format
  	 */
  	public String getFormat()
  	{
  		return format;
  	}
	}
	
	/**
	 * A pointer to a file on the server.
	 */
	public static class CachedFileData extends FileData
	{
		private String id;
		
		/**
		 * Class constructor.
		 * @param filename the name of the file
		 */
		public CachedFileData(String filename)
		{
			super(filename);
		}
	}
	
  /**
   * A structure to store information about applications.
   */
  public static class Application
  {
    public String name = "";
    public String alias = "";
    public Vector<Operation> operations = new Vector<Operation>();
    
    /**
     * Class constructor.
     * @param name the name of the application
     * @param alias the alias of the application
     */
    public Application(String name, String alias)
    {
    	this.name = name;
      this.alias = alias;
    }
  }
  
  /**
   * A structure representing an operation an application supports (think of mathematical functions).
   * @author Kenton McHenry
   */
  public static class Operation
  {
  	public String name;
  	public Vector<Data> inputs = new Vector<Data>();
  	public Vector<Data> outputs = new Vector<Data>();
  	public String script;
  	
  	/**
  	 * Class constructor.
  	 * @param name the name of the operation
  	 */
  	public Operation(String name)
  	{
  		this.name = name;
  	}
  }
}