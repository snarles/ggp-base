package org.ggp.base.util.propnet.architecture.components;

import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.propnet.architecture.Component;

/**
 * The Proposition class is designed to represent named latches.
 */
@SuppressWarnings("serial")
public final class Proposition extends Component
{

	/** The value of the Proposition. */
	private boolean value;

	/**
	 * Creates a new Proposition with name <tt>name</tt>.
	 *
	 * @param name
	 *            The name of the Proposition.
	 */
	public Proposition(GdlSentence name)
	{
		this.name = name;
		this.value = false;
	}


    /**
     * Setter method.
     *
     * This should only be rarely used; the name of a proposition
     * is usually constant over its entire lifetime.
     *
     * @return The name of the Proposition.
     */
    public void setName(GdlSentence newName)
    {
        name = newName;
    }

	/**
	 * Returns the current value of the Proposition.
	 *
	 * @see org.ggp.base.util.propnet.architecture.Component#getValue()
	 */
	@Override
	public boolean getValue()
	{
		return value;
	}

	/**
	 * Setter method.
	 *
	 * @param value
	 *            The new value of the Proposition.
	 */
	public void setValue(boolean value)
	{
		this.value = value;
	}

	/**
	 * @see org.ggp.base.util.propnet.architecture.Component#toString()
	 */
	@Override
	public String toString()
	{
		return toDot("circle", value ? "red" : "white", name.toString());
	}
	// New methods
	@Override
	public String toString2()
	{
		String s = getIdString();
		s =s.concat(name.toString());
		if (getSp().length() > 0) {
			s=s.concat("{").concat(getSp()).concat("}");
		}
		return s;
	}

	@Override
	public String toString3()
	{
		String s = "";
		s=s.concat(toString2());
		s=s.concat("   [IN:]");
		for (Component c : getInputs()) {
			s=s.concat(c.toString2()).concat(";");
		}
		s=s.concat("   [OUT:]");
		for (Component c : getOutputs()) {
			s=s.concat(c.toString2()).concat(";");
		}
		return s;
	}
}