package edu.ncsa.icr.polyglot;
import edu.ncsa.icr.*;
import java.util.*;

/**
 * A class that coordinates the use of several ICR clients via I/O-graphs to perform file
 * format conversions.
 * @author Kenton McHenry
 */
public class PolyglotSteward implements Polyglot
{
	private Vector<ICRClient> icr_clients = new Vector<ICRClient>();
	
	/**
	 * Add an ICR client.
	 * @param icr an ICR client
	 */
	public void add(ICRClient icr)
	{
		icr_clients.add(icr);
	}
}