package edu.illinois.ncsa.isda.softwareserver;
import edu.illinois.ncsa.isda.softwareserver.SoftwareServerAuxiliary.*;
import edu.illinois.ncsa.isda.softwareserver.SoftwareServerUtility;
import kgm.utility.Utility;
import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.text.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.AMQP;

/**
 * Software Server REST interface utility functions.
 * @author Kenton McHenry
 */
public class SoftwareServerRESTUtilities
{
	/**
	 * Get an icon representation of the available software.
	 * @param applications the list of applications
	 * @return the HTML for the icon representation
	 */
	public static String getApplicationStack(Vector<Application> applications)
	{
		String buffer = "";
		
		for(int i=0; i<applications.size(); i++){	
			buffer += "<div style=\"float:left\">\n";
			buffer += "<table>\n";
			buffer += "<tr><td align=\"center\">\n";
			buffer += "<a href=\"form/get?application=" + applications.get(i).alias + "\">";
			buffer += "<img src=\"image/" + applications.get(i).alias + ".jpg\" width=\"50\" border=\"0\">";
			buffer += "</a>\n";
			buffer += "</td></tr><tr><td align=\"center\">\n";
			buffer += "<font size=\"-1\">" + applications.get(i).name + "</font>\n";
			buffer += "</td></tr>\n";
			buffer += "</table>\n";
			buffer += "</div>\n";
			buffer += "\n";
		}
		
		//Add ping
		if(true){
			buffer += "<i><font size=\"-1\" color=\"#777777\"><div name=\"ping\" id=\"ping\" style=\"position:absolute;bottom:0\"></div></font></i>\n";
			buffer += "\n";
			buffer += "<script type=\"text/javascript\" src=\"https://ajax.googleapis.com/ajax/libs/jquery/1.5.2/jquery.js\"></script>\n";
			buffer += "<script type=\"text/javascript\">\n";
			buffer += "function refreshPing(){\n";
			buffer += "  $.ajax({\n";
			buffer += "    beforeSend: function(){\n";
			buffer += "      window.startTime = new Date();\n";
			buffer += "    },\n";
			buffer += "    \n";
			buffer += "    url: '/',\n";
			buffer += "    \n";
			buffer += "    success: function(){\n";
			buffer += "      window.endTime = new Date();\n";
			//buffer += "      document.getElementById('ping').innerHTML = window.endTime - window.startTime + \" ms\";\n";
			buffer += "      document.getElementById('ping').innerHTML = \"" + Runtime.getRuntime().availableProcessors() + " cores, \" + (window.endTime - window.startTime) + \" ms\";\n";
			buffer += "    }\n";
			buffer += "  });\n";
			buffer += "  \n";
			buffer += "  setTimeout('refreshPing()', 1000);\n";
			buffer += "}\n";
			buffer += "\n";
			buffer += "refreshPing();\n";
			buffer += "</script>\n";
		}
		
		return buffer;
	}
	
	/**
	 * Get a web form interface for this restful service.
	 * @param applications the list of applications
	 * @param application_tasks the list of available application tasks
	 * @param alias_map the application alias map
	 * @param POST_UPLOADS true if this form should use POST rather than GET for uploading files
	 * @param selected_application the default application
	 * @param HIDE_APPLICATIONS true if applications menu should be hidden
	 * @return the form
	 */
	public static String getForm(Vector<Application> applications, Vector<TreeSet<TaskInfo>> application_tasks, TreeMap<String,Application> alias_map, boolean POST_UPLOADS, String selected_application, boolean HIDE_APPLICATIONS)
	{
		String buffer = "";
		String format;
		Application application;
		TaskInfo task_info;
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
		
		for(int i=0; i<applications.size(); i++){
			if(i > 0) buffer += "\n";
			buffer += "  if(application == \"" + applications.get(i).alias + "\"){\n";
			count = 0;
			
			for(Iterator<TaskInfo> itr=application_tasks.get(i).iterator(); itr.hasNext();){
				task_info = itr.next();
				buffer += "    tasks.options[" + count + "] = new Option(\"" + task_info.name + "\", \"" + task_info.name + "\", " + (count == 0) + ", " + (count == 0) + ");\n";
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
		
		for(int i=0; i<applications.size(); i++){
			for(Iterator<TaskInfo> itr1=application_tasks.get(i).iterator(); itr1.hasNext();){
				task_info = itr1.next();
				if(FIRST_BLOCK) FIRST_BLOCK = false; else	buffer += "\n";
				buffer += "  if(application == \"" + applications.get(i).alias + "\" && task == \"" + task_info.name + "\"){\n";	
				buffer += "    inputs.innerHTML = \"";
				FIRST_VALUE = true;
				
				for(Iterator<String> itr2=task_info.inputs.iterator(); itr2.hasNext();){
					if(FIRST_VALUE) FIRST_VALUE = false; else buffer += ", ";
					buffer += itr2.next();
				}
				
				buffer += "\";\n";
				count = 0;
				
				for(Iterator<String> itr2=task_info.outputs.iterator(); itr2.hasNext();){
					format = itr2.next();
					buffer += "    outputs.options[" + count + "] = new Option(\"" + format + "\", \"" + format + "\", false, false);\n";
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
		buffer += "</script>\n";
		buffer += "\n";
		buffer += "<center>\n";
		
		if(!POST_UPLOADS){
			buffer += "<form name=\"converson\" action=\"\" method=\"get\">\n";
		}else{
			buffer += "<form enctype=\"multipart/form-data\" name=\"converson\" action=\"\" method=\"post\">\n";
		}
		
		buffer += "<table>\n";
		
		if(selected_application != null && HIDE_APPLICATIONS){
			application = alias_map.get(selected_application);
			
			buffer += "<tr><td>";
			if(application.icon != null) buffer += "<img src=\"../image/" + application.alias + ".jpg\" width=\"50\" border=\"0\">";
			buffer += "</td><td><h2>" + application.name + "</h2></td></tr>\n";
		}
		
		buffer += "<tr><td>";
		if(!HIDE_APPLICATIONS) buffer += "<b>Application:</b>";
		buffer += "</td>\n";
		buffer += "<td><select name=\"application\" id=\"application\" onchange=\"setTasks();\"";
		if(HIDE_APPLICATIONS) buffer += " style=\"visibility:hidden;\"";
		buffer += ">\n";
		
		for(int i=0; i<applications.size(); i++){
			buffer += "<option value=\"" + applications.get(i).alias + "\"";
			
			if(selected_application != null && selected_application.equals(applications.get(i).alias)){
				buffer += " selected";
			}

			buffer += ">" + applications.get(i) + "</option>\n";
		}
		
		buffer += "</select></td></tr>\n";
		buffer += "<tr><td><b>Task:</b></td>\n";
		buffer += "<td><select name=\"task\" id=\"task\" onchange=\"setFormats();\">\n";
				
		for(Iterator<TaskInfo> itr=application_tasks.get(0).iterator(); itr.hasNext();){
			task_info = itr.next();
			buffer += "<option value=\"" + task_info.name + "\">" + task_info.name + "</option>\n";
		}
		
		buffer += "</select></td></tr>\n";
		buffer += "<tr><td><td width=\"100\"><i><font size=\"-1\"><div id=\"inputs\">";
		FIRST_VALUE = true;

		for(Iterator<String> itr=application_tasks.get(0).first().inputs.iterator(); itr.hasNext();){
			if(FIRST_VALUE) FIRST_VALUE = false; else buffer += ", ";
			buffer += itr.next();
		}
		
		buffer += "</div></font></i></td></tr>\n";
		
		if(!POST_UPLOADS){
			buffer += "<tr><td><b>File URL:</b></td><td><input type=\"text\" name=\"file\" id=\"file\" size=\"100\"></td></tr>\n";
		}else{
			buffer += "<tr><td><b>File:</b></td><td><input type=\"file\" name=\"file\" id=\"file\" size=\"100\"></td></tr>\n";
		}
		
		buffer += "<tr><td><b>Format:</b></td>\n";
		buffer += "<td><select name=\"format\" id=\"format\">\n";
		
		for(Iterator<String> itr=application_tasks.get(0).first().outputs.iterator(); itr.hasNext();){
			format = itr.next();
			buffer += "<option value=\"" + format + "\">" + format + "</option>\n";
		}
		
		buffer += "</select></td></tr>\n";		
		buffer += "<tr><td></td><td><input type=\"submit\" value=\"Submit\"></td></tr>\n";
		buffer += "<tr><td height=\"25\"></td><td></td></tr>\n";
		buffer += "<tr><td></td><td align=\"center\"><div name=\"api\" id=\"api\"></div></td></tr>\n";
		buffer += "</table>\n";
		buffer += "</form>\n";
		buffer += "</center>\n";
		buffer += "\n";
		buffer += "<script type=\"text/javascript\">setAPICall();</script>\n";
	
		if(selected_application != null){
			buffer += "<script type=\"text/javascript\">setTasks();</script>\n";
		}
		
		return buffer;
	}
	
	/**
	 * Get a web form interface for converting via this restful service.
	 * @param applications the list of applications
	 * @param application_tasks the list of available application tasks
	 * @return the form
	 */
	public static String getConvertForm(Vector<Application> applications, Vector<TreeSet<TaskInfo>> application_tasks)
	{
		TaskInfo convert_task;
		String buffer = "";
		String format;
		int count;
		boolean FIRST_VALUE;
		
		buffer += "<script type=\"text/javascript\">\n";
		buffer += "function setFormats(){\n";
		buffer += "  var select = document.getElementById('application');\n";
		buffer += "  var application = select.options[select.selectedIndex].value;\n";
		buffer += "  var inputs = document.getElementById('inputs');\n";
		buffer += "  var outputs = document.getElementById('format');\n";
		buffer += "  \n";
		buffer += "  inputs.innerHTML = \"\";\n";
		buffer += "  outputs.options.length = 0;\n";
		buffer += "  \n";
		
		for(int i=0; i<applications.size(); i++){
			convert_task = TaskInfo.getTask(application_tasks.get(i), "convert");
			
			if(convert_task != null){
				if(i > 0) buffer += "\n";
				buffer += "  if(application == \"" + applications.get(i).alias + "\"){\n";	
				buffer += "    inputs.innerHTML = \"";
				FIRST_VALUE = true;
				
				for(Iterator<String> itr=convert_task.inputs.iterator(); itr.hasNext();){
					if(FIRST_VALUE) FIRST_VALUE = false; else buffer += ", ";
					buffer += itr.next();
				}
				
				buffer += "\";\n";
				count = 0;
				
				for(Iterator<String> itr=convert_task.outputs.iterator(); itr.hasNext();){
					format = itr.next();
					buffer += "    outputs.options[" + count + "] = new Option(\"" + format + "\", \"" + format + "\", false, false);\n";
					count++;
				}
				
				buffer += "  }\n";
			}			
		}
		
		buffer += "}\n";
		buffer += "</script>\n\n";
		
		buffer += "<form name=\"converson\" action=\"form/\" method=\"get\">\n";
		buffer += "<input type=\"hidden\" name=\"task\" value=\"convert\">\n";
		buffer += "<table>\n";
		buffer += "<tr><td><b>Application:</b></td>\n";
		buffer += "<td><select name=\"application\" id=\"application\" onchange=\"setFormats();\">\n";
		
		for(int i=0; i<applications.size(); i++){
			buffer += "<option value=\"" + applications.get(i).alias + "\">" + applications.get(i) + "</option>\n";
		}
		
		buffer += "</select></td></tr>\n";
		buffer += "<tr><td><td width=\"100\"><i><font size=\"-1\"><div id=\"inputs\">";
		
		convert_task = TaskInfo.getTask(application_tasks.get(0), "convert");
		
		if(convert_task != null){
			FIRST_VALUE = true;

			for(Iterator<String> itr=convert_task.inputs.iterator(); itr.hasNext();){
				if(FIRST_VALUE) FIRST_VALUE = false; else buffer += ", ";
				buffer += itr.next();
			}
		}
		
		buffer += "</div></font></i></td></tr>\n";
		buffer += "<tr><td><b>File:</b></td><td><input type=\"text\" name=\"file\" size=\"100\"></td></tr>\n";
		buffer += "<tr><td><b>Format:</b></td>\n";
		buffer += "<td><select name=\"format\" id=\"format\">\n";
		
		if(convert_task != null){
			for(Iterator<String> itr=convert_task.outputs.iterator(); itr.hasNext();){
				format = itr.next();
				buffer += "<option value=\"" + format + "\">" + format + "</option>\n";
			}
		}
		
		buffer += "</select></td></tr>\n";		
		buffer += "<tr><td></td><td><input type=\"submit\" value=\"Convert\"></td></tr>\n";
		buffer += "</table>\n";
		buffer += "</form>";
		
		return buffer;
	}
	
	/**
	 * Query an endpoint.
	 * @param url the URL of the endpoint
	 * @return the text obtained from the endpoint
	 */
	public static String queryEndpoint(String url)
	{
		HttpURLConnection.setFollowRedirects(false);
	  HttpURLConnection conn = null;
	  BufferedReader ins;
	  StringBuilder outs = new StringBuilder();
	  char[] buffer = new char[1024];
	  String text = null;
	  int tmpi;
			  			
	  try{
	    conn = (HttpURLConnection)new URL(url).openConnection();
	    conn.setRequestProperty("Accept", "text/plain");
	    conn.connect();
	    ins = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	    
	    do{
	      tmpi = ins.read(buffer, 0, buffer.length);
	      if(tmpi>0) outs.append(buffer, 0, tmpi);
	    }while(tmpi >= 0);
	    
	    text = outs.toString();
	    conn.disconnect();
	  }catch(Exception e){
	    //e.printStackTrace();
	  }finally{
	    if(conn != null) conn.disconnect();
	  }
	  
	  return text;
	}

	/**
	 * Remove parameters from a URL.
	 * @param url the URL of a file
	 * @return the URL of a file without parameters
	 */
	public static String removeParameters(String url)
	{
		int tmpi = url.lastIndexOf('?');
	
		if(tmpi >= 0){
			return url.substring(0, tmpi);
		}else{
			return url;
		}
	}

	/**
	 * Convert a line separated list into an HTML list of links.
	 * @param list the line separated list of items
	 * @param link the URL base
	 * @param APPEND true if list items should be appended to the end of link
	 * @param title the title of the generated HTML page (can be null)
	 * @return an HTML version of the given list
	 */
	public static String createHTMLList(String list, String link, boolean APPEND, String title)
	{
		String buffer = "";
		Scanner scanner = new Scanner(list);
		String line, endpoint;
		int tmpi;
		
		if(title != null){
			buffer += "<h1>" + title + "</h1>\n";
		}
		
		buffer += "<ul>\n";
		
		while(scanner.hasNextLine()){
			line = scanner.nextLine().trim();
			endpoint = line;
			tmpi = endpoint.indexOf('(');
			
			if(tmpi >= 0){	//Remove full application name within parenthesis of software list
				endpoint = endpoint.substring(0, tmpi).trim();
			}
			
			buffer += "<li><a href=\"" + link;
			if(APPEND) buffer += endpoint;
			buffer += "\">" + line + "</a></li>\n";
		}
		
		buffer += "</ul>\n";
		scanner.close(); // added by Edgar Black
		return buffer;
	}
	
	/**
	 * Have a Software Server listen to a RabbitMQ bus for jobs.
	 * @param softwareserver_username the Software Server user to use
	 * @param softwareserver_password the Software Server user password
	 * @param softwareserver_port the Software Server port
	 * @param softwareserver_applications the available applications
	 * @param rabbitmq_uri the rabbitmq URI, overrides below parameters
	 * @param rabbitmq_server the rabbitmq server
	 * @param rabbitmq_vhost the rabbitmq virtual host
	 * @param rabbitmq_username the rabbitmq user
	 * @param rabbitmq_password the rabbitmq user password
	 * @param WAIT true if should wait for jobs to finish before acknowledging completion
	 * @param CHECKIN_URL true if the URL to the output file should be sent back to Polygot as opposed to the actual file
	 * @param public_path the path where the converted files reside, has trailing slash "/"
	 */
	public static void rabbitMQHandler(final String softwareserver_username, final String softwareserver_password, int softwareserver_port, Vector<Application> softwareserver_applications, String rabbitmq_uri, String rabbitmq_server, String rabbitmq_vhost, String rabbitmq_username, String rabbitmq_password, final boolean WAIT, final boolean CHECKIN_URL, String public_path)
	{
		String softwareserver_authentication = "";
		final int softwareserver_port_final = softwareserver_port;
		final Vector<Application> softwareserver_applications_final = softwareserver_applications;
		final ConnectionFactory factory = new ConnectionFactory();
		
		//public_path contains a trailing slash "/".
		if(public_path.charAt(public_path.length()-1) != File.separatorChar){
			public_path += File.separator;
		}

		final String public_path_final = public_path;
		final String cache_path_final = Paths.get(public_path).getParent().toString();
      
		//Set heartbeat
		factory.setRequestedHeartbeat(180);

		if(softwareserver_username != null && softwareserver_password != null){
			Authenticator.setDefault (new Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication (softwareserver_username, softwareserver_password.toCharArray());
				}
			});	

			softwareserver_authentication = softwareserver_username + ":" + softwareserver_password + "@";
		}

		if(rabbitmq_uri != null){
			try{
				factory.setUri(rabbitmq_uri);
			}catch(Exception e) {e.printStackTrace();}
		}else{
			factory.setHost(rabbitmq_server);
			factory.setVirtualHost(rabbitmq_vhost);
	  
			if((rabbitmq_username != null) && (rabbitmq_password != null)){
				factory.setUsername(rabbitmq_username);
				factory.setPassword(rabbitmq_password);
			}
		}

		final String softwareserver_authentication_final = softwareserver_authentication;
	
		//Maintain connection to rabbitmq in a constantly running thread 
		new Thread(){
			public void run(){
				while(true){ 
					System.out.println("Connecting to RabbitMQ server and starting consumer thread");

					try{
						Connection connection = factory.newConnection();
						final Channel channel = connection.createChannel();
						final QueueingConsumer consumer = new QueueingConsumer(channel);

						//Create queues if not yet created
						for(int i=0; i<softwareserver_applications_final.size(); i++){
							channel.queueDeclare(softwareserver_applications_final.get(i).alias, true, false, false, null);
						}
			
						channel.basicQos(1);	//Fetch only one message at a time
			
						//Bind to needed queues
						for(int i=0; i<softwareserver_applications_final.size(); i++){
							channel.basicConsume(softwareserver_applications_final.get(i).alias, false, consumer);
						}
			
						final int port = softwareserver_port_final;

						//Monitor bus
						while(true){
							try{
								//Wait for next message
								final QueueingConsumer.Delivery delivery = consumer.nextDelivery();
		  	    
								//Process each message in a new thread
								new Thread(){
									public void run(){
										ObjectMapper mapper = new ObjectMapper();
										String api_call, result, checkin_call;
										String api_call_without_credentials;

										try{
											JsonNode message = mapper.readValue(delivery.getBody(), JsonNode.class);
											String polyglot_ip = message.get("polyglot_ip").asText();
											String polyglot_auth = message.get("polyglot_auth").asText();
											int job_id = Integer.parseInt(message.get("job_id").asText());
											int step = Integer.parseInt(message.get("step").asText());
											String input = message.get("input").asText();
											String input_without_credentials = SoftwareServerUtility.removeCredentials(input);
											String application = message.get("application").asText();
											String output_format = message.get("output_format").asText();
											
											System.out.println("[" + SoftwareServerUtility.getTimeStamp() + "] [rabbitm] [" + job_id + "]: Consuming job-" + job_id + ", " + input_without_credentials + "->" + output_format + " via " + application);
		  	    	
											//Execute job using Software Server REST interface (leverage implementation)
											if(step > 0){
												api_call = "http://" + softwareserver_authentication_final + "localhost:" + port + "/software/" + application + "/convert/" + output_format + "/" + URLEncoder.encode(SoftwareServerUtility.addAuthentication(input, polyglot_auth), "UTF-8");
											}else{
												api_call = "http://" + softwareserver_authentication_final + "localhost:" + port + "/software/" + application + "/convert/" + output_format + "/" + URLEncoder.encode(input, "UTF-8");
											}
											
											api_call_without_credentials = "http://" + softwareserver_authentication_final + "localhost:" + port + "/software/" + application + "/convert/" + output_format + "/" + URLEncoder.encode(input_without_credentials, "UTF-8");
											System.out.println("[" + SoftwareServerUtility.getTimeStamp() + "] [rabbitm] [" + job_id + "]: API call, " + api_call_without_credentials);

											result = SoftwareServerUtility.readURL(api_call, "text/plain");
											//result = SoftwareServerUtility.addAuthentication(result, softwareserver_authentication_final);	//Make sure restlet doesn't already use guest!

											while(WAIT && !SoftwareServerUtility.existsURL(result)){
												Utility.pause(1000);
											}
											
											System.out.println("[" + SoftwareServerUtility.getTimeStamp() + "] [rabbitm] [" + job_id + "]: API call complete, result at " + result);

											if(CHECKIN_URL){		//Checkin a URL to the output file hosted on the Software Server
												//System.out.println("Setting Polylgot authentication for " + polyglot_ip + ": " + polyglot_auth);
												SoftwareServerUtility.setDefaultAuthentication(polyglot_auth);
												checkin_call = "http://" + polyglot_ip + ":8184/checkin/" + job_id + "/" + Utility.urlEncode(result);
												System.out.println("[" + SoftwareServerUtility.getTimeStamp() + "] [rabbitm] [" + job_id + "]: Checking in result with Polyglot, " + checkin_call);

												if(SoftwareServerUtility.readURL(checkin_call).equals("ok")){
													channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
													System.out.println("[" + SoftwareServerUtility.getTimeStamp() + "] [rabbitm] [" + job_id + "]: Checkin acknowledged!");
												}else{						//Wait a bit and try one more time
													Utility.pause(5000);
													System.out.println("[" + SoftwareServerUtility.getTimeStamp() + "] [rabbitm] [" + job_id + "]: Second attempt at checkin result with Polyglot, " + checkin_call);

													if(SoftwareServerUtility.readURL(checkin_call).equals("ok")){
														channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
														System.out.println("[" + SoftwareServerUtility.getTimeStamp() + "] [rabbitm] [" + job_id + "]: Second checkin acknowledged!");
													}else{
														channel.basicReject(delivery.getEnvelope().getDeliveryTag(), false);
														System.out.println("[" + SoftwareServerUtility.getTimeStamp() + "] [rabbitm] [" + job_id + "]: Rejecting job after second checkin un-acknowledged!");
													}
												}
                                              
												//TODO: possible to delete files under cache folder, currently CHECKIN_URL is always set to false.
											}else{							//Checkin the actual output file
												//The filename to post: <public_path>/<session>_<filename>. "result" is http://.../file/<session>_<filename>, so strip out the path (after last '/').
												String filename = result.substring(result.lastIndexOf('/')+1);
												String absolute_filename = public_path_final + filename;
												String log_file = Utility.readURL("http://localhost:8182/file/" + filename + ".log");
												
												//System.out.println("Setting Polylgot authentication for " + polyglot_ip + ": " + polyglot_auth);
												SoftwareServerUtility.setDefaultAuthentication(polyglot_auth);
												checkin_call = "http://" + polyglot_ip + ":8184/checkin/" + job_id + "/" + URLEncoder.encode(log_file, "UTF-8");
												//System.out.println("[" + SoftwareServerUtility.getTimeStamp() + "] [rabbitm] [" + job_id + "]: Checking in result with Polyglot, " + checkin_call);
												System.out.println("[" + SoftwareServerUtility.getTimeStamp() + "] [rabbitm] [" + job_id + "]: Checking in result with Polyglot, " + checkin_call.substring(0, checkin_call.lastIndexOf('/')+1) + "...");
												
												//If a directory was created zip it up to send it back
												if(Utility.isDirectory(absolute_filename)){
													SoftwareServerUtility.zip(absolute_filename + ".checkin.zip", absolute_filename);
													absolute_filename += ".checkin.zip";
												}

												if(SoftwareServerUtility.postFile(checkin_call, absolute_filename, null, polyglot_auth).equals("ok")){
													channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
													System.out.println("[" + SoftwareServerUtility.getTimeStamp() + "] [rabbitm] [" + job_id + "]: Checkin acknowledged!");
												}else{						//Wait a bit and try one more time
													Utility.pause(5000);
													System.out.println("[" + SoftwareServerUtility.getTimeStamp() + "] [rabbitm] [" + job_id + "]: Second attempt at checkin result with Polyglot, " + checkin_call);

													if(SoftwareServerUtility.postFile(checkin_call, absolute_filename, null, polyglot_auth).equals("ok")){
														channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
														System.out.println("[" + SoftwareServerUtility.getTimeStamp() + "] [rabbitm] [" + job_id + "]: Second checkin acknowledged!");
													}else{
														channel.basicReject(delivery.getEnvelope().getDeliveryTag(), false);
														System.out.println("[" + SoftwareServerUtility.getTimeStamp() + "] [rabbitm] [" + job_id + "]: Rejecting job after second checkin un-acknowledged!");
													}
												}
												
												String result_filename = Utility.getFilename(result);
												int index = result_filename.indexOf('_');
                                              
												if(index != -1) {
													int session = Integer.parseInt(result_filename.substring(0, index));
													
                                                    // delete files under cache folder.
													final String tmp_sessionlog = ".session_" + session + ".log";
													SoftwareServerUtility.deleteStoreFiles(cache_path_final, session, tmp_sessionlog);
													
                                                    // delete files under public folder.
													final String public_sessionlog = result_filename + ".log";
													SoftwareServerUtility.deleteStoreFiles(public_path_final, session, public_sessionlog);
												}
											}
										}catch(Exception e) {e.printStackTrace();}
									}
								}.start();
							}catch(Exception e){
								e.printStackTrace();
								break;
							}
						}
					}catch(Exception e){
						e.printStackTrace();
						Utility.pause(1000);		//Wait a bit before reconnecting to rabbitmq
					}
				}
			}
		}.start();
	}


	/**
	 * Create a thread to send registration messages to the RabbitMQ SS-registration queue.
	 * @param rabbitmq_uri the rabbitmq URI
	 * @param regis_queue_name the name of the SS registration queue in RabbitMQ
	 * @param regis_msg_ttl the TTL (in milliseconds) for the registration msgs
	 * @param msg the string to be published into the queue
	 */
	public static void registration(String rabbitmq_uri, String regis_queue_name, int regis_msg_ttl, String msg)
	{
		//System.out.println("Registration: regis_queue_name: '" + regis_queue_name + "', regis_msg_ttl: " + regis_msg_ttl/1000 + " seconds.");
		final String QUEUE_NAME = regis_queue_name;
		final ConnectionFactory factory = new ConnectionFactory();
		factory.setRequestedHeartbeat(180);

		if(rabbitmq_uri != null){
			try{
				factory.setUri(rabbitmq_uri);
			}catch(Exception e) {e.printStackTrace(); System.exit(1);}
		}else{
			System.out.println("Software Server now requires a defined rabbitmq_uri.");
			System.exit(1);
		}

		final AMQP.BasicProperties properties = new AMQP.BasicProperties();
		final int msgTTL = regis_msg_ttl;
		final String msgTTLStr = String.valueOf(regis_msg_ttl);
		properties.setExpiration(msgTTLStr);
		final String sentmsg = msg;

		//Maintain connection to rabbitmq in a constantly running thread 
		new Thread(){
			public void run(){
				while(true){ 
					System.out.println("Connecting to RabbitMQ server and starting registration thread");
					//System.out.println("Registration msg to send: " + sentmsg);

					Connection connection = null;
					Channel channel = null;
					try{
						connection = factory.newConnection();
						channel = connection.createChannel();
            
						//Create the registration queue if not yet created
						channel.queueDeclare(QUEUE_NAME, true, false, false, null);
						channel.basicQos(1);    //Fetch only one message at a time

						//Send the registration message
						while(true){
							try{
								channel.basicPublish("", QUEUE_NAME, properties, sentmsg.getBytes());

								//Post registration messages every TTL seconds.
								Utility.pause(msgTTL);
							}catch(Exception e){
								e.printStackTrace();
								break;  //So it goes out and retries the RabbitMQ connection, otherwise stays in this error condition and exception msgs flood.
							}
						}
					}catch(Exception e1) {e1.printStackTrace();}
					
					try{
						if(null != channel) channel.close();
						if(null != connection) connection.close();
					}catch(Exception e2) {e2.printStackTrace();}
						
					System.out.println("SS having issues, wait for a while before retrying...");
					Utility.pause(10000); // Pause 10 secs before retrying, otherwise logs flood.
				}
			}
		}.start();
	}
}
