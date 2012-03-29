package de.tudarmstadt.ukp.dkpro.lab.task;

/**
 * Dimensions with a fixed size may implement this interface. It can be used to calculate the
 * progress.
 * 
 * @author Richard Eckart de Castilho
 */
public interface FixedSizeDimension
{
	/**
	 * Size of the dimension. 
	 */
	int size();
}
