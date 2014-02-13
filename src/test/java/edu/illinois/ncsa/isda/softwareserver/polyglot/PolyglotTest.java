package edu.illinois.ncsa.isda.softwareserver.polyglot;
import edu.illinois.ncsa.isda.softwareserver.*;
import kgm.utility.Utility;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

/**
 * Polyglot integration tests.
 * @author Kenton McHenry
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PolyglotTest
{
	/**
	 * Test Polyglot Server and Client.
	 */
	@Test
	public void test1() throws Exception
	{
		System.out.println("\n=== Polyglot: Test 1 ===");

		//Start Software Server    	
		SoftwareServer sserver = new SoftwareServer("SoftwareServer.conf");
    
    //Start Polyglot
		PolyglotServer pserver = new PolyglotServer("PolyglotServer.conf");
		PolyglotClient pclient = new PolyglotClient("localhost", 50002);
		
		//Run the test
		pclient.convert("data/demo/Lenna.png", "data/tmp/", "gif");
		pclient.close();
		pserver.stop();

		assertTrue(Utility.existsAndRecent("data/tmp/Lenna.gif", 100000));
		
		//Stop Software Server
		sserver.stop();
	}
	
	/**
	 * Test PolyglotRestlet.
	 */
	@Test
	public void test2() throws Exception
	{
		System.out.println("\n=== Polyglot: Test 2 ===");

		//Start Software Server    	
		SoftwareServer sserver = new SoftwareServer("SoftwareServer.conf");
    
		/*
    //Start Polyglot
		PolyglotServer pserver = new PolyglotServer("PolyglotServer.conf");
		PolyglotClient pclient = new PolyglotClient("localhost", 50002);
		pclient.convert("data/demo/Lenna.png", "data/tmp/", "gif");
		pclient.close();
		pserver.stop();

		assertTrue(Utility.existsAndRecent("data/tmp/Lenna.gif", 100000));
		*/
		
		//Stop Software Server
		sserver.stop();
	}
}