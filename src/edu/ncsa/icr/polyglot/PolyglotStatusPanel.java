package edu.ncsa.icr.polyglot;
import edu.ncsa.icr.*;
import edu.ncsa.utility.*;
import java.awt.*;
import javax.swing.*;
import java.util.*;

public class PolyglotStatusPanel extends HTMLPanel implements Runnable
{
	private PolyglotClient polyglot;
	
	public PolyglotStatusPanel(String server, int port)
	{
		polyglot = new PolyglotClient(server, port);
		new Thread(this).start();
	}
	
	public void run()
	{
		TreeSet<String> connections = new TreeSet<String>();
		TreeSet<SoftwareReuseClient> clients = new TreeSet<SoftwareReuseClient>();
		TreeSet<String> client_strings = new TreeSet<String>();
		SoftwareReuseClient client;
		String connection;
		String buffer;
		
		while(true){
			connections.clear();
			connections.addAll(polyglot.getConnections());
			
			//Remove old connections
			for(Iterator<SoftwareReuseClient> itr=clients.iterator(); itr.hasNext();){
				client = itr.next();
				
				if(!connections.contains(client.toString())){
					client.close();
					clients.remove(client);
				}
			}
			
			for(Iterator<String> itr=client_strings.iterator(); itr.hasNext();){
				connection = itr.next();
				
				if(!connections.contains(connection)){
					client_strings.remove(connection);
				}
			}
			
			//Add new connections
			for(Iterator<String> itr=connections.iterator(); itr.hasNext();){
				connection = itr.next();
				
				if(!client_strings.contains(connection)){
					client = new SoftwareReuseClient(Utility.getHost(connection), Utility.getPort(connection));
					clients.add(client);
					client_strings.add(client.toString());
				}
			}
			
			buffer = "";
			
			for(Iterator<SoftwareReuseClient> itr=clients.iterator(); itr.hasNext();){
				buffer += itr.next().getStatus();
			}
			
			setText(buffer);						
			Utility.pause(100);
		}
	}
	
	public static void main(String args[])
	{
		JFrame frame = new JFrame();
		PolyglotStatusPanel psp;
		String server = "localhost";
		int port = 50002;
		int tmpi;  
		
  	if(args.length > 0){
  		tmpi = args[0].lastIndexOf(':');
  		
  		if(tmpi != -1){
  			server = args[0].substring(0, tmpi);
  			port = Integer.valueOf(args[0].substring(tmpi+1));
  		}
  	}
  	        
  	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
  	frame.add(new PolyglotStatusPanel(server, port));
  	frame.setPreferredSize(new Dimension(500, 300));
  	frame.pack();
    frame.setVisible(true);
	}
}