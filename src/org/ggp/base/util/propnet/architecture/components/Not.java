package org.ggp.base.util.propnet.architecture.components;

import org.ggp.base.util.propnet.architecture.Component;

/**
 * The Not class is designed to represent logical NOT gates.
 */
@SuppressWarnings("serial")
public final class Not extends Component
{
	/**
	 * Returns the inverse of the input to the not.
	 *
	 * @see org.ggp.base.util.propnet.architecture.Component#getValue()
	 */
	@Override
	public boolean getValue()
	{
		return !getSingleInput().getValue();
	}

	/**
	 * @see org.ggp.base.util.propnet.architecture.Component#toString()
	 */
	@Override
	public String toString()
	{
		return toDot("invtriangle", "grey", "NOT");
	}
	// New methods
	@Override
	public String toString2()
	{
		String s = getIdString();
		s = s.concat("NOT");
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