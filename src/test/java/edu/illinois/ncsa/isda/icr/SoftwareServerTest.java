package edu.illinois.ncsa.isda.icr;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class SoftwareServerTest
{
	@Test
	public void testSoftwareServer() throws Exception
	{
		System.out.println("ok1: " + System.getProperty("user.dir"));

		SoftwareServer server = new SoftwareServer("SoftwareServer.conf");

		assertEquals("blah", "blah");
	}
}