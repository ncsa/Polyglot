package edu.ncsa.icr;
import javax.swing.*;
import javax.swing.filechooser.*;
import java.awt.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import sun.awt.shell.*;
import java.io.*;

/**
 * A JLabel with an associated file.
 * @author Kenton McHenry
 */
public class FileLabel extends JLabel implements Comparable
{
	private File file;
	private int selection_color = 0x00aad1f3;
	
	/**
	 * Class constructor.
	 * @param file the file this instance represents
	 * @param mouse_listener the mouse listener this instance should use
	 */
	public FileLabel(File file, MouseListener mouse_listener)
	{
		this(file, file.getName(), mouse_listener);
	}
	
	/**
	 * Class constructor.
	 * @param file the file this instance represents
	 * @param text the text to associate with this file
	 * @param mouse_listener the mouse listener this instance should use
	 */
	public FileLabel(File file, String text, MouseListener mouse_listener)
	{
		this.file = file;
		setText(text);
		setVerticalTextPosition(JLabel.BOTTOM);
		setHorizontalTextPosition(JLabel.CENTER);
		addMouseListener(mouse_listener);
	}
	
	/**
	 * Get the stored file.
	 * @return the file
	 */
	public File getFile()
	{
		return file;
	}

	/**
	 * Set the color when this label is selected.
	 * @param selection_color the selection color represented as an ARGB integer
	 */
	public void setSelectionColor(int selection_color)
	{
		this.selection_color = selection_color;
	}
	
	/**
	 * Set this label to deselected.
	 */
	public void setDeselected()
	{
		setOpaque(false);		
	}
	
	/**
	 * Set this label to selected.
	 */
	public void setSelected()
	{
		setBackground(new Color(selection_color));
		setOpaque(true);		
	}
	
	/**
	 * Set the icon to the associated files small icon.
	 */
	public void setSmallIcon()
	{
		//setIcon(new JFileChooser().getIcon(file));
		setIcon(FileSystemView.getFileSystemView().getSystemIcon(file));
	}
	
	/**
	 * Set the icon to the associated files large icon.
	 */
	public void setLargeIcon()
	{
		try{
			ShellFolder shell_folder = ShellFolder.getShellFolder(file);
			setIcon(new ImageIcon(shell_folder.getIcon(true)));
		}catch(Exception e) {e.printStackTrace();}
	}
	
	/**
	 * Compare to another file (based on associated files name).
	 * @param object the object to compare to
	 * @return 0 if equal, -1 if less than, 1 if greater than
	 */
	public int compareTo(Object object)
	{		
		if(this == object){
			return 0;
		}else if(object instanceof FileLabel){
			return file.getName().compareTo(((FileLabel)object).file.getName());
		}else{
			return -1;
		}
	}
}