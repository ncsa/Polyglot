package edu.ncsa.icr.polyglot;
import java.util.*;

/**
 * A convenient structure for holding vertex information when displaying an IOGraph.
 */
public class Point2D implements Comparable
{
  public String text;
  public int x;
  public int y;
    
  /**
   * Class constructor.
   * @param text the text to display with this vertex
   */
  public Point2D(String text)
  {
    this.text = text;
    x = 0;
    y = 0;
  }

	/**
	 * Compare this object to another object.
	 * @param object the object to compare to
	 * @return 0 if the same, -1 if less, and 1 if greater
	 */
	public int compareTo(Object object)
	{
		if(this == object){
			return 0;
		}else if(object instanceof Point2D){
			return text.compareTo(((Point2D)object).text);
		}else{
			return -1;
		}
	}
	
  /**
   * Create vertices from a vector names.
   * @param strings the vector of names
   * @return the vector of vertices
   */
  public static Vector<Point2D> getVertices(Vector<String> strings)
  {
  	Vector<Point2D> vertices = new Vector<Point2D>();
  	
  	for(int i=0; i<strings.size(); i++){
      vertices.add(new Point2D(strings.get(i)));
  	}
  	
  	return vertices;
  }
  
  /**
   * Set vertex positions to form a circular graph.
   * @param vertices the vertices
   * @param offset the initial offset angle to start placing vertices (in degrees)
   */
  public static void setCircularGraph(Vector<Point2D> vertices, int width, int height, double offset)
  {
  	Vector<Point2D> sorted_vertices = new Vector<Point2D>(vertices);
  	int half_width = width / 2;
  	int half_height = height / 2;
  	double radius;
  	
  	Collections.sort(sorted_vertices);
  	offset = Math.PI * offset / 180.0;
  	
  	for(int i=0; i<sorted_vertices.size(); i++){
      if(i%2 == 0){
        radius = 0.81;
      }else{
        radius = 0.9;
      }
      
  		sorted_vertices.get(i).x = (int)Math.round(Math.cos((2.0*Math.PI*i)/sorted_vertices.size()+offset)*radius*half_width + half_width);
  		sorted_vertices.get(i).y = (int)Math.round(Math.sin((2.0*Math.PI*i)/sorted_vertices.size()+offset)*radius*half_height + half_height);
  	}
  }
}