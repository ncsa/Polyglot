package edu.illinois.ncsa.isda.icr.polyglot.weighted;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.JFrame;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import edu.illinois.ncsa.isda.icr.SoftwareServerClient;
import edu.illinois.ncsa.isda.icr.ICRAuxiliary.Application;
import edu.illinois.ncsa.isda.icr.ICRAuxiliary.Data;
import edu.illinois.ncsa.isda.icr.ICRAuxiliary.Operation;

/**
 * Graph that contains all possible conversions. Conversions are from an input
 * to an output using a specific application. The conversion can have a weight
 * associated which is the correctness of the conversion (ranging from 0 - no
 * information loss, to 9999 - data lost).
 * 
 * @author Rob Kooper
 */
public class IOGraph2 {
    private static Log                     log               = LogFactory.getLog(IOGraph2.class);

    /** empty list returned if no answers available */
    private static final List<Conversion2> EMPTY_LIST        = new ArrayList<Conversion2>();

    /** list of all conversions */
    private List<Conversion2>              conversions       = new ArrayList<Conversion2>();

    /** list of all conversions by id */
    private Map<Integer, Conversion2>      idMap             = new HashMap<Integer, Conversion2>();

    /** list of all conversions that can be done by a specific application */
    private Map<String, List<Conversion2>> applicationMap    = new HashMap<String, List<Conversion2>>();

    /** list of all conversions that can convert a specific input */
    private Map<String, List<Conversion2>> inputMap          = new HashMap<String, List<Conversion2>>();

    /** list of all conversions that can create specific output */
    private Map<String, List<Conversion2>> outputMap         = new HashMap<String, List<Conversion2>>();

    /** applications taken in consideration when computing paths */
    private Set<String>                    validApplications = new HashSet<String>();

    /** all vertices, both input and output */
    private Set<String>                    vertices          = new TreeSet<String>();

    /** minimum weight for a conversion */
    private double                         maximumWeight     = Conversion2.UNKNOWN_WEIGHT;

    /**
     * Create an empty IOGraph.
     */
    public IOGraph2() {
    }

    /**
     * Reset the IOGraph. This will remove all conversion from the list.
     */
    public void reset() {
        conversions.clear();
        idMap.clear();
        applicationMap.clear();
        inputMap.clear();
        outputMap.clear();
        validApplications.clear();
        vertices.clear();
        maximumWeight = Conversion2.UNKNOWN_WEIGHT;
    }

    /**
     * Add all conversions found on the software server found at the host. This
     * will use the default port (50000) to connect. The weights assigned to the
     * conversion are set to be undefined.
     * 
     * @param host
     *            the host running the software server.
     * @throws PolyglotException
     *             throws PolyglotException if there was a problem adding the
     *             conversions to the existing graph.
     */
    public void addConversions(String host) throws PolyglotException {
        addConversions(host, 50000);
    }

    /**
     * Add all conversions found on the software server found at the host. The
     * weights assigned to the conversion are set to be undefined.
     * 
     * @param host
     *            the host running the software server.
     * @param port
     *            the port to connect to on the software server
     * @throws PolyglotException
     *             throws PolyglotException if there was a problem adding the
     *             conversions to the existing graph.
     */
    public void addConversions(String host, int port) throws PolyglotException {
        addConversions(new SoftwareServerClient(host, port));
    }

    /**
     * Add all conversions found on the software server. The weights assigned to
     * the conversion are set to be undefined.
     * 
     * @param server
     *            the server that has the software server running
     * @throws PolyglotException
     *             throws PolyglotException if there was a problem adding the
     *             conversions to the existing graph.
     */
    public void addConversions(SoftwareServerClient server) throws PolyglotException {
        for (Application app : server.getApplications() ) {
            for (Operation op : app.operations ) {
                for (Data inp : op.inputs ) {
                    for (Data out : op.outputs ) {
                        addConversion(inp.toString(), out.toString(), app.name);
                    }
                }
            }
        }
    }

    /**
     * This will add the conversion from <code>input</code> to
     * <code>output</code> using the <code>application</code> for the
     * conversion. The weights assigned to the conversion are set to be
     * undefined.
     * 
     * @param input
     *            input format
     * @param output
     *            output format
     * @param application
     *            application used for the conversion
     * @throws PolyglotException
     *             throws PolyglotException if there was a problem adding the
     *             conversions to the existing graph.
     */
    public void addConversion(String input, String output, String application) throws PolyglotException {
        Integer id = 0;
        while(idMap.containsKey(id)) {
            id = new Random().nextInt();
        }
        addConversion(id, input, output, application, Conversion2.UNKNOWN_WEIGHT);
    }

    /**
     * This will add the conversion from <code>input</code> to
     * <code>output</code> using the <code>application</code> for the conversion
     * with the <code>weight</weight>.
     * 
     * @param input
     *            input format
     * @param output
     *            output format
     * @param application
     *            application used for the conversion
     * @param weight
     *            the weight assigned to the conversion, ranging from 0 (perfect
     *            conversion) to unknown weight (10000, all information lost).
     * @throws PolyglotException
     *             throws PolyglotException if there was a problem adding the
     *             conversions to the existing graph.
     */
    public void addConversion(String input, String output, String application, double weight) throws PolyglotException {
        Integer id = 0;
        while(idMap.containsKey(id)) {
            id = new Random().nextInt();
        }
        addConversion(id, input, output, application, weight);
    }

    /**
     * This will add the conversion from <code>input</code> to
     * <code>output</code> using the <code>application</code> for the
     * conversion. The weights assigned to the conversion are set to be
     * undefined.
     * 
     * @param id
     *            unique id used to reference the conversion
     * @param input
     *            input format
     * @param output
     *            output format
     * @param application
     *            application used for the conversion
     * @throws PolyglotException
     *             throws PolyglotException if there was a problem adding the
     *             conversions to the existing graph.
     */
    public void addConversion(int id, String input, String output, String application) throws PolyglotException {
        addConversion(id, input, output, application, Conversion2.UNKNOWN_WEIGHT);
    }

    /**
     * This will add the conversion from <code>input</code> to
     * <code>output</code> using the <code>application</code> for the conversion
     * with the <code>weight</weight>.
     * 
     * @param id
     *            unique id used to reference the conversion
     * @param input
     *            input format
     * @param output
     *            output format
     * @param application
     *            application used for the conversion
     * @param weight
     *            the weight assigned to the conversion, ranging from 0 (perfect
     *            conversion) to unknown weight (10000, all information lost).
     * @throws PolyglotException
     *             throws PolyglotException if there was a problem adding the
     *             conversions to the existing graph.
     */
    public void addConversion(int id, String input, String output, String application, double weight) throws PolyglotException {
        addConversion(new Conversion2(id, input, output, application, weight));
    }

    /**
     * This will add the conversion to the IOGraph.
     * 
     * @param c
     *            the conversion to add to the IOGraph
     */
    public void addConversion(Conversion2 c) {
        conversions.add(c);
        vertices.add(c.getInput());
        vertices.add(c.getOutput());
        if (idMap.containsKey(c.getId())) {
            log.warn("Already contain conversion for id : " + c.getId());
        }
        idMap.put(c.getId(), c);
        List<Conversion2> list = applicationMap.get(c.getApplication());
        if (list == null) {
            list = new ArrayList<Conversion2>();
            applicationMap.put(c.getApplication(), list);
        }
        list.add(c);
        list = inputMap.get(c.getInput());
        if (list == null) {
            list = new ArrayList<Conversion2>();
            inputMap.put(c.getInput(), list);
        }
        list.add(c);
        list = outputMap.get(c.getOutput());
        if (list == null) {
            list = new ArrayList<Conversion2>();
            outputMap.put(c.getOutput(), list);
        }
        list.add(c);
    }

    public void addParameter(int id, String name, String values, String defaultValue) {
        Conversion2 c = idMap.get(id);
        if (c != null) {
            c.addParameter(name, values, defaultValue);
        } else {
            log.error("Could not find conversion with id : " + id);
        }
    }

    /**
     * Sets all the weights of all conversions to unknown.
     * 
     * @throws PolyglotException
     *             throws PolyglotException if there was an error setting the
     *             weights.
     */
    public void resetWeights() throws PolyglotException {
        for (Conversion2 c : conversions ) {
            c.setWeight(Conversion2.UNKNOWN_WEIGHT);
        }
    }

    /**
     * Sets the weight of all conversions matching the triple [input, output,
     * application].
     * 
     * @param input
     *            input format
     * @param output
     *            output format
     * @param application
     *            application used for the conversion
     * @param weight
     *            the weight assigned to the conversion, ranging from 0 (perfect
     *            conversion) to unknown weight (10000, all information lost).
     * @throws PolyglotException
     *             throws PolyglotException if there was an error setting the
     *             weights.
     */
    public void setWeight(String input, String output, String application, double weight) throws PolyglotException {
        for (Conversion2 c : conversions ) {
            if (c.getInput().equals(input) && c.getOutput().equals(output) && c.getApplication().equals(application)) {
                c.setWeight(weight);
            }
        }
    }

    /**
     * Sets the list of applications that should be considered when finding a
     * conversion path. Setting this to null, or an empty set will result in all
     * applications used.
     * 
     * @param validApplications
     *            list of valid applications to include when searching for the
     *            shortest path.
     */
    public void setValidApplications(Set<String> validApplications) {
        this.validApplications.clear();
        if (validApplications != null) {
            this.validApplications.addAll(validApplications);
        }
    }

    /**
     * Returns the list of all applications used when finding the shortest path.
     * 
     * @return list of applications used when finding shortest path.
     */
    public Set<String> getValidApplications() {
        if (validApplications.isEmpty()) {
            return applicationMap.keySet();
        }
        return validApplications;
    }

    /**
     * List of all applications known to the IOGraph. This includes those
     * applications not used when searching for the shortest path.
     * 
     * @return list of all known applications
     */
    public Set<String> getApplications() {
        return applicationMap.keySet();
    }

    /**
     * List of all vertices in the graph. This is combined set of input and
     * output formats.
     * 
     * @return list of all vertices in the IOGraph.
     */
    public Set<String> getVertices() {
        return vertices;
    }

    public Conversion2 getConversion(int id) {
        return idMap.get(id);
    }

    /**
     * List of all conversions known to the IOGraph. These conversions are the
     * edges in the IOGraph.
     * 
     * @return list of all possible conversion in the IOGraph.
     */
    public List<Conversion2> getConversions() {
        return conversions;
    }

    /**
     * Return the list of all possible conversions starting at the vertex.
     * 
     * @param vertex
     *            the starting point in the graph.
     * @return list of all conversions starting at the vertex.
     */
    public List<Conversion2> getRange(String vertex) {
        List<Conversion2> result = inputMap.get(vertex);
        if (result == null) {
            return EMPTY_LIST;
        }
        return result;
    }

    /**
     * Return the list of all possible conversions reaching the vertex.
     * 
     * @param vertex
     *            the ending point in the graph.
     * @return list of all conversions reaching the vertex.
     */
    public List<Conversion2> getDomain(String vertex) {
        List<Conversion2> result = outputMap.get(vertex);
        if (result == null) {
            return EMPTY_LIST;
        }
        return result;
    }

    /**
     * Returns the shortest path from the input to the output. This will return
     * a list of conversions from the input vertex to the output vertex. This
     * will use the Dijkstra algorithm to find the shortest path.
     * 
     * @param input
     *            starting vertex in the IOGraph.
     * @param output
     *            the finishing vertex in the IOGraph.
     * @param useWeights
     *            should the shortest path be based on the weights in the
     *            conversion (true), or the smallest number of conversions
     *            (false).
     * @return list of conversions to convert from input format to output
     *         format.
     * @throws PolyglotException
     *             throws PolyglotException if no path could be found to go from
     *             input to output.
     */
    public List<Conversion2> getShortestPath(String input, String output, boolean useWeights) throws PolyglotException {
        Map<String, Conversion2> previous = getShortestPath(input, useWeights);
        List<Conversion2> result = new ArrayList<Conversion2>();

        String x = output;
        while (!input.equals(x)) {
            Conversion2 c = previous.get(x);
            if (c == null) {
                throw (new PolyglotException("No path found from " + input + " to " + output));
            }
            result.add(c);
            x = c.getInput();
        }

        Collections.reverse(result);

        return result;
    }

    /**
     * Return a map that shows for each node what conversion to use to get to
     * the previous node, resulting in then end finding the path all the way
     * back to the source node. This function uses Dijstra's algorithm to find
     * the shortest path. This is a greedy algorithm and will not be able to
     * handle negative weights (which are not allowed by the conversions). This
     * has a running time of O( |E| + |V| log |V| )
     * 
     * @param input
     *            the node to start the search from
     * @return map from the a output to the previous node to reach the input.
     */
    public Map<String, Conversion2> getShortestPath(String input, boolean useWeights) {
        Set<String> Q = new HashSet<String>(vertices);
        Map<String, Double> dist = new HashMap<String, Double>();
        Map<String, Conversion2> previous = new HashMap<String, Conversion2>();

        for (String u : Q ) {
            dist.put(u, Double.MAX_VALUE);
            previous.put(u, null);
        }
        dist.put(input, 0.0);

        while (!Q.isEmpty()) {
            // System.out.println(dist.toString());

            // u := vertex in Q with smallest dist[] ;
            Iterator<String> iter = Q.iterator();
            String u = iter.next();
            double du = dist.get(u);
            while (iter.hasNext()) {
                String x = iter.next();
                double dx = dist.get(x);
                if (dx < du) {
                    u = x;
                    du = dx;
                }
            }
            // System.out.println(u);

            // all remaining vertices are inaccessible from source.
            if (du == Double.MAX_VALUE) {
                break;
            }

            // remove from Q
            Q.remove(u);

            // for each neighbor v of u:
            if (inputMap.get(u) != null) {
                for (Conversion2 c : inputMap.get(u) ) {
                    if (getValidApplications().contains(c.getApplication())) {
                        String v = c.getOutput();
                        if (Q.contains(v)) {
                            if (!useWeights || (c.getWeight() < maximumWeight)) {
                                double alt = du;
                                if (useWeights) {
                                    alt += c.getWeight();
                                } else {
                                    alt++;
                                }
                                if (alt < dist.get(v)) {
                                    dist.put(v, alt);
                                    previous.put(v, c);
                                }
                            }
                        }
                    }
                }
            }
        }

        return previous;
    }

    /**
     * Print statistics about the IOGraph.
     */
    public void complexity() {
        System.out.println("TOTAL EDGES       : " + getConversions().size());
        System.out.println("TOTAL VERTICES    : " + getVertices().size());
        System.out.println("TOTAL EDGES NAMES : " + getApplications().size());
        System.out.println("AVERAGE NEIGHBORS : " + ((double) getConversions().size() / getVertices().size()));
    }

    // ----------------------------------------------------------------------
    // DEBUG FUNCTIONS
    // ----------------------------------------------------------------------

    public static void timings(int edges, int formats, int software) throws PolyglotException {
        long l;
        IOGraph2 iograph = new IOGraph2();

        l = System.currentTimeMillis();
        Random r = new Random();
        int s, t;
        for (int i = 0; i < edges; i++ ) {
            do {
                s = r.nextInt(formats);
                t = r.nextInt(formats);
            } while (s == t);
            iograph.addConversion("" + s, "" + t, "" + r.nextInt(software), (double) r.nextInt(100));
        }

        l = System.currentTimeMillis() - l;
        iograph.complexity();
        System.out.println("CREATE            : " + l);

        l = System.currentTimeMillis();
        iograph.getShortestPath("0", true);
        l = System.currentTimeMillis() - l;
        System.out.println("DIJKSTRA          : " + l);

        // l = System.currentTimeMillis();
        // try{
        // iograph.bellmanFord(0);
        // }catch(Exception e){
        // e.printStackTrace();
        // }
        // l = System.currentTimeMillis() - l;
        // System.out.println("BELLMAN           : " + l);
    }

    public static void main(String... args) throws Exception {
        // timings(1000000, 5000, 1000);

        // example from http://en.wikipedia.org/wiki/Dijkstra's_algorithm
        IOGraph2 iograph = new IOGraph2();
        iograph.addConversion("1", "2", "A", 7);
        iograph.addConversion("1", "3", "A", 9);
        iograph.addConversion("1", "6", "A", 14);
        iograph.addConversion("2", "3", "A", 10);
        iograph.addConversion("2", "4", "A", 15);
        iograph.addConversion("3", "4", "A", 11);
        iograph.addConversion("3", "6", "A", 2);
        iograph.addConversion("4", "5", "A", 6);
        iograph.addConversion("5", "6", "A", 9);

        iograph.addConversion("2", "1", "B", 7);
        iograph.addConversion("3", "1", "B", 9);
        iograph.addConversion("6", "1", "B", 14);
        iograph.addConversion("3", "2", "B", 10);
        iograph.addConversion("4", "2", "B", 15);
        iograph.addConversion("4", "3", "B", 11);
        iograph.addConversion("6", "3", "B", 2);
        iograph.addConversion("5", "4", "B", 6);
        iograph.addConversion("6", "5", "B", 9);

        IOGraph2Panel pnlIOGraph = new IOGraph2Panel(iograph);
        pnlIOGraph.useWeights(true);

        JFrame frm = new JFrame();
        frm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frm.add(pnlIOGraph);
        frm.pack();
        frm.setVisible(true);

        List<Conversion2> path = iograph.getShortestPath("1", "5", false);
        System.out.print("1");
        for (Conversion2 c : path ) {
            System.out.print(" -(" + c.getApplication() + ")-> " + c.getOutput());
        }
        System.out.println();
        path = iograph.getShortestPath("1", "5", true);
        System.out.print("1");
        for (Conversion2 c : path ) {
            System.out.print(" -(" + c.getApplication() + ")-> " + c.getOutput());
        }
        System.out.println();
    }
}
