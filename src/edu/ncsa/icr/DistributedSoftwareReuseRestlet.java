package edu.ncsa.icr;
import edu.ncsa.icr.SoftwareReuseAuxiliary.*;
import edu.ncsa.utility.*;
import java.util.*;
import java.io.*;
import java.net.*;
import org.restlet.*;
import org.restlet.data.*;
import org.restlet.representation.*;
import org.restlet.resource.*;
import org.restlet.routing.*;

/**
 * A restful interface to multiple software reuse servers.
 * @author Kenton McHenry
 */
public class DistributedSoftwareReuseRestlet extends ServerResource
{
	private static TreeMap<RemoteTaskInfo, TreeSet<String>> tasks = new TreeMap<RemoteTaskInfo, TreeSet<String>>();
	private static TreeSet<String> servers = new TreeSet<String>();
	private static String[] servers_array = new String[0];
	
	/**
	 * Record the existence of a software server and the applications/tasks it provides.
	 * @param server a software server
	 */
	public synchronized static void addServer(String server)
	{
		Vector<String> applications, application_tasks, task_outputs, task_inputs;
		Vector<Vector<String>> task_inputs_outputs;
		RemoteTaskInfo rti;
		String server_url = "http://" + server + "/software/";
		String application_name;
		int tmpi;
		
		if(SoftwareReuseRestlet.queryEndpoint(server_url + "alive") != null){
			System.out.print("Adding server: " + server + " ");
			servers.add(server);
			servers_array = servers.toArray(new String[0]);
			applications = getEndpointValues(server_url);
			
			for(String application : applications){
				tmpi = application.indexOf('(');
				
				if(tmpi >= 0){
					application_name = application.substring(tmpi+1, application.length()-1).trim();
					application = application.substring(0, tmpi).trim();
				}else{
					application_name = application;
				}
				
				System.out.print(".");
				
				application_tasks = getEndpointValues(server_url + application);
				
				for(String task : application_tasks){
					task_inputs_outputs = getEndpointCommaSeparatedValues(server_url + application + "/" + task + "/*");
					task_inputs = task_inputs_outputs.get(0);
					task_outputs = task_inputs_outputs.get(1);
					
					for(String output : task_outputs){						
						for(String input : task_inputs){
							rti = new RemoteTaskInfo(application, task, output, input);
							
							if(tasks.containsKey(rti)){
								rti.servers = tasks.get(rti);
							}else{
								rti.application_name = application_name;
								tasks.put(rti, rti.servers);
							}
							
							rti.servers.add(server);
						}
					}
				}
			}
			
			System.out.println(" [queried]");
		}
	}
	
	/**
	 * Remove dead servers and their associated tasks.
	 */
	public synchronized static void checkServers()
	{
		String server, server_url;
		RemoteTaskInfo rti;
				
		for(Iterator<String> itr1=servers.iterator(); itr1.hasNext();){
			server = itr1.next();
			server_url = "http://" + server + "/software/";
			
			if(SoftwareReuseRestlet.queryEndpoint(server_url + "alive") == null){
				System.out.println("Dropping: " + server);
				itr1.remove();
				
				//Remove associated tasks
				for(Iterator<RemoteTaskInfo> itr2=tasks.keySet().iterator(); itr2.hasNext();){
					rti = itr2.next();
					rti.servers.remove(server);
					if(rti.servers.isEmpty()) itr2.remove();
				}
			}
		}
		
		if(servers_array.length != servers.size()){
			servers_array = servers.toArray(new String[0]);
		}
	}
	
	/**
	 * Get the software servers registered to this master service.
	 * @return the registered software servers
	 */
	public String getServers()
	{
		String buffer = "";
		
		for(String server : servers){
			buffer += server + "\n";
		}
		
		return buffer;
	}
	
	/**
	 * Get the applications available.
	 * @return the applications
	 */
	public String getApplications()
	{
		String buffer = "";
		TreeSet<String> applications = new TreeSet<String>();
				
		for(RemoteTaskInfo rti : tasks.keySet()){
			applications.add(rti.application_alias + " (" + rti.application_name + ")");
		}
		
		for(String application : applications){
			buffer += application + "\n";
		}
		
		return buffer;
	}
	
	/**
	 * Get the tasks available for the given application.
	 * @param alias the application alias
	 * @return the application tasks
	 */
	public String getApplicationTasks(String alias)
	{
		String buffer = "";
		TreeSet<String> application_tasks = new TreeSet<String>();
		
		for(RemoteTaskInfo rti : tasks.keySet()){
			if(rti.application_alias.equals(alias)){
				application_tasks.add(rti.task);
			}
		}
		
		for(String task : application_tasks){
			buffer += task + "\n";
		}
		
		return buffer;
	}
	
	/**
	 * Get the output formats supported by the given task.
	 * @param alias the application alias
	 * @param task the application task
	 * @return the input formats supported
	 */
	public String getApplicationTaskOutputs(String alias, String task)
	{
		String buffer = "";
		TreeSet<String> task_outputs = new TreeSet<String>();
		
		for(RemoteTaskInfo rti : tasks.keySet()){
			if(rti.application_alias.equals(alias) && rti.task.equals(task)){
				task_outputs.add(rti.output);
			}
		}
		
		for(String output : task_outputs){
			buffer += output + "\n";
		}
		
		return buffer;
	}

	/**
	 * Get the input formats supported by the given task.
	 * @param alias the application alias
	 * @param task the application task
	 * @return the input formats supported
	 */
	public String getApplicationTaskInputs(String alias, String task)
	{
		String buffer = "";
		TreeSet<String> task_inputs = new TreeSet<String>();
		
		for(RemoteTaskInfo rti : tasks.keySet()){
			if(rti.application_alias.equals(alias) && rti.task.equals(task)){
				task_inputs.add(rti.input);
			}
		}
		
		for(String input : task_inputs){
			buffer += input + "\n";
		}
		
		return buffer;
	}
	
	/**
	 * Get the input and output formats supported by the given task.
	 * @param alias the application alias
	 * @param task the application task
	 * @return the input and output formats supported
	 */
	public String getApplicationTaskInputsOutputs(String alias, String task)
	{
		String buffer = "";
		TreeSet<String> task_inputs = new TreeSet<String>();
		TreeSet<String> task_outputs = new TreeSet<String>();
		boolean FIRST_VALUE;
		
		for(RemoteTaskInfo rti : tasks.keySet()){
			if(rti.application_alias.equals(alias) && rti.task.equals(task)){
				task_inputs.add(rti.input);
				task_outputs.add(rti.output);
			}
		}
		
		FIRST_VALUE = true;
		
		for(String input : task_inputs){
			if(FIRST_VALUE) FIRST_VALUE = false; else buffer += ", ";
			buffer += input;
		}
		
		buffer += "\n";
		FIRST_VALUE = true;
		
		for(String output : task_outputs){
			if(FIRST_VALUE) FIRST_VALUE = false; else buffer += ", ";
			buffer += output;
		}
		
		return buffer;
	}
	
	/**
	 * Get a web form interface for this restful service.
	 * @return the form
	 */
	public String getForm()
	{		
		TreeSet<Pair<String,String>> applications = new TreeSet<Pair<String,String>>();
		TreeSet<String> application_tasks = new TreeSet<String>();
		TreeSet<String> task_inputs = new TreeSet<String>();
		TreeSet<String> task_outputs = new TreeSet<String>();
		String buffer = "";
		int count;
		boolean FIRST_BLOCK;
		boolean FIRST_VALUE;
		
		buffer += "<script type=\"text/javascript\">\n";
		buffer += "function setTasks(){\n";
		buffer += "  var select = document.getElementById('application');\n";
		buffer += "  var application = select.options[select.selectedIndex].value;\n";
		buffer += "  var tasks = document.getElementById('task');\n";
		buffer += "  \n";
		buffer += "  tasks.options.length = 0;\n";
		buffer += "  \n";
		
		//Build a list of applications
		for(RemoteTaskInfo rti : tasks.keySet()){
			applications.add(new Pair<String,String>(rti.application_alias, rti.application_name));
		}
		
		//Build application selection options
		FIRST_BLOCK = true;
		
		for(Pair<String,String> application : applications){
			if(FIRST_BLOCK) FIRST_BLOCK = false; else	buffer += "\n";
			buffer += "  if(application == \"" + application.first + "\"){\n";
			
			//Build application tasks
			application_tasks.clear();
			
			for(RemoteTaskInfo rti : tasks.keySet()){
				if(rti.application_alias.equals(application.first)){
					application_tasks.add(rti.task);
				}
			}
			
			//Add task selection options
			count = 0;

			for(String task : application_tasks){
				buffer += "    tasks.options[" + count + "] = new Option(\"" + task + "\", \"" + task + "\", " + (count == 0) + ", " + (count == 0) + ");\n";
				count++;
			}
			
			buffer += "  }\n";		
		}
		
		buffer += "  \n";
		buffer += "  setFormats();\n";
		buffer += "}\n";
		buffer += "\n";
		buffer += "function setFormats(){\n";
		buffer += "  var select = document.getElementById('application');\n";
		buffer += "  var application = select.options[select.selectedIndex].value;\n";
		buffer += "  var select = document.getElementById('task');\n";
		buffer += "  var task = select.options[select.selectedIndex].value;\n";
		buffer += "  var inputs = document.getElementById('inputs');\n";
		buffer += "  var outputs = document.getElementById('format');\n";
		buffer += "  \n";
		buffer += "  inputs.innerHTML = \"\";\n";
		buffer += "  outputs.options.length = 0;\n";
		buffer += "  \n";
		
		FIRST_BLOCK = true;
		
		for(Pair<String,String> application : applications){
			//Build application tasks
			application_tasks.clear();
			
			for(RemoteTaskInfo rti : tasks.keySet()){
				if(rti.application_alias.equals(application.first)){
					application_tasks.add(rti.task);
				}
			}
			
			for(String task : application_tasks){
				//Build task inputs/outputs
				task_outputs.clear();
				
				for(RemoteTaskInfo rti : tasks.keySet()){
					if(rti.application_alias.equals(application.first) && rti.task.equals(task)){
						task_outputs.add(rti.output);
					}
				}
				
				task_inputs.clear();
				
				for(RemoteTaskInfo rti : tasks.keySet()){
					if(rti.application_alias.equals(application.first) && rti.task.equals(task)){
						task_inputs.add(rti.input);
					}
				}
				
				//Add input/output selection options
				if(FIRST_BLOCK) FIRST_BLOCK = false; else	buffer += "\n";
				buffer += "  if(application == \"" + application.first + "\" && task == \"" + task + "\"){\n";	
				buffer += "    inputs.innerHTML = \"";
				FIRST_VALUE = true;
				
				for(String input : task_inputs){
					if(FIRST_VALUE) FIRST_VALUE = false; else buffer += ", ";
					buffer += input;
				}
				
				buffer += "\";\n";
				count = 0;
				
				for(String output : task_outputs){
					buffer += "    outputs.options[" + count + "] = new Option(\"" + output + "\", \"" + output + "\", false, false);\n";
					count++;
				}
				
				buffer += "  }\n";
			}			
		}
		
		buffer += "}\n";
		buffer += "</script>\n\n";
		
		buffer += "<form name=\"converson\" action=\"form/\" method=\"get\">\n";
		buffer += "<table>\n";
		buffer += "<tr><td><b>Application:</b></td>\n";
		buffer += "<td><select name=\"application\" id=\"application\" onchange=\"setTasks();\">\n";
		
		for(Pair<String,String> application : applications){
			buffer += "<option value=\"" + application.first + "\">" + application.second + "</option>\n";
		}
		
		buffer += "</select></td></tr>\n";
		buffer += "<tr><td><b>Task:</b></td>\n";
		buffer += "<td><select name=\"task\" id=\"task\" onchange=\"setFormats();\">\n";
				
		//Build application tasks (for the first application)
		application_tasks.clear();
		
		for(RemoteTaskInfo rti : tasks.keySet()){
			if(rti.application_alias.equals(applications.first().first)){
				application_tasks.add(rti.task);
			}
		}
		
		//Add task selection options
		for(String task : application_tasks){
			buffer += "<option value=\"" + task + "\">" + task + "</option>\n";
		}
		
		buffer += "</select></td></tr>\n";
		buffer += "<tr><td><td width=\"100\"><i><font size=\"-1\"><div id=\"inputs\">";
		
		//Build task inputs/outputs (for the first task of the first operation)
		task_outputs.clear();
		
		for(RemoteTaskInfo rti : tasks.keySet()){
			if(rti.application_alias.equals(applications.first().first) && rti.task.equals(application_tasks.first())){
				task_outputs.add(rti.output);
			}
		}
		
		task_inputs.clear();
		
		for(RemoteTaskInfo rti : tasks.keySet()){
			if(rti.application_alias.equals(applications.first().first) && rti.task.equals(application_tasks.first())){
				task_inputs.add(rti.input);
			}
		}
		
		//Add input/output selection options
		FIRST_VALUE = true;

		for(String input : task_inputs){
			if(FIRST_VALUE) FIRST_VALUE = false; else buffer += ", ";
			buffer += input;
		}
		
		buffer += "</div></font></i></td></tr>\n";
		buffer += "<tr><td><b>File:</b></td><td><input type=\"text\" name=\"file\" size=\"100\"></td></tr>\n";
		buffer += "<tr><td><b>Format:</b></td>\n";
		buffer += "<td><select name=\"format\" id=\"format\">\n";
		
		for(String output : task_outputs){
			buffer += "<option value=\"" + output + "\">" + output + "</option>\n";
		}
		
		buffer += "</select></td></tr>\n";		
		buffer += "<tr><td></td><td><input type=\"submit\" value=\"Submit\"></td></tr>\n";
		buffer += "</table>\n";
		buffer += "</form>";
		
		return buffer;
	}
	
	/**
	 * Check if the given server is busy.
	 * @param server the software server to check
	 * @return true if the server said it was busy
	 */
	private boolean isServerBusy(String server)
	{		
		return SoftwareReuseRestlet.queryEndpoint("http://" + server + "/software/busy").equals("true");
	}

	@Get
	/**
	 * Handle HTTP GET requests.
	 */
	public Representation httpGetHandler()
	{
		Vector<String> parts = Utility.split(getReference().getRemainingPart(), '/', true);
		String part1 = (parts.size() > 0) ? parts.get(0) : "";
		String part2 = (parts.size() > 1) ? parts.get(1) : "";
		String part3 = (parts.size() > 2) ? parts.get(2) : "";
		String part4 = (parts.size() > 3) ? parts.get(3) : "";
		String application = null, task = null, file = null, format = null, file_format, server, url;
		Form form;
		Parameter p;
		RemoteTaskInfo rti;
		
		if(part1.isEmpty()){
			return new StringRepresentation(getApplications(), MediaType.TEXT_PLAIN);
		}else{
			if(part1.equals("form")){
				form = getRequest().getResourceRef().getQueryAsForm();
				p = form.getFirst("application"); if(p != null) application = p.getValue();
				p = form.getFirst("task"); if(p != null) task = p.getValue();
				p = form.getFirst("file"); if(p != null) file = p.getValue();
				p = form.getFirst("format"); if(p != null) format = p.getValue();
								
				if(application != null && task != null && file != null && format != null){
					url = getRootRef() + "/" + application + "/" + task + "/" + format + "/" + URLEncoder.encode(file);

					return new StringRepresentation("<html><head><meta http-equiv=\"refresh\" content=\"1; url=" + url + "\"></head</html>", MediaType.TEXT_HTML);
				}else{
					return new StringRepresentation(getForm(), MediaType.TEXT_HTML);
				}
			}else if(part1.equals("register")){
				if(part2.isEmpty()){
					return new StringRepresentation("invalid endpoint", MediaType.TEXT_PLAIN);
				}else{
					server = URLDecoder.decode(part2);

					if(!servers.contains(server)){
						final String server_final = server;
						
				  	new Thread(){
				  		public void run(){
				  			checkServers();		//While we are here go ahead and check on previously added servers
				  			addServer(server_final);
				  		}
				  	}.start();
						
						return new StringRepresentation(server + " scheduled to be queried", MediaType.TEXT_PLAIN);
					}else{
						return new StringRepresentation(server + " registered", MediaType.TEXT_PLAIN);
					}
				}
			}else if(part1.equals("servers")){				
				return new StringRepresentation(getServers(), MediaType.TEXT_PLAIN);
			}else if(part1.equals("alive")){
				return new StringRepresentation("yes", MediaType.TEXT_PLAIN);
			}else if(part2.isEmpty()){
				return new StringRepresentation(getApplicationTasks(part1), MediaType.TEXT_PLAIN);
			}else{
				if(part3.isEmpty()){
					return new StringRepresentation(getApplicationTaskInputs(part1, part2), MediaType.TEXT_PLAIN);
				}else{
					if(part3.equals("*")){
						return new StringRepresentation(getApplicationTaskInputsOutputs(part1, part2), MediaType.TEXT_PLAIN);
					}else if(part4.isEmpty()){
						return new StringRepresentation(getApplicationTaskOutputs(part1, part2), MediaType.TEXT_PLAIN);
					}else{
						application = part1;
						task = part2;
						format = part3;
						file = part4;
						
						file_format = Utility.getFilenameExtension(file);
						rti = new RemoteTaskInfo(application, task, format, file_format);
						rti.servers = tasks.get(rti);
						
						//Find a non-busy server
						server = null;
						
						for(int i=0; i<servers_array.length; i++){
							if(!isServerBusy(servers_array[i])){
								server = servers_array[i];
								break;
							}
						}
						
						if(server == null){		//If all servers are busy then pick one randomly
							server = servers_array[new Random().nextInt()%servers_array.length];
						}
						
						//Return the software server connection that will perform the task
						System.out.println("[" + server + "]: " + application + "/" + task + " " + file_format + "->" + format);
						url = "http://" + server + "/software/" + application + "/" + task + "/" + format + "/" + file;
						
						return new StringRepresentation("<html><head><meta http-equiv=\"refresh\" content=\"1; url=" + url + "\"></head</html>", MediaType.TEXT_HTML);
					}
				}
			}
		}
	}
	
	/**
	 * Query a restlet endpoint that responds with a line separated text list of values.
	 * @param url the URL of the restlet endpoint to query
	 * @return the values returned from the endpoint
	 */
	public static Vector<String> getEndpointValues(String url)
	{
		Vector<String> values = new Vector<String>();
		String text = SoftwareReuseRestlet.queryEndpoint(url);
		Scanner scanner = new Scanner(text);
				
		while(scanner.hasNextLine()){
			values.add(scanner.nextLine().trim());
		}
		
		return values;
	}
	
	/**
	 * Query a restlet endpoint that responds with lines of comma separated values.
	 * @param url the URL of the restlet endpoint to query
	 * @return the values returned from the endpoint
	 */
	public static Vector<Vector<String>> getEndpointCommaSeparatedValues(String url)
	{
		Vector<Vector<String>> values = new Vector<Vector<String>>();
		String text = SoftwareReuseRestlet.queryEndpoint(url);
		String line;
		Scanner scanner = new Scanner(text);

		while(scanner.hasNextLine()){
			line = scanner.nextLine().trim();
			values.add(Utility.split(line, ',', true, true));
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
	    BufferedReader ins = new BufferedReader(new FileReader("DistributedSoftwareReuseRestlet.ini"));
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
		
	  //Start the service		
		try{			
			Component component = new Component();
			component.getServers().add(Protocol.HTTP, port);
			component.getClients().add(Protocol.HTTP);
			component.getLogService().setEnabled(false);
			
			org.restlet.Application application = new org.restlet.Application(){
				@Override
				public Restlet createInboundRoot(){
					Router router = new Router(getContext());
					router.attachDefault(DistributedSoftwareReuseRestlet.class);
					return router;
				}
			};
			
			component.getDefaultHost().attach("/distributed_software", application);
			component.start();
		}catch(Exception e) {e.printStackTrace();}
		
		System.out.println("\nDistributed software reuse restlet is running...\n");
	}
}