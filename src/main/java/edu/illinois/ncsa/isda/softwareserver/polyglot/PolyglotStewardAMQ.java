package edu.illinois.ncsa.isda.softwareserver.polyglot;
import edu.illinois.ncsa.isda.softwareserver.*;
import edu.illinois.ncsa.isda.softwareserver.SoftwareServerAuxiliary.Application;
import edu.illinois.ncsa.isda.softwareserver.SoftwareServerAuxiliary.Data;
import edu.illinois.ncsa.isda.softwareserver.polyglot.PolyglotAuxiliary.*;
import kgm.utility.*;
import java.io.*;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.text.*;
import org.apache.http.*;
import org.apache.http.impl.client.*;
import org.apache.http.util.*;
import org.apache.http.client.methods.*;
import org.json.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.mongodb.*;
import com.mongodb.util.*;
import com.rabbitmq.client.*;

/**
 * A class that coordinates the use of several software servers to perform file format conversions.
 * @author Kenton McHenry
 */
public class PolyglotStewardAMQ extends Polyglot implements Runnable
{
	private static String polyglot_username = null;
	private static String polyglot_password = null;
	private static String rabbitmq_uri = null;
	private static String rabbitmq_server = null;
	private static String rabbitmq_vhost = "/";
	private static String rabbitmq_username = null;
	private static String rabbitmq_password = null;
	private static String softwareserver_authentication = "";
	private int heartbeat;
	private TreeMap<String,Long> software_servers = new TreeMap<String,Long>();
	private IOGraph<String,SoftwareServerApplication> iograph = new IOGraph<String,SoftwareServerApplication>();
	private MongoClient mongoClient;
	private DB db;
	private DBCollection collection;
	private ConnectionFactory factory;
	private Connection connection;
	private Channel channel;
	private String polyglot_ip;
	private AtomicInteger job_counter = new AtomicInteger();

	/**
	 * Class constructor.
	 */
	public PolyglotStewardAMQ()
	{
		this(true);
	}
	
	/**
	 * Class constructor.
	 * @param START start immediately
	 */
	public PolyglotStewardAMQ(boolean START)
	{
		loadConfiguration("PolyglotStewardAMQ.conf");
		
		//Connect to MongoDB
		try{
			Properties properties = new Properties();
			properties.load(new FileInputStream("mongo.properties"));
			mongoClient = new MongoClient(properties.getProperty("server"));
			//db = mongoClient.getDB(properties.getProperty("database"));
			db = mongoClient.getDB("polyglot");		//Use this database as mongo.properties file will set this for DAP level
			collection = db.getCollection("steward");
		}catch(Exception e) {e.printStackTrace();}
		
		//Connect to RabbitMQ
	  factory = new ConnectionFactory();

		if(rabbitmq_uri != null){
			try{
				factory.setUri(rabbitmq_uri);
				rabbitmq_server = factory.getHost();
				rabbitmq_vhost = factory.getVirtualHost();
				rabbitmq_username = factory.getUsername();
				rabbitmq_password = factory.getPassword();
			}catch(Exception e) {e.printStackTrace();}
		}else{
    	factory.setHost(rabbitmq_server);
			factory.setVirtualHost(rabbitmq_vhost);
	  
    	if((rabbitmq_username != null) && (rabbitmq_password != null)){
	  		factory.setUsername(rabbitmq_username);
	  		factory.setPassword(rabbitmq_password);
	  	}
		}

		try {
			connection = factory.newConnection();
			channel = connection.createChannel();
		} catch(Exception e) {
			e.printStackTrace();
		}

		polyglot_ip = Utility.getLocalHostIP();
    
		if(START) new Thread(this).start();		//Start main thread
	}
	
	/**
	 * Initialize based on parameters within the given configuration file.
	 * @param filename the file name of the configuration file
	 */
	public void loadConfiguration(String filename)
	{
	  try{
	    BufferedReader ins = new BufferedReader(new FileReader(filename));
	    String line, key, value;
	    int tmpi;
	    
	    while((line=ins.readLine()) != null){
	      if(line.contains("=")){
	        key = line.substring(0, line.indexOf('='));
	        value = line.substring(line.indexOf('=')+1);
	        
	        if(key.charAt(0) != '#'){
	        	if(key.equals("RabbitMQURI")){
	        		rabbitmq_uri = value;
	        	}else if(key.equals("RabbitMQServer")){
	        		rabbitmq_server = value;
	        	}else if(key.equals("RabbitMQVirtualHost")){
	        		rabbitmq_vhost = value;
	        	}else if(key.equals("RabbitMQUsername")){
	        		rabbitmq_username = value;
	        	}else if(key.equals("RabbitMQPassword")){
	        		rabbitmq_password = value;
	        	}else if(key.equals("SoftwareServerAuthentication")){
							softwareserver_authentication = value + "@";
	  	        final String username = value.substring(0, value.indexOf(':')).trim();
	  	        final String password = value.substring(value.indexOf(':')+1).trim();
							System.out.println("Software Server authentication: " + username);
							
							Authenticator.setDefault (new Authenticator() {
								protected PasswordAuthentication getPasswordAuthentication() {
									return new PasswordAuthentication (username, password.toCharArray());
								}
							});	
						}else if(key.equals("Heartbeat")){
	        		heartbeat = Integer.valueOf(value);
	        	}
	        }
	      }
	    }
	    
	    ins.close();
	  }catch(Exception e) {}
	}

	/**
	 * Start the main thread (as a separate function so can be called externally, e.g. if jobs should be purged first)
	 */
	public void start()
	{
		new Thread(this).start();
	}
	
	/**
	 * Get the software available.
	 * @return the list of software
	 */
	public TreeSet<String> getSoftware()
	{
		return iograph.getUniqueEdgeStrings();
	}
	
	/**
	 * Get the outputs available.
	 * @return the list of outputs
	 */
	@Override
	public TreeSet<String> getOutputs()
	{
		return iograph.getRangeStrings();
	}

	/**
	 * Get the outputs available for the given input type.
	 * @param input the input type
	 * @return the list of outputs
	 */
	@Override
	public TreeSet<String> getOutputs(String input)
	{
		return iograph.getRangeStrings(input);
	}

	/**
	 * Get the common outputs available for the given input types.
	 * @param inputs the input types
	 * @return the list of outputs
	 */
	@Override
	public TreeSet<String> getOutputs(TreeSet<String> inputs)
	{
		return iograph.getRangeIntersectionStrings(inputs);
	}
	
	/**
	 * Get the inputs available.
	 * @return the list of inputs
	 */
	public TreeSet<String> getInputs()
	{
		return iograph.getDomainStrings();
	}
	
	/**
	 * Get the inputs available for the given output type.
	 * @param output the output type
	 * @return the list of inputs
	 */
	public TreeSet<String> getInputs(String output)
	{
		return iograph.getDomainStrings(output);
	}

	/**
	 * Get the available inputs, outputs, and conversions.
	 * @return an IOGraph containing the information as strings
	 */
	@Override
	public IOGraph<String,String> getInputOutputGraph()
	{
		return iograph.getIOGraphStrings();
	}

	/**
	 * Convert a files format.
	 * @param input the absolute name of the input file
	 * @param output_path the output path
	 * @param output_format the output format
	 * @return the output file name (if changed, null otherwise)
	 */
	@Override
	public String convert(String input, String output_path, String output_format)
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode request, task;
		ArrayNode path;
		int job_id = job_counter.incrementAndGet();
		String input_format;
		Vector<Conversion<String,SoftwareServerApplication>> conversions;
		boolean MULTIPLE_EXTENSIONS = true;
			
		System.out.println("[" + SoftwareServerUtility.getTimeStamp() + "] [steward]: Searching for conversion paths for " + input + "->" + output_format);
		
		//Get conversion path, give multiple extensions precedence (i.e. if a script exists that supports such a thing it should be run)
		input_format = Utility.getFilenameExtension(Utility.getFilename(input), true).toLowerCase();
		conversions = iograph.getShortestConversionPath(input_format, output_format, false);

		if(conversions == null){
			MULTIPLE_EXTENSIONS = false;
			input_format = Utility.getFilenameExtension(input).toLowerCase();
			conversions = iograph.getShortestConversionPath(input_format, output_format, false);
		}

		//Add to mongo
		if(conversions != null){
			String ts_str = SoftwareServerUtility.getTimeStamp();
			System.out.println("[" + ts_str + "] [steward] [" + job_id + "]: Found path for " + input + "->" + output_format + ", submitting as job-" + job_id);

			request = mapper.createObjectNode();
			request.put("job_id", job_id);
			request.put("timestamp", ts_str);
			request.put("multiple_extensions", MULTIPLE_EXTENSIONS);
			request.put("input", input);
			request.put("output_path", output_path);
			request.put("output_format", output_format);
			request.put("step", -1);
			request.put("step_status", 1);	//0 = waiting for something, 1 = ready to move on
			
			path = mapper.createArrayNode();
			
			for(int i=0; i<conversions.size(); i++){
				//System.out.print(conversions.get(i).input + " -> " + conversions.get(i).edge + " -> " + conversions.get(i).output);

				task = mapper.createObjectNode();
				task.put("input", conversions.get(i).input);
				task.put("application", conversions.get(i).edge.name);
				task.put("output", conversions.get(i).output);
				task.put("result", "");
				path.add(task);
			}

			request.put("path", path);
			collection.insert((DBObject)JSON.parse(request.toString()));
			
			//Create a place holder file, URL is empty
			Utility.save(output_path + "/" + job_id + "_" + Utility.getFilenameName(input) + "." + output_format + ".url", "[InternetShortcut]\nURL=");
		
			return job_id + "_" + Utility.getFilenameName(input, MULTIPLE_EXTENSIONS) + "." + output_format;
		}else{
			return "404";
		}
	}
	
	/**
	 * Get a list of connected software servers.
	 * @return a list of connected software servers
	 */
	@Override
	public Vector<String> getServers()
	{
		return new Vector<String>(software_servers.keySet());
	}

	/**
	 * Get a string only version of this IOGraph encoded with host machines.
	 * @return a string version of this IOGraph
	 */
	@Override
	public IOGraph<String,String> getDistributedInputOutputGraph()
	{
  	IOGraph<String,String> iograph_strings = new IOGraph<String,String>();
  	Vector<String> vertices = iograph.getVertices();
  	Vector<Vector<SoftwareServerApplication>> edges = iograph.getEdges();
  	Vector<Vector<Integer>> adjacency_list = iograph.getAdjacencyList();
  	
  	for(int i=0; i<vertices.size(); i++){
  		iograph_strings.addVertex(vertices.get(i));
  	}
  	
  	for(int i=0; i<adjacency_list.size(); i++){
  		for(int j=0; j<adjacency_list.get(i).size(); j++){
  			iograph_strings.addEdge(vertices.get(i), vertices.get(adjacency_list.get(i).get(j)), edges.get(i).get(j).toString());
  		}
  	}
  	
  	return iograph_strings;
	}

  /**
	 * Close anything that needs to be closed.
	 */
	@Override
	public void close() {}
	
	/**
	 * Set job counter based on a previous execution.
	 * @param path the artifact path to examine for previous job IDs
	 */
	public void setJobCounter(String path)
	{
		//Examine tasks remaining in database
		DBCursor cursor = collection.find();
		DBObject document;
		int last_job_id = 0;
		int job_id;
		
		try{
			while(cursor.hasNext()){
				document = cursor.next();
				job_id = Integer.parseInt(document.get("job_id").toString());
				if(job_id > last_job_id) last_job_id = job_id;
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			cursor.close();
		}
		
		//Examine artifacts in specified folder
		job_id = SoftwareServer.getLastSession(path);
		if(job_id > last_job_id) last_job_id = job_id;
		
		//Set the job counter
		job_counter = new AtomicInteger(last_job_id);		
	}

	/**
	 * Set the username and password to use when others communicate back to polyglot.
	 */
	public void setAuthentication(String username, String password)
	{
		polyglot_username = username;
		polyglot_password = password;
	}

	/**
	 * Discover Software Servers consuming on the given RabbitMQ bus (adds them to I/O-graph).
	 */
	public void discoveryAMQ()
	{
		JsonNode consumers = queryEndpoint("http://" + rabbitmq_username + ":" + rabbitmq_password + "@" + rabbitmq_server + ":15672/api/consumers/" + Utility.urlEncode(rabbitmq_vhost));
		Set<String> hosts = new TreeSet<String>();
		JsonNode queue;
		JsonNode applications;
		long startup_time;
		boolean UPDATED = false;
	
		for(int i=0; i<consumers.size(); i++) {
			String host = consumers.get(i).get("channel_details").get("peer_host").asText();
			
			//Make sure we use the public IP for local software servers 
			if(host.equals("127.0.0.1") || host.equals("localhost")){
				host = Utility.getLocalHostIP();
			}

			hosts.add(host);
		}

		for (String host: hosts) {
			try{
				startup_time = Long.parseLong(SoftwareServerRESTUtilities.queryEndpoint("http://" + softwareserver_authentication + host + ":8182/alive"));

				synchronized(software_servers){
					if(!software_servers.containsKey(host) || software_servers.get(host) != startup_time){
						System.out.println("[" + SoftwareServerUtility.getTimeStamp() + "] [steward]: Adding " + host);
						UPDATED = true;

						//Get applications on server
						applications = queryEndpoint("http://" + softwareserver_authentication + host + ":8182/applications");
						iograph.addGraph(new IOGraph<String,SoftwareServerApplication>(applications, host));
						software_servers.put(host, startup_time);
					}
				}
			}catch(NumberFormatException e) {
				//e.printStackTrace();
			}
		}
		
		if(UPDATED) iograph.save("tmp/iograph.txt");
	}
	
	/**
	 * Checks on Software Servers to see if they are still alive (removes them from I/O-graph).
	 */
	public void heartbeat()
	{
		Set<String> hosts = null;
		String host = null;
		long startup_time;
		boolean UPDATED = false;
		
		synchronized(software_servers){
			hosts = software_servers.keySet();
		}
		
		for(Iterator<String> itr=hosts.iterator(); itr.hasNext();){
	  	try{
	  		host = itr.next();
	    	startup_time = Long.parseLong(SoftwareServerRESTUtilities.queryEndpoint("http://" + softwareserver_authentication + host + ":8182/alive"));
	  	}catch(NumberFormatException e){
	  		synchronized(software_servers){
		  		System.out.println("[" + SoftwareServerUtility.getTimeStamp() + "] [steward]: Dropping " + host);
		  		UPDATED = true;
		  		
	  			iograph.removeEdges(host);
	  			software_servers.remove(host);
		  	}
	  	}
		}
		
		if(UPDATED) iograph.save("tmp/iograph.txt");
	}

	/**
	 * Process jobs pending in mongo.
	 */
	public void process_jobs()
	{
		DBCursor cursor = collection.find();
		DBObject document;
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode message;
		int job_id, step, step_status;
		String polyglot_auth = "", input, application, output_format, output_path;
		boolean MULTIPLE_EXTENSIONS;	//Was a path found for an input with multiple extensions
		
                if(polyglot_username != null && polyglot_password != null) {
                    polyglot_auth = polyglot_username + ":" + polyglot_password;
                }

		try{
			while(cursor.hasNext()){
				document = cursor.next();
				//polyglot_ip = InetAddress.getLocalHost().getHostAddress();
				job_id = Integer.parseInt(document.get("job_id").toString());
				MULTIPLE_EXTENSIONS = Boolean.parseBoolean(document.get("multiple_extensions").toString());
				step = Integer.parseInt(document.get("step").toString());
				step_status = Integer.parseInt(document.get("step_status").toString());
				int steps = ((BasicDBList)document.get("path")).size();

				//Move the job along
				if(step_status == 1){
					if(step == -1){
						input = document.get("input").toString();
					}else{
						input = ((BasicDBObject)((BasicDBList)document.get("path")).get(step)).get("result").toString();
					}
					
					step++;
					
					if(step < steps){
						application = ((BasicDBObject)((BasicDBList)document.get("path")).get(step)).get("application").toString();
						output_format = ((BasicDBObject)((BasicDBList)document.get("path")).get(step)).get("output").toString();
						
						System.out.println("[" + SoftwareServerUtility.getTimeStamp() + "] [steward] [" + job_id + "]: Submitting job-" + job_id + "'s next step, " + Utility.getFilename(input) + "->" + output_format + " via " + application);
						
						//Build message
						message = mapper.createObjectNode();
						message.put("polyglot_ip", polyglot_ip);
						message.put("polyglot_auth", polyglot_auth);
						message.put("job_id", job_id);
						message.put("input", input);
						message.put("application", application);
						message.put("output_format", output_format);
						
						//Submit the next step for execution
				    channel.queueDeclare(application, true, false, false, null);
				    channel.basicPublish("", application, MessageProperties.PERSISTENT_TEXT_PLAIN, message.toString().getBytes());
						
						//Update the job entry
						document.put("step", step);
						document.put("step_status", 0);
						collection.update(new BasicDBObject().append("job_id", job_id), document);
					}else{
						output_path = document.get("output_path").toString();
						output_format = document.get("output_format").toString();
						Utility.save(output_path + "/" + job_id + "_" + Utility.getFilenameName(document.get("input").toString(), MULTIPLE_EXTENSIONS) + "." + output_format + ".url", "[InternetShortcut]\nURL=" + URLDecoder.decode(input, "UTF-8"));
						collection.remove(document);
						System.out.println("[" + SoftwareServerUtility.getTimeStamp() + "] [steward] [" + job_id + "]: Job-" + job_id + " completed, result hosted at " + URLDecoder.decode(input, "UTF-8"));
					}
				}
			}
		}catch(com.rabbitmq.client.AlreadyClosedException e){
			// If the connection is closed, re-create the connection and channel. There seems no need to call connection.close() as it's already closed.
			// Other types of exceptions are ConsumerCancelledException, JsonRpcException, MalformedFrameException, MissedHeartbeatException, PossibleAuthenticationFailureException, ProtocolVersionMismatchException, TopologyRecoveryException. This AlreadyClosedException occured multiple types and haven't seen other types, so handle this for now. Can add handling of other exceptions while we see them.
			try {
				connection = factory.newConnection();
				channel = connection.createChannel();
			} catch(Exception e1) {
				e1.printStackTrace();
			}
			System.out.println("[" + SoftwareServerUtility.getTimeStamp() + "] [steward]: RabbitMQ connection was closed, submitting job failed. Re-created the connection.");
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			cursor.close();
		}
	}
	
	/**
	 * Update status of job task (TODO: needs to do some checks to ensure validity of result!)
	 * @param host the host checking in (should be a Software Server)
	 * @param job_id the job id being worked on
	 * @return the response to the Software Server
	 */
	public String checkin(String host, int job_id, String result)
	{
		BasicDBObject query = new BasicDBObject().append("job_id", job_id);
		DBObject document = collection.findOne(query);
		int step = Integer.parseInt(document.get("step").toString());
		
		//Update the job entry
		((BasicDBObject)((BasicDBList)document.get("path")).get(step)).put("result", result);
		document.put("step_status", 1);
		collection.update(query, document);
		
		return "ok";
	}

	/**
	 * The main thread.
	 */
	@Override
	public void run()
	{
		//Start discovery thread to add new servers
  	new Thread(){
  		public void run(){
  			while(true){
					try{	//If rabbitmq goes down it will throw an excpetion
  					discoveryAMQ();
					}catch(Exception e) {e.printStackTrace();}

  				Utility.pause(30000);
  			}
  		}
  	}.start();
  	
  	//Start job processing thread
  	new Thread(){
  		public void run(){
  			while(true){
  				process_jobs();
				// It was 100 -- 0.1 s, too frequent to cause high CPU usage. Changed to 3000 - 3 seconds.
  				Utility.pause(3000);
  			}
  		}
  	}.start();
  	
  	//Start heartbeat thread to remove dead servers
  	new Thread(){
  		public void run(){
	  		while(true){
	  			heartbeat();
	  			Utility.pause(heartbeat);
  			}
  		}
  	}.start();
	}
	
	/**
	 * Query an endpoint.
	 * @param url the URL of the endpoint
	 * @return the json obtained from the endpoint
	 */
	public static JsonNode queryEndpoint(String url)
	{
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpGet httpget = new HttpGet(url);
		CloseableHttpResponse response = null;
		HttpEntity entity = null;
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootnode = null;
		
		try{
			response = httpclient.execute(httpget);
			entity = response.getEntity();
			rootnode = mapper.readValue(EntityUtils.toString(entity), JsonNode.class);
		}catch(Exception e){
			e.printStackTrace();
		}finally{
	    try{
	    	if(entity != null) entity.getContent().close();
				if(response != null) response.close();
			}catch(IOException e){e.printStackTrace();}
		}
		
		return rootnode;
	}
	
	/**
	 * Purge current and previous jobs.
	 */
	public static void purgeJobs()
	{
		Process p;
		BufferedReader reader;
		String command, line;
	
		System.out.println("[" + SoftwareServerUtility.getTimeStamp() + "] [steward]: Purging mongo & rabbitmq ...");
	
		//Purge mongo
		command = "mongo polyglot --eval \"db.steward.drop()\"";
		System.out.println("> " + command);
		Utility.executeAndWait(command, -1, true, true);
	
		System.out.println();
	
		//Purge rabbitmq queue
		if(!System.getProperty("os.name").startsWith("Windows")){
			command = "curl -i -u " + rabbitmq_username + ":" + rabbitmq_password + " -H \"content-type:application/json\" -XDELETE http://" + rabbitmq_server + ":15672/api/queues/" + Utility.urlEncode(rabbitmq_vhost) + "/ImageMagick/contents";
			System.out.println("> " + command);
			Utility.executeAndWait(command, -1, true, true);
		}else{
			System.out.println("Warning: not purging rabbitmq!");		//TODO: need to do this without a curl call!
		}
	}

	/**
	 * Start the Polyglot Steward.
	 * @param args command line arguments
	 */
	public static void main(String[] args)
	{
		PolyglotStewardAMQ polyglot = new PolyglotStewardAMQ();
	}
}
