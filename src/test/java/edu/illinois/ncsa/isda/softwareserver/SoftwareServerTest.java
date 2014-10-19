package edu.illinois.ncsa.isda.softwareserver;
import edu.illinois.ncsa.isda.softwareserver.SoftwareServerAuxiliary.*;
import kgm.utility.Utility;
import org.junit.*;
import static org.junit.Assert.*;
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
	private static String script_name = "ImageMagick_convert.ahk";
	private static SoftwareServerRestlet restlet = null;
	private static DistributedSoftwareServerRestlet drestlet = null;
	private static SoftwareServerClient client = null;

	/**
	 * Test ScriptInstaller and start up services/connections.
	 */
	@BeforeClass
	public static void setup()
	{		
		//Configure wrapper script
		if(System.getProperty("os.name").contains("Windows")){		//Only run the test on Windows machines!
			ScriptDebugger debugger = new ScriptDebugger("ScriptDebugger.conf");
			debugger.configureScript("scripts/ahk/" + script_name);
		}
		
		//Start the services/connections	
		System.out.println("\n--- Starting services/connections ---");
		
	  restlet = new SoftwareServerRestlet();
		SoftwareServerRestlet.main(new String[0]);
		
		drestlet = new DistributedSoftwareServerRestlet();
		drestlet.main(new String[0]);
		
		client = new SoftwareServerClient("localhost", 50000);;

	}
	
	/**
	 * Test configuration and basic connection.
	 */
	@Test
	public void test1()
	{
		System.out.println("\n=== Software Server: Test 1 (configuration and connection) ===");
		
		//Check script configuration
		try{
			assertTrue("Error: Script not configured", Utility.existsNotEmptyAndRecent("scripts/ahk-configured/" + script_name, 10000));
			System.out.println("Script configuration OK");
		}catch(AssertionError e){
			System.out.println(e.getMessage());
		}
				
		//Check Software Server client connection		
		try{
			assertTrue("Error: SoftwareServerClient not connected", client.isAlive());
			System.out.println("SoftwareServerClient connection OK");
		}catch(AssertionError e){
			System.out.println(e.getMessage());
		}
	}
	
	/**
	 * Check SoftwareServer applications.
	 */	
	@Test
	public void test2()
	{    
		System.out.println("\n=== Software Server: Test 2 (applications) ===");
	  
    Vector<Application> applications = client.getApplications();
    boolean FOUND_IMAGEMAGICK = false;
    
    for(int i=0; i<applications.size(); i++){
    	if(applications.get(i).alias.equals("ImageMagick")){
    		FOUND_IMAGEMAGICK = true;
    		break;
    	}
    }
    
    try{
    	assertTrue("Error: Did not find ImageMagick", FOUND_IMAGEMAGICK);
    	System.out.println("ImageMagick OK");
    }catch(AssertionError e){
			System.out.println(e.getMessage());
		}
	}
	
	/**
	 * Test SoftwareServer task.
	 */	
	@Test
	public void test3()
	{  		
		System.out.println("\n=== Software Server: Test 3 (task) ===");

    client.sendData(new FileData("data/demo/Lenna.png", true));
		
		Task task = new Task(client);
		task.add("ImageMagick", "convert", "data/demo/Lenna.png", "Lenna.jpg");
		task.execute("data/tmp/");
		
		try{
			assertTrue("Error: Conversion failed", Utility.existsNotEmptyAndRecent("data/tmp/Lenna.jpg", 10000));
			System.out.println("Conversion task OK");
		}catch(AssertionError e){
			System.out.println(e.getMessage());
		}
	}
	
	/**
	 * Test SoftwareServerRestlet.
	 */	
	@Test
	public void test4()
	{  				
		System.out.println("\n=== Software Server: Test 4 (REST interface) ===");
	
		String result = Utility.postFile("http://localhost:8182/software/ImageMagick/convert/jpg/", "data/demo/Lenna.png", "text/plain");
		assertNotNull("result is null", result);
		Utility.pause(2000);
		
		//Check if the desired endpoint, and thus the server, is active
		try{
			assertTrue("Error: Did not get answer from URL [" + result + "]", Utility.existsURL(result));
			System.out.println("Endpoint OK");
		}catch(AssertionError e){
			System.out.println(e.getMessage());
		}
	
		//Carry out the conversion
		Utility.downloadFile("data/tmp/", "Lenna4", result);
		
		try{
			assertTrue("Error: Conversion failed", Utility.existsNotEmptyAndRecent("data/tmp/Lenna4.jpg", 10000));
			System.out.println("Conversion OK");
		}catch(AssertionError e){
			System.out.println(e.getMessage());
		}
	}
	
	/**
	 * Test DistributedSoftwareServerRestlet.
	 */	
	@Test
	public void test5()
	{  		
		System.out.println("\n=== Software Server: Test 5 (distributed restlet) ===");

		Utility.pause(2000);
		String result = Utility.postFile("http://localhost:8183/software/ImageMagick/convert/jpg/", "data/demo/Lenna.png", "text/plain");
		assertNotNull("result is null", result);
		Utility.pause(2000);
		
		//Check if the desired endpoint, and thus the server, is active
		try{
			assertTrue("Error: Did not get answer from URL [" + result + "]", Utility.existsURL(result));
			System.out.println("Endpoint OK");
		}catch(AssertionError e){
			System.out.println(e.getMessage());
		}
				
		//Carry out the conversion
		Utility.downloadFile("data/tmp/", "Lenna5", result);
		
		try{
			assertTrue("Error: Conversion failed", Utility.existsNotEmptyAndRecent("data/tmp/Lenna5.jpg", 10000));
			System.out.println("Conversion OK");
		}catch(AssertionError e){
			System.out.println(e.getMessage());
		}
	}

	/**
	 * Stop services/connections.
	 */
	@AfterClass
	public static void tearDown()
	{
		System.out.println("\n--- Stopping services ---");
		
		if(client != null) client.close();
		if(drestlet != null) drestlet.stop();		
		if(restlet != null) restlet.stop();
	}
}