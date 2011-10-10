package edu.ncsa.icr.polyglot;

import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JApplet;
import javax.swing.JLabel;

/**
 * An applet to simply hold an IOGraphPanel.
 * 
 * @author Kenton McHenry, Rob Kooper
 */
public class IOGraph2Applet extends JApplet {
    /**
     * Initialize the applet.
     */
    public void init() {
        // parse the url
        URL url = null;
        if (getParameter("url") != null) {
            try {
                url = new URL(getParameter("url"));
            } catch (MalformedURLException e) {
                e.printStackTrace();
                add(new JLabel("Invalid url."));
                return;
            }
        } else {
            url = getDocumentBase();
        }

        add(new CSRPanel(url));
    }
}