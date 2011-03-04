package edu.ncsa.utility;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;

/**
 * A panel built for easily displaying text.
 * @author Kenton McHenry
 */
public class TextPanel extends JPanel
{
  JTextPane tp;
  JScrollPane sp;
  SimpleAttributeSet as;
  String text = "";
  
  /**
   * Class constructor.  Sets margins, font, and scroll pane.
   */
  public TextPanel()
  {
    super();
    
    tp = new JTextPane();
    tp.setEditable(false);
    tp.setBorder(new EmptyBorder(0, 5, 0, 5));  //Add padding around text
    
    as = new SimpleAttributeSet();
    StyleConstants.setFontFamily(as, "Monospaced");
    StyleConstants.setFontSize(as, 12);
    StyleConstants.setBold(as, true);  
    tp.getStyledDocument().setParagraphAttributes(0, 0, as, true);
    
    sp = new JScrollPane(tp);
    sp.setBorder(new EmptyBorder(0, 0, 0, 0));
    this.setLayout(new BorderLayout());
    this.add(sp, BorderLayout.CENTER);
  }
  
  /**
   * Center the text in this panel.
   *  @param CENTER if true text will be centered
   */
  public void alignCenter(boolean CENTER)
  {
    if(CENTER){
      StyleConstants.setAlignment(as, StyleConstants.ALIGN_CENTER);
    }else{
      StyleConstants.setAlignment(as, StyleConstants.ALIGN_LEFT);
    }
    
    tp.getStyledDocument().setParagraphAttributes(0, 0, as, true);  
  }
  
  /**
   * Set the text to display in the panel.
   *  @param s the desired text
   */
  public void setText(String s)
  {
    if(!s.equals(text)){
      text = s;
      tp.setText(text);
      tp.setCaretPosition(0);
    }
  }
}