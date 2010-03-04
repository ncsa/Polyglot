package edu.ncsa.icr.polyglot;
import edu.ncsa.icr.*;
import edu.ncsa.icr.ICRAuxiliary.*;
import edu.ncsa.utility.*;
import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;

public class IOGraph<V extends Comparable,E>
{
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
	public IOGraph(ICRClient icr)
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
    BufferedReader ins;
  	String application, input_format, output_format;
  	String line;
  	int tmpi;
  	
    HttpURLConnection.setFollowRedirects(false);

    try{
      conn = (HttpURLConnection)new URL(url).openConnection();
      conn.connect();
      ins = new BufferedReader(new InputStreamReader(conn.getInputStream()));
     
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
		weights.get(source_index).add(1.0);
		active.get(source_index).add(true);
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
	 * Get the adjacency list representing graph edges.
	 * @return the edge adjacency list
	 */
	public Vector<Vector<Integer>> getAdjacencyList()
	{
		return adjacency_list;
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
	 * Get the maximum weight along the edge specified.
	 * @param v0 the index of the starting vertex
	 * @param v1 the index of the ending vertex
	 * @return the weight [0, 1]
	 */
	public double getMaxEdgeWeight(int v0, int v1)
	{
		double maxr = -Double.MAX_VALUE;
		int index = -1;
		
		for(int i=0; i<adjacency_list.get(v0).size(); i++){
			if(active.get(v0).get(i) && adjacency_list.get(v0).get(i) == v1){
				if(weights.get(v0).get(i) > maxr){
					maxr = weights.get(v0).get(i);
					index = i;
				}
			}
		}
		
		if(index >= 0){
			return weights.get(v0).get(index);
		}
		
		return 0;
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
  	Set<Integer> set = new TreeSet<Integer>();
    Vector<Integer> path = new Vector<Integer>();
    Vector<Double> weight = new Vector<Double>();
    Iterator<Integer> itr;
    int u, v, tmpi;
    double maxr, tmpd;
    
    //Initialize structures
    for(int i=0; i<vertices.size(); i++){
    	set.add(i);
      path.add(-1); 
      
    	if(i == source){
    		weight.add(1.0);
    	}else{
    		weight.add(0.0);
    	}
    }
    
    //Update shortest paths
    while(!set.isEmpty()){    	
    	u = 0;
    	maxr = 0;
    	itr = set.iterator();
    	
    	//Find vertex with shortest distance
    	while(itr.hasNext()){
    		tmpi = itr.next();
    		
    		if(weight.get(tmpi) >= maxr){
    			u = tmpi;
    			maxr = weight.get(tmpi);
    		}
    	}
    	
    	set.remove(u);
      
    	//Update quality to neighbors
      if(weight.get(u) == 0){ 		//All remaining vertices are inaccessible
      	break;
      }else{
	      for(int i=0; i<adjacency_list.get(u).size(); i++){
	      	if(active.get(u).get(i)){
		      	v = adjacency_list.get(u).get(i);
		      	tmpd = weight.get(u) * (weights.get(u).get(i)/100);	//Total weight = product of all traversed weights!
		      	
		      	if(tmpd > weight.get(v)){
		          path.set(v, u);
		          weight.set(v, tmpd);
		      	}
	      	}
	      }
      }
    }
    
    //Scale qualities back to [0,100]
    for(int i=0; i<weight.size(); i++){
    	weight.set(i, weight.get(i)*100);
    }
    
    return new Pair<Vector<Integer>,Vector<Double>>(path, weight);
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
	 * Get the weights along the path specified.
	 * @param path the indices of the conversion path
	 * @return a vector of weights along the given path
	 */
	public Vector<Double> getPathWeights(Vector<Integer> path)
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
	  int source = vertex_string_map.get(source_string);
	  int target = vertex_string_map.get(target_string);
	  int i0, i1;
	  
	  if(source >= 0 && target >= 0){
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
          edge_indices.clear();
          
          //Find all parallel edges
          for(int j=0; j<adjacency_list.get(i0).size(); j++){
            if(adjacency_list.get(i0).get(j) == i1){
            	edge_indices.add(j);
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
    int count = 0;
    
    if(true){
    	ICRClient icr = new ICRClient("localhost", 30);
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
    	args = new String[]{"obj", "x3d", "-all"};
    }
    
    //Parse command line arguments
    for(int i=0; i<args.length; i++){
      if(args[i].charAt(0) == '-'){
        if(args[i].equals("-all")){
          ALL = true;
        }else if(args[i].equals("-domain")){
          DOMAIN = true;
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
    }
  }
}