package edu.illinois.ncsa.isda.softwareserver.polyglot;
import org.jboss.resteasy.plugins.server.tjws.TJWSEmbeddedJaxrsServer;
//import org.restlet.resource.ServerResource;

public class PolyglotResteasy // extends ServerResource
{
	protected static TJWSEmbeddedJaxrsServer tjws;
	public static void main(String[] args) 	{

		tjws = new TJWSEmbeddedJaxrsServer();
		tjws.setPort(8184);
		tjws.start();
		//tjws.getDeployment().getRegistry().addPerRequestResource(ResteasyResources.class);
		tjws.getDeployment().getRegistry().addSingletonResource(new PolyglotResteasyResources());
	} // end of main() //
	
    public static void stop() {
        try{
            tjws.stop();
        }catch(Exception e) {e.printStackTrace();}
	} // end of stop() //
} // end of class PolyglotResteasy
