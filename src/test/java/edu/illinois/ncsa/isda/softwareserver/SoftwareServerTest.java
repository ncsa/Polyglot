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
	public void test1()
	{
		System.out.println("\n=== Software Server: Test 1 ===");
		
		if(System.getProperty("os.name").contains("Windows")){		//Only run the test on Windows machines!
			String script_name = "ImageMagick_convert.ahk";
			ScriptDebugger debugger = new ScriptDebugger("ScriptDebugger.conf");
			debugger.configureScript("scripts/ahk/" + script_name);
			
			assertTrue("Script not configured", Utility.existsNotEmptyAndRecent("scripts/ahk-configured/" + script_name, 10000));
		}
	}
	
	/**
	 * Test SoftwareServer connection.
	 */	
	@Test
	public void test2()
	{    
		SoftwareServer server = null;
		SoftwareServerClient client = null;
		
		System.out.println("\n=== Software Server: Test 2 ===");

		try {
			//Start the Software Server
	  	server = new SoftwareServer("SoftwareServer.conf");
			client = new SoftwareServerClient("localhost", 50000);
	    
	    //Run the tests
	    assertTrue("SoftwareServerClient not connected", client.isAlive());
	    
	    Vector<Application> applications = client.getApplications();
	    boolean FOUND_IMGMGK = false;
	    
	    for(int i=0; i<applications.size(); i++){
	    	if(applications.get(i).alias.equals("ImageMagick")){
	    		FOUND_IMGMGK = true;
	    		break;
	    	}
	    }
	    
	    assertTrue("Did not find ImageMagick", FOUND_IMGMGK);
		} finally {
	    //Stop the Software Server
	    if (client != null) client.close();
	    if (server != null) server.stop();
		}
	}
	
	/**
	 * Test SoftwareServer task.
	 */	
	@Test
	public void test3()
	{  		
		SoftwareServer server = null;
		SoftwareServerClient client = null;

		System.out.println("\n=== Software Server: Test 3 ===");

		try {
			//Start the Software Server
		  server = new SoftwareServer("SoftwareServer.conf");
	    client = new SoftwareServerClient("localhost", 50000);
	    
	    //Run the test
	    client.sendData(new FileData("data/demo/Lenna.png", true));
			
			Task task = new Task(client);
			task.add("ImageMagick", "convert", "data/demo/Lenna.png", "Lenna.jpg");
			task.execute("data/tmp/");
			
			assertTrue("Conversion failed", Utility.existsNotEmptyAndRecent("data/tmp/Lenna.jpg", 10000));
			
		} finally {
	    //Stop the Software Server
	    if (client != null) client.close();
	    if (server != null) server.stop();
		}
	}
	
	/**
	 * Test SoftwareServerRestlet.
	 */	
	@Test
	public void test4()
	{  		
		SoftwareServerRestlet server = null;
		
		System.out.println("\n=== Software Server: Test 4 ===");

		try {
			//Start the Software Server and its REST interface
		  server = new SoftwareServerRestlet();
			SoftwareServerRestlet.main(new String[0]);
			
			//Run the test
			String result = Utility.postFile("http://localhost:8182/software/ImageMagick/convert/pgm/", "data/demo/Lenna.png", "text/plain");
			assertNotNull("result is null", result);
			Utility.pause(2000);
			
			assertTrue("Did not get answer from URL [" + result + "]", Utility.existsURL(result));
			
			Utility.downloadFile("data/tmp/", "Lenna4", result);
			assertTrue("Conversion failed", Utility.existsNotEmptyAndRecent("data/tmp/Lenna4.pgm", 10000));
		} finally {
			//Stop the REST interface and its underlying Software Server
			if (server != null) server.stop();
		}
	}
	
	/**
	 * Test DistributedSoftwareServerRestlet.
	 */	
	@Test
	public void test5()
	{  		
		SoftwareServerRestlet sserver = null;
		DistributedSoftwareServerRestlet dsserver = null;
				
		System.out.println("\n=== Software Server: Test 5 ===");

		try {
			//Start services
		  sserver = new SoftwareServerRestlet();
			sserver.main(new String[0]);
			dsserver = new DistributedSoftwareServerRestlet();
			dsserver.main(new String[0]);
	
			//Run the test
			Utility.pause(2000);
			String result = Utility.postFile("http://localhost:8183/software/ImageMagick/convert/pgm/", "data/demo/Lenna.png", "text/plain");
			assertNotNull("result is null", result);
			Utility.pause(2000);
			
			assertTrue("Did not get answer from URL [" + result + "]", Utility.existsURL(result));
					
			Utility.downloadFile("data/tmp/", "Lenna5", result);
			assertTrue("Conversion failed", Utility.existsNotEmptyAndRecent("data/tmp/Lenna5.pgm", 10000));
		
		} finally {
			//Stop the services
			if (dsserver != null) dsserver.stop();		
			if (sserver != null) sserver.stop();
		}
	}
}
