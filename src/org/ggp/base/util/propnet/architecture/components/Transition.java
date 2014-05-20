package org.ggp.base.util.propnet.architecture.components;

import org.ggp.base.util.propnet.architecture.Component;

/**
 * The Transition class is designed to represent pass-through gates.
 */
@SuppressWarnings("serial")
public final class Transition extends Component
{
	/**
	 * Returns the value of the input to the transition.
	 *
	 * @see org.ggp.base.util.propnet.architecture.Component#getValue()
	 */
	@Override
	public boolean getValue()
	{
		return getSingleInput().getValue();
	}


	/**
	 * @see org.ggp.base.util.propnet.architecture.Component#toString()
	 */
	@Override
	public String toString()
	{
		return toDot("box", "grey", "TRANSITION");
	}
	// New methods
	@Override
	public String toString2()
	{
		String s = getIdString();
		s=s.concat("TRANS");
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