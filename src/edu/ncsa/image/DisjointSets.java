package edu.ncsa.image;
import java.util.*;

/**
 * An implementation of the disjoint sets ADT.
 */
public class DisjointSets
{
	private int n;
	private int[] parent;
	private int[] rank;
	private int[] size;
	private double[] value;
	
	/**
	 * Class constructor.
	 * @param n the total number of elements
	 */
	public DisjointSets(int n)
	{
		this.n = n;
		parent = new int[n];
		rank = new int[n];
		size = new int[n];
		value = new double[n];
		
		for(int i=0; i<n; i++){
			makeSet(i);
		}
	}
	
	/**
	 * Get the size of the specified set.
	 * @param x the element representing the desired set
	 * @return the size of this set
	 */
	public int getSize(int x)
	{
		return size[x];
	}
	
	/**
	 * Get the value associated with the specified set
	 * @param x the element representing the desired set
	 * @return the value associated with this set
	 */
	public double getValue(int x)
	{
		return value[x];
	}
	
	/**
	 * Initialize a set for the given element.
	 * @param x the element to initialize as a set
	 */
	public void makeSet(int x)
	{
		parent[x] = x;
		rank[x] = 0;
		size[x] = 1;
		value[x] = 0;
	}
	
	/**
	 * Find the set to which the given element belongs.
	 * @param x the element
	 * @return the element representing the set to which this element belongs
	 */
	public int find(int x)
	{
		if(parent[x] != parent[parent[x]]){
			parent[x] = find(parent[x]);
		}
		
		return parent[x];
	}
	
	/**
	 * Merge two sets.
	 * @param x an element representing a set
	 * @param y an element representing another set
	 * @param v a value to associate with this new set
	 */
	public void unionFind(int x, int y, double v)
	{
	  int xhat = find(x);
	  int yhat = find(y);

	  if(rank[xhat] > rank[yhat]){
	    parent[yhat] = xhat;
	    size[xhat] += size[yhat];
	    value[xhat] = v;
	  }else{
	    parent[xhat] = yhat;
	    if(rank[xhat] == rank[yhat]) rank[yhat] = rank[yhat] + 1;
	    size[yhat] += size[xhat];
	    value[yhat] = v;
	  }
	}
	
	/**
	 * Merge sets that have small sizes.
	 * @param edges the topology of the sets
	 * @param min_size the minimum allowed size for a set
	 */
	public void mergeSmallSets(Vector<WeightedEdge> edges, double min_size)
	{
		Vector<Vector<WeightedEdge>> set_edges = new Vector<Vector<WeightedEdge>>();
		boolean[] small = new boolean[n];
		int s1, s2, minj;
		int count = 1;
		double mind;
		
		while(count > 0){
			count = 0;
			
			//Mark small regions
  		for(int i=0; i<n; i++){
  			small[i] = false;
  		}
  		
  		for(int i=0; i<n; i++){
  			s1 = find(i);
  			
  			if(size[s1] < min_size){
  				small[s1] = true;
  				count++;
  			}
  		}
  			  		
  		System.out.println("Merging " + count + " small regions...");
  		
  		if(count > 0){
	  		//Store only relevent edges for each set (i.e. connecting it to another)
	  		set_edges.clear();
	  	  for(int i=0; i<n; i++) set_edges.add(new Vector<WeightedEdge>());
	  	  
	  	  for(int i=0; i<edges.size(); i++){
	  	    s1 = find(edges.get(i).p0);
	  	    s2 = find(edges.get(i).p1);

	  	    if(s1 != s2){
	  	      if(small[s1] || small[s2]){
	  	        set_edges.get(s1).add(edges.get(i));
	  	        set_edges.get(s2).add(edges.get(i));
	  	      }
	  	    }
	  	  }

	  	  //Merge small regions with best neighbor
	  	  for(int i=0; i<n; i++){
	  	    if(small[i]){
	  	      mind = Double.MAX_VALUE;
	  	      minj = -1;
	  	      
	  	      for(int j=0; j<set_edges.get(i).size(); j++){
	  	        if(set_edges.get(i).get(j).w < mind){
	  	          mind = set_edges.get(i).get(j).w;
	  	          minj = j;
	  	        }
	  	      }
	  	      	
	  	      if(minj >= 0){
	  	        s1 = find(set_edges.get(i).get(minj).p0);
	  	        s2 = find(set_edges.get(i).get(minj).p1);
	  	        small[s1] = false;
	  	        small[s2] = false;
	  	        unionFind(s1, s2, set_edges.get(i).get(minj).w);
	  	      }
	  	    }
  	    }
  	  }
		}
	}
}