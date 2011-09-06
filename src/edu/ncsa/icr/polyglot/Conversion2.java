package edu.ncsa.icr.polyglot;

import java.util.HashMap;
import java.util.Map;

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
    public static final double  UNKNOWN_WEIGHT = 10000.0;

    private String              input;
    private String              output;
    private String              application;
    private double              weight;
    private Map<String, String> properties     = new HashMap<String, String>();

    public Conversion2(String input, String output, String application) throws PolyglotException {
        this(input, output, application, UNKNOWN_WEIGHT);
    }

    public Conversion2(String input, String output, String application, double weight) throws PolyglotException {
        this.input = input;
        this.output = output;
        this.application = application;
        setWeight(weight);
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
}
