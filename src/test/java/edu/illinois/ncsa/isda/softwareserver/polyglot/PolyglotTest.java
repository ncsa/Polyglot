package edu.illinois.ncsa.isda.softwareserver.polyglot;
import edu.illinois.ncsa.isda.softwareserver.*;
import kgm.utility.Utility;
import java.lang.*;
import java.io.*;
import org.junit.*;
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
	 * Purge mongo and rabbitmq to ensure a fresh run.
	 */
	@BeforeClass
	public static void setup()
	{
		Process p;
		BufferedReader reader;
		String command, line;

		System.out.println("\n--- Purging mongo & rabbitmq ---");

		//Purge mongo
		try{
			command = "mongo polyglot --eval \"db.steward.drop()\"";
			System.out.println("> " + command);
			p = Runtime.getRuntime().exec(command);
			p.waitFor();
			reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
	
			while((line = reader.readLine()) != null){
				System.out.println(line);
			}
		}catch(Exception e) {e.printStackTrace();}

		System.out.println();

		//Purge rabbitmq queue
		try{
			command = "curl -i -u guest:guest -H \"content-type:application/json\" -XDELETE http://localhost:15672/api/queues/%2F/ImageMagick/contents";
			System.out.println("> " + command);
			p = Runtime.getRuntime().exec(command);
			p.waitFor();
			reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
	
			while((line = reader.readLine()) != null){
				System.out.println(line);
			}
		}catch(Exception e) {e.printStackTrace();}
	}
		
	/**
	 * Test conversion via client.
	 */
	@Test
	public void test1()
	{
		System.out.println("\n=== Polyglot: Test 1 (client) ===");
		
		//Start Software Server
		SoftwareServer sserver = new SoftwareServer("SoftwareServer.conf");

		//Start Polyglot
		PolyglotServer pserver = new PolyglotServer("PolyglotServer.conf");
		PolyglotClient pclient = new PolyglotClient("localhost", 50002);
		
		//Run the test
		pclient.convert("data/demo/Lenna.png", "data/tmp/", "gif");
		
		try{
			assertTrue("Error: Conversion failed", Utility.existsNotEmptyAndRecent("data/tmp/Lenna.gif", 100000));
			System.out.println("Conversion OK");
		}catch(AssertionError e){
			System.out.println(e.getMessage());
			throw e;
		}finally{
			pclient.close();
			pserver.stop();
			sserver.stop();
		}
	}
	
	/**
	 * Test conversion via REST interface.
	 */
	@Test
	public void test2()
	{
		System.out.println("\n=== Polyglot: Test 2 (REST interface) ===");
		
		//Start Software Server
		//SoftwareServer sserver = new SoftwareServer("SoftwareServer.conf");
		SoftwareServerRestlet sserver = new SoftwareServerRestlet();
		SoftwareServerRestlet.main(new String[0]);
		
		//Start Polyglot
		PolyglotRestlet prestlet = new PolyglotRestlet();
		prestlet.main(new String[0]);
		prestlet.setReturnURL(true);
		prestlet.setSoftwareServerRESTInterface(false);
		
		//Run the test
		Utility.pause(2000);
		String result = Utility.postFile("http://localhost:8184/convert/gif/", "data/demo/Lenna.png", "text/plain");
		Utility.pause(5000);
		
		//Check if the output endpoint is available
		try{
			assertTrue("Did not get answer from URL [" + result + "]", Utility.existsURL_bak(result));
			System.out.println("Endpoint OK");
		}catch(AssertionError e){
			System.out.println(e.getMessage());
			throw e;
		}
		
		//Download the output of the conversion
		Utility.downloadFile("data/tmp/", "Lenna2", result);
		
		try{
			assertTrue("Conversion failed", Utility.existsNotEmptyAndRecent("data/tmp/Lenna2.gif", 10000));
			System.out.println("Conversion OK");
		}catch(AssertionError e){
			System.out.println(e.getMessage());
			throw e;
		}finally{
			prestlet.stop();
			sserver.stop();
		}
	}
}
