package edu.illinois.ncsa.isda.softwareserver.polyglot;
import edu.illinois.ncsa.isda.softwareserver.SoftwareServerAuxiliary.*;
import kgm.utility.*;
import java.util.*;
import java.util.concurrent.atomic.*;

/**
 * An interface for file format conversion services that contain at its heart an I/O-Graph built
 * around a set of Software Server clients.
 * @author Kenton McHenry
 */
public abstract class Polyglot
{
	private AtomicInteger pending_asynchronous_calls = new AtomicInteger();
	
	public abstract TreeSet<String> getSoftware();
	public abstract TreeSet<String> getOutputs();
	public abstract TreeSet<String> getOutputs(String input);
	public abstract TreeSet<String> getOutputs(TreeSet<String> inputs);
	public abstract TreeSet<String> getInputs();
	public abstract TreeSet<String> getInputs(String output);
	public abstract IOGraph<String,String> getInputOutputGraph();
	public abstract String convert(String input_filename, String output_path, String output_type);
	
	public abstract Vector<String> getServers();
	public abstract IOGraph<String,String> getDistributedInputOutputGraph();

	/**
	 * Get the outputs available for the given input type, pruning any that requre more than n hops.  Should be overridden, by default returns all possible outputs.
	 * @param input the input type
	 * @param n the maximum number of hops
	 * @return the list of outputs
	 */
	public TreeSet<String> getOutputs(String input, int n)
	{	
		return getOutputs(input);
	}
	
	/**
	 * Should be overriden. Atomically increase current value of job_counter by one
	 * and return new jobid
	 * @return a new jobid
	 */
	public int incrementAndGetJobID()
	{
		return 0;
	}
	
	/**
	 * Convert a files format.  Should be overriden, by default searches for a conversion path.
	 * @param application the specific application to use
	 * @param input the absolute name of the input file
	 * @param output_path the output path
	 * @param output_format the output format
	 * @return the output file name (if changed, null otherwise)
	 */
	public String convert(String application, String input, String output_path, String output_format)
	{
		return convert(input, output_path, output_format);
	}
	
	/**
	 * Convert a files format and email the result.  Should be overriden, by default searches for a conversion path.
	 * @param input the absolute name of the input file
	 * @param output_path the output path
	 * @param output_format the output format
	 * @param email address to send result to
	 * @return the output file name (if changed, null otherwise)
	 */
	public String convertAndEmail(String input, String output_path, String output_format, String email)
	{
		return convert(input, output_path, output_format);
	}
	
	/**
	 * Convert a files format and email the result.  Should be overriden, by default searches for a conversion path.
	 * @param job id of this conversion
	 * @param input the absolute name of the input file
	 * @param output_path the output path
	 * @param output_format the output format
	 * @param email address to send result to
	 * @return the output file name (if changed, null otherwise)
	 */
	public String convertAndEmail(int jobid, String input, String output_path, String output_format, String email)
	{
		return convert(input, output_path, output_format);
	}
	
	/**
	 * Convert a files format and email the result.  Should be overriden, by default searches for a conversion path.
	 * @param application the specific application to use
	 * @param input the absolute name of the input file
	 * @param output_path the output path
	 * @param output_format the output format
	 * @param email address to send result to
	 * @return the output file name (if changed, null otherwise)
	 */
	public String convertAndEmail(String application, String input, String output_path, String output_format, String email)
	{
		return convert(application, input, output_path, output_format);
	}

	/**
	 * Convert a files format and email the result.  Should be overriden, by default searches for a conversion path.
	 * @param job id of this conversion
	 * @param application the specific application to use
	 * @param input the absolute name of the input file
	 * @param output_path the output path
	 * @param output_format the output format
	 * @param email address to send result to
	 * @return the output file name (if changed, null otherwise)
	 */
	public String convertAndEmail(int jobid, String application, String input, String output_path, String output_format, String email)
	{
		return convert(application, input, output_path, output_format);
	}
	
	/**
	 * Convert a files format (asynchronously).
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

	public abstract void close();
}
