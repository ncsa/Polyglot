package kgm.image;

/**
 * A structure to store a 2D image pixel/point.
 */
public class Pixel implements Comparable
{
  public int x;
  public int y;
  public int rgb;
  public double value;
  
  public Pixel() {}
  
  /**
   * Class constructor.
   *  @param x the x coordinate
   *  @param y the y coordinate
   */
  public Pixel(int x, int y)
  {
    this.x = x;
    this.y = y;
    this.rgb = 0;
  }
  
  /**
   * Class constructor.
   *  @param x the x coordinate
   *  @param y the y coordinate
   *  @param rgb the color of the point
   */
  public Pixel(int x, int y, int rgb)
  {
    this.x = x;
    this.y = y;
    this.rgb = rgb;
  }
  
  /**
   * Class constructor.
   *  @param x the x coordinate
   *  @param y the y coordinate
   *  @param value the value of the point
   */
  public Pixel(int x, int y, double value)
  {
  	this.x = x;
  	this.y = y;
  	this.value = value;
  }
  
  /**
   * Class constructor.
   *  @param x the x coordinate
   *  @param y the y coordinate
   */
  public Pixel(double x, double y)
  {
    this.x = (int)Math.round(x);
    this.y = (int)Math.round(y);
    this.rgb = 0;
  }
  
  /**
   * Compare this point to another.
   *  @param o the point to compare to
   *  @return the result (-1=less, 0=equal, 1=greater)
   */
  public int compareTo(Object o)
  {
    Pixel v = (Pixel)o;
    
    if(x==v.x && y==v.y){
      return 0;
    }else{
      if(x < v.x){
        return -1;
      }else if(x > v.x){
        return 1;
      }else{
        if(y < v.y){
          return -1;
        }else{
          return 1;
        }
      }
    }
  }
}