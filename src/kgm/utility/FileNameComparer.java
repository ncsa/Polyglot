package kgm.utility;
import java.util.*;

/**
 * A class implementing a comparator for file names.
 * @author Kenton McHenry
 */
public class FileNameComparer implements Comparator
{
  /**
   * The method to compare two file names.
   *  @param obj1 the first object to compare
   *  @param obj2 the second object to compare
   *  @return the result (-1=less, 0=equal, 1=greater)
   */
  public int compare(Object obj1, Object obj2)
  {
    String str1 = (String)obj1;
    String str2 = (String)obj2;
    int tmpi;
    
    tmpi = str1.lastIndexOf('/');
    if(tmpi >= 0) str1 = str1.substring(tmpi+1);
    tmpi = str2.lastIndexOf('/');
    if(tmpi >= 0) str2 = str2.substring(tmpi+1);
    
    return str1.compareTo(str2);
  }
}