package edu.ncsa.utility;
import java.io.*;

/**
 * A simple structure to hold three objects.
 * @param <T1> the type of the first object
 * @param <T2> the type of the second object
 * @param <T3> the type of the third object
 * @author Kenton McHenry
 */
public class Triple<T1,T2,T3> implements  Comparable, Serializable
{
	public static final long serialVersionUID = 1L;
  public T1 first;
  public T2 second;
  public T3 third;
  
  public Triple() {}
  
  /**
   * Class constructor.
   *  @param a the first value
   *  @param b the second value
   *  @param c the third value
   */
  public Triple(T1 a, T2 b, T3 c)
  {
    first = a;
    second = b;
    third = c;
  }
  
  /**
   * Compare this triple to another based on the first element.
   * Note: currently only supports doubles as the first element!
   *  @param o the pair to compare to
   *  @return the result (-1=less, 0=equal, 1=greater)
   */
  public int compareTo(Object o)
  {
  	if(o instanceof Triple && ((Triple)o).first instanceof Double){
  		return ((Double)first).compareTo((Double)((Triple)o).first);
  	}else{
  		return 0;
  	}
  }
}