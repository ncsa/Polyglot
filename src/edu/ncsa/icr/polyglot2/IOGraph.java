package edu.ncsa.icr.polyglot2;
import edu.ncsa.icr.*;
import edu.ncsa.icr.ICRAuxiliary.*;
import edu.ncsa.utility.*;
import java.util.*;

public class IOGraph<V extends Comparable,E>
{
	private Vector<V> vertices = new Vector<V>();
  private TreeMap<String,Integer> vertex_map = new TreeMap<String,Integer>();  
	private Vector<Vector<E>> edges = new Vector<Vector<E>>();
	private Vector<Vector<Integer>> adjacency_list = new Vector<Vector<Integer>>();
	private Vector<Vector<Double>> weights = new Vector<Vector<Double>>();
	private Vector<Vector<Boolean>> active = new Vector<Vector<Boolean>>();

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
								addEdge((E)application, (V)input, (V)output);
							}
						}
					}else{															//Open/Import operation
						for(int i=0; i<application.operations.size(); i++){
							if(application.operations.get(i).inputs.isEmpty() && !application.operations.get(i).outputs.isEmpty()){
								for(int j=0; j<operation.inputs.size(); j++){
									input = operation.inputs.get(j);
									
									for(int k=0; k<application.operations.get(i).outputs.size(); k++){
										output = application.operations.get(i).outputs.get(k);
										addEdge((E)application, (V)input, (V)output);
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	public int addVertex(V vertex)
	{
		int index = getVertexIndex(vertex);
		
		if(index  == -1){
			index = vertices.size();
			vertices.add(vertex);
			vertex_map.put(vertex.toString(), index);
			edges.add(new Vector<E>());
			adjacency_list.add(new Vector<Integer>());
			weights.add(new Vector<Double>());
			active.add(new Vector<Boolean>());
		}
		
		return index;
	}
	
	/**
	 * Add an edge to the graph.
	 * @param edge the edge
	 * @param source the source vertex
	 * @param target the target vertex
	 */
	public void addEdge(E edge, V source, V target)
	{
		int source_index = addVertex(source);
		int target_index = addVertex(target);
		
		edges.get(source_index).add(edge);
		adjacency_list.get(source_index).add(target_index);
		weights.get(source_index).add(1.0);
		active.get(source_index).add(true);
	}
	
	/**
	 * Set all edges to active.
	 * @return the active edge list
	 */
	public Vector<Vector<Boolean>> setActiveEdges()
	{
		for(int i=0; i<edges.size(); i++){
			for(int j=0; j<edges.get(i).size(); j++){
				active.get(i).set(j,true);
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
	 * Get the index associated with the vertex having the given string representation.
	 * @param string a string representation for the vertex
	 * @return the index of the vertex
	 */
	public int getVertexIndex(V v)
	{
		int index = -1;
		
	  for(int i=0; i<vertices.size(); i++){
	    if(vertices.get(i).compareTo(v) == 0){
	      index = i;
	      break;
	    }
	  }
	  
	  return index;
	}
  
  /**
   * Get the index associated with the vertex having the given string representation.
   * @param string a string representation for the vertex
   * @return the index of the vertex
   */
  public int getVertexIndex(String string)
  {
  	return vertex_map.get(string);
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
	  
	  for(int i=0; i<path.size(); i++){
	    index = path.get(i);
	    if(index_last >= 0) output_string += "(" + edges.get(index_last).get(adjacency_list.get(index_last).indexOf(index)).toString() + ") -> ";
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
  public Set<Integer> getSpanningTree(int index)
  {
    Set<Integer> set = new TreeSet<Integer>(); 
    Vector<Integer> path = getShortestPaths(index);
    
    for(int j=0; j<path.size(); j++){
      if(path.get(j)>=0 && j!=index){
        set.add(j);    
      }
    }
    
    return set;
  }
  
  /**
   * Returns a vector of vertex abbreviations that are reachable from a given source vertex.
   * @param string string associated with the input vertex
   * @return the vector of reachable vertices
   */
  public Vector<String> getSpanningTree(String string)
  {
    Vector<String> span = new Vector<String>();
    Set<Integer> set;
    Iterator<Integer> itr; 
    int index = -1;
    
    for(int i=0; i<vertices.size(); i++){
      if(vertices.get(i).toString().equals(string)){
        index = i;
        break;
      }
    }
    
    if(index >= 0){
      set = getSpanningTree(index);
      itr = set.iterator();
      
      while(itr.hasNext()){
        span.add(vertices.get(itr.next()).toString());
      }
    }
    
    return span;
  }
  
  /**
   * Get a list of all vertices that can reach the target vertex.
   * @param target the index of the target vertex
   * @return the vector of vertices which can reach the target
   */
  public Vector<Integer> getDomain(int target)
  {
  	Vector<Integer> domain = new Vector<Integer>();
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
   * Get a list of all vertices that can reach the target vertex.
   * @param target the target vertex name
   * @return the vector of vertices which can reach the target
   */
  public Vector<String> getDomain(String target)
  {
  	Vector<String> domain = new Vector<String>();
  	Vector<Integer> tmpv = getDomain(vertex_map.get(target));
  	
  	for(int i=0; i<tmpv.size(); i++){
  		domain.add(vertices.get(tmpv.get(i)).toString());
  	}
  	
  	return domain;
  }
  
  /**
	 * Get a list of the conversions tasks required to convert from a given input to a given output.
	 * @param source_string a string representing the input type
	 * @param target_string a string representing the output type
	 * @param ENABLE_WEIGHTED_PATHS use edge weights to determine conversion paths
	 * @return the line separated tasks, with each line containing an edge, input, and output
	 */
	public String getConversionTask(String source_string, String target_string, boolean ENABLE_WEIGHTED_PATHS)
	{
	  String task = "";
	  Vector<Integer> paths;
	  Vector<Integer> path = new Vector<Integer>();
	  E edge;        
	  int source = getVertexIndex(source_string);
	  int target = getVertexIndex(target_string);
	  int i0, i1;
	  
	  if(source >= 0 && target >= 0){
	  	if(!ENABLE_WEIGHTED_PATHS){
	  		paths = getShortestPaths(source);
	  	}else{
	  		paths = getShortestWeightedPaths(source).first;
	  	}
	  	
	  	path = getPath(paths, source, target);
	      
	    if(path.size() <= 1){
	      task = "null\n";
	    }else{
	      for(int i=1; i<path.size(); i++){
	        i0 = path.get(i-1);
	        i1 = path.get(i);
	        edge = edges.get(i0).get(adjacency_list.get(i0).indexOf(i1));
	        
	        task += edge.toString() + " ";
	        task += vertices.get(i0) + " ";
	        task += vertices.get(i1);
	        task += "\n";
	      }
	    }
	  }
	  
	  return task;
	}

	/**
	 * Get a list of the conversions tasks required to convert from a given input to a given output.
	 * Note: this version returns all paths along the shortest path from the source
	 * @param source_string a string representing the input type
	 * @param target_string a string representing the output type
	 * @return a vector of line separated tasks, with each line containing an edge, input, and output
	 */
	public Vector<String> getConversionTasks(String source_string, String target_string)
	{
	  Vector<String> tasks = new Vector<String>();
	  String task;
	  Vector<String> task_buffer;
	  Vector<String> task_buffer_new;
	  Vector<Integer> edge_converter_indices = new Vector<Integer>();
	  Vector<Integer> paths;
	  Vector<Integer> path = new Vector<Integer>();
	  E edge;
	  int source = getVertexIndex(source_string);
	  int target = getVertexIndex(target_string);
	  int i0, i1;
	  
	  if(source >= 0 && target >= 0){
	  	paths = getShortestPaths(source);
	  	path = getPath(paths, source, target);
	      
	    if(!path.isEmpty()){
	      task_buffer = new Vector<String>();
	      
	      if(path.size() == 1){
	        task_buffer.add("null\n");
	      }else{
	        task_buffer.add("");
	        
	        for(int i=1; i<path.size(); i++){
	          i0 = path.get(i-1);
	          i1 = path.get(i);
	          edge_converter_indices.clear();
	          
	          //Find all parallel edges
	          for(int j=0; j<adjacency_list.get(i0).size(); j++){
	            if(adjacency_list.get(i0).get(j) == i1){
	            	edge_converter_indices.add(j);
	            }
	          }
	                      
	          //Add tasks for each parallel edge
	          task_buffer_new = new Vector<String>();
	          
	          for(int j=0; j<edge_converter_indices.size(); j++){
	            edge = edges.get(i0).get(edge_converter_indices.get(j));
	            
	            for(int k=0; k<task_buffer.size(); k++){
	              task = task_buffer.get(k);
	              
	              task += edge.toString() + " ";
	              task += vertices.get(i0).toString() + " ";
	              task += vertices.get(i1).toString();
	              task += "\n";
	              
	              task_buffer_new.add(task);
	            }
	          }
	          
	          task_buffer = task_buffer_new;
	        }
	      }
	      
	      for(int i=0; i<task_buffer.size(); i++){
	        tasks.add(task_buffer.get(i));
	      }
	    }
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
  	ICRClient icr = new ICRClient("ICRClient.ini");
  	IOGraph<Data,Application> iograph = new IOGraph<Data,Application>(icr); icr.close();
    Vector<String> tmpv;
    boolean ALL = false;
    boolean DOMAIN = false;
    int count = 0;
    
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
    		tmpv = iograph.getDomain(args[0]);
    	}else{			//Span/range
        tmpv = iograph.getSpanningTree(args[0]);
    	}
    	
      for(int i=0; i<tmpv.size(); i++){
        System.out.println(tmpv.get(i));
      }
    }else if(count == 2){
      if(ALL){		//All parallel shortest paths
        tmpv = iograph.getConversionTasks(args[0], args[1]);
        
        for(int i=0; i<tmpv.size(); i++){
          System.out.println(tmpv.get(i));
        }
      }else{			//Shortest path
      	System.out.print(iograph.getConversionTask(args[0], args[1], false));
      }
    }
  }
}