package edu.illinois.ncsa.isda.softwareserver.polyglot.weighted;

import java.util.Map;
import java.util.TreeMap;

/**
 * Conversion using an application from input to output. This can store the
 * information loss of the conversion (ranging from 0 - no information loss to
 * 9999 - all information lost). The information loss can be used to find the
 * best conversion path.
 * 
 * @author Rob Kooper
 * 
 */
public class Conversion2 {
    public static final double     UNKNOWN_WEIGHT = 10000.0;

    private int                    id;
    private String                 input;
    private String                 output;
    private String                 application;
    private double                 weight;
    private Map<String, Parameter> parameters     = new TreeMap<String, Parameter>();

    public Conversion2(int id, String input, String output, String application) throws PolyglotException {
        this(id, input, output, application, UNKNOWN_WEIGHT);
    }

    public Conversion2(int id, String input, String output, String application, double weight) throws PolyglotException {
        this.id = id;
        this.input = input;
        this.output = output;
        this.application = application;
        setWeight(weight);
    }

    public int getId() {
        return id;
    }

    public String getApplication() {
        return application;
    }

    public String getInput() {
        return input;
    }

    public String getOutput() {
        return output;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) throws PolyglotException {
        if (weight < 0) {
            throw (new PolyglotException("No negative weights allowed."));
        }
        if (weight > UNKNOWN_WEIGHT) {
            weight = UNKNOWN_WEIGHT;
        }
        this.weight = weight;
    }

    public Map<String, Parameter> getParameters() {
        return parameters;
    }

    public void addParameter(String name, String values, String defaultValue) {
        Parameter p = new Parameter(name, values, defaultValue);
        parameters.put(p.name, p);
    }

    class Parameter {
        String name;
        String values;
        String defaultValue;

        public Parameter(String name, String values, String defaultValue) {
            this.name = name;
            this.values = values;
            this.defaultValue = defaultValue;
        }
    }
}
