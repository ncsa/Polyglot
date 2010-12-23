package edu.ncsa.icr.polyglot;
import edu.ncsa.icr.*;
import edu.ncsa.utility.*;
import java.awt.*;
import javax.swing.*;
import java.util.*;

/**
 * A tool to monitor the status of a Polyglot server.
 * @author Kenton McHenry
 */
public class PolyglotMonitor extends HTMLPanel implements Runnable
{
	private PolyglotClient polyglot;
	
	/**
	 * Class constructor.
	 * @param server the Polyglot server
	 * @param port the port of the Polyglot server
	 */
	public PolyglotMonitor(String server, int port)
	{
		polyglot = new PolyglotClient(server, port);
		new Thread(this).start();
	}
	
	/**
	 * Continuously check the status of the Polyglot server and display the results.
	 */
	public void run()
	{
		TreeSet<String> servers = new TreeSet<String>();
		Vector<String> clients;
		TreeSet<SoftwareReuseClient> server_clients = new TreeSet<SoftwareReuseClient>();
		TreeSet<String> server_client_strings = new TreeSet<String>();
		SoftwareReuseClient server_client;
		Vector<String> tokens;
		String server;
		String line, buffer, color;
		int count;
		
		while(true){
			servers.clear();
			servers.addAll(polyglot.getServers());
			clients = polyglot.getClients();
			
			//Remove old connections
			for(Iterator<SoftwareReuseClient> itr=server_clients.iterator(); itr.hasNext();){
				server_client = itr.next();
				
				if(!servers.contains(server_client.toString())){
					server_client.close();
					server_clients.remove(server_client);
				}
			}
			
			for(Iterator<String> itr=server_client_strings.iterator(); itr.hasNext();){
				server = itr.next();
				
				if(!servers.contains(server)){
					server_client_strings.remove(server);
				}
			}
			
			//Add new connections
			for(Iterator<String> itr=servers.iterator(); itr.hasNext();){
				server = itr.next();
				
				if(!server_client_strings.contains(server)){
					server_client = new SoftwareReuseClient(Utility.getHost(server), Utility.getPort(server));
					server_clients.add(server_client);
					server_client_strings.add(server_client.toString());
				}
			}
			
			//Display the status table
			buffer = "<b><font size=\"+1\">Software Servers</font></b><br><br>";
			buffer += "<center><table border=\"1\" style=\"border-style: outset;\"><tr>";
			buffer += "<td width=\"30\" align=\"center\"><b>#</b></td>";
			buffer += "<td width=\"300\" align=\"center\"><b>server</b></td>";
			buffer += "<td width=\"100\" align=\"center\"><b>status</b></td>";
			buffer += "<td width=\"200\" align=\"center\"><b>software</b></td>";
			buffer += "<td width=\"100\" align=\"center\"><b>operation</b></td>";
			buffer += "<td width=\"300\" align=\"center\"><b>file</b></td></tr>";
			
			count = 0;
			
			for(Iterator<SoftwareReuseClient> itr=server_clients.iterator(); itr.hasNext();){
				server_client = itr.next(); 
				count++;
				
				line = server_client.getStatus();
				tokens = Utility.split(line, ',', false, true);
				
				if(tokens.get(0).equals("idle")){
					color = "ffffff";
				}else{
					color = "b7cee4";
				}		
				
				buffer += "<tr bgcolor=\"#" + color + "\">";
				buffer += "<td align=\"center\"><b>" + count + "</b></td>";
				buffer += "<td align=\"center\"><i>" + server_client + "</i></td>";

				for(int i=0; i<4; i++){
					if(i < tokens.size()){
						buffer += "<td align=\"center\">" + tokens.get(i) + "</td>";
					}else{
						buffer += "<td></td>";
					}
				}
				
				buffer += "</tr>";
			}
			
			buffer += "</table></center><br>";
			buffer += "<b><font size=\"+1\">Clients</font></b><br><br>";
			
			for(int i=0; i<clients.size(); i++){
				buffer += "&nbsp;&nbsp;" + (i+1) + ". " + clients.get(i) + "<br>";
			}
			
			setText(buffer);
			Utility.pause(100);
		}
	}
	
	/**
	 * Add this panel to a frame.
	 * @return the created frame
	 */
	public JFrame createFrame()
	{
		JFrame frame = new JFrame("Polyglot Server Status");
		
  	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
  	frame.add(this);
  	frame.setPreferredSize(new Dimension(1100, 400));
  	frame.pack();
    frame.setVisible(true);
    
    return frame;
	}
	
	/**
	 * Monitor a Polyglot server.
	 * @param args the command line arguments
	 */
	public static void main(String args[])
	{
		PolyglotMonitor pm;
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
  	
  	pm = new PolyglotMonitor(server, port);
  	pm.createFrame();
	}
}