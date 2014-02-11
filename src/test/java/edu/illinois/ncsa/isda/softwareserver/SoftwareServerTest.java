package edu.illinois.ncsa.isda.softwareserver;
import edu.illinois.ncsa.isda.softwareserver.SoftwareServerAuxiliary.*;
import kgm.utility.Utility;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import org.restlet.data.MediaType;
import org.restlet.representation.*;
import java.util.*;

/**
 * SoftwareServer integration tests.
 * @author Kenton McHenry
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SoftwareServerTest
{
	/**
	 * Test ScriptInstaller.
	 */	
	@Test
	public void test1() throws Exception
	{
		System.out.println("\n=== Software Server: Test 1 ===");
		
		String script_name = "ImgMgk_convert.ahk";
		ScriptDebugger debugger = new ScriptDebugger("ScriptDebugger.conf");
		debugger.configureScript("scripts/ahk/" + script_name);
		
		assertTrue(Utility.existsAndRecent("scripts/ahk-configured/" + script_name, 10000));
	}
	
	/**
	 * Test SoftwareServer connection.
	 */	
	@Test
	public void test2() throws Exception
	{    
		System.out.println("\n=== Software Server: Test 2 ===");

		//Start the Software Server
  	SoftwareServer server = new SoftwareServer("SoftwareServer.conf");
		SoftwareServerClient client = new SoftwareServerClient("localhost", 50000);
    
    //Run the tests
    assertTrue(client.isAlive());
    
    Vector<Application> applications = client.getApplications();
    assertTrue(applications.size() > 0 && applications.get(0).alias.equals("ImgMgk"));
    
    //Stop the Software Server
    client.close();
    server.stop();
	}
	
	/**
	 * Test SoftwareServer task.
	 */	
	@Test
	public void test3() throws Exception
	{  		
		System.out.println("\n=== Software Server: Test 3 ===");

		//Start the Software Server
		SoftwareServer server = new SoftwareServer("SoftwareServer.conf");
    SoftwareServerClient client = new SoftwareServerClient("localhost", 50000);
    
    //Run the test
    client.sendData(new FileData("data/demo/Lenna.png", true));
		
		Task task = new Task(client);
		task.add("ImgMgk", "convert", "data/demo/Lenna.png", "Lenna.jpg");
		task.execute("data/tmp/");
		
		assertTrue(Utility.existsAndRecent("data/tmp/Lenna.jpg", 10000));
		
		//Stop the Software Server
		client.close();
		server.stop();
	}
	
	/**
	 * Test SoftwareServerRestlet.
	 */	
	@Test
	public void test4() throws Exception
	{  		
		System.out.println("\n=== Software Server: Test 4 ===");

		//Start the Software Server and its REST interface
		SoftwareServerRestlet server = new SoftwareServerRestlet();
		server.main(new String[0]);
		
		//Run the test
		String result = Utility.postFile("http://localhost:8182/software/ImgMgk/convert/pgm/", "data/demo/Lenna.png", "text/plain");
		Utility.pause(2000);
		
		assertTrue(Utility.existsURL(result));
		
		Utility.downloadFile("data/tmp/", "Lenna4", result);
		assertTrue(Utility.existsAndRecent("data/tmp/Lenna4.pgm", 10000));

		//Stop the REST interface and its underlying Software Server
		server.stop();
	}
	
	/**
	 * Test DistributedSoftwareServerRestlet.
	 */	
	@Test
	public void test5() throws Exception
	{  		
		System.out.println("\n=== Software Server: Test 5 ===");

		//Start services
		SoftwareServerRestlet sserver = new SoftwareServerRestlet();
		sserver.main(new String[0]);
		DistributedSoftwareServerRestlet dsserver = new DistributedSoftwareServerRestlet();
		dsserver.main(new String[0]);

		//Run the test
		Utility.pause(2000);
		String result = Utility.postFile("http://localhost:8183/software/ImgMgk/convert/pgm/", "data/demo/Lenna.png", "text/plain");
		Utility.pause(2000);
		
		assertTrue(Utility.existsURL(result));
				
		Utility.downloadFile("data/tmp/", "Lenna5", result);
		assertTrue(Utility.existsAndRecent("data/tmp/Lenna5.pgm", 10000));
		
		//Stop the services
		dsserver.stop();		
		sserver.stop();
	}
}