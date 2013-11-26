package edu.illinois.ncsa.isda.icr;
import edu.illinois.ncsa.isda.icr.ICRAuxiliary.*;
import kgm.utility.Utility;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
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
		//Check if the Software Server is running from a previous test
		SoftwareServerClient client = new SoftwareServerClient("localhost", 50000);

    if(!client.isAlive()){
    	SoftwareServer server = new SoftwareServer("SoftwareServer.conf");
    	client = new SoftwareServerClient("localhost", 50000);
    }
    
    //Run the test
    assertTrue(client.isAlive());
    
    Vector<Application> applications = client.getApplications();
    
    assertTrue(applications.size() > 0 && applications.get(0).alias.equals("ImgMgk"));
	}
	
	/**
	 * Test SoftwareServer task.
	 */	
	@Test
	public void test3() throws Exception
	{
		//Check if Software Server is running from a previous test
    SoftwareServerClient client = new SoftwareServerClient("localhost", 50000);

    if(!client.isAlive()){
  		SoftwareServer server = new SoftwareServer("SoftwareServer.conf");
  		client = new SoftwareServerClient("localhost", 50000);
    }
    
    //Run the test
    client.sendData(new FileData("data/demo/Lenna.png", true));
		
		Task task = new Task(client);
		task.add("ImgMgk", "convert", "data/demo/Lenna.png", "Lenna.jpg");
		task.execute("data/tmp/");
		
		assertTrue(Utility.existsAndRecent("data/tmp/Lenna.jpg", 10000));
	}
}