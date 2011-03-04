package edu.ncsa.utility;
import java.io.*;

/**
 * A simple structure to hold two objects.
 * @param <T1> the type of the first object
 * @param <T2> the type of the second object
 * @author Kenton McHenry
 */
public class Pair<T1,T2> implements  Comparable, Serializable
{
	public static final long serialVersionUID = 1L;
  public T1 first;
  public T2 second;
  
  public Pair() {}
  
  /**
   * Class constructor.
   *  @param a the first value
   *  @param b the second value
   */
  public Pair(T1 a, T2 b)
  {
    first = a;
    second = b;
  }
  
  /**
   * Compare this pair to another based on the first element.
   * Note: currently only supports doubles as the first element!
   *  @param o the pair to compare to
   *  @return the result (-1=less, 0=equal, 1=greater)
   */
  public int compareTo(Object o)
  {
  	if(o instanceof Pair){
  		if(((Pair)o).first instanceof Double){
  			return ((Double)first).compareTo((Double)((Pair)o).first);
  		}else if(((Pair)o).first instanceof String){
    		return ((String)first).compareTo((String)((Pair)o).first);
  		}else{
  			return -1;
  		}
  	}else{
  		return -1;
  	}
  }
}