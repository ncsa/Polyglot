package edu.ncsa.icr.polyglot;
import edu.ncsa.icr.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import kgm.utility.*;

/**
 * A tool to monitor the status of a Polyglot server.
 * @author Kenton McHenry
 */
public class PolyglotMonitor extends HTMLPanel implements Runnable, ActionListener
{
	private Polyglot polyglot;

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
	 * Class constructor.
	 * @param polyglot the Polyglot steward to use
	 */
	public PolyglotMonitor(PolyglotSteward polyglot)
	{
		this.polyglot = polyglot;		
		new Thread(this).start();
	}
	
	/**
	 * Continuously check the status of the Polyglot server and display the results.
	 */
	public void run()
	{
		TreeSet<String> servers = new TreeSet<String>();
		Vector<String> clients = new Vector<String>();
		TreeSet<SoftwareServerClient> server_clients = new TreeSet<SoftwareServerClient>();
		TreeSet<String> server_client_strings = new TreeSet<String>();
		Vector<SoftwareServerClient> lost_server_clients = new Vector<SoftwareServerClient>();
		Vector<String> lost_server_client_strings = new Vector<String>();
		SoftwareServerClient server_client;
		Vector<String> tokens;
		String server;
		String line, buffer, color;
		int count;
		
		while(true){
			servers.clear();
			servers.addAll(polyglot.getServers());

			if(polyglot instanceof PolyglotClient){
				clients = ((PolyglotClient)polyglot).getClients();
			}
						
			//Remove lost connections
			lost_server_clients.clear();
			
			for(Iterator<SoftwareServerClient> itr=server_clients.iterator(); itr.hasNext();){
				server_client = itr.next();
				
				if(!servers.contains(server_client.toString())){
					server_client.close();
					lost_server_clients.add(server_client);
				}
			}
			
			for(int i=0; i<lost_server_clients.size(); i++){
				server_clients.remove(lost_server_clients.get(i));
			}
			
			lost_server_client_strings.clear();
			
			for(Iterator<String> itr=server_client_strings.iterator(); itr.hasNext();){
				server = itr.next();
				
				if(!servers.contains(server)){
					lost_server_client_strings.add(server);
				}
			}
			
			for(int i=0; i<lost_server_client_strings.size(); i++){
				server_client_strings.remove(lost_server_client_strings.get(i));
			}
			
			//Add new connections
			for(Iterator<String> itr=servers.iterator(); itr.hasNext();){
				server = itr.next();
				
				if(!server_client_strings.contains(server)){
					server_client = new SoftwareServerClient(Utility.getHost(server), Utility.getPort(server));
					server_clients.add(server_client);
					server_client_strings.add(server_client.toString());
				}
			}
			
			//Display the status table
			buffer = "<b><font size=\"+1\">Software Servers</font></b><br><br>\n";
			buffer += "<center><table border=\"1\" style=\"border-style: outset;\"><tr>\n";
			buffer += "<td width=\"30\" align=\"center\"><b>#</b></td>\n";
			buffer += "<td width=\"300\" align=\"center\"><b>server</b></td>\n";
			buffer += "<td width=\"100\" align=\"center\"><b>status</b></td>\n";
			buffer += "<td width=\"200\" align=\"center\"><b>software</b></td>\n";
			buffer += "<td width=\"100\" align=\"center\"><b>operation</b></td>\n";
			buffer += "<td width=\"300\" align=\"center\"><b>file</b></td></tr>\n";
			
			count = 0;
			
			for(Iterator<SoftwareServerClient> itr=server_clients.iterator(); itr.hasNext();){
				server_client = itr.next(); 
				count++;
				
				line = server_client.getStatus();
				tokens = Utility.split(line, ',', false, true);
				
				if(!tokens.isEmpty()){
					if(tokens.get(0).equals("idle")){
						color = "ffffff";
					}else{
						color = "b7cee4";
					}		
					
					buffer += "<tr bgcolor=\"#" + color + "\">\n";
					buffer += "<td align=\"center\"><b>" + count + "</b></td>\n";
					buffer += "<td align=\"center\"><i>" + server_client + "</i></td>\n";
	
					for(int i=0; i<4; i++){
						if(i < tokens.size()){
							buffer += "<td align=\"center\">" + tokens.get(i) + "</td>\n";
						}else{
							buffer += "<td></td>\n";
						}
					}
					
					buffer += "</tr>\n";
				}
			}
			
			buffer += "</table></center><br>\n";
			buffer += "<b><font size=\"+1\">Clients</font></b><br><br>\n";
			
			if(clients.isEmpty()){
				buffer += "&nbsp;&nbsp;None<br>\n";
			}else{
				for(int i=0; i<clients.size(); i++){
					buffer += "&nbsp;&nbsp;" + (i+1) + ". " + clients.get(i) + "<br>\n";
				}
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
		JFrame frame;
		JMenuBar menubar = new JMenuBar();
		JMenu menu;
		JMenuItem item;
		
		menu = new JMenu("File");
		item = new JMenuItem("Exit"); item.addActionListener(this); menu.add(item);
		menubar.add(menu);
		menu = new JMenu("Window");
		item = new JMenuItem("I/O-Graph"); item.addActionListener(this); menu.add(item);
		menubar.add(menu);
		
		frame = new JFrame("Polyglot Server Status");
		frame.add(this);
		frame.setJMenuBar(menubar);
    frame.setPreferredSize(new Dimension(1100, 400));
  	frame.pack();
    frame.setVisible(true);
    
    return frame;
	}
	
	/**
	 * Action event listener.
	 * @param e an action event
	 */
	public void actionPerformed(ActionEvent e)
	{
		if(((JMenuItem)e.getSource()).getText().equals("Exit")){
			System.exit(0);
		}else	if(((JMenuItem)e.getSource()).getText().equals("I/O-Graph")){
			new DistributedSoftwareIOGraphPanel(polyglot.getDistributedInputOutputGraph(), 2).createFrame();
		}
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
		
		//Debug arguments
		if(true && args.length == 0){
			args = new String[]{"polyglot1.ncsa.illinois.edu:50002"};
		}
		
		//Process command line arguments
		for(int i=0; i<args.length; i++){			
			if(args[i].equals("-?")){
				System.out.println("Usage: ScriptInstaller [options] [host:port]");
				System.out.println();
				System.out.println("Options: ");
				System.out.println("  -?: display this help");
				System.out.println();
				System.exit(0);
			}else{
	  		tmpi = args[i].lastIndexOf(':');
	  		
	  		if(tmpi != -1){
	  			server = args[i].substring(0, tmpi);
	  			port = Integer.valueOf(args[i].substring(tmpi+1));
	  		}				
			}
		}
  	  	
  	JFrame frame = new PolyglotMonitor(server, port).createFrame();
  	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
}