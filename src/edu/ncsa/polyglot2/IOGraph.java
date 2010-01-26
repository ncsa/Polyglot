package edu.ncsa.polyglot2;
import edu.ncsa.polyglot2.IOGraphViewerAuxiliary.*;
import edu.ncsa.icr.*;
import edu.ncsa.icr.ICRAuxiliary.*;
import edu.ncsa.utility.*;
import java.util.*;

public class IOGraph<V extends Comparable,E>
{
	private Vector<V> vertices = new Vector<V>();
	private Vector<Vector<E>> edges = new Vector<Vector<E>>();
	private Vector<Vector<Integer>> adjacency_list = new Vector<Vector<Integer>>();
	private Vector<Vector<Double>> weights = new Vector<Vector<Double>>();

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
			edges.add(new Vector<E>());
			adjacency_list.add(new Vector<Integer>());
			weights.add(new Vector<Double>());
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
				strings.get(i).add(edges.get(i).get(j).toString());
			}
		}
		
		return strings;
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
        if(!visited.get(adjacency_list.get(source).get(i))){
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
   * @return the paths vector indicating from which vertex we must come to get to this vertex and the quality along each path
   */
  public Pair<Vector<Integer>,Vector<Double>> getShortestWeightedPaths(int source)
  {    
  	Set<Integer> set = new TreeSet<Integer>();
    Vector<Integer> path = new Vector<Integer>();
    Vector<Double> quality = new Vector<Double>();
    Iterator<Integer> itr;
    int u, v, tmpi;
    double maxr, tmpd;
    
    //Initialize structures
    for(int i=0; i<vertices.size(); i++){
    	set.add(i);
      path.add(-1); 
      
    	if(i == source){
    		quality.add(1.0);
    	}else{
    		quality.add(0.0);
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
    		
    		if(quality.get(tmpi) >= maxr){
    			u = tmpi;
    			maxr = quality.get(tmpi);
    		}
    	}
    	
    	set.remove(u);
      
    	//Update quality to neighbors
      if(quality.get(u) == 0){ 		//All remaining vertices are inaccessible
      	break;
      }else{
	      for(int i=0; i<adjacency_list.get(u).size(); i++){
	      	v = adjacency_list.get(u).get(i);
	      	tmpd = quality.get(u) * (weights.get(u).get(i)/100);	//Total quality = product of all traversed qualities!
	      	
	      	if(tmpd > quality.get(v)){
	          path.set(v, u);
	          quality.set(v, tmpd);
	      	}
	      }
      }
    }
    
    //Scale qualities back to [0,100]
    for(int i=0; i<quality.size(); i++){
    	quality.set(i, quality.get(i)*100);
    }
    
    return new Pair<Vector<Integer>,Vector<Double>>(path, quality);
  }
  
  /**
   * Get the weights along the path specified.
   * @param path the indices of the conversion path
   * @return a vector of weights along the given path
   */
  public Vector<Double> getPathQuality(Vector<Integer> path)
  {
  	Vector<Double> path_weights = new Vector<Double>();
  	
  	for(int i=0; i<path.size()-1; i++){
  		path_weights.add(getMaxEdgeWeight(path.get(i), path.get(i+1)));
  	}
  	
  	return path_weights;
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
  		if(adjacency_list.get(v0).get(i) == v1){
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
   * Convert a path into a string representation.
   * @param path the path from a source vertex to a target vertex
   * @return the string representation
   */
  public String toString(Vector<Integer> path)
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
   * @param name the input vertex name
   * @return the vector of reachable vertices
   */
  public Vector<String> getSpanningTree(String name)
  {
    Vector<String> span = new Vector<String>();
    Set<Integer> set;
    Iterator<Integer> itr; 
    int index = -1;
    
    for(int i=0; i<vertices.size(); i++){
      if(vertices.get(i).toString().equals(name)){
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
  
//  /**
//   * Get a list of all vertices that can reach the target vertex.
//   * @param target the target vertex name
//   * @return the vector of vertices which can reach the target
//   */
//  public Vector<String> getDomain(String target)
//  {
//  	Vector<String> domain = new Vector<String>();
//  	Vector<Integer> tmpv = getDomain(vmap.get(target));
//  	
//  	for(int i=0; i<tmpv.size(); i++){
//  		domain.add(vertices.get(tmpv.get(i)).toString());
//  	}
//  	
//  	return domain;
//  }
  
//  /**
//   * Get the conversions task for the Polyglot daemon.
//   *  @param s0 the input type
//   *  @param s1 the output type
//   *  @param ENABLE_WEIGHTED_PATHS use edge quality to determine conversion paths
//   *  @return the line seperated task, with each line containing an IOObject name and the IO fields and types to perform the conversion
//   */
//  public String getTask(String s0, String s1, boolean ENABLE_WEIGHTED_PATHS)
//  {
//    String task = "";
//    Converter converter;    
//    Vector<Integer> v0_paths;
//    Vector<Integer> v01 = new Vector<Integer>();
//    int v0 = -1;
//    int v1 = -1;
//    int i0, i1;
//    
//    for(int i=0; i<vertices.size(); i++){
//      if(vertices.get(i).equals(s0)) v0 = i;
//      if(vertices.get(i).equals(s1)) v1 = i;
//      if(v0 >= 0 && v1 >= 0) break;
//    }
//    
//    if(v0 >= 0 && v1 >= 0){
//    	if(!ENABLE_WEIGHTED_PATHS){
//    		v0_paths = getShortestPaths(v0);
//    	}else{
//    		v0_paths = getShortestWeightedPaths(v0).first;
//    	}
//    	
//    	v01 = getPath(v0, v1, v0_paths);
//        
//      if(v01.size() <= 1){
//        task = "null\n";
//      }else{
//        for(int i=1; i<v01.size(); i++){
//          i0 = v01.get(i-1);
//          i1 = v01.get(i);
//          converter = converters.get(edge_converters.get(i0).get(edges.get(i0).indexOf(i1)));
//          
//          if(converter.alias.isEmpty()){
//            task += "\"" + converter.name + "\" ";
//          }else{
//            task += converter.alias + " ";
//          }
//          
//          task += converter.input_fields.get(converter.inputs.indexOf(vertices.get(i0))) + " ";
//          task += vertices.get(i0) + " ";
//          task += converter.output_fields.get(converter.outputs.indexOf(vertices.get(i1))) + " ";
//          task += vertices.get(i1);
//          task += "\n";
//        }
//      }
//    }
//    
//    return task;
//  }
//  
//  /**
//   * Get the conversions tasks for the Polyglot daemon.
//   * Note: this version returns all paths along the shortest path from the source
//   * to the target (using all parrallel edges along the way).
//   *  @param s0 the input type
//   *  @param s1 the output type
//   *  @return A vector of line seperated tasks, with each line containing an IOObject name and the IO fields and types to perform the conversion
//   */
//  public Vector<String> getTasks(String s0, String s1)
//  {
//    Vector<String> tasks = new Vector<String>();
//    String task;
//    Vector<String> task_buffer;
//    Vector<String> task_buffer_new;
//    Vector<Integer> edge_converter_indices = new Vector<Integer>();
//    Converter converter;
//    Vector<Integer> v0_paths;
//    Vector<Integer> v01 = new Vector<Integer>();
//    int v0 = -1;
//    int v1 = -1;
//    int i0, i1;
//    
//    for(int i=0; i<vertices.size(); i++){
//      if(vertices.get(i).equals(s0)) v0 = i;
//      if(vertices.get(i).equals(s1)) v1 = i;
//      if(v0 >= 0 && v1 >= 0) break;
//    }
//    
//    if(v0 >= 0 && v1 >= 0){
//    	v0_paths = getShortestPaths(v0);
//    	v01 = getPath(v0, v1, v0_paths);
//        
//      if(!v01.isEmpty()){
//        task_buffer = new Vector<String>();
//        
//        if(v01.size() == 1){
//          task_buffer.add("null\n");
//        }else{
//          task_buffer.add("");
//          
//          for(int i=1; i<v01.size(); i++){
//            i0 = v01.get(i-1);
//            i1 = v01.get(i);
//            edge_converter_indices.clear();
//            
//            //Find all parrallel edges
//            for(int j=0; j<edges.get(i0).size(); j++){
//              if(edges.get(i0).get(j) == i1){
//              	edge_converter_indices.add(j);
//              }
//            }
//                        
//            //Add tasks for each parrallel edge
//            task_buffer_new = new Vector<String>();
//            
//            for(int j=0; j<edge_converter_indices.size(); j++){
//              converter = converters.get(edge_converters.get(i0).get(edge_converter_indices.get(j)));
//              
//              for(int k=0; k<task_buffer.size(); k++){
//                task = task_buffer.get(k);
//                
//                if(converter.alias.isEmpty()){
//                  task += "\"" + converter.name + "\" ";
//                }else{
//                  task += converter.alias + " ";
//                }
//                
//                task += converter.input_fields.get(converter.inputs.indexOf(vertices.get(i0))) + " ";
//                task += vertices.get(i0) + " ";
//                task += converter.output_fields.get(converter.outputs.indexOf(vertices.get(i1))) + " ";
//                task += vertices.get(i1);
//                task += "\n";
//                
//                task_buffer_new.add(task);
//              }
//            }
//            
//            task_buffer = task_buffer_new;
//          }
//        }
//        
//        for(int i=0; i<task_buffer.size(); i++){
//          tasks.add(task_buffer.get(i));
//        }
//      }
//    }
//    
//    return tasks;
//  }
  
  /**
   * Get the index associated with the vertex having the given string representation.
   * @param string a string representation for the vertex
   * @return the index of the vertex
   */
  public int getVertexIndex(String string)
  {
  	int index = -1;
  	
    for(int i=0; i<vertices.size(); i++){
      if(vertices.get(i).toString().equals(string)){
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
  
//  /**
//   * Display results obtained from quality information such as optimal path, etc...
//   */
//  public void printQualityInfo()
//  {
//  	if(true){		//Get optimal single conversion
//	  	double maxr = 0;
//	  	int maxi = 0;
//	  	int maxj = 0;
//	  	
//	  	for(int i=0; i<edge_quality.size(); i++){
//	  		for(int j=0; j<edge_quality.get(i).size(); j++){
//		  	  if(edge_quality.get(i).get(j) > maxr){
//		  	  	maxr = edge_quality.get(i).get(j);
//		  	  	maxi = i;
//		  	  	maxj = j;
//		  	  }
//	  		}
//	  	}
//	  	  	
//	  	System.out.println("Optimal single conversion: \"" + converters.get(edge_converters.get(maxi).get(maxj)).name + "\" " + vertices.get(maxi) + "->" + vertices.get(edges.get(maxi).get(maxj)) + " (" + maxr + ")");
//  	}
//  	
//  	if(true){		//Get optimal format to convert to
//  		Pair<Vector<Integer>,Vector<Double>> tmpp;
//  		Vector<Integer> path;
//  		Vector<Double> quality;
//  		Vector<Double> mean_quality = new Vector<Double>();
//  		Vector<Integer> domain_count = new Vector<Integer>();
//  		double maxr = 0;
//  		int maxi = 0;
// 		
//  		for(int i=0; i<vertices.size(); i++){
//  			mean_quality.add(0.0);
//  			domain_count.add(0);
//  		}
//  		
//  		for(int i=0; i<vertices.size(); i++){
//  		  tmpp = getShortestWeightedPaths(i);
//  		  path = tmpp.first;
//  		  quality = tmpp.second;
//  		  
//  		  for(int j=0; j<vertices.size(); j++){
//  		  	if(i!=j && quality.get(j)>0){
//  		  		mean_quality.set(j, mean_quality.get(j)+quality.get(j));
//  		  		domain_count.set(j, domain_count.get(j)+1);
//  		  	}
//  		  }
//  		}
//  		
//  		for(int i=0; i<vertices.size(); i++){
//  			if(domain_count.get(i) > 0){
//  				mean_quality.set(i, mean_quality.get(i)/domain_count.get(i));
//  			}
//  		}
//  		
//  		for(int i=0; i<mean_quality.size(); i++){
//  			if(mean_quality.get(i) > maxr){
//  				maxr = mean_quality.get(i);
//  				maxi = i;
//  			}
//  		}
//  		
//  		System.out.println("Optimal format: " + vertices.get(maxi) + " (" + maxr + "), n=" + domain_count.get(maxi));
//  	}
//  }
  
  /**
	 * Follow a path back from target to source given the single source shortest paths from the source.
	 * @param source the source vertex
	 * @param target the destination vertex
	 * @param paths the single source shortest paths from the source
	 * @return the path from source to target represented as indices to vertices
	 */
	public static Vector<Integer> getPath(int source, int target, Vector<Integer> paths)
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
  	IOGraph<Data,Application> iograph = new IOGraph<Data,Application>(icr);
  	System.out.println("Vertices: " + iograph.vertices.size());
  	icr.close();
  }
}