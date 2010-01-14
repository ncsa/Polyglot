package edu.ncsa.icr;

/**
 * Helper classes for the ICR package.
 * @author Kenton McHenry
 */
public class ICRAuxiliary
{
	/**
	 * A generic parent container for types of data.
	 */
	public static class Data
	{}
	
	/**
	 * A buffered file.
	 */
	public static class FileData extends Data
	{
		
	}
	
	/**
	 * A pointer to a file on the server.
	 */
	public static class CachedFileData extends Data
	{
		
	}
}