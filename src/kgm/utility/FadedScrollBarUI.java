package kgm.utility;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;

/**
 * A replacement for BasicScrollBarUI with a faded appearance.
 * @author Kenton McHenry
 */
public class FadedScrollBarUI extends BasicScrollBarUI
{
  public static int size = 15;    //The preferred the width and height of the arrowbuttons
  public static int alpha = 180;
  
  /**
   * A replacement for BasicArrowButton that returns the set size.
   */
  private class MyArrowButton extends BasicArrowButton
  {
    public MyArrowButton(int orientation) {super(orientation);}
    public Dimension getPreferredSize() {return new Dimension(size, size);}
  }
    
  /**
   * Get the preferred size of this component.
   *  @param c
   *  @return the size
   */
  public Dimension getPreferredSize(JComponent c)
  {
    return (scrollbar.getOrientation()==JScrollBar.VERTICAL) ?  new Dimension(size, 48) : new Dimension(48, size);
  }
  
  /**
   * Draw the slider.
   *  @param g the graphics context to draw to
   *  @param c
   *  @param bounds the bounds of this component
   */
  public void paintThumb(Graphics g, JComponent c, Rectangle bounds)
  {
    Graphics2D g2 = (Graphics2D)g;
    GradientPaint gradientColour = new GradientPaint(0, 1, new Color(255, 255, 255, alpha), 15, 1, new Color(0xdd, 0xdd, 0xdd, alpha));

    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setPaint(gradientColour);
    g2.fill(new RoundRectangle2D.Double(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(), 5, 5));

    //Draw border
    if(true){
      g2.setPaint(new Color(0x00dddddd));
      g2.draw(new RoundRectangle2D.Double(bounds.getX(), bounds.getY(), bounds.getWidth()-2, bounds.getHeight()-1, 5, 5));
    }
    
    //Draw bars
    if(true){
      //Black bar one, two, and three    
      g2.setPaint(Color.black);
      g2.draw(new Line2D.Double(bounds.getX()+5, bounds.getCenterY()-2, bounds.getWidth()-5, bounds.getCenterY()-2));
      g2.draw(new Line2D.Double(bounds.getX()+5, bounds.getCenterY(), bounds.getWidth()-5, bounds.getCenterY()));
      g2.draw(new Line2D.Double(bounds.getX()+5, bounds.getCenterY()+2, bounds.getWidth()-5, bounds.getCenterY()+2));
      
      //White bar one, two, and three
      g2.setPaint(Color.white);      
      g2.draw(new Line2D.Double(bounds.getX()+4, bounds.getCenterY()-1, bounds.getWidth()-6, bounds.getCenterY()-1));
      g2.draw(new Line2D.Double(bounds.getX()+4, bounds.getCenterY()+1, bounds.getWidth()-6, bounds.getCenterY()+1));
      g2.draw(new Line2D.Double(bounds.getX()+4, bounds.getCenterY()+3, bounds.getWidth()-6, bounds.getCenterY()+3));
    }
  }

  /**
   * Draw the track.
   *  @param g the graphics context to draw to
   *  @param c
   *  @param bounds the bounds of this component
   */
  protected void paintTrack(Graphics g, JComponent c, Rectangle bounds)
  {
    Graphics2D g2 = (Graphics2D)g;
    GradientPaint gradientColour = new GradientPaint(0, 1, Color.lightGray, 15, 1, Color.white);

    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setPaint(new Color(0xfa, 0xfa, 0xfa, alpha));
    g2.fill(new RoundRectangle2D.Double(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(), 0, 0));

    //Draw border
    if(false){
      g2.setPaint(Color.black);
      g2.draw(new RoundRectangle2D.Double(bounds.getX(), bounds.getY(), bounds.getWidth()-2, bounds.getHeight()-1, 0, 0));
    }
  }
  
  public static ComponentUI createUI(JComponent c) {return new FadedScrollBarUI();}  
  public JButton createDecreaseButton(int orientation) {return new MyArrowButton(orientation);}
  public JButton createIncreaseButton(int orientation) {return new MyArrowButton(orientation);}
}