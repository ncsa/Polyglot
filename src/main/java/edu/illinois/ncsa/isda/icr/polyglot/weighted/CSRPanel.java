package edu.illinois.ncsa.isda.icr.polyglot.weighted;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@SuppressWarnings("serial")
public class CSRPanel extends JPanel {
    private static Log    log     = LogFactory.getLog(CSRPanel.class);

    private IOGraph2Panel iographPanel;
    private IOGraph2      iograph = null;
    private URL           url;
    private JMenu         menu;

    public CSRPanel(URL url) {
        super(new BorderLayout());

        this.url = url;

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

        add(iographPanel, BorderLayout.CENTER);
    }

    public IOGraph2Panel getIOGraphPanel() {
        return iographPanel;
    }

    public IOGraph2 getIOGraph() {
        return iograph;
    }

    private void refresh() {
        try {
            refreshIOGraph();
            refreshParameters();
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
            int id = Integer.parseInt(line.substring(tmpi + 1, line.length() - 4));
            line = line.substring(0, tmpi);
            tmpi = line.lastIndexOf(" ");
            String output = line.substring(tmpi + 1, line.length());
            line = line.substring(0, tmpi);
            tmpi = line.lastIndexOf(" ");
            String input = line.substring(tmpi + 1, line.length());
            String application = line.substring(0, tmpi);
            iograph.addConversion(id, input, output, application);
        }
        br.close();

        iograph.complexity();
    }

    private void refreshParameters() throws IOException {
        // List of all possible conversions in CSR
        String line;
        BufferedReader br = new BufferedReader(new InputStreamReader(new URL(url, "get_parameters.php").openStream()));
        while ((line = br.readLine()) != null) {
            String[] parts = line.split("\t");
            if (parts.length != 4) {
                log.error("Invalid line : [" + line + "]");
                continue;
            }
            parts[3] = parts[3].substring(0, parts[3].length() - 4);
            iograph.addParameter(Integer.parseInt(parts[0]), parts[1], parts[2], parts[3]);
        }
        br.close();
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
