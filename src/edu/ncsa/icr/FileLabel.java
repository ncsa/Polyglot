package edu.ncsa.icr;
import javax.swing.*;
import javax.swing.filechooser.*;
import java.awt.*;
import java.awt.event.*;
import sun.awt.shell.*;
import java.io.*;

public class FileLabel extends JLabel implements Comparable
{
	private File file;
	private int selection_color = 0x00aad1f3;
	
	public FileLabel(File file, MouseListener mouse_listener)
	{
		this.file = file;
		setText(""+file.getName());
		setVerticalTextPosition(JLabel.BOTTOM);
		setHorizontalTextPosition(JLabel.CENTER);
		addMouseListener(mouse_listener);
	}
	
	public void setSelectionColor(int selection_color)
	{
		this.selection_color = selection_color;
	}
	
	public void setDeselected()
	{
		setOpaque(false);		
	}
	
	public void setSelected()
	{
		setBackground(new Color(selection_color));
		setOpaque(true);		
	}
	
	public void setSmallIcon()
	{
		//setIcon(new JFileChooser().getIcon(file));
		setIcon(FileSystemView.getFileSystemView().getSystemIcon(file));
	}
	
	public void setLargeIcon()
	{
		try{
			ShellFolder shell_folder = ShellFolder.getShellFolder(file);
			setIcon(new ImageIcon(shell_folder.getIcon(true)));
		}catch(Exception e) {e.printStackTrace();}
	}
	
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