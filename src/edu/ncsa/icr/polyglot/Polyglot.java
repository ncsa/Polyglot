package edu.ncsa.icr.polyglot;
import edu.ncsa.utility.*;
import java.util.*;
import java.util.concurrent.atomic.*;

/**
 * An interface for file format conversion services.
 * @author Kenton McHenry
 */
public abstract class Polyglot
{
	private AtomicInteger pending_asynchronous_calls = new AtomicInteger();
	
	public abstract TreeSet<String> getOutputs(String string);
	public abstract TreeSet<String> getOutputs(TreeSet<String> set);
	public abstract void convert(String input_filename, String output_path, String output_type);
	public abstract void close();
	
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
		
		pending_asynchronous_calls.incrementAndGet();
		
		new Thread(){
			public void run(){
				convert(input_filename_final, output_path_final, output_type_final);
				pending_asynchronous_calls.decrementAndGet();
			}
		}.start();
	}
	
	/**
	 * Wait for all pending asynchronous calls to complete.
	 */
	public void waitOnPending()
	{
		while(pending_asynchronous_calls.get() > 0){
			Utility.pause(500);
		}
	}
}