package kgm.image;

/**
 * A structure to represent a weighted edge between two points.
 */
public class WeightedEdge implements Comparable
{
	public int p0;
	public int p1;
	public double w = 0;
	
	public WeightedEdge() {}
	
	/**
	 * Class constructor.
	 * @param p0 the starting vertex of the edge
	 * @param p1 the ending vertex of the edge
	 * @param w the weight of the edge
	 */
	public WeightedEdge(int p0, int p1, double w)
	{
		this.p0 = p0;
		this.p1 = p1;
		this.w = w;
	}
	
	/**
   * Compare this edge to another.
   *  @param o the edge to compare to
   *  @return the result (-1=less, 0=equal, 1=greater)
   */
  public int compareTo(Object o)
  {
    WeightedEdge e = (WeightedEdge)o;
    
    if(w==e.w){
      return 0;
    }else{
      if(w < e.w){
        return -1;
      }else{
        return 1;
      }
    }
  }
}