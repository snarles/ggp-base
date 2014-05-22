package org.ggp.base.util.propnet.architecture;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlProposition;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.propnet.architecture.components.Transition;

/**
 * The root class of the Component hierarchy, which is designed to represent
 * nodes in a PropNet. The general contract of derived classes is to override
 * all methods.
 */

public abstract class Component implements Serializable, Comparable<Component>
{

	private static final long serialVersionUID = 352524175700224447L;
    /** The inputs to the component. */
    private final Set<Component> inputs;
    /** The outputs of the component. */
    protected final Set<Component> outputs;
    //inputs which are not transitions
    private final Set<Component> ntransInputs;
    //outputs given that C is a transition
    protected final Set<Component> ntransOutputs;
    //inputs which are not transitions
    private final Set<Component> transInputs;
    //outputs given that C is a transition
    protected final Set<Component> transOutputs;
    /** The name of the Proposition. */
	protected GdlSentence name;

    private int id;
    private final Set<Component> special;
    private String sp="";
    private GdlConstant owner;
    // Level for topological sort
    private int level=0;
    private ArrayList<Integer> outCount = new ArrayList<Integer>();
    private int goal;
    /**
     * Creates a new Component with no inputs or outputs.
     */
    public Component()
    {
        this.inputs = new HashSet<Component>();
        this.outputs = new HashSet<Component>();
        this.transInputs = new HashSet<Component>();
        this.transOutputs = new HashSet<Component>();
        this.ntransInputs = new HashSet<Component>();
        this.ntransOutputs = new HashSet<Component>();
        this.special = new HashSet<Component>();
        this.name=new GdlProposition(new GdlConstant("UNDEF"));
    }

    /**
     * Adds a new input.
     *
     * @param input
     *            A new input.
     */
    public void addInput(Component input)
    {
    	inputs.add(input);

    }

    public void postProcess() {
    	for (Component input : inputs ) {
	    	if (input instanceof Transition) {
	    		transInputs.add(input);
	    	}
	    	else {
	    		ntransInputs.add(input);
	    	}
    	}
    	for (Component output : outputs) {
    		if (sp=="zTRANS") {
    			transOutputs.add(output);
    		}
    		else {
    			ntransOutputs.add(output);
    		}
    	}
    }

    public void removeInput(Component input)
    {
    	inputs.remove(input);
    }

    public void removeOutput(Component output)
    {
    	outputs.remove(output);
    }

    public void removeAllInputs()
    {
		inputs.clear();
	}

	public void removeAllOutputs()
	{
		outputs.clear();
	}

    /**
     * Adds a new output.
     *
     * @param output
     *            A new output.
     */
    public void addOutput(Component output)
    {
        outputs.add(output);
    }

    /**
     * Getter method.
     *
     * @return The inputs to the component.
     */
    public Set<Component> getInputs()
    {
        return inputs;
    }

    /**
     * A convenience method, to get a single input.
     * To be used only when the component is known to have
     * exactly one input.
     *
     * @return The single input to the component.
     */
    public Component getSingleInput() {
        assert inputs.size() == 1;
        return inputs.iterator().next();
    }

    /**
     * Getter method.
     *
     * @return The outputs of the component.
     */
    public Set<Component> getOutputs()
    {
        return outputs;
    }

    public Set<Component> getNtransOutputs()
    {
        return ntransOutputs;
    }

    public Set<Component> getNtransInputs()
    {
        return ntransInputs;
    }
    public Set<Component> getTransOutputs()
    {
        return transOutputs;
    }

    public Set<Component> getTransInputs()
    {
        return transInputs;
    }

    /**
     * A convenience method, to get a single output.
     * To be used only when the component is known to have
     * exactly one output.
     *
     * @return The single output to the component.
     */
    public Component getSingleOutput() {
        assert outputs.size() == 1;
        return outputs.iterator().next();
    }

    /**
     * Returns the value of the Component.
     *
     * @return The value of the Component.
     */
    public abstract boolean getValue();

    /**
     * Returns a representation of the Component in .dot format.
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public abstract String toString();

    /**
     * Returns a configurable representation of the Component in .dot format.
     *
     * @param shape
     *            The value to use as the <tt>shape</tt> attribute.
     * @param fillcolor
     *            The value to use as the <tt>fillcolor</tt> attribute.
     * @param label
     *            The value to use as the <tt>label</tt> attribute.
     * @return A representation of the Component in .dot format.
     */
    protected String toDot(String shape, String fillcolor, String label)
    {
        StringBuilder sb = new StringBuilder();

        sb.append("\"@" + Integer.toHexString(hashCode()) + "\"[shape=" + shape + ", style= filled, fillcolor=" + fillcolor + ", label=\"" + label + "\"]; ");
        for ( Component component : getOutputs() )
        {
            sb.append("\"@" + Integer.toHexString(hashCode()) + "\"->" + "\"@" + Integer.toHexString(component.hashCode()) + "\"; ");
        }

        return sb.toString();
    }
    //New methods
    public abstract String toString2();
    public abstract String toString3();
    public int getId()
    {
    	return id;
    }
    public Integer getIdInt() {
    	return new Integer(id);
    }
    public String getIdString()
    {
    	return String.valueOf(level).concat("(").concat(String.valueOf(id)).concat(")");
    }
    public void setId(int i) {
    	id=i;
    }
    public String getSp() {
    	return sp;
    }
    public void setSp(String s) {
    	sp=s;
    }
    public Set<Component> getSpecial() {
    	return special;
    }
    // updates topological sort level
    public int getLevel() {
    	return level;
    }
    public boolean topoSort() {
    	int maxl = 0;
    	for (Component c : getInputs()) {
    		//! (c instanceof Constant)  &&
    		if (! (c instanceof Transition)) {
	    		if (maxl < c.getLevel()+1) {
	    			maxl = c.getLevel()+1;
	    		}
    		}
    	}
    	boolean ans = (maxl <= level);
    	if (! ans) {
    		level = maxl;
    	}
    	return ans;
    }

    @Override
    public int compareTo(Component c) {
    	int ans= level-c.getLevel();
    	if (ans==0) {
    		ans = sp.compareTo(c.getSp());
    		if (ans==0) {
    			ans = name.toString().compareTo(c.getName().toString());
    		}
    	}
    	return ans;
    }

    public Set<Integer> getOutputIds() {
    	Set<Integer> ans = new HashSet<Integer>();
    	for (Component c : ntransOutputs) {
    		ans.add(new Integer(c.getId()));
    	}
    	return ans;
    }
    public Set<Integer> getInputIds() {
    	Set<Integer> ans = new HashSet<Integer>();
    	for (Component c : ntransInputs) {
    		ans.add(new Integer(c.getId()));
    	}
    	return ans;
    }

    public Set<Integer> getTransOutputIds() {
    	Set<Integer> ans = new HashSet<Integer>();
    	for (Component c : transOutputs) {
    		ans.add(new Integer(c.getId()));
    	}
    	return ans;
    }
    public Set<Integer> getTransInputIds() {
    	Set<Integer> ans = new HashSet<Integer>();
    	for (Component c : transInputs) {
    		ans.add(new Integer(c.getId()));
    	}
    	return ans;
    }


    public int countOutputs(int depth) {
    	int sum=0;
    	if (depth >= outCount.size()) {
	    	if (depth ==0 ) {
	    		sum =  outputs.size();
	    	}
	    	else {
	    		for (Component c : outputs) {
	    			sum = sum + c.countOutputs(depth-1);
	    		}
	    	}
    		outCount.add(new Integer(sum));
    		return  sum;
    	}
    	else {
    		return outCount.get(depth).intValue();
    	}
    }
    public int getOutcount(int depth) {
    	if (depth >= outCount.size()) {
    		return 0;
    	}
    	else {
    		return outCount.get(depth);
    	}
    }
    public void setOwner(GdlConstant g) {
    	owner = g;
    }
    public GdlConstant getOwner() {
    	return owner;
    }
    public void setGoal(int i) {
    	goal = i;
    }
    public int getGoal() {
    	return goal;
    }
	/**
	 * Getter method.
	 *
	 * @return The name of the Proposition.
	 */
	public GdlSentence getName()
	{
		return name;
	}


}