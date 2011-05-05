package kgm.utility;
import java.awt.*;

/**
  * A modified version of FlowLayout that allows containers using this Layout 
  * to behave in a reasonable manner when placed inside a JScrollPane. 
  * @author Babu Kalakrishnan
  * @author Kenton McHenry
  */
public class FlowLayout extends java.awt.FlowLayout
{
	private boolean HORIZONTAL_LAYOUT = true;
	
	/**
	 * Class constructor.
	 */
	public FlowLayout()
	{
		super();
	}
	
	/**
	 * Class constructor
	 * @param align
	 */
	public FlowLayout(int align)
	{
		super(align);
	}
	
	/**
	 * Class constructor.
	 * @param align
	 * @param hgap
	 * @param vgap
	 */
	public FlowLayout(int align, int hgap, int vgap)
	{
		super(align, hgap, vgap);
	}
	
	/**
	 * Set whether components should be display along a horizontal layout or not.
	 * @param value true if components should be displayed in a horizontal layout
	 */
	public void setHorizontalLayout(boolean value)
	{
		HORIZONTAL_LAYOUT = value;
	}
	
	/**
	 * Get the size of largest component, so we can resize it in
	 * either direction with something like a split-pane.
	 * @param target
	 */
	public Dimension minimumLayoutSize(Container target)
	{
		int minx = Integer.MAX_VALUE;
		int miny = Integer.MIN_VALUE;
		boolean FOUND = false;
		int count;
		
		synchronized(target.getTreeLock()){
			count = target.getComponentCount();
	
			for(int i=0; i<count; i++){
				Component component = target.getComponent(i);
				
				if(component.isVisible()){
					FOUND = true;
					Dimension dimension = component.getPreferredSize();
					minx = Math.min(minx, dimension.width);
					miny = Math.min(miny, dimension.height);
				}
			}
			
			if(FOUND){
				return new Dimension(minx, miny);
			}else{
				return new Dimension(0, 0);
			}
		}
	}

	/**
	 * Get the preferred layout size.
	 * @param target
	 */
	public Dimension preferredLayoutSize(Container target)
	{
		synchronized(target.getTreeLock()){
			int hgap = getHgap();
			int vgap = getVgap();
			int w = target.getWidth();
	
			//Let this behave like a regular FlowLayout (single row) if the container hasn't been assigned any size yet
			if(w == 0) w = Integer.MAX_VALUE;
	     
			Insets insets = target.getInsets();
			if(insets == null) insets = new Insets(0, 0, 0, 0);
	     
			int required_width = 0;
			int maxwidth = w - (insets.left + insets.right + hgap * 2);
			int count = target.getComponentCount();
			int x = 0;
			int y = insets.top + vgap;		//FlowLayout starts by adding vgap, so do that here too.
			int row_height = 0;
	     
			for(int i=0; i<count; i++){
				Component c = target.getComponent(i);

				if(c.isVisible()){
					Dimension d = c.getPreferredSize();
					
					if((x == 0) || ((x + d.width)<=maxwidth)){	//Fits in current row
						if(x > 0) x += hgap;
						x += d.width;
						row_height = Math.max(row_height, d.height);
					}else{																			//Start of new row
						x = d.width;
						y += vgap + row_height;
						row_height = d.height;
					}
					
					required_width = Math.max(required_width, x);
				}
			}

			y += row_height;
			y += insets.bottom;
			
			return new Dimension(required_width+insets.left+insets.right, y);
		}
	}
	
	/**
	 * Centers the elements in the specified row, if there is any slack.
	 * @param target the component which needs to be moved
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param width the width dimensions
	 * @param height the height dimensions
	 * @param rowStart the beginning of the row
	 * @param rowEnd the the ending of the row
	 * @param ttb true if top to bottom
	 */
	private void moveComponents(Container target, int x, int y, int width, int height, int rowStart, int rowEnd, boolean ttb)
	{
		synchronized(target.getTreeLock()){
			switch(getAlignment()){
			case LEFT:
				y += ttb ? 0 : height;
				break;
			case CENTER:
				y += height / 2;
				break;
			case RIGHT:
				y += ttb ? height : 0;
				break;
			case LEADING:
				break;
			case TRAILING:
				y += height;
				break;
			}
			
			for(int i=rowStart; i<rowEnd; i++){
				Component component = target.getComponent(i);
				
				if(component.isVisible()){
					if(ttb){
						component.setLocation(x+(width-component.getWidth())/2, y);
					}else{
						component.setLocation(x+(width-component.getWidth())/2, target.getHeight()-y-component.getHeight());
					}
					
					y += component.getHeight() + getVgap();
				}
			}
		}
	}
	
	/*
	//Default horizontal version
	private void moveComponents(Container target, int x, int y, int width, int height, int rowStart, int rowEnd, boolean ltr)
	{
		synchronized(target.getTreeLock()){
			switch(getAlignment()){
			case LEFT:
				x += ltr ? 0 : width;
				break;
			case CENTER:
				x += width / 2;
				break;
			case RIGHT:
				x += ltr ? width : 0;
				break;
			case LEADING:
				break;
			case TRAILING:
				x += width;
				break;
			}
			
			for(int i=rowStart; i<rowEnd; i++){
				Component component = target.getComponent(i);
				
				if(component.isVisible()){
					if(ltr){
						component.setLocation(x, y+(height-component.getHeight())/2);
					}else{
						component.setLocation(target.getWidth()-x-component.getWidth(), y+(height-component.getHeight())/2);
					}
					
					x += component.getWidth() + getHgap();
				}
			}
		}
	}
	*/
	
	/**
	* Lays out the container. This method lets each visible component take
	* its preferred size by reshaping the components in the
	* target container in order to satisfy the alignment of
	* this FlowLayout object.
	* @param target the specified component being laid out
	* @see Container
	* @see java.awt.Container#doLayout
	*/
	public void layoutContainer(Container target)
	{
		synchronized(target.getTreeLock()){
			if(HORIZONTAL_LAYOUT){
				super.layoutContainer(target);
			}else{
				Insets insets = target.getInsets();
				int maxheight = target.getHeight() - (insets.top+insets.bottom+getVgap()*2);
				int nmembers = target.getComponentCount();
				int x = insets.left + getHgap();
				int y = 0;
				int roww = 0;
				int start = 0;
	
				boolean ttb = target.getComponentOrientation().isLeftToRight();
				
				for(int i=0; i<nmembers; i++){
					Component component = target.getComponent(i);
					
					if(component.isVisible()){
						Dimension dimension = component.getPreferredSize();
						component.setSize(dimension.width, dimension.height);
	
						if((y==0) || ((y+dimension.height)<=maxheight)){
							if(y > 0) y += getVgap();
							y += dimension.height;
							roww = Math.max(roww, dimension.width);
						}else{
							moveComponents(target, x, insets.top+getVgap(), roww, maxheight-y, start, i, ttb);
							x += getHgap() + roww;
							y = dimension.height;
							roww = dimension.width;
							start = i;
						}
					}
				}
				
				moveComponents(target, x, insets.top+getVgap(), roww, maxheight-y, start, nmembers, ttb);
			}
		}
	}
	
	/*
	//Default horizontal version
	public void layoutContainer(Container target)
	{
		synchronized(target.getTreeLock()){
			if(HORIZONTAL_LAYOUT){
				super.layoutContainer(target);
			}else{
				Insets insets = target.getInsets();
				int maxwidth = target.getWidth() - (insets.left+insets.right+getHgap()*2);
				int nmembers = target.getComponentCount();
				int x = 0;
				int y = insets.top + getVgap();
				int rowh = 0;
				int start = 0;
	
				boolean ltr = target.getComponentOrientation().isLeftToRight();
	
				for(int i=0; i<nmembers; i++){
					Component component = target.getComponent(i);
					
					if(component.isVisible()){
						Dimension dimension = component.getPreferredSize();
						component.setSize(dimension.width, dimension.height);
	
						if((x==0) || ((x+dimension.width)<=maxwidth)){
							if(x > 0) x += getHgap();
							x += dimension.width;
							rowh = Math.max(rowh, dimension.height);
						}else{
							moveComponents(target, insets.left+getHgap(), y, maxwidth-x, rowh, start, i, ltr);
							x = dimension.width;
							y += getVgap() + rowh;
							rowh = dimension.height;
							start = i;
						}
					}
				}
				
				moveComponents(target, insets.left+getHgap(), y, maxwidth-x, rowh, start, nmembers, ltr);
			}
		}
	}
	*/
}