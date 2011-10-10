package edu.ncsa.icr.polyglot;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
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
import java.awt.geom.Line2D;
import java.net.URL;
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
import javax.swing.BorderFactory;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ncsa.icr.polyglot.Conversion2.Parameter;

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
    private Conversion2              parameter        = null;
    private List<Conversion2>        path             = new ArrayList<Conversion2>();
    private boolean                  showRange        = false;
    private boolean                  showDomain       = false;
    private boolean                  showEdgeQuality  = false;
    private boolean                  useWeights       = false;
    private boolean                  onlyParameters   = false;

    private JPopupMenu               popupMenu        = null;
    private Point                    clicked          = new Point(0, 0);
    private JLabel                   lblPath          = null;
    private ParameterModel           model;

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
                for (Object o : lstApplications.getSelectedValues() ) {
                    apps.add(o.toString());
                }
                IOGraph2Panel.this.iograph.setValidApplications(apps);
                vertexMap.clear();
                compute();
            }
        });
        JScrollPane scrollpane = new JScrollPane(lstApplications);
        scrollpane.setBorder(BorderFactory.createEmptyBorder());

        lblPath = new JLabel("No path selected.", JLabel.CENTER);
        lblPath.setOpaque(true);
        lblPath.setBackground(Color.white);

        JPanel pnl = new JPanel(new BorderLayout());
        pnl.add(new IOGraph2Component(), BorderLayout.CENTER);
        pnl.add(lblPath, BorderLayout.SOUTH);
        pnl.setBorder(BorderFactory.createEmptyBorder());

        JSplitPane splitPane1 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(lstApplications), pnl);
        splitPane1.setDividerLocation(200);

        model = new ParameterModel();
        JTable tblParams = new JTable(model);

        JSplitPane splitPane2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, splitPane1, new JScrollPane(tblParams));
        splitPane2.setDividerLocation(splitPane2.getSize().height - 200);

        add(splitPane2, BorderLayout.CENTER);
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
                for (Conversion2 c : path ) {
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

    private Set<Conversion2> getValidConversions() {
        // compute valid conversions
        Set<Conversion2> valid = new HashSet<Conversion2>();
        for (Conversion2 c : iograph.getConversions() ) {
            if (iograph.getValidApplications().contains(c.getApplication()) && (!onlyParameters || !c.getParameters().isEmpty()) && (!useWeights || (c.getWeight() != Conversion2.UNKNOWN_WEIGHT))) {
                valid.add(c);
            }
        }
        return valid;
    }

    public void showParameters(Conversion2 c) {
        parameter = c;
        model.fireTableDataChanged();
        repaint();
    }

    @SuppressWarnings("serial")
    class ParameterModel extends AbstractTableModel {
        private String[] columns = new String[] { "Name", "Values", "Default Value" };

        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        public int getRowCount() {
            if (parameter == null) {
                return 0;
            } else {
                return parameter.getParameters().size();
            }
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            if (parameter == null) {
                return "?";
            }

            List<Parameter> lst = new ArrayList<Parameter>(parameter.getParameters().values());
            if (lst.size() < rowIndex) {
                return "?";
            }

            switch (columnIndex) {
                case 0:
                    return lst.get(rowIndex).name;
                case 1:
                    return lst.get(rowIndex).values;
                case 2:
                    return lst.get(rowIndex).defaultValue;
                default:
                    return "?";
            }
        }

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
                    for (Entry<String, Point> entry : vertexMap.entrySet() ) {
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
                    for (Entry<String, Point> entry : vertexMap.entrySet() ) {
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
            popupMenu.add(new JMenuItem(new AbstractAction("Parameters") {
                public void actionPerformed(ActionEvent e) {
                    double d = Double.MAX_VALUE;
                    Conversion2 v = null;
                    for (Conversion2 c : getValidConversions() ) {
                        Point p1 = vertexMap.get(c.getInput());
                        Point p2 = vertexMap.get(c.getOutput());
                        Line2D l = new Line2D.Double(p1, p2);
                        double x = l.ptLineDist(clicked);
                        if (x < d) {
                            d = x;
                            v = c;
                        }
                    }
                    showParameters(v);
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
            popupMenu.add(new JCheckBoxMenuItem(new AbstractAction("Only Parameterized") {
                public void actionPerformed(ActionEvent e) {
                    onlyParameters = ((JCheckBoxMenuItem) e.getSource()).getState();
                    vertexMap.clear();
                    repaint();
                }
            }));
        }

        @Override
        public void paint(Graphics g) {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            long t = System.currentTimeMillis();
            Graphics2D g2d = (Graphics2D) g;

            int width = getSize().width;
            int height = getSize().height;

            // clear canvas
            g2d.setColor(Color.white);
            g2d.fillRect(0, 0, width, height);

            // get valid conversions
            long l = System.currentTimeMillis();
            Set<Conversion2> valid = getValidConversions();
            log.debug("CONVERSIONS : " + (System.currentTimeMillis() - l));

            // compute vertex locations if needed
            l = System.currentTimeMillis();
            if (vertexMap.isEmpty()) {
                int half_width = width / 2;
                int half_height = height / 2;
                Set<String> vertices = new TreeSet<String>();
                for (Conversion2 c : valid ) {
                    vertices.add(c.getInput());
                    vertices.add(c.getOutput());
                }
                log.debug("Compute vertexmap for " + vertices.size() + " vertices.");
                double theta0 = 0;
                int rings = 1;
                int size = vertices.size();
                int i = 0;
                for (String v : vertices ) {
                    double radius = Math.pow(0.9, (i % rings) + 1);
                    Point p = new Point();
                    p.x = (int) Math.round(Math.cos((2.0 * Math.PI * i) / size + theta0) * radius * half_width + half_width);
                    p.y = (int) Math.round(Math.sin((2.0 * Math.PI * i) / size + theta0) * radius * half_height + half_height);
                    vertexMap.put(v, p);
                    i++;
                }
            }
            log.debug("VERTEXMAP   : " + (System.currentTimeMillis() - l));

            // draw edges
            l = System.currentTimeMillis();
            g2d.setStroke(new BasicStroke(2));
            g2d.setColor(new Color(0xcccccc));
            for (Conversion2 c : valid ) {
                Point p1 = vertexMap.get(c.getInput());
                Point p2 = vertexMap.get(c.getOutput());
                if ((p1 != null) && (p2 != null)) {
                    drawArrow(g2d, p1.x, p1.y, p2.x, p2.y, true, null);
                }
            }
            if (parameter != null) {
                Point p1 = vertexMap.get(parameter.getInput());
                Point p2 = vertexMap.get(parameter.getOutput());
                g2d.setColor(Color.red);
                if ((p1 != null) && (p2 != null)) {
                    drawArrow(g2d, p1.x, p1.y, p2.x, p2.y, true, null);
                }
            }
            log.debug("EDGES       : " + (System.currentTimeMillis() - l));

            g2d.setStroke(new BasicStroke(4));

            // draw range
            l = System.currentTimeMillis();
            if (showRange) {
                g2d.setColor(new Color(0x00c0c0c0));
                for (Conversion2 c : iograph.getRange(source) ) {
                    if (!useWeights || (c.getWeight() != Conversion2.UNKNOWN_WEIGHT)) {
                        Point p1 = vertexMap.get(c.getInput());
                        Point p2 = vertexMap.get(c.getOutput());
                        if ((p1 != null) && (p2 != null)) {
                            drawArrow(g2d, p1.x, p1.y, p2.x, p2.y, true, null);
                        }
                    }
                }
            }
            log.debug("RANGE       : " + (System.currentTimeMillis() - l));

            // draw domain
            l = System.currentTimeMillis();
            if (showDomain) {
                g2d.setColor(new Color(0x00a0a0a0));
                for (Conversion2 c : iograph.getDomain(target) ) {
                    if (!useWeights || (c.getWeight() != Conversion2.UNKNOWN_WEIGHT)) {
                        Point p1 = vertexMap.get(c.getInput());
                        Point p2 = vertexMap.get(c.getOutput());
                        if ((p1 != null) && (p2 != null)) {
                            drawArrow(g2d, p1.x, p1.y, p2.x, p2.y, true, null);
                        }
                    }
                }
            }
            log.debug("DOMAIN      : " + (System.currentTimeMillis() - l));

            // show conversion path
            l = System.currentTimeMillis();
            g2d.setColor(new Color(0x00c3a3bd));
            for (Conversion2 c : path ) {
                Point p1 = vertexMap.get(c.getInput());
                Point p2 = vertexMap.get(c.getOutput());
                if ((p1 != null) && (p2 != null) && showEdgeQuality && (c.getWeight() != Conversion2.UNKNOWN_WEIGHT)) {
                    drawArrow(g2d, p1.x, p1.y, p2.x, p2.y, true, "" + c.getWeight());
                } else {
                    drawArrow(g2d, p1.x, p1.y, p2.x, p2.y, true, null);
                }
            }
            log.debug("PATH        : " + (System.currentTimeMillis() - l));

            // draw vertices
            l = System.currentTimeMillis();
            FontMetrics fm = g2d.getFontMetrics();
            int h = fm.getDescent() / 2 + fm.getAscent() / 2;
            for (Entry<String, Point> entry : vertexMap.entrySet() ) {
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
            log.debug("VERTICES    : " + (System.currentTimeMillis() - l));
            log.debug("TOTAL       : " + (System.currentTimeMillis() - t));

            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
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


    public static void main(String[] args) throws Exception {
        JFrame frm = new JFrame();
        frm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frm.add(new CSRPanel(new URL("http://141.142.227.69/php/search/")).getIOGraphPanel());
        frm.pack();
        frm.setVisible(true);
    }
}
