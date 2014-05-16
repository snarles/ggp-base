package org.ggp.base.util.propnet.architecture.components;

import org.ggp.base.util.propnet.architecture.Component;

/**
 * The And class is designed to represent logical AND gates.
 */
@SuppressWarnings("serial")
public final class And extends Component
{
	/**
	 * Returns true if and only if every input to the and is true.
	 *
	 * @see org.ggp.base.util.propnet.architecture.Component#getValue()
	 */
	@Override
	public boolean getValue()
	{
		for ( Component component : getInputs() )
		{
			if ( !component.getValue() )
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * @see org.ggp.base.util.propnet.architecture.Component#toString()
	 */
	@Override
	public String toString()
	{
		return toDot("invhouse", "grey", "AND");
	}

	// New methods
	@Override
	public String toString2()
	{
		String s = getIdString();
		s=s.concat("AND");
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
