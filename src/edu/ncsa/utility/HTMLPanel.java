package edu.ncsa.utility;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.html.*;

/**
 * A panel built for easily displaying html.
 * @author Kenton McHenry
 */
public class HTMLPanel extends JPanel
{
  private JEditorPane ep;
  private JScrollPane sp;
  private StyleSheet css;  
  private String text = "";
  private int horizontal_offset = 8;
  private int vertical_offset = 2;
  private int previous_scroll_position = 0;
  
  /**
   * Class constructor.  Sets margins, editor and scroll pane.
   */
  public HTMLPanel()
  {
    super();
    
    ep = new JEditorPane("text/html", text);
    ep.setEditable(false);
    ep.setDoubleBuffered(true);
    ep.setBorder(new EmptyBorder(vertical_offset, horizontal_offset, vertical_offset, horizontal_offset));

    /*
    css = new StyleSheet();
    css.addRule("BODY{color:#ff0000;}");
    ((HTMLEditorKit)ep.getEditorKit()).setStyleSheet(css);
    */
    
    sp = new JScrollPane(ep);
    sp.setBackground(Color.white);    
    sp.setBorder(new EmptyBorder(0, 0, 0, 0));
    this.setLayout(new BorderLayout());
    this.add(sp, BorderLayout.CENTER);
  }
  
  /**
   * Set the size of this panel.
   * @param width the panels width
   * @param height the panels height
   */
  public void setSize(int width, int height)
  {
    super.setSize(width, height);
    ep.setSize(width, height);
    sp.setSize(width, height);
  }
  
  /**
   * Set the size of this panel.
   * @param d the dimensions of the panel
   */
  public void setPreferredSize(Dimension d)
  {    
    super.setPreferredSize(d);
    ep.setPreferredSize(d);
    sp.setPreferredSize(d);
  }
  
  /**
   * Sets left margin.
   * @param x the offset for the left margin
   */
  public void setLeftOffset(int x)
  {
    ep.setBorder(new EmptyBorder(vertical_offset, x, vertical_offset, horizontal_offset));
  }
  
  /**
   * Sets top and left margins.
   * @param x the offset for the left margin
   * @param y the offset for the top margin
   */
  public void setTopLeftOffset(int x, int y)
  {
    ep.setBorder(new EmptyBorder(y, x, vertical_offset, horizontal_offset));
  }
  
  /**
   * Sets horizontal margins.
   *  @param x the new value for the horizontal margin (in pixels)
   */
  public void setHorizontalOffset(int x)
  {
    horizontal_offset = x;
    ep.setBorder(new EmptyBorder(vertical_offset, horizontal_offset, vertical_offset, horizontal_offset));
  }
  
  /**
   * Set the background color of this panel.
   *  @param c the desired background color
   */
  public void setBackground(Color c)
  {
    super.setBackground(c);
    if(ep != null) ep.setBackground(c);
    if(sp != null) sp.setBackground(c);
  }
  
  /**
   * Set the text to display in the panel.
   *  @param s the desired text
   */
  public void setText(String s)
  {
    if(!s.equals(text)){
      text = s;
      ep.setText(text);
      ep.setCaretPosition(0);
    }
  }
  
  /**
   * Add text to the panel (caret will be at the end!).
   *  @param s the text to add
   */
  public void addText(String s)
  {
    text += s;
    
    try{
      ep.setCaretPosition(ep.getDocument().getLength());
    	((HTMLDocument)ep.getDocument()).insertAfterEnd(((HTMLDocument)ep.getDocument()).getCharacterElement(ep.getCaretPosition()), s);
    }catch(Exception e) {e.printStackTrace();}
    
    ep.setCaretPosition(ep.getDocument().getLength());
  }
  
  /**
   * Add text to the panel but don't update.
   *  @param s the text to add
   */
  public void putText(String s)
  {
    text += s;
  }
  
  /**
   * Update the text in the panel (caret position does not move!).
   */
  public void flush()
  {
    int caret_position = ep.getCaretPosition();
    
    ep.setText(text);
    ep.setCaretPosition(caret_position);
  }
  
  /**
   * Clear all text.
   */
  public void clear()
  {
  	previous_scroll_position = sp.getVerticalScrollBar().getValue();

    text = "";
    ep.setText(text);
    ep.setCaretPosition(0);
  }
  
  /**
   * Set the vertical scroll bar to the position it was at before the pane was cleared.
   */
  public void setPreviousScrollPosition()
  {  	
  	sp.updateUI();
  	
  	SwingUtilities.invokeLater(new Runnable(){
  		public void run(){
  			sp.getVerticalScrollBar().setValue(previous_scroll_position);
  		}
  	});
  }
  
  /**
   * Get the currently displayed text.
   *  @return the currently displayed text
   */
  public String getText()
  {
    return text; 
  }
}