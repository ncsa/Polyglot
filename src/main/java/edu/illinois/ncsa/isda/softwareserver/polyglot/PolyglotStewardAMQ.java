package edu.illinois.ncsa.isda.softwareserver.polyglot;
import edu.illinois.ncsa.isda.softwareserver.*;
import edu.illinois.ncsa.isda.softwareserver.SoftwareServerAuxiliary.Application;
import edu.illinois.ncsa.isda.softwareserver.SoftwareServerAuxiliary.Data;
import edu.illinois.ncsa.isda.softwareserver.polyglot.PolyglotAuxiliary.*;
import kgm.utility.*;
import java.io.*;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
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
	private String rabbitmq_server = null;
	private String rabbitmq_username = null;
	private String rabbitmq_password = null;
	private TreeMap<String,Long> software_servers = new TreeMap<String,Long>();
	private IOGraph<String,SoftwareServerApplication> iograph = new IOGraph<String,SoftwareServerApplication>();
	private MongoClient mongoClient;
	private DB db;
	private DBCollection collection;
	private ConnectionFactory factory;
	private AtomicInteger job_counter = new AtomicInteger();	//TODO: need to initilize off largest job_id in mongo
	
	/**
	 * Class constructor.
	 */
	public PolyglotStewardAMQ()
	{
		loadConfiguration("PolyglotStewardAMQ.conf");
		
		//Connect to MongoDB
		try{
			mongoClient = new MongoClient("localhost");
			db = mongoClient.getDB("polyglot");
			collection = db.getCollection("steward");
		}catch(Exception e) {e.printStackTrace();}
		
		//Connect to RabbitMQ
	  factory = new ConnectionFactory();
    factory.setHost(rabbitmq_server);
	  
    if((rabbitmq_username != null) && (rabbitmq_password != null)){
	  	factory.setUsername(rabbitmq_username);
	  	factory.setPassword(rabbitmq_password);
	  }
    
    //Start main thread
		new Thread(this).start();
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
	        	if(key.equals("RabbitMQServer")){
	        		rabbitmq_server = value;
	        	}else if(key.equals("RabbitMQUsername")){
	        		rabbitmq_username = value;
	        	}else if(key.equals("RabbitMQPassword")){
	        		rabbitmq_password = value;
	        	}
	        }
	      }
	    }
	    
	    ins.close();
	  }catch(Exception e) {}
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
				
		//Get conversion path
		String input_format = Utility.getFilenameExtension(input);
		Vector<Conversion<String,SoftwareServerApplication>> conversions = iograph.getShortestConversionPath(input_format, output_format, false);

		//Add to mongo
		if(conversions != null){
			request = mapper.createObjectNode();
			request.put("job_id", job_id);
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
		}
		
		return job_id + "_" + Utility.getFilenameName(input) + "." + output_format;
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
	 * Discover Software Servers consuming on the given RabbitMQ bus (adds them to I/O-graph).
	 */
	public void discoveryAMQ()
	{
		JsonNode queues = queryEndpoint("http://" + rabbitmq_username + ":" + rabbitmq_password + "@" + rabbitmq_server + ":15672/api/queues/");
		JsonNode queue;
		JsonNode applications;
		String name, host;
		long startup_time;
		boolean UPDATED = false;
	
		for(int i=0; i<queues.size(); i++) {
			name = queues.get(i).get("name").asText();
	    queue = queryEndpoint("http://" + rabbitmq_username + ":" + rabbitmq_password + "@" + rabbitmq_server + ":15672/api/queues/%2F/" + name);
	    	    
	    for(int j=0; j<queue.get("consumer_details").size(); j++){
	    	host = queue.get("consumer_details").get(j).get("channel_details").get("peer_host").asText();
	    	
	    	try{
		    	startup_time = Long.parseLong(SoftwareServerRESTUtilities.queryEndpoint("http://" + host + ":8182/alive"));
			    //System.out.println(name + ", " + host + ", " + startup_time);

		    	synchronized(software_servers){
			    	if(!software_servers.containsKey(host) || software_servers.get(host) != startup_time){
			    		System.out.println("[Steward]: Adding " + host);
			    		UPDATED = true;

			    		//Get applications on server
			    		applications = queryEndpoint("http://" + host + ":8182/applications");
			    		iograph.addGraph(new IOGraph<String,SoftwareServerApplication>(applications, host));
			    		software_servers.put(host, startup_time);
			    	}
		    	}
	    	}catch(NumberFormatException e) {}
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
		
		synchronized(software_servers){
			hosts = software_servers.keySet();
		}
		
		for(Iterator<String> itr=hosts.iterator(); itr.hasNext();){
	  	try{
	  		host = itr.next();
	    	startup_time = Long.parseLong(SoftwareServerRESTUtilities.queryEndpoint("http://" + host + ":8182/alive"));
	  	}catch(NumberFormatException e){
	  		synchronized(software_servers){
	  			software_servers.remove(host);
	  		}
	  		
	  		System.out.println("[Steward]: Dropping " + host);
	  		//TODO: prune the I/O-graph
	  	}
		}
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
		String polyglot_ip, input, application, output_format, output_path;
		
		try{
			while(cursor.hasNext()){
				document = cursor.next();
				polyglot_ip = InetAddress.getLocalHost().getHostAddress();
				job_id = Integer.parseInt(document.get("job_id").toString());
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
						
						System.out.println("[Steward]: submitting Job-" + job_id + "'s next step, " + application + "->" + output_format);
						
						//Build message
						message = mapper.createObjectNode();
						message.put("polyglot_ip", polyglot_ip);
						message.put("job_id", job_id);
						message.put("input", input);
						message.put("application", application);
						message.put("output_format", output_format);
						
						//Submit the next step for execution
				    Connection connection = factory.newConnection();
				    Channel channel = connection.createChannel();
				    channel.queueDeclare(application, true, false, false, null);
				    channel.basicPublish( "", application, MessageProperties.PERSISTENT_TEXT_PLAIN, message.toString().getBytes());
				    channel.close();
				    connection.close();
						
						//Update the job entry
						document.put("step", step);
						document.put("step_status", 0);
						collection.update(new BasicDBObject().append("job_id", job_id), document);
					}else{
						output_path = document.get("output_path").toString();
						output_format = document.get("output_format").toString();
						Utility.save(output_path + "/" + job_id + "_" + Utility.getFilenameName(document.get("input").toString()) + "." + output_format + ".url", "[InternetShortcut]\nURL=" + URLDecoder.decode(input, "UTF-8"));

						collection.remove(document);
						System.out.println("[Steward]: Job-" + job_id + " completed!, " + URLDecoder.decode(input, "UTF-8"));
					}
				}
			}
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
  				discoveryAMQ();
  				Utility.pause(1000);
  			}
  		}
  	}.start();
  	
  	//Start job processing thread
  	new Thread(){
  		public void run(){
  			while(true){
  				process_jobs();
  				Utility.pause(100);
  			}
  		}
  	}.start();
  	
  	//Start heartbeat thread to remove dead servers
  	if(false){	
	  	new Thread(){
	  		public void run(){
		  		while(true){
		  			heartbeat();
		  			Utility.pause(1000);
	  			}
	  		}
	  	}.start();
  	}
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
	 * Start the Polyglot Steward.
	 * @param args command line arguments
	 */
	public static void main(String[] args)
	{
		PolyglotStewardAMQ polyglot = new PolyglotStewardAMQ();
	}
}