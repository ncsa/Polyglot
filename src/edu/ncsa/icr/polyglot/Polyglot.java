package edu.ncsa.icr.polyglot;

/**
 * An interface for file format conversion services.
 * @author Kenton McHenry
 */
public abstract class Polyglot
{
	public abstract void convert(String input_filename, String output_path, String output_type);
	
	/**
	 * Convert a files format(asynchronously).
	 * @param input_filename the absolute name of the input file
	 * @param output_path the output path
	 * @param output_type the name of the output type
	 */
	public void convertLater(String input_filename, String output_path, String output_type)
	{
		final String input_filename_final = input_filename;
		final String output_path_final = output_path;
		final String output_type_final = output_type;
		
		new Thread(){
			public void run(){
				convert(input_filename_final, output_path_final, output_type_final);
			}
		}.start();
	}
}