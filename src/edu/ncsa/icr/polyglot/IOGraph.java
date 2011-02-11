package edu.ncsa.icr.polyglot;
import edu.ncsa.icr.polyglot.PolyglotAuxiliary.*;
import edu.ncsa.icr.*;
import edu.ncsa.icr.SoftwareReuseAuxiliary.*;
import edu.ncsa.utility.*;
import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import java.util.zip.*;

public class IOGraph<V extends Comparable,E> implements Serializable
{
	public static final long serialVersionUID = 1L;
	private Vector<V> vertices = new Vector<V>();
	private TreeMap<V,Integer> vertex_map = new TreeMap<V,Integer>();
  private TreeMap<String,Integer> vertex_string_map = new TreeMap<String,Integer>();  
	private Vector<Vector<E>> edges = new Vector<Vector<E>>();
	private Vector<Vector<Integer>> adjacency_list = new Vector<Vector<Integer>>();
	private Vector<Vector<Double>> weights = new Vector<Vector<Double>>();
	private Vector<Vector<Boolean>> active = new Vector<Vector<Boolean>>();
	
	public IOGraph() {}
	
	/**
	 * Class constructor.
	 * @param icr an ICR client
	 */
	public IOGraph(SoftwareReuseClient icr)
	{
		Vector<Application> applications = icr.getApplications();
		Application application;
		Operation operation;
		Data input, output;
		
		for(int a=0; a<applications.size(); a++){
			application = applications.get(a);
			
			for(int o=0; o<application.operations.size(); o++){
				operation = application.operations.get(o);
				
				if(!operation.inputs.isEmpty()){
					if(!operation.outputs.isEmpty()){		//Conversion operation
						for(int i=0; i<operation.inputs.size(); i++){
							input = operation.inputs.get(i);
	
							for(int j=0; j<operation.outputs.size(); j++){
								output = operation.outputs.get(j);
								addEdge((V)input, (V)output, (E)application);
							}
						}
					}else{															//Open/Import operation
						for(int i=0; i<application.operations.size(); i++){
							if(application.operations.get(i).inputs.isEmpty() && !application.operations.get(i).outputs.isEmpty()){
								for(int j=0; j<operation.inputs.size(); j++){
									input = operation.inputs.get(j);
									
									for(int k=0; k<application.operations.get(i).outputs.size(); k++){
										output = application.operations.get(i).outputs.get(k);
										addEdge((V)input, (V)output, (E)application);
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
  /**
   * Class constructor (loads data from a CSR database).
   * @param server the server containing the database
   * @param user the user name to use
   * @param password the password for this user
   */
  public IOGraph(String server, String user, String password)
  {
  	Connection connection = null;
  	Statement statement;
  	ResultSet result;
  	String application, input_format, output_format, tmp;
  	
  	try{
  		//Open connection to database
  		connection = DriverManager.getConnection(server, user, password);
  		
  		//Query the database
  		statement = connection.createStatement();
  		statement.executeQuery("SELECT software.name, inputs.default_extension, outputs.default_extension FROM conversions, software, formats AS inputs, formats AS outputs WHERE conversions.software=software.software_id AND conversions.input_format=inputs.format_id AND conversions.output_format=outputs.format_id");
  		result = statement.getResultSet();
  		
  		while(result.next()){
  			application = result.getString("software.name");
  			input_format = result.getString("inputs.default_extension");
  			output_format = result.getString("outputs.default_extension");
  			
  			//System.out.println(application +", " + input_format + ", " + output_format);
  			
  			addVertex((V)input_format);
  			addVertex((V)output_format);
  			addEdge((V)input_format, (V)output_format, (E)application);
  		}
  	
	  	//Close connection
  		result.close();
  		statement.close();
			connection.close();  		
  	}catch(Exception e) {e.printStackTrace();}
  }
  
  /**
   * Class constructor (loads data from a CSR database via a web script to bypass database access restrictions).
   * @param url the URL of the web script
   */
  public IOGraph(String url)
  {   
  	HttpURLConnection conn = null;
    BufferedReader ins = null;
  	String application, input_format, output_format;
  	String line;
  	int tmpi;
  	
    HttpURLConnection.setFollowRedirects(false);

    try{
      conn = (HttpURLConnection)new URL(url).openConnection();
      conn.connect();
      
      if(url.endsWith(".gz")){
      	ins = new BufferedReader(new InputStreamReader(new GZIPInputStream(conn.getInputStream())));
      }else{
      	ins = new BufferedReader(new InputStreamReader(conn.getInputStream()));
      }
     
      while((line = ins.readLine()) != null){
				tmpi = line.lastIndexOf(" ");
				output_format = line.substring(tmpi+1, line.length()-4);
				line = line.substring(0, tmpi);
				tmpi = line.lastIndexOf(" ");
				input_format = line.substring(tmpi+1, line.length());
				application = line.substring(0, tmpi);
				
				//System.out.println(application +", " + input_format + ", " + output_format);
				
				addVertex((V)input_format);
				addVertex((V)output_format);
				addEdge((V)input_format, (V)output_format, (E)application);       
      }
 
      conn.disconnect();
    }catch(Exception e){
      e.printStackTrace();
    }finally{
      if(conn != null) conn.disconnect();
    }	
  }
	
	/**
	 * Clear the graph.
	 */
	public void clear()
	{
		vertices.clear();
		vertex_map.clear();
	  vertex_string_map.clear();  
		edges.clear();
		adjacency_list.clear();
		weights.clear();
		active.clear();
	}

	/**
	 * Add a vertex to the graph if not already present.
	 * @param vertex a vertex to add
	 * @return the index of the vertex
	 */
	public int addVertex(V vertex)
	{
		Integer index = vertex_map.get(vertex);
		
		if(index  == null){
			index = vertices.size();
			vertices.add(vertex);
			vertex_map.put(vertex, index);
			vertex_string_map.put(vertex.toString(), index);
			edges.add(new Vector<E>());
			adjacency_list.add(new Vector<Integer>());
			weights.add(new Vector<Double>());
			active.add(new Vector<Boolean>());
		}
		
		return index;
	}
	
	/**
	 * Add an edge to the graph.
	 * @param source the source vertex
	 * @param target the target vertex
	 * @param edge the edge
	 */
	public void addEdge(V source, V target, E edge)
	{
		int source_index = addVertex(source);
		int target_index = addVertex(target);
		
		edges.get(source_index).add(edge);
		adjacency_list.get(source_index).add(target_index);
		weights.get(source_index).add(null);
		active.get(source_index).add(true);
	}
	
	/**
	 * Add an edge to the graph only if it doesn't exist.
	 * @param source the source vertex
	 * @param target the target vertex
	 * @param edge the edge
	 */
	public void addEdgeOnce(V source, V target, E edge)
	{
		int source_index = addVertex(source);
		int target_index = addVertex(target);
		boolean FOUND = false;
		
		//Check if this edge already exists
		for(int i=0; i<adjacency_list.get(source_index).size(); i++){
			if(adjacency_list.get(source_index).get(i) == target_index){
				if(edges.get(source_index).get(i).toString().equals(edge)){
					FOUND = true;
				}
			}
		}
		
		if(!FOUND){
			edges.get(source_index).add(edge);
			adjacency_list.get(source_index).add(target_index);
			weights.get(source_index).add(null);
			active.get(source_index).add(true);			
		}
	}
	
	/**
	 * Merge another I/O-graph into this one.
	 * @param iograph another I/O-graph
	 */
	public void addGraph(IOGraph<V,E> iograph)
	{
		V v0, v1;
				
		//Add unique vertices
		for(int i=0; i<iograph.vertices.size(); i++){
			if(vertex_map.get(iograph.vertices.get(i)) == null){
				addVertex(iograph.vertices.get(i));
			}
		}
		
		//Add all edges (should not add same ICR twice!)
		for(int i=0; i<iograph.adjacency_list.size(); i++){
			v0 = iograph.vertices.get(i);

			for(int j=0; j<iograph.adjacency_list.get(i).size(); j++){
				v1 = iograph.vertices.get(iograph.adjacency_list.get(i).get(j));
				addEdge(v0, v1, iograph.edges.get(i).get(j));
			}
		}
	}
	
	/**
	 * Get the graph vertices.
	 * @return the vertices
	 */
	public Vector<V> getVertices()
	{
		return vertices;
	}
	
	/**
	 * Get the graph edges.
	 * @return the edges
	 */
	public Vector<Vector<E>> getEdges()
	{
		return edges;
	}
	
	/**
	 * Get the adjacency list representing graph edges.
	 * @return the edge adjacency list
	 */
	public Vector<Vector<Integer>> getAdjacencyList()
	{
		return adjacency_list;
	}

	/**
	 * Set all edges to the given value.
	 * @return the active edge list
	 */
	public Vector<Vector<Boolean>> setActiveEdges(Boolean value)
	{
		for(int i=0; i<edges.size(); i++){
			for(int j=0; j<edges.get(i).size(); j++){
				active.get(i).set(j,value);
			}
		}
		
		return active;
	}
	
	/**
	 * Set only listed edges to active.
	 * @param selected_edges a set of strings associated with selected edges
	 * @return the active edge list
	 */
	public Vector<Vector<Boolean>> setActiveEdges(TreeSet<String> selected_edges)
	{		
		for(int i=0; i<edges.size(); i++){
			for(int j=0; j<edges.get(i).size(); j++){
				if(selected_edges.contains(edges.get(i).get(j).toString())){
					active.get(i).set(j,true);
				}else{
					active.get(i).set(j,false);
				}
			}
		}
		
		return active;
	}
	
	/**
	 * Determine and return which vertices are being used by the currently active edges.
	 * @return the active vertices
	 */
	public Vector<Boolean> getActiveVertices()
	{
		Vector<Boolean> active_vertices = new Vector<Boolean>();
		TreeSet<Integer> set = new TreeSet<Integer>();
		
		for(int i=0; i<adjacency_list.size(); i++){
			for(int j=0; j<adjacency_list.get(i).size(); j++){
				if(active.get(i).get(j)){
					set.add(i);
					set.add(adjacency_list.get(i).get(j));
				}
			}
		}
		
		for(int i=0; i<vertices.size(); i++){
			if(set.contains(i)){
				active_vertices.add(true);
			}else{
				active_vertices.add(false);
			}
		}
		
		return active_vertices;
	}

	/**
	 * Get the list indicating which edges are currently active.
	 * @return the active edge list
	 */
	public Vector<Vector<Boolean>> getActiveEdges()
	{
		return active;
	}

	/**
	 * Get the string representations for the vertices.
	 * @return the string representation for each vertex
	 */
	public Vector<String> getVertexStrings()
	{
		Vector<String> strings = new Vector<String>();
		
		for(int i=0; i<vertices.size(); i++){
			strings.add(vertices.get(i).toString());
		}
		
		return strings;
	}
	
	/**
	 * Get the string representations for the vertices that start with the given prefix.
	 * @param prefix the string vertex strings should start with
	 * @return the string representation for each of the matching vertices
	 */
	public Vector<String> getVertexStringsStartingWith(String prefix)
	{
		Vector<String> strings = new Vector<String>();
		String string;
		
		for(int i=0; i<vertices.size(); i++){
			string = vertices.get(i).toString();
			
			if(string.startsWith(prefix)){
				strings.add(string);
			}
		}
		
		return strings;
	}
	
	/**
	 * Get the string representations for the edges.
	 * @return the string representation for each edge
	 */
	public Vector<Vector<String>> getEdgeStrings()
	{
		Vector<Vector<String>> strings = new Vector<Vector<String>>();
		
		for(int i=0; i<edges.size(); i++){
			strings.add(new Vector<String>());
			
			for(int j=0; j<edges.get(i).size(); j++){
				if(active.get(i).get(j)){
					strings.get(i).add(edges.get(i).get(j).toString());
				}
			}
		}
		
		return strings;
	}
	
	/**
	 * Get a set of the unique edges in the graph.
	 * @return a set of unique edges
	 */
	public TreeSet<E> getUniqueEdges()
	{
		TreeSet<E> set = new TreeSet<E>();
		
		for(int i=0; i<edges.size(); i++){			
			for(int j=0; j<edges.get(i).size(); j++){
				if(active.get(i).get(j)){
					set.add(edges.get(i).get(j));
				}
			}
		}
		
		return set;
	}
		  
	/**
	 * Get a list of edges parallel to the given edge.
	 * @param source the source vertex
	 * @param target the target vertex
	 * @param edge the actual edge (can be null)
	 * @return a list of parallel edges
	 */
	public Vector<E> getParallelEdges(V source, V target, E edge)
	{
		Vector<E> parallel_edges = new Vector<E>();
		int v0 = vertex_map.get(source);
		int v1 = vertex_map.get(target);
		
		for(int i=0; i<adjacency_list.get(v0).size(); i++){
			if(adjacency_list.get(v0).get(i) == v1){
				if(edge==null || edges.get(v0).get(i).toString().equals(edge.toString())){
					parallel_edges.add(edges.get(v0).get(i));
				}
			}
		}
		
		return parallel_edges;
	}

	/**
	 * Get the maximum weight among parallel edges between the vertices specified.
	 * @param v0 the index of the starting vertex
	 * @param v1 the index of the ending vertex
	 * @return the weight
	 */
	public Double getMaxEdgeWeight(int v0, int v1)
	{
		Double maxw = -Double.MAX_VALUE;
		int index = -1;
		
		for(int i=0; i<adjacency_list.get(v0).size(); i++){
			if(active.get(v0).get(i) && adjacency_list.get(v0).get(i) == v1){
				if(weights.get(v0).get(i) != null && weights.get(v0).get(i) > maxw){
					maxw = weights.get(v0).get(i);
					index = i;
				}
			}
		}
		
		if(index == -1) maxw = null;
		
		return maxw;
	}
	
	/**
	 * Set the weight of the given edge.
	 * @param source the source vertex string
	 * @param target the target vertex string
	 * @param edge the edge string
	 * @param weight the weight for this edge
	 */
	public void setEdgeWeight(String source, String target, String edge, Double weight)
	{
		int v0 = vertex_string_map.get(source);
		int v1 = vertex_string_map.get(target);
		
		for(int i=0; i<adjacency_list.get(v0).size(); i++){
			if(adjacency_list.get(v0).get(i) == v1){
				if(edges.get(v0).get(i).toString().equals(edge)){
					weights.get(v0).set(i, weight);
				}
			}
		}
	}
	
	/**
	 * Load graph from the given file.
	 * @param filename a file containing the graph as lines of: edge source target weight
	 */
	public void load(String filename)
	{
		IOGraph<String,String> iograph = new IOGraph<String,String>();
		
	  try{
	    BufferedReader ins = new BufferedReader(new FileReader(filename));
	    String line;
	    String edge, input, output, weight;
	    int tmpi;
	    
	    while((line = ins.readLine()) != null){
				tmpi = line.lastIndexOf(" ");
				weight = line.substring(tmpi+1, line.length());
				line = line.substring(0, tmpi);
				tmpi = line.lastIndexOf(" ");
				output = line.substring(tmpi+1, line.length());
				line = line.substring(0, tmpi);
				tmpi = line.lastIndexOf(" ");
				input = line.substring(tmpi+1, line.length());
				edge = line.substring(0, tmpi);
				
				//System.out.println(edge +", " + input + ", " + output + ", " + weight);
				
				addVertex((V)input);
				addVertex((V)output);
				addEdgeOnce((V)input, (V)output, (E)edge);       
	    }
	
	    ins.close();
	  }catch(Exception e){
	    e.printStackTrace();
	  }
	}

	/**
	 * Load edge weights from the given file.
	 * @param filename a file containing lines with: edge source target weight (can be a URL)
	 * @param invalid_value the value to use for null weights (can be null indicating they should be ignored)
	 */
	public void loadEdgeWeights(String filename, Double invalid_value)
	{
		TreeMap<String,Vector<Double>> edge_weights = new TreeMap<String,Vector<Double>>();
		Vector<Double> vector;
		Vector<String> lines;
		String buffer;
		String string, weight_string, edge_string, edge, input, output;
		Double weight;
		int tmpi;
		
		if(filename.startsWith("http://")){
			buffer = Utility.readURL(filename);
		}else{
			buffer = Utility.loadToString(filename);
		}
		
		lines = Utility.split(buffer, '\n', false);
		
		//Accumulate weights for each edge
		for(int i=0; i<lines.size(); i++){
	  	string = lines.get(i);
	  	if(string.endsWith("<br>")) string = string.substring(0, string.length()-4).trim();
	  	tmpi = string.lastIndexOf(' ');
	  	weight_string = string.substring(tmpi+1);
	  	edge_string = string.substring(0, tmpi);
	  	
	  	if(weight_string.equals("null") || weight_string.equals("NaN")){		//A measurement failed!
	  		weight = invalid_value;
		  }else{
		  	weight = Double.valueOf(weight_string);
		  }
	
	  	if(weight != null){
		  	vector = edge_weights.get(edge_string);
		  	
		  	if(vector == null){
		  		vector = new Vector<Double>();
		  		edge_weights.put(edge_string, vector);
		  	}
		  	
		  	vector.add(weight);
			}
		}
		
		//Average weights for each edge
		for(Iterator<String> itr=edge_weights.keySet().iterator(); itr.hasNext();){
			edge_string = itr.next();
	  	tmpi = edge_string.lastIndexOf(' ');
	  	output = edge_string.substring(tmpi+1);
	  	string = edge_string.substring(0, tmpi);
	  	tmpi = string.lastIndexOf(' ');
	  	input = string.substring(tmpi+1);
	  	edge = string.substring(0, tmpi);
	  	
	  	vector = edge_weights.get(edge_string);
	  	weight = 0.0;
	  	
	  	for(int i=0; i<vector.size(); i++){
	  		weight += vector.get(i);
	  	}
	  	
	  	weight /= vector.size();
	  	setEdgeWeight(input, output, edge, weight);
		}
	}
	
	/**
	 * Transform edge weights according to the given expression.
	 * @param expression an expression to apply to all edge weights
	 */
	public void transformEdgeWeights(String expression)
	{
		Function f = new Function(new String[]{"x"}, expression);
		
		for(int i=0; i<weights.size(); i++){
			for(int j=0; j<weights.get(i).size(); j++){
				if(weights.get(i).get(j) != null){
					weights.get(i).set(j, f.get(new double[]{weights.get(i).get(j)}));
				}
			}
		}
	}
	
	/**
   * Perform a breadth first search from the vertex at the given index and store the resulting paths.
   * @param source the index of the source vertex
   * @return the paths vector indicating from which vertex we must come to get to this vertex
   */
  public Vector<Integer> getShortestPaths(int source)
  {
    Vector<Integer> path = new Vector<Integer>();
    Vector<Boolean> visited = new Vector<Boolean>();
    
    for(int i=0; i<vertices.size(); i++){
      visited.add(false);
      path.add(-1);
    }
    
    Queue<Integer> queue = new LinkedList<Integer>();
    queue.add(source);
    
    while(queue.size() > 0){
      source = queue.remove();
      
      for(int i=0; i<adjacency_list.get(source).size(); i++){
        if(active.get(source).get(i) && !visited.get(adjacency_list.get(source).get(i))){
          visited.set(adjacency_list.get(source).get(i), true);
          path.set(adjacency_list.get(source).get(i), source);
          queue.add(adjacency_list.get(source).get(i));
        }
      }
    }
    
    return path;
  }
  
  /**
   * Perform Dijkstra's algorithm from the vertex at the given source index and store the resulting paths.
   * @param source the index of the source vertex
   * @return the paths vector indicating from which vertex we must come to get to this vertex and the weight along each path
   */
  public Pair<Vector<Integer>,Vector<Double>> getShortestWeightedPaths(int source)
  {    
  	PriorityQueue<Pair<Double,Integer>> queue = new PriorityQueue<Pair<Double,Integer>>();
  	Pair<Double,Integer> pair;
    Vector<Integer> paths = new Vector<Integer>();
    Vector<Double> path_weights = new Vector<Double>();
    int u, v;
    double tmpd;
    
    //Initialize structures
    for(int i=0; i<vertices.size(); i++){
      paths.add(-1); 
      
    	if(i == source){
    		path_weights.add(0.0);
    	}else{
    		path_weights.add(Double.MAX_VALUE);
    	}
    	
    	queue.add(new Pair<Double,Integer>(path_weights.get(i), i));
    }
    
    //Update shortest paths
    while(!queue.isEmpty()){
    	pair = queue.poll();
    	u = pair.second;
      
    	//Update quality to neighbors
      if(path_weights.get(u) == Double.MAX_VALUE){ 		//All remaining vertices are inaccessible
      	break;
      }else{
	      for(int i=0; i<adjacency_list.get(u).size(); i++){
	      	if(active.get(u).get(i) && weights.get(u).get(i) != null){
		      	v = adjacency_list.get(u).get(i);
		      	tmpd = weights.get(u).get(i) + path_weights.get(u);
		      	
		      	if(tmpd < path_weights.get(v)){
		          paths.set(v, u);
		          path_weights.set(v, tmpd);
		      	}
	      	}
	      }
      }
    }
    
    return new Pair<Vector<Integer>,Vector<Double>>(paths, path_weights);
  }
  
  /**
	 * Convert a path into a string representation.
	 * @param path the path from a source vertex to a target vertex
	 * @return the string representation
	 */
	public String getPathString(Vector<Integer> path)
	{
		String output_string = "";
		int index;
	  int index_last = -1;
	  int edge_index = 0;
	  
	  for(int i=0; i<path.size(); i++){
	    index = path.get(i);
	    
	    //Find edge
	    if(index_last >= 0){
	    	for(int j=0; j<adjacency_list.get(index_last).size(); j++){
	    		if(active.get(index_last).get(j) && adjacency_list.get(index_last).get(j) == index){
	    			edge_index = j;
	    		}
	    	}
	    	
	    	output_string += "(" + edges.get(index_last).get(edge_index).toString() + ") -> ";
	    }
	    
	    output_string += vertices.get(index).toString();
	    if(i < path.size()-1) output_string += " -> ";
	    index_last = index;
	  }
		
		return output_string;
	}
		  
  /**
	 * Get the maximum weights along the path specified.
	 * @param path the indices of the conversion path
	 * @return a vector of weights along the given path
	 */
	public Vector<Double> getMaxPathWeights(Vector<Integer> path)
	{
		Vector<Double> path_weights = new Vector<Double>();
		
		for(int i=0; i<path.size()-1; i++){
			path_weights.add(getMaxEdgeWeight(path.get(i), path.get(i+1)));
		}
		
		return path_weights;
	}

	/**
   * Return a set of all reachable vertices from the given source.
   * @param index the index of the source vertex
   * @return the set of reachable vertex indices
   */
  public TreeSet<Integer> getRange(int index)
  {
    TreeSet<Integer> range = new TreeSet<Integer>(); 
    Vector<Integer> path = getShortestPaths(index);
    
    for(int j=0; j<path.size(); j++){
      if(path.get(j)>=0 && j!=index){
        range.add(j);    
      }
    }
    
    return range;
  }
  
  /**
   * Returns a set of vertex strings that are reachable from other vertices.
   * @return the set of reachable vertex strings
   */
  public TreeSet<String> getRangeStrings()
  {
    TreeSet<String> range = new TreeSet<String>();
    Set<Integer> range_indices;
    
    for(int i=0; i<vertices.size(); i++){
      range_indices = getRange(i);
      
      for(Iterator<Integer> itr=range_indices.iterator(); itr.hasNext();){
        range.add(vertices.get(itr.next()).toString());
      }
    }
    
    return range;
  }
  
  /**
   * Returns a set of vertex strings that are reachable from a given source vertex.
   * @param string string associated with the input vertex
   * @return the set of reachable vertex strings
   */
  public TreeSet<String> getRangeStrings(String string)
  {
    TreeSet<String> range = new TreeSet<String>();
    Set<Integer> range_indices;
    Iterator<Integer> itr;
    Integer index = vertex_string_map.get(string);
    
    if(index != null){
      range_indices = getRange(index);
      itr = range_indices.iterator();
      
      while(itr.hasNext()){
        range.add(vertices.get(itr.next()).toString());
      }
    }
    
    return range;
  }
  
  /**
   * Get the set of all vertices that can reach the target vertex.
   * @param target the index of the target vertex
   * @return the set of vertices which can reach the target
   */
  public TreeSet<Integer> getDomain(int target)
  {
  	TreeSet<Integer> domain = new TreeSet<Integer>();
  	Vector<Integer> paths;
  	
  	for(int i=0; i<vertices.size(); i++){
  		if(i != target){
	  	  paths = getShortestPaths(i);
	  	  if(paths.get(target) != -1) domain.add(i);
  		}
  	}
  	
  	return domain;
  }
  
  /**
   * Get a set of all vertices that can reach the target vertex.
   * @param target the target vertex string
   * @return the set of vertices which can reach the target
   */
  public TreeSet<String> getDomainStrings(String target)
  {
  	TreeSet<String> domain = new TreeSet<String>();
  	TreeSet<Integer> domain_indices = getDomain(vertex_string_map.get(target));
  	Iterator<Integer> itr = domain_indices.iterator();
  	
  	while(itr.hasNext()){
  		domain.add(vertices.get(itr.next()).toString());
  	}
  	
  	return domain;
  }
  
  /**
   * Get the range intersection of the vertices in the given set.
   * @param set a set of vertex indices
   * @return a set of vertex indices within the given sets range intersection
   */
  public TreeSet<Integer> getRangeIntersection(TreeSet<Integer> set)
  {
  	TreeSet<Integer> intersection = null;
  	Iterator<Integer> itr = set.iterator();
  	
  	while(itr.hasNext()){
  		if(intersection == null){
  			intersection = getRange(itr.next());
  		}else{
  			intersection.retainAll(getRange(itr.next()));
  		}
  	}
  	
  	return intersection;
  }
  
  /**
   * Get the range intersection of the vertices in the given set.
   * @param set a set of vertex strings
   * @return a set of vertex strings within the given sets range intersection
   */
  public TreeSet<String> getRangeIntersectionStrings(TreeSet<String> set)
  {
  	TreeSet<String> intersection = null;
  	Iterator<String> itr = set.iterator();
  	
  	while(itr.hasNext()){
  		if(intersection == null){
  			intersection = getRangeStrings(itr.next());
  		}else{
  			intersection.retainAll(getRangeStrings(itr.next()));
  		}
  	}
  	
  	return intersection;
  }
  
  /**
	 * Get the conversion possessing the given name, input, and output
	 * @param edge_string the string associated with the conversion edge
	 * @param input_string the input
	 * @param output_string the output
	 * @return the conversion from input to output
	 */
	public Conversion<V,E> getConversion(String edge_string, String input_string, String output_string)
	{
	  int input = vertex_string_map.get(input_string);
	  int output = vertex_string_map.get(output_string);
	  
	  if(input >= 0 && output >= 0){
	  	for(int i=0; i<edges.get(input).size(); i++){
	  		if(edges.get(input).get(i).toString().equals(edge_string) && adjacency_list.get(input).get(i) == output){
	  			return new Conversion<V,E>(vertices.get(input), vertices.get(output), edges.get(input).get(i));
	  		}
	  	}
	  }
	  
	  return null;
	}
	
	/**
	 * Parse a string containing conversion information
	 * @param conversions_string a string containing line separated conversions of the form: Application input output
	 * @return a vector of conversions corresponding to the given string
	 */
	public Vector<Conversion<V,E>> getConversions(String conversions_string)
	{
		Vector<Conversion<V,E>> conversions = new Vector<Conversion<V,E>>();
	  Vector<String> lines = Utility.split(conversions_string, '\n', false);
	  String line, edge_string, input_string, output_string;
	  int tmpi;
	  
	  for(int i=0; i<lines.size(); i++){
	  	line = lines.get(i);
	  	tmpi = line.lastIndexOf(' ');
	  	output_string = line.substring(tmpi+1);
	  	line = line.substring(0, tmpi);
	  	tmpi = line.lastIndexOf(' ');
	  	input_string = line.substring(tmpi+1);
	  	edge_string = line.substring(0, tmpi);
	  	
	  	conversions.add(getConversion(edge_string, input_string, output_string));
	  }
	  
		return conversions;
	}

	/**
	 * Get the shortest list of the conversion tasks required to convert from a given input to a given output.
	 * @param source_string a string representing the input type
	 * @param target_string a string representing the output type
	 * @param ENABLE_WEIGHTED_PATHS use edge weights to determine conversion paths
	 * @return a vector of conversions containing an edge, input, and output (null if no conversions found)
	 */
	public Vector<Conversion<V,E>> getShortestConversionPath(String source_string, String target_string, boolean ENABLE_WEIGHTED_PATHS)
	{
		Vector<Conversion<V,E>> conversions = null;
	  Vector<Integer> paths;
	  Vector<Integer> path = new Vector<Integer>();
	  E edge;        
	  Integer source = vertex_string_map.get(source_string);
	  Integer target = vertex_string_map.get(target_string);
	  int i0, i1;
	  
	  if(source != null && target != null){
	  	if(!ENABLE_WEIGHTED_PATHS){
	  		paths = getShortestPaths(source);
	  	}else{
	  		paths = getShortestWeightedPaths(source).first;
	  	}
	  	
	  	path = getPath(paths, source, target);
	      
	    if(path.size() > 1){
	    	conversions = new Vector<Conversion<V,E>>();
	    	
	      for(int i=1; i<path.size(); i++){
	        i0 = path.get(i-1);
	        i1 = path.get(i);
	        edge = edges.get(i0).get(adjacency_list.get(i0).indexOf(i1));
	        conversions.add(new Conversion<V,E>(vertices.get(i0), vertices.get(i1), edge));
	      }
	    }
	  }
	  
	  return conversions;
	}
	
  /**
	 * Get the shortest list of the conversion tasks required to convert from a given input to a given output.
	 * @param source_string a string representing the input type
	 * @param target_string a string representing the output type
	 * @param ENABLE_WEIGHTED_PATHS use edge weights to determine conversion paths
	 * @return the line separated tasks, with each line containing an edge, input, and output
	 */
	public String getShortestConversionPathString(String source_string, String target_string, boolean ENABLE_WEIGHTED_PATHS)
	{
		Vector<Conversion<V,E>> conversions = getShortestConversionPath(source_string, target_string, ENABLE_WEIGHTED_PATHS);
	  String task = "";

    if(conversions.isEmpty()){
      task = "null\n";
    }else{
      for(int i=0; i<conversions.size(); i++){
        task += conversions.get(i).edge.toString() + " ";
        task += conversions.get(i).input + " ";
        task += conversions.get(i).output;
        task += "\n";
      }
    }
	  
	  return task;
	}

	/**
	 * Get the shortest list of the conversion tasks required to convert from a given input to a given output.
	 * Note: this version returns all parallel paths along the shortest path from the source
	 * @param source_string a string representing the input type
	 * @param target_string a string representing the output type
	 * @return a vector of conversions containing an edge, input, and output (null if no conversions found)
	 */
	public Vector<Vector<Conversion<V,E>>> getShortestConversionPaths(String source_string, String target_string)
	{
		Vector<Vector<Conversion<V,E>>> conversions_buffer = new Vector<Vector<Conversion<V,E>>>();
		Vector<Vector<Conversion<V,E>>> conversions_buffer_new;
		Vector<Conversion<V,E>> conversions;
		TreeSet<E> edge_set = new TreeSet<E>();
	  Vector<Integer> edge_indices = new Vector<Integer>();
	  Vector<Integer> paths;
	  Vector<Integer> path = new Vector<Integer>();
	  E edge;
	  int source = vertex_string_map.get(source_string);
	  int target = vertex_string_map.get(target_string);
	  int i0, i1;
	  
	  if(source >= 0 && target >= 0){
	  	paths = getShortestPaths(source);
	  	path = getPath(paths, source, target);
	      
      if(path.size() > 1){
        conversions_buffer.add(new Vector<Conversion<V,E>>());
        
        for(int i=1; i<path.size(); i++){
          i0 = path.get(i-1);
          i1 = path.get(i);
          edge_set.clear();
          edge_indices.clear();
          
          //Find all parallel edges
          for(int j=0; j<adjacency_list.get(i0).size(); j++){
            if(adjacency_list.get(i0).get(j) == i1){
            	if(!edge_set.contains(edges.get(i0).get(j))){
            		edge_set.add(edges.get(i0).get(j));
            		edge_indices.add(j);
            	}
            }
          }
                      
          //Add tasks for each parallel edge
          conversions_buffer_new = new Vector<Vector<Conversion<V,E>>>();
          
          for(int j=0; j<edge_indices.size(); j++){
            edge = edges.get(i0).get(edge_indices.get(j));
            
            for(int k=0; k<conversions_buffer.size(); k++){
              conversions = new Vector<Conversion<V,E>>(conversions_buffer.get(k));
              conversions.add(new Conversion<V,E>(vertices.get(i0), vertices.get(i1), edge));
              conversions_buffer_new.add(conversions);
            }
          }
          
          conversions_buffer = conversions_buffer_new;
        }
      }
	  }
	  
	  return conversions_buffer;
	}
	
	/**
	 * Get the shortest list of the conversion tasks required to convert from a given input to a given output.
	 * Note: this version returns all parallel paths along the shortest path from the source
	 * @param source_string a string representing the input type
	 * @param target_string a string representing the output type
	 * @return a vector of line separated tasks, with each line containing an edge, input, and output
	 */
	public Vector<String> getShortestConversionPathStrings(String source_string, String target_string)
	{
		Vector<Vector<Conversion<V,E>>> conversions = getShortestConversionPaths(source_string, target_string);
		Vector<Conversion<V,E>> conversion;
		Vector<String> tasks = new Vector<String>();
		String task;

		for(int i=0; i<conversions.size(); i++){
			conversion = conversions.get(i);
			task = "";
			
			for(int j=0; j<conversion.size(); j++){
				task += conversion.get(j).edge.toString() + " ";
				task += conversion.get(j).input.toString() + " ";
				task += conversion.get(j).output.toString();
				task += "\n";
			}
			
			tasks.add(task);
		}
	  
	  return tasks;
	}

	/**
	 * Follow a path back from target to source given the single source shortest paths from the source.
	 * @param paths the single source shortest paths from the source
	 * @param source the source vertex
	 * @param target the destination vertex
	 * @return the path from source to target represented as indices to vertices
	 */
	public static Vector<Integer> getPath(Vector<Integer> paths, int source, int target)
	{
		Vector<Integer> path = new Vector<Integer>();
		Stack<Integer> stack = new Stack<Integer>();
		int index;
		
		if(source >= 0 && target >= 0){
	    index = target;
	    
	    if(paths.get(index) != -1){
	      while(index != source){
	        stack.push(index);
	        index = paths.get(index);
	      }
	      
	      stack.push(source);
	      
	      while(!stack.empty()){
	        index = stack.pop();
	        path.add(index);
	      }
	    }
		}
		
		return path;
	}
	
	/**
	 * Get a string only version of this IOGraph.
	 * @return a string version of this IOGraph
	 */
  public IOGraph<String,String> getIOGraphStrings()
  {   
  	IOGraph<String,String> iograph_strings = new IOGraph<String,String>();

  	for(int i=0; i<vertices.size(); i++){
  		iograph_strings.addVertex(vertices.get(i).toString());
  	}
  	
  	for(int i=0; i<adjacency_list.size(); i++){
  		for(int j=0; j<adjacency_list.get(i).size(); j++){
  			iograph_strings.addEdge(vertices.get(i).toString(), vertices.get(adjacency_list.get(i).get(j)).toString(), edges.get(i).get(j).toString());
  		}
  	}
  	
  	return iograph_strings;
  }
	
  /**
   * Find the edge with the smallest weight.
   */
  public void printMinWeightedEdge()
  {
  	double minw = Double.MAX_VALUE;
  	int mini = 0;
  	int minj = 0;
  	
  	for(int i=0; i<weights.size(); i++){
  		for(int j=0; j<weights.get(i).size(); j++){
	  	  if(weights.get(i).get(j) != null && weights.get(i).get(j) < minw){
	  	  	minw = weights.get(i).get(j);
	  	  	mini = i;
	  	  	minj = j;
	  	  }
  		}
  	}
  	  	
  	System.out.println("Min weighted edge: \"" + edges.get(mini).get(minj).toString() + "\" " + vertices.get(mini) + "->" + vertices.get(adjacency_list.get(mini).get(minj)) + " (" + minw + ")");
  }
  
  /**
   * Find the edge with the largest weight.
   */
  public void printMaxWeightedEdge()
  {
  	double maxw = Double.MAX_VALUE;
  	int maxi = 0;
  	int maxj = 0;
  	
  	for(int i=0; i<weights.size(); i++){
  		for(int j=0; j<weights.get(i).size(); j++){
	  	  if(weights.get(i).get(j) != null && weights.get(i).get(j) > maxw){
	  	  	maxw = weights.get(i).get(j);
	  	  	maxi = i;
	  	  	maxj = j;
	  	  }
  		}
  	}
  	  	
  	System.out.println("Max weighted edge: \"" + edges.get(maxi).get(maxj).toString() + "\" " + vertices.get(maxi) + "->" + vertices.get(adjacency_list.get(maxi).get(maxj)) + " (" + maxw + ")");
  }
  
  /**
   * Find the vertex with the shortest combined traversed weight when visited from other vertices.
   */
  public void printMinWeightedVertex()
  {
		Pair<Vector<Integer>,Vector<Double>> tmpp;
		Vector<Integer> paths;
		Vector<Double> path_weights;
		Vector<Double> mean_path_weights = new Vector<Double>();
		Vector<Integer> domain_size = new Vector<Integer>();
		double minw = Double.MAX_VALUE;
		int mini = 0;
	
		//Initialize values
		for(int i=0; i<vertices.size(); i++){
			mean_path_weights.add(0.0);
			domain_size.add(0);
		}
		
		//Calculate mean path weights for each reachable vertex
		for(int i=0; i<vertices.size(); i++){
		  tmpp = getShortestWeightedPaths(i);
		  paths = tmpp.first;
		  path_weights = tmpp.second;
		  
		  for(int j=0; j<vertices.size(); j++){
		  	if(i != j && path_weights.get(j) < Double.MAX_VALUE){
		  		mean_path_weights.set(j, mean_path_weights.get(j)+path_weights.get(j));
		  		domain_size.set(j, domain_size.get(j)+1);
		  	}
		  }
		}
		
		for(int i=0; i<vertices.size(); i++){
			if(domain_size.get(i) > 0){
				mean_path_weights.set(i, mean_path_weights.get(i)/domain_size.get(i));
			}else{
				mean_path_weights.set(i, null);
			}
		}
		
		//Find vertex with smallest mean path weights
		for(int i=0; i<mean_path_weights.size(); i++){
			if(mean_path_weights.get(i) != null && mean_path_weights.get(i) < minw){
				minw = mean_path_weights.get(i);
				mini = i;
			}
		}
		
		System.out.println("Min weighted vertex: " + vertices.get(mini) + " (" + minw + "), n=" + domain_size.get(mini));
  }
	
  /**
   * Display information about each unique edge.
   */
  public void printEdgeInformation()
  {
  	TreeSet<E> unique_edges = getUniqueEdges();
  	
  	for(Iterator<E> itr=unique_edges.iterator(); itr.hasNext();){
  		E edge = itr.next();
  		System.out.println("\n" + edge);
  		printEdgeInformation(edge);
  	}
  }
  
  /**
   * Display information about an edge.
   * @param edge the edge to display information about
   */
  public void printEdgeInformation(E edge)
  {
  	Vector<Triple<Double,String,String>> triples = new Vector<Triple<Double,String,String>>();
  	Triple<Double,String,String> triple;
  	
  	//Accumulate edges as triples
  	for(int i=0; i<edges.size(); i++){
  		for(int j=0; j<edges.get(i).size(); j++){
  			if(edges.get(i).get(j).toString().equals(edge)){
  				triple = new Triple<Double,String,String>(weights.get(i).get(j), vertices.get(i).toString(), vertices.get(adjacency_list.get(i).get(j)).toString());
  				triples.add(triple);
  			}
  		}
  	}
  	
  	//Sort triples
  	Collections.sort(triples);
  	
  	//Display triples
  	for(int i=triples.size()-1; i>=0; i--){
  		triple = triples.get(i);
  		//System.out.println(triple.first + ": " + triple.second + " -> " + triple.third);
  		System.out.println(triple.first + ", " + triple.second + ", " + triple.third);
  	}
  }
  
  /**
   * A main for debug purposes.
   * @param args command line arguments
   */
  public static void main(String args[])
  {  
  	IOGraph iograph = null;
  	Vector<String> vector;
  	TreeSet<String> set;
  	Iterator<String> itr;
    boolean ALL = false;
    boolean DOMAIN = false;
    boolean OPTIMAL = false;
    int count = 0;
    
    if(true){
    	SoftwareReuseClient icr = new SoftwareReuseClient("localhost", 30);
    	iograph = new IOGraph<Data,Application>(icr);
    	icr.close();
    }else{
    	iograph = new IOGraph<String,String>("jdbc:mysql://isda.ncsa.uiuc.edu/csr", "demo", "demo");
    }
    
    //Set some test arguments if none provided
    if(args.length == 0){	
    	//args = new String[]{"obj"};
    	//args = new String[]{"obj", "-domain"};
    	//args = new String[]{"obj", "x3d"};
    	//args = new String[]{"obj", "x3d", "-all"};
    	args = new String[]{"-optimal"};
    }
    
    //Parse command line arguments
    for(int i=0; i<args.length; i++){
      if(args[i].charAt(0) == '-'){
        if(args[i].equals("-all")){
          ALL = true;
        }else if(args[i].equals("-domain")){
          DOMAIN = true;
        }else if(args[i].equals("-optimal")){
        	OPTIMAL = true;
        }
      }else{
        count++;
      }
    }
  
    //Query I/O-Graph
    if(count == 1){
    	if(DOMAIN){	//Domain
    		set = iograph.getDomainStrings(args[0]);
    	}else{			//Span/range
        set = iograph.getRangeStrings(args[0]);
    	}
    	
    	itr = set.iterator();
    	
    	while(itr.hasNext()){
        System.out.println(itr.next());
      }
    }else if(count == 2){
      if(ALL){		//All parallel shortest paths
        vector = iograph.getShortestConversionPathStrings(args[0], args[1]);
        
        for(int i=0; i<vector.size(); i++){
          System.out.println(vector.get(i));
        }
      }else{			//Shortest path
      	System.out.print(iograph.getShortestConversionPathString(args[0], args[1], false));
      }
    }else if(OPTIMAL){
    	iograph.loadEdgeWeights("data/weights.txt", null);
    	iograph.printMinWeightedEdge();
    	iograph.printMinWeightedVertex();
    }
  }
}