package edu.illinois.ncsa.isda.icr.polyglot;
import edu.illinois.ncsa.isda.softwareserver.SoftwareServer;
import edu.illinois.ncsa.isda.softwareserver.SoftwareServerClient;
import edu.illinois.ncsa.isda.softwareserver.polyglot.PolyglotClient;
import edu.illinois.ncsa.isda.softwareserver.polyglot.PolyglotServer;
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
	@Test
	public void test1() throws Exception
	{
		//Software Server
    SoftwareServerClient sclient = new SoftwareServerClient("localhost", 50000);

    if(!sclient.isAlive()){
    	SoftwareServer sserver = new SoftwareServer("SoftwareServer.conf");
    }
    
    //Polyglot
		PolyglotServer pserver = new PolyglotServer("PolyglotServer.conf");
		
		PolyglotClient pclient = new PolyglotClient("localhost", 50002);
		pclient.convert("data/demo/Lenna.png", "data/tmp/", "gif");
		pclient.close();
		
		assertTrue(Utility.existsAndRecent("data/tmp/Lenna.gif", 100000));
	}
}