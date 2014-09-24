package edu.illinois.ncsa.isda.softwareserver;
import org.jboss.resteasy.plugins.server.tjws.TJWSEmbeddedJaxrsServer;

public class SoftwareServerResteasy {

	protected static TJWSEmbeddedJaxrsServer tjws;
	
	public static void main(String[] args) 	{

		tjws = new TJWSEmbeddedJaxrsServer();
		tjws.setPort(8182);
		tjws.start();
		//tjws.getDeployment().getRegistry().addPerRequestResource(ResteasyResources.class);
		tjws.getDeployment().getRegistry().addSingletonResource(new ResteasyResources());
	} // end of main() //
	
    public static void stop() {
        try{
            tjws.stop();
        }catch(Exception e) {e.printStackTrace();}
	} // end of stop() //

} // end of class SoftwareServerResteasy //
