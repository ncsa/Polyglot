package edu.ncsa.icr.polyglot;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import javax.swing.*;

/**
 * An applet to simply hold an IOGraphPanel.
 * 
 * @author Kenton McHenry, Rob Kooper
 */
public class IOGraph2Applet extends JApplet {
    private IOGraph2Panel iographPanel;
    private IOGraph2      iograph = null;
    private URL           url;
    private JMenu         menu;

    /**
     * Initialize the applet.
     */
    public void init() {
        // parse the url
        if (getParameter("url") != null) {
            try {
                url = new URL(getParameter("url"));
            } catch (MalformedURLException e) {
                e.printStackTrace();
                add(new JLabel("Invalid url."));
                return;
            }
        } else {
            try {
                url = new URL("http://isda.ncsa.illinois.edu/NARA/CSR/php/search/");
            } catch (MalformedURLException e) {
                e.printStackTrace();
                add(new JLabel("Invalid url."));
                return;
            }
        }

        iograph = new IOGraph2();
        iographPanel = new IOGraph2Panel(iograph);

        iographPanel.getPopupMenu().addSeparator();
        iographPanel.getPopupMenu().add(new JMenuItem(new AbstractAction("Refresh") {
            public void actionPerformed(ActionEvent e) {
                refresh();
            }
        }));

        menu = new JMenu("Weights");
        iographPanel.getPopupMenu().add(menu);
        refresh();

        add(iographPanel);
    }

    private void refresh() {
        try {
            refreshIOGraph();
            refreshMeasurements();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void refreshIOGraph() throws IOException, PolyglotException {
        iograph.reset();

        // List of all possible conversions in CSR
        String line;
        BufferedReader br = new BufferedReader(new InputStreamReader(new URL(url, "get_conversions.php").openStream()));
        while ((line = br.readLine()) != null) {
            int tmpi = line.lastIndexOf(" ");
            String output = line.substring(tmpi + 1, line.length() - 4);
            line = line.substring(0, tmpi);
            tmpi = line.lastIndexOf(" ");
            String input = line.substring(tmpi + 1, line.length());
            String application = line.substring(0, tmpi);
            iograph.addConversion(input, output, application);
        }
        br.close();

        iograph.complexity();
    }

    private void refreshMeasurements() throws IOException {
        menu.removeAll();

        // no weights used when computing
        ButtonGroup group = new ButtonGroup();
        JRadioButtonMenuItem mi = new JRadioButtonMenuItem(new AbstractAction("No weights") {
            public void actionPerformed(ActionEvent e) {
                try {
                    iograph.resetWeights();
                    iographPanel.showEdgeQuality(false);
                    iographPanel.useWeights(false);
                } catch (PolyglotException e1) {
                    e1.printStackTrace();
                }
            }
        });
        mi.setSelected(true);
        menu.add(mi);
        group.add(mi);

        // all known measurements in CSR
        String line;
        BufferedReader br = new BufferedReader(new InputStreamReader(new URL(url, "get_measures.php").openStream()));
        while ((line = br.readLine()) != null) {
            String[] pieces = line.split("\t");
            if (pieces.length != 5) {
                continue;
            }
            final String menuitem = pieces[1];
            mi = new JRadioButtonMenuItem(new AbstractAction(menuitem) {
                public void actionPerformed(ActionEvent e) {
                    try {
                        iographPanel.showEdgeQuality(true);
                        iograph.resetWeights();

                        URL m_url = new URL(url, "get_weights_average.php?measure=" + URLEncoder.encode(menuitem, "UTF8"));
                        BufferedReader br = new BufferedReader(new InputStreamReader(m_url.openStream()));
                        String line;
                        while ((line = br.readLine()) != null) {
                            if (line.toLowerCase().endsWith("<br>")) {
                                line = line.substring(0, line.length() - 4);
                            }
                            String[] parts = line.split("\t");
                            if (parts.length != 4) {
                                System.err.println("Bad line : " + line);
                            } else {
                                try {
                                    Double w = Math.abs(Double.parseDouble(parts[3]));
                                    iograph.setWeight(parts[1], parts[2], parts[0], w);
                                } catch (NumberFormatException exc) {
                                    System.err.println("Bad line : " + line);
                                    exc.printStackTrace();
                                }
                            }
                        }
                        br.close();
                        iographPanel.useWeights(true);
                    } catch (Exception exc) {
                        exc.printStackTrace();
                    }
                }
            });
            mi.setSelected(false);
            menu.add(mi);
            group.add(mi);
        }
        br.close();
    }
}