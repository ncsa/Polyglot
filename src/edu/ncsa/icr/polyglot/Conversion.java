package edu.ncsa.icr.polyglot;

/**
 * A simple structure to store a single conversion task.
 * @author Kenton McHenry
 */
public class Conversion<V extends Comparable,E>
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