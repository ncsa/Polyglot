package edu.ncsa.icr.polyglot;

/**
 * An interface for file format conversion services.
 * @author Kenton McHenry
 */
public interface Polyglot
{
	public void convert(String input_filename, String output_type);
}