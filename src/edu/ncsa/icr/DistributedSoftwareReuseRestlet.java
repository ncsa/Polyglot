package edu.ncsa.icr;
import edu.ncsa.icr.SoftwareReuseAuxiliary.*;
import edu.ncsa.utility.Utility;
import java.util.*;
import java.io.*;
import org.restlet.*;
import org.restlet.data.*;
import org.restlet.routing.*;

/**
 * A restful interface to multiple software reuse servers.
 * @author Kenton McHenry
 */
public class DistributedSoftwareReuseRestlet
{
	private static TreeSet<RemoteTaskInfo> tasks = new TreeSet<RemoteTaskInfo>();
	
	/**
	 * Initialize.
	 */
	public static void initialize()
	{
		
	}
	
	/**
	 * Query a restlet endpoint that responds with a line separated text list of values.
	 * @param url the URL of the restlet endpoint to query
	 * @return the values returned from the endpoint
	 */
	public static Vector<String> queryTextEndPoint(String url)
	{
		Vector<String> values = new Vector<String>();
		String buffer = Utility.readURL(url);
		Scanner scanner = new Scanner(buffer);
		
		while(scanner.hasNextLine()){
			values.add(scanner.nextLine());
		}
		
		return values;
	}
	
	/**
	 * Start the restful service.
	 * @param args the input arguments
	 */
	public static void main(String[] args)
	{		
		int port = 8182;
		
		//Load *.ini file
	  try{
	    BufferedReader ins = new BufferedReader(new FileReader("SoftwareReuseMasterRestlet.ini"));
	    String line, key, value;
	    
	    while((line=ins.readLine()) != null){
	      if(line.contains("=")){
	        key = line.substring(0, line.indexOf('='));
	        value = line.substring(line.indexOf('=')+1);
	        
	        if(key.charAt(0) != '#'){
	        	if(key.equals("Port")){
	        		port = Integer.valueOf(value);
	          }
	        }
	      }
	    }
	    
	    ins.close();
	  }catch(Exception e) {e.printStackTrace();}
		
	  //Initialize and start the service
		initialize();
		
		try{			
			Component component = new Component();
			component.getServers().add(Protocol.HTTP, port);
			component.getClients().add(Protocol.HTTP);
			component.getLogService().setEnabled(false);
			
			org.restlet.Application application = new org.restlet.Application(){
				@Override
				public Restlet createInboundRoot(){
					Router router = new Router(getContext());
					router.attachDefault(SoftwareReuseRestlet.class);
					return router;
				}
			};
			
			component.getDefaultHost().attach("/distributed_software", application);
			component.start();
		}catch(Exception e) {e.printStackTrace();}
	}
}