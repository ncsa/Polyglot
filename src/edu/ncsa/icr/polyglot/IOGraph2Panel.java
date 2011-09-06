package edu.ncsa.icr.polyglot;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Draw the IOGraph in a panel as well as the list of applications and the path
 * when a source and target vertex are selected.
 * 
 * @author Rob Kooper
 * 
 */
public class IOGraph2Panel extends JPanel {
    private static final long        serialVersionUID = 1L;
    private static Log               log              = LogFactory.getLog(IOGraph2Panel.class);

    private IOGraph2                 iograph          = null;

    private SortedMap<String, Point> vertexMap        = new TreeMap<String, Point>();

    private String                   source           = null;
    private String                   target           = null;
    private List<Conversion2>        path             = new ArrayList<Conversion2>();
    private boolean                  showRange        = false;
    private boolean                  showDomain       = false;
    private boolean                  showEdgeQuality  = false;
    private boolean                  useWeights       = false;

    private JPopupMenu               popupMenu        = null;
    private Point                    clicked          = new Point(0, 0);
    private JLabel                   lblPath;

    @SuppressWarnings("serial")
    public IOGraph2Panel(IOGraph2 iograph) {
        super(new BorderLayout());
        this.iograph = iograph;

        // create layout
        final JList lstApplications = new JList(new AbstractListModel() {
            public Object getElementAt(int index) {
                List<String> applications = new ArrayList<String>(IOGraph2Panel.this.iograph.getApplications());
                Collections.sort(applications);
                return applications.get(index);
            }

            public int getSize() {
                return IOGraph2Panel.this.iograph.getApplications().size();
            }
        });
        lstApplications.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                Set<String> apps = new HashSet<String>();
                for (Object o : lstApplications.getSelectedValues()) {
                    apps.add(o.toString());
                }
                IOGraph2Panel.this.iograph.setValidApplications(apps);
                vertexMap.clear();
                compute();
            }
        });

        lblPath = new JLabel("No path selected.", JLabel.CENTER);

        JPanel pnl = new JPanel(new BorderLayout());
        pnl.add(new IOGraph2Component(), BorderLayout.CENTER);
        pnl.add(lblPath, BorderLayout.SOUTH);
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(lstApplications), pnl);
        splitPane.setDividerLocation(200);
        add(splitPane, BorderLayout.CENTER);
    }

    public JPopupMenu getPopupMenu() {
        return popupMenu;
    }

    public void showEdgeQuality(boolean showEdgeQuality) {
        this.showEdgeQuality = showEdgeQuality;
        repaint();
    }

    public void useWeights(boolean useWeights) {
        this.useWeights = useWeights;
        vertexMap.clear();
        compute();
    }

    public void layoutVertices() {
        vertexMap.clear();
        repaint();
    }

    public void compute() {
        if ((source != null) && (target != null)) {
            try {
                path = iograph.getShortestPath(source, target, useWeights);
                String str = source;
                for (Conversion2 c : path) {
                    str += " -(" + c.getApplication() + ")-> " + c.getOutput();
                }
                lblPath.setText(str);
            } catch (PolyglotException e) {
                e.printStackTrace();
                path = new ArrayList<Conversion2>();
                lblPath.setText(e.getMessage());
            }
        }
        repaint();
    }

    /**
     * Paint the IOGraph. This will draw all the vertices, edges and paths from
     * the IOGraph.
     * 
     * @author Rob Kooper
     * 
     */
    class IOGraph2Component extends JComponent {
        public IOGraph2Component() {
            setPreferredSize(new Dimension(600, 600));

            addComponentListener(new ComponentListener() {
                public void componentShown(ComponentEvent e) {
                }

                public void componentResized(ComponentEvent e) {
                    vertexMap.clear();
                    repaint();
                }

                public void componentMoved(ComponentEvent e) {
                }

                public void componentHidden(ComponentEvent e) {
                }
            });

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        clicked = e.getPoint();
                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        clicked = e.getPoint();
                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            });

            popupMenu = new JPopupMenu();
            popupMenu.add(new JMenuItem(new AbstractAction("Source") {
                public void actionPerformed(ActionEvent e) {
                    double d = Double.MAX_VALUE;
                    String v = null;
                    for (Entry<String, Point> entry : vertexMap.entrySet()) {
                        double x = clicked.distance(entry.getValue());
                        if (x < d) {
                            d = x;
                            v = entry.getKey();
                        }
                    }
                    if (v == null) {
                        return;
                    }
                    source = v;
                    compute();
                }
            }));
            popupMenu.add(new JMenuItem(new AbstractAction("Target") {
                public void actionPerformed(ActionEvent e) {
                    double d = Double.MAX_VALUE;
                    String v = null;
                    for (Entry<String, Point> entry : vertexMap.entrySet()) {
                        double x = clicked.distance(entry.getValue());
                        if (x < d) {
                            d = x;
                            v = entry.getKey();
                        }
                    }
                    if (v == null) {
                        return;
                    }
                    target = v;
                    compute();
                }
            }));
            popupMenu.addSeparator();
            popupMenu.add(new JCheckBoxMenuItem(new AbstractAction("Range") {
                public void actionPerformed(ActionEvent e) {
                    showRange = ((JCheckBoxMenuItem) e.getSource()).getState();
                    repaint();
                }
            }));
            popupMenu.add(new JCheckBoxMenuItem(new AbstractAction("Domain") {
                public void actionPerformed(ActionEvent e) {
                    showDomain = ((JCheckBoxMenuItem) e.getSource()).getState();
                    repaint();
                }
            }));
        }

        @Override
        public void paint(Graphics g) {
            long t = System.currentTimeMillis();
            Graphics2D g2d = (Graphics2D) g;

            int width = getSize().width;
            int height = getSize().height;

            // clear canvas
            g2d.setColor(Color.white);
            g2d.fillRect(0, 0, width, height);

            // compute vertex locations if needed
            long l = System.currentTimeMillis();
            if (vertexMap.isEmpty()) {
                int half_width = width / 2;
                int half_height = height / 2;
                Set<String> vertices = new TreeSet<String>();
                for (Conversion2 c : iograph.getConversions()) {
                    if (iograph.getValidApplications().contains(c.getApplication()) && (!useWeights || (c.getWeight() != Conversion2.UNKNOWN_WEIGHT))) {
                        vertices.add(c.getInput());
                        vertices.add(c.getOutput());
                    }
                }
                log.debug("Compute vertexmap for " + vertices.size() + " vertices.");
                double theta0 = 0;
                int rings = 1;
                int size = vertices.size();
                int i = 0;
                for (String v : vertices) {
                    double radius = Math.pow(0.9, (i % rings) + 1);
                    Point p = new Point();
                    p.x = (int) Math.round(Math.cos((2.0 * Math.PI * i) / size + theta0) * radius * half_width + half_width);
                    p.y = (int) Math.round(Math.sin((2.0 * Math.PI * i) / size + theta0) * radius * half_height + half_height);
                    vertexMap.put(v, p);
                    i++;
                }
            }
            log.debug("VERTEXMAP : " + (System.currentTimeMillis() - l));

            // draw edges
            l = System.currentTimeMillis();
            g2d.setStroke(new BasicStroke(2));
            g2d.setColor(new Color(0xcccccc));
            for (Conversion2 c : iograph.getConversions()) {
                if (iograph.getValidApplications().contains(c.getApplication()) && (!useWeights || (c.getWeight() != Conversion2.UNKNOWN_WEIGHT))) {
                    Point p1 = vertexMap.get(c.getInput());
                    Point p2 = vertexMap.get(c.getOutput());
                    if ((p1 != null) && (p2 != null)) {
                        drawArrow(g2d, p1.x, p1.y, p2.x, p2.y, true, null);
                    }
                }
            }
            log.debug("EDGES     : " + (System.currentTimeMillis() - l));

            g2d.setStroke(new BasicStroke(4));

            // draw range
            l = System.currentTimeMillis();
            if (showRange) {
                g2d.setColor(new Color(0x00c0c0c0));
                for (Conversion2 c : iograph.getRange(source)) {
                    if (!useWeights || (c.getWeight() != Conversion2.UNKNOWN_WEIGHT)) {
                        Point p1 = vertexMap.get(c.getInput());
                        Point p2 = vertexMap.get(c.getOutput());
                        if ((p1 != null) && (p2 != null)) {
                            drawArrow(g2d, p1.x, p1.y, p2.x, p2.y, true, null);
                        }
                    }
                }
            }
            log.debug("RANGE     : " + (System.currentTimeMillis() - l));

            // draw domain
            l = System.currentTimeMillis();
            if (showDomain) {
                g2d.setColor(new Color(0x00a0a0a0));
                for (Conversion2 c : iograph.getDomain(target)) {
                    if (!useWeights || (c.getWeight() != Conversion2.UNKNOWN_WEIGHT)) {
                        Point p1 = vertexMap.get(c.getInput());
                        Point p2 = vertexMap.get(c.getOutput());
                        if ((p1 != null) && (p2 != null)) {
                            drawArrow(g2d, p1.x, p1.y, p2.x, p2.y, true, null);
                        }
                    }
                }
            }
            log.debug("DOMAIN    : " + (System.currentTimeMillis() - l));

            // show conversion path
            l = System.currentTimeMillis();
            g2d.setColor(new Color(0x00c3a3bd));
            for (Conversion2 c : path) {
                Point p1 = vertexMap.get(c.getInput());
                Point p2 = vertexMap.get(c.getOutput());
                if ((p1 != null) && (p2 != null) && showEdgeQuality && (c.getWeight() != Conversion2.UNKNOWN_WEIGHT)) {
                    drawArrow(g2d, p1.x, p1.y, p2.x, p2.y, true, "" + c.getWeight());
                } else {
                    drawArrow(g2d, p1.x, p1.y, p2.x, p2.y, true, null);
                }
            }
            log.debug("PATH      : " + (System.currentTimeMillis() - l));

            // draw vertices
            l = System.currentTimeMillis();
            FontMetrics fm = g2d.getFontMetrics();
            int h = fm.getDescent() / 2 + fm.getAscent() / 2;
            for (Entry<String, Point> entry : vertexMap.entrySet()) {
                String v = entry.getKey();
                Point p = entry.getValue();
                int w = fm.stringWidth(v);
                if (v.equals(source)) {
                    g2d.setColor(Color.red);
                } else if (v.equals(target)) {
                    g2d.setColor(Color.blue);
                } else {
                    g2d.setColor(Color.black);
                }
                g2d.drawString(v, p.x - w / 2, p.y - h);
            }
            log.debug("VERTICES  : " + (System.currentTimeMillis() - l));
            log.debug("TOTAL     : " + (System.currentTimeMillis() - t));
        }

        /**
         * Draw an arrow to the given graphics context.
         * 
         * @param g
         *            the graphics context to draw to
         * @param x0
         *            the starting x coordinate
         * @param y0
         *            the starting y coordinate
         * @param x1
         *            the ending x coordinate
         * @param y1
         *            the ending y coordinate
         * @param drawArrow
         *            true if the arrow head should be drawn
         * @param label
         *            optional label to draw in the arrow
         */
        private void drawArrow(Graphics g, int x0, int y0, int x1, int y1, boolean drawArrow, String label) {
            g.drawLine(x0, y0, x1, y1);

            if (drawArrow) {
                double arrow_h = 5;
                double arrow_w = 5;

                double dx = x1 - x0;
                double dy = y1 - y0;
                double length = Math.sqrt(dx * dx + dy * dy);
                dx /= length;
                dy /= length;

                double alpha = (1 - arrow_h / length) * length;
                double beta = arrow_w;

                g.drawLine(x0 + (int) (alpha * dx + beta * dy), y0 + (int) (alpha * dy - beta * dx), x1, y1);
                g.drawLine(x0 + (int) (alpha * dx - beta * dy), y0 + (int) (alpha * dy + beta * dx), x1, y1);
            }

            if (label != null) {
                FontMetrics fm = g.getFontMetrics();
                int ascent = fm.getMaxAscent();
                int descent = fm.getMaxDescent();
                int msg_width = fm.stringWidth(label);
                int x = (x1 + x0) / 2;
                int y = (y1 + y0) / 2;
                int w = (int) Math.round(1.5 * msg_width);
                int h = (int) Math.round(1.25 * (descent + ascent));
                Color color = g.getColor();

                g.setColor(Color.white);
                g.fillRect(x - w / 2, y - h / 2, w, h);
                g.setColor(color);
                g.drawRect(x - w / 2, y - h / 2, w, h);
                g.setColor(Color.black);
                g.drawString(label, x - msg_width / 2, y - descent / 2 + ascent / 2);
                g.setColor(color);
            }
        }
    }

    // ----------------------------------------------------------------------
    // DEBUG FUNCTIONS
    // ----------------------------------------------------------------------

    public static void showCSR(final URL url) throws IOException, PolyglotException {
        final IOGraph2 iograph = new IOGraph2();
        final IOGraph2Panel iographPanel = new IOGraph2Panel(iograph);

        String line;
        BufferedReader br;

        JMenu menu = new JMenu("Weights");
        iographPanel.getPopupMenu().addSeparator();
        iographPanel.getPopupMenu().add(menu);

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

        br = new BufferedReader(new InputStreamReader(new URL(url, "get_measures.php").openStream()));
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

        br = new BufferedReader(new InputStreamReader(new URL(url, "get_conversions.php").openStream()));
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

        JFrame frm = new JFrame();
        frm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frm.add(iographPanel);
        frm.pack();
        frm.setVisible(true);
    }

    public static void main(String[] args) throws Exception {
        showCSR(new URL("http://141.142.227.69/php/search/"));
    }
}
