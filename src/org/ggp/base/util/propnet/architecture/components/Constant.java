package org.ggp.base.util.propnet.architecture.components;

import org.ggp.base.util.propnet.architecture.Component;

/**
 * The Constant class is designed to represent nodes with fixed logical values.
 */
@SuppressWarnings("serial")
public final class Constant extends Component
{
	/** The value of the constant. */
	private final boolean value;

	/**
	 * Creates a new Constant with value <tt>value</tt>.
	 *
	 * @param value
	 *            The value of the Constant.
	 */
	public Constant(boolean value)
	{
		this.value = value;
	}

	/**
	 * Returns the value that the constant was initialized to.
	 *
	 * @see org.ggp.base.util.propnet.architecture.Component#getValue()
	 */
	@Override
	public boolean getValue()
	{
		return value;
	}

	/**
	 * @see org.ggp.base.util.propnet.architecture.Component#toString()
	 */
	@Override
	public String toString()
	{
		return toDot("doublecircle", "grey", Boolean.toString(value).toUpperCase());
	}

	// New methods
	@Override
	public String toString2()
	{
		String s = getIdString();
		s = s.concat("CONS");
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