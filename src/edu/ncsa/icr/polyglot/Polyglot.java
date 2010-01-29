package edu.ncsa.icr.polyglot2;
import edu.ncsa.icr.*;
import edu.ncsa.icr.ICRAuxiliary.*;
import java.util.*;

/**
 * An API for performing file format conversions via ICR clients.
 * @author Kenton McHenry
 */
public class Polyglot
{
	private Vector<ICRClient> icr_clients = new Vector<ICRClient>();
	
	public Polyglot() {}
	
	/**
	 * Add an ICR client.
	 * @param icr an ICR client
	 */
	public void add(ICRClient icr)
	{
		icr_clients.add(icr);
	}
}