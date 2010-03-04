package edu.ncsa.icr.polyglot;
import edu.ncsa.icr.ICRAuxiliary.Application;
import edu.ncsa.icr.ICRAuxiliary.Data;
import edu.ncsa.utility.*;

import java.util.*;
import java.util.concurrent.atomic.*;

/**
 * An interface for file format conversion services that contain at its heart an I/O-Graph built
 * around a set of ICR clients.
 * @author Kenton McHenry
 */
public abstract class Polyglot
{
	protected IOGraph<Data,Application> iograph = new IOGraph<Data,Application>();
	private AtomicInteger pending_asynchronous_calls = new AtomicInteger();
	
	public abstract void convert(String input_filename, String output_path, Vector<Conversion<Data,Application>> conversions);
	public abstract void close();
	
	/**
	 * Get the I/O-graph.
	 * @return the I/O-graph
	 */
	public IOGraph<Data,Application> getIOGraph()
	{
		return iograph;
	}
	
	/**
	 * Get the outputs available for the given input type.
	 * @param string a string representing the input type
	 */
	public TreeSet<String> getOutputs(String string)
	{
		return iograph.getRangeStrings(string);
	}
	
	/**
	 * Get the common outputs available for the given input types.
	 * @param set a set of strings representing the input types
	 */
	public TreeSet<String> getOutputs(TreeSet<String> set)
	{
		return iograph.getRangeIntersectionStrings(set);
	}
	
	/**
	 * Convert a files format.
	 * @param input_filename the absolute name of the input file
	 * @param output_path the output path
	 * @param output_type the name of the output type
	 */
	public void convert(String input_filename, String output_path, String output_type)
	{		
		String input_type = Utility.getFilenameExtension(input_filename);
		Vector<Conversion<Data,Application>> conversions = iograph.getShortestConversionPath(input_type, output_type, false);
		
		convert(input_filename, output_path, conversions);
	}
	
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