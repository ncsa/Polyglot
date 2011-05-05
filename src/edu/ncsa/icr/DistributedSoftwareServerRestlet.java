package edu.ncsa.icr;
import edu.ncsa.icr.ICRAuxiliary.*;
import java.io.*;
import java.net.*;
import java.util.*;
import kgm.utility.*;
import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.disk.*;
import org.restlet.*;
import org.restlet.data.*;
import org.restlet.ext.fileupload.*;
import org.restlet.representation.*;
import org.restlet.resource.*;
import org.restlet.routing.*;

/**
 * A restful interface to multiple software reuse servers.
 * @author Kenton McHenry
 */
public class DistributedSoftwareServerRestlet extends ServerResource
{
	private static TreeMap<RemoteTaskInfo, TreeSet<String>> tasks = new TreeMap<RemoteTaskInfo, TreeSet<String>>();
	private static TreeSet<String> servers = new TreeSet<String>();
	
	private static TreeSet<String> applications = new TreeSet<String>();
	private static TreeMap<String,String> name_map = new TreeMap<String,String>();
	private static TreeMap<String,String> server_map = new TreeMap<String,String>();
	
	/**
	 * Record the existence of a software server and the applications/tasks it provides.
	 * @param server a software server
	 */
	public synchronized static void addServer(String server)
	{
		Vector<String> server_applications, application_tasks, task_outputs, task_inputs;
		Vector<Vector<String>> task_inputs_outputs;
		RemoteTaskInfo rti;
		String server_url = "http://" + server + "/";
		String application_name;
		int tmpi;
		
		if(SoftwareServerRestlet.queryEndpoint(server_url + "alive") != null){
			System.out.print("Adding server: " + server + " ");
			servers.add(server);
			server_applications = getEndpointValues(server_url + "software");
			
			for(String application : server_applications){
				tmpi = application.indexOf('(');
				
				if(tmpi >= 0){
					application_name = application.substring(tmpi+1, application.length()-1).trim();
					application = application.substring(0, tmpi).trim();
				}else{
					application_name = application;
				}
				
				applications.add(application);
				name_map.put(application, application_name);
				server_map.put(application, server_url);
				System.out.print(".");
				
				application_tasks = getEndpointValues(server_url + "software/" + application);
				
				for(String task : application_tasks){
					task_inputs_outputs = getEndpointCommaSeparatedValues(server_url + "software/" + application + "/" + task + "/*");
					task_inputs = task_inputs_outputs.get(0);
					task_outputs = task_inputs_outputs.get(1);
											
					for(String input : task_inputs){
						for(String output : task_outputs){						
							rti = new RemoteTaskInfo(application, task, input, output);
							
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
		String server;
		RemoteTaskInfo rti;
		boolean SERVER_DROPPED = false;
		
		for(Iterator<String> itr1=servers.iterator(); itr1.hasNext();){
			server = itr1.next();
			
			if(SoftwareServerRestlet.queryEndpoint("http://" + server + "/alive") == null){
				System.out.println("Dropping: " + server);
				itr1.remove();
				SERVER_DROPPED = true;
				
				//Remove associated tasks
				for(Iterator<RemoteTaskInfo> itr2=tasks.keySet().iterator(); itr2.hasNext();){
					rti = itr2.next();
					rti.servers.remove(server);
					if(rti.servers.isEmpty()) itr2.remove();
				}
			}
		}
		
		if(SERVER_DROPPED){
			applications.clear();
			
			for(Iterator<RemoteTaskInfo> itr = tasks.keySet().iterator(); itr.hasNext();){
				applications.add(itr.next().application_alias);
			}
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
		
		for(String application : applications){
			buffer += application + " (" + name_map.get(application) + ")\n";
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
	 * Get an icon representation of the available software.
	 * @return the HTML for the icon representation
	 */
	public String getApplicationStack()
	{
		String buffer = "";
		
		for(String application : applications){
			buffer += "<div style=\"float:left\">\n";
			buffer += "<table>\n";
			buffer += "<tr><td align=\"center\">\n";
			buffer += "<a href=\"form/get?application=" + application + "\">";
			buffer += "<img src=\"" + server_map.get(application) + "image/" + application + ".jpg\" width=\"50\" border=\"0\">";
			buffer += "</a>\n";
			buffer += "</td></tr><tr><td align=\"center\">\n";
			buffer += "<font size=\"-1\">" + name_map.get(application) + "</font>\n";
			buffer += "</td></tr>\n";
			buffer += "</table>\n";
			buffer += "</div>\n";
			buffer += "\n";
		}
		
		return buffer;
	}
	
	/**
	 * Get a web form interface for this restful service.
	 * @param POST_UPLOADS true if this form should use POST rather than GET for uploading files
	 * @param selected_application the default application
	 * @param HIDE_APPLICATIONS true if applications menu should be hidden
	 * @return the form
	 */
	public String getForm(boolean POST_UPLOADS, String selected_application, boolean HIDE_APPLICATIONS)
	{		
		TreeSet<Pair<String,String>> applications = new TreeSet<Pair<String,String>>();
		TreeSet<String> application_tasks = new TreeSet<String>();
		TreeSet<String> task_inputs = new TreeSet<String>();
		TreeSet<String> task_outputs = new TreeSet<String>();
		String buffer = "";
		String icon_url;
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
		buffer += "\n";
		buffer += "function setAPICall(){\n";
		buffer += "  var select = document.getElementById('application');\n";
		buffer += "  var application = select.options[select.selectedIndex].value;\n";
		buffer += "  var select = document.getElementById('task');\n";
		buffer += "  var task = select.options[select.selectedIndex].value;\n";
		buffer += "  var file = document.getElementById('file').value;\n";
		buffer += "  var select = document.getElementById('format');\n";
		buffer += "  var format = select.options[select.selectedIndex].value;\n";

		if(!POST_UPLOADS){
			buffer += "  var api_url = \"http://\" + location.host + \"/software/\" + application + \"/\" + task + \"/\" + format + \"/\" + encodeURIComponent(file);\n";
			buffer += "  var api_html = \"http://\" + location.host + \"/software/<font color=\\\"#ff7777\\\">\" + application + \"</font>/<font color=\\\"#77ff77\\\">\" + task + \"</font>/<font color=\\\"#7777ff\\\">\" + format + \"</font>/<font color=\\\"#777777\\\">\" + encodeURIComponent(file) + \"</font>\";\n";
		}else{
			buffer += "  var api_url = \"http://\" + location.host + \"/software/\" + application + \"/\" + task + \"/\" + format + \"/\";\n";
			buffer += "  var api_html = \"http://\" + location.host + \"/software/<font color=\\\"#ff7777\\\">\" + application + \"</font>/<font color=\\\"#77ff77\\\">\" + task + \"</font>/<font color=\\\"#7777ff\\\">\" + format + \"</font>/\";\n";
		}
		
		buffer += "  \n";
		buffer += "  api.innerHTML = \"<i><b><font color=\\\"#777777\\\">RESTful API call</font></b><br><br><a href=\\\"\" + api_url + \"\\\"style=\\\"text-decoration:none; color:#777777\\\">\" + api_html + \"</a></i>\";\n";
		buffer += "  setTimeout('setAPICall()', 500);\n";
		buffer += "}\n";
		buffer += "</script>\n\n";
		
		buffer += "<center>\n";
		
		if(!POST_UPLOADS){
			buffer += "<form name=\"converson\" action=\"\" method=\"get\">\n";
		}else{
			buffer += "<form enctype=\"multipart/form-data\" name=\"converson\" action=\"\" method=\"post\">\n";
		}
		
		buffer += "<table>\n";
		
		if(selected_application != null && HIDE_APPLICATIONS){			
			buffer += "<tr><td>";
			icon_url = server_map.get(selected_application) + "image/" + selected_application + ".jpg";
			if(Utility.existsURL(icon_url)) buffer += "<img src=\"" + icon_url + "\" width=\"50\" border=\"0\">";
			buffer += "</td><td><h2>" + name_map.get(selected_application) + "</h2></td></tr>\n";
		}
		
		buffer += "<tr><td>";
		if(!HIDE_APPLICATIONS) buffer += "<b>Application:</b>";
		buffer += "</td>\n";
		buffer += "<td><select name=\"application\" id=\"application\" onchange=\"setTasks();\"";
		if(HIDE_APPLICATIONS) buffer += " style=\"visibility:hidden;\"";
		buffer += ">\n";
		
		for(Pair<String,String> application : applications){
			buffer += "<option value=\"" + application.first + "\"";
			
			if(selected_application != null && selected_application.equals(application.first)){
				buffer += " selected";
			}
			
			buffer += ">" + application.second + "</option>\n";
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
		
		if(!POST_UPLOADS){
			buffer += "<tr><td><b>File URL:</b></td><td><input type=\"text\" name=\"file\" id=\"file\" size=\"100\"></td></tr>\n";
		}else{
			buffer += "<tr><td><b>File:</b></td><td><input type=\"file\" name=\"file\" id=\"file\" size=\"100\"></td></tr>\n";
		}
		
		buffer += "<tr><td><b>Format:</b></td>\n";
		buffer += "<td><select name=\"format\" id=\"format\">\n";
		
		for(String output : task_outputs){
			buffer += "<option value=\"" + output + "\">" + output + "</option>\n";
		}
		
		buffer += "</select></td></tr>\n";		
		buffer += "<tr><td></td><td><input type=\"submit\" value=\"Submit\"></td></tr>\n";
		buffer += "<tr><td height=\"25\"></td><td></td></tr>\n";
		buffer += "<tr><td></td><td align=\"center\"><div name=\"api\" id=\"api\"></div></td></tr>\n";
		buffer += "</table>\n";
		buffer += "</form>";
		buffer += "</center>\n";
		buffer += "\n";
		buffer += "<script type=\"text/javascript\">setAPICall();</script>\n";		
		
		if(selected_application != null){
			buffer += "<script type=\"text/javascript\">setTasks();</script>\n";
		}
		
		return buffer;
	}
	
	/**
	 * Get a web form interface for this restful service.
	 * @return the form
	 */
	public String getForm()
	{
		return getForm(false, null, false);
	}
	
	/**
	 * Check if the given server is busy.
	 * @param server the software server to check
	 * @return true if the server said it was busy
	 */
	private boolean isServerBusy(String server)
	{		
		return SoftwareServerRestlet.queryEndpoint("http://" + server + "/busy").equals("true");
	}

	/**
	 * Find a non-busy server that can perform the given task.
	 * @param application the application to use
	 * @param task the task to perform
	 * @param input the input format
	 * @param output the output format
	 * @return the available server
	 */
	public String getNonBusyServer(String application, String task, String input, String output)
	{
		String server = null;
		String[] servers_array;
		RemoteTaskInfo rti;
		
		rti = new RemoteTaskInfo(application, task, input, output);
		rti.servers = tasks.get(rti);
		
		if(rti.servers != null && !rti.servers.isEmpty()){
			servers_array = rti.servers.toArray(new String[0]);
			
			for(int i=0; i<servers_array.length; i++){
				if(!isServerBusy(servers_array[i])){
					server = servers_array[i];
					break;
				}
			}
			
			//If all servers are busy then pick one randomly
			if(server == null){
				server = servers_array[new Random().nextInt()%servers_array.length];
			}
		}
		
		return server;
	}
	
	@Get
	/**
	 * Handle HTTP GET requests.
	 */
	public Representation httpGetHandler()
	{
		Vector<String> parts = Utility.split(getReference().getRemainingPart(), '/', true);
		String part0 = (parts.size() > 0) ? parts.get(0) : "";
		String part1 = (parts.size() > 1) ? parts.get(1) : "";
		String part2 = (parts.size() > 2) ? parts.get(2) : "";
		String part3 = (parts.size() > 3) ? parts.get(3) : "";
		String part4 = (parts.size() > 4) ? parts.get(4) : "";
		String application = null, task = null, file = null, format = null, server, url, result;
		String buffer;
		Form form;
		Parameter p;
		
		if(part0.isEmpty()){
			return new StringRepresentation(getApplicationStack(), MediaType.TEXT_HTML);
		}else if(part0.equals("software")){
			if(part1.isEmpty()){
				return new StringRepresentation(getApplications(), MediaType.TEXT_PLAIN);
			}else{
				if(part2.isEmpty()){
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
							
							server = getNonBusyServer(application, task, Utility.getFilenameExtension(file), format);
							url = "http://" + server + "/software/" + application + "/" + task + "/" + format + "/" + file;

							System.out.println("[" + server + "]: " + application + "/" + task + "/" + format + "/" + file);
							result = Utility.readURL(url, "text/plain");
							
							if(SoftwareServerRestlet.isTextOnly(Request.getCurrent())){
								return new StringRepresentation(result, MediaType.TEXT_PLAIN);
							}else{
								return new StringRepresentation("<a href=" + result + ">" + result + "</a>", MediaType.TEXT_HTML);
							}
						}
					}
				}
			}
		}else if(part0.equals("form")){
			if(part1.isEmpty()){
				buffer = "";
				buffer += "get\n";
				buffer += "post\n";
				
				return new StringRepresentation(buffer, MediaType.TEXT_PLAIN);
			}else{
				form = getRequest().getResourceRef().getQueryAsForm();
				p = form.getFirst("application"); if(p != null) application = p.getValue();
				p = form.getFirst("task"); if(p != null) task = p.getValue();
				p = form.getFirst("file"); if(p != null) file = p.getValue();
				p = form.getFirst("format"); if(p != null) format = p.getValue();
								
				if(application != null && task != null && file != null && format != null){
					url = getRootRef() + "software/" + application + "/" + task + "/" + format + "/" + URLEncoder.encode(file);
	
					return new StringRepresentation("<html><head><meta http-equiv=\"refresh\" content=\"1; url=" + url + "\"></head</html>", MediaType.TEXT_HTML);
				}else{
					if(part1.startsWith("get")){
						return new StringRepresentation(getForm(false, application, application!=null), MediaType.TEXT_HTML);
					}else if(part1.startsWith("post")){
						return new StringRepresentation(getForm(true, application, application!=null), MediaType.TEXT_HTML);
					}else{
						return new StringRepresentation("Error: invalid endpoint", MediaType.TEXT_PLAIN);
					}
				}
			}
		}else if(part0.equals("register")){
			if(part1.isEmpty()){
				return new StringRepresentation("Error: invalid endpoint", MediaType.TEXT_PLAIN);
			}else{
				server = URLDecoder.decode(part1);

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
		}else if(part0.equals("servers")){				
			return new StringRepresentation(getServers(), MediaType.TEXT_PLAIN);
		}else if(part0.equals("alive")){
			return new StringRepresentation("yes", MediaType.TEXT_PLAIN);
		}else{
			return new StringRepresentation("Error: invalid endpoint", MediaType.TEXT_PLAIN);
		}
	}
	
	@Post
	/**
	 * Handle HTTP POST requests.
	 * @param entity the entity
	 */
	public Representation httpPostHandler(Representation entity)
	{
		Vector<String> parts = Utility.split(getReference().getRemainingPart(), '/', true);
		String part0 = (parts.size() > 0) ? parts.get(0) : "";
		String part1 = (parts.size() > 1) ? parts.get(1) : "";
		String part2 = (parts.size() > 2) ? parts.get(2) : "";
		String part3 = (parts.size() > 3) ? parts.get(3) : "";
		TreeMap<String,String> parameters = new TreeMap<String,String>();
		String application = null, task = null, format = null, server, url, result = null;
		boolean FORM_POST = !part0.isEmpty() && part0.equals("form");
		boolean TASK_POST = !part1.isEmpty() && !part2.isEmpty() && !part3.isEmpty();
		FileItem file = null;
		
		if(FORM_POST || TASK_POST){
			if(MediaType.MULTIPART_FORM_DATA.equals(entity.getMediaType(), true)){
				DiskFileItemFactory factory = new DiskFileItemFactory();
				factory.setSizeThreshold(1000240);
				RestletFileUpload upload = new RestletFileUpload(factory);
				List<FileItem> items;
				FileItem fi;
				
				try{
					items = upload.parseRequest(getRequest());
					
					for(Iterator<FileItem> itr = items.iterator(); itr.hasNext();){
						fi = itr.next();
						
						if(fi.getName() == null){
							parameters.put(fi.getFieldName(), new String(fi.get(), "UTF-8"));
						}else{
							file = fi;
						}
					}
				}catch(Exception e) {e.printStackTrace();}
			}
			
			if(FORM_POST){
				application = parameters.get("application");
				task = parameters.get("task");
				format = parameters.get("format");
			}else if(TASK_POST){
				application = part1;
				task = part2;
				format = part3;				
			}
			
			if(application != null && task != null && file != null && format != null){	
				server = getNonBusyServer(application, task, Utility.getFilenameExtension(Utility.unixPath(file.getName())), format);
				url = "http://" + server + "/software/" + application + "/" + task + "/" + format + "/";
				
				System.out.println("[" + server + "]: " + application + "/" + task + "/" + format + "/[" + file.getName() + "]");

				try{				
					result = Utility.postFile(url, file.getName(), file.getInputStream(), "text/plain");
				}catch(Exception e) {e.printStackTrace();}

				if(result == null){
					return new StringRepresentation("Error: could not POST file", MediaType.TEXT_PLAIN);
				}else if(SoftwareServerRestlet.isTextOnly(Request.getCurrent())){
					return new StringRepresentation(result, MediaType.TEXT_PLAIN);
				}else{
					return new StringRepresentation("<a href=" + result + ">" + result + "</a>", MediaType.TEXT_HTML);
				}
			}
		}
		
		return httpGetHandler();
	}
	
	/**
	 * Query a restlet endpoint that responds with a line separated text list of values.
	 * @param url the URL of the restlet endpoint to query
	 * @return the values returned from the endpoint
	 */
	public static Vector<String> getEndpointValues(String url)
	{
		Vector<String> values = new Vector<String>();
		String text = SoftwareServerRestlet.queryEndpoint(url);
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
		String text = SoftwareServerRestlet.queryEndpoint(url);
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
		
		//Load configuration file
	  try{
	    BufferedReader ins = new BufferedReader(new FileReader("DistributedSoftwareServerRestlet.conf"));
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
					router.attachDefault(DistributedSoftwareServerRestlet.class);
					return router;
				}
			};
			
			component.getDefaultHost().attach("/", application);
			component.start();
		}catch(Exception e) {e.printStackTrace();}
  	
		System.out.println("\nDistributed software reuse restlet is running...\n");
	}
}