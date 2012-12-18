package de.tudarmstadt.ukp.dkpro.lab.storage;

import org.springframework.dao.DataAccessResourceFailureException;

/**
 * Exception thrown when a task context could not be found, meaning that a task (possibly with a
 * certain configuration of discriminators) has never been executed or is outside of the scope.
 * 
 * @author Richard Eckart de Castilho
 */
public class TaskContextNotFoundException
	extends DataAccessResourceFailureException
{

	public TaskContextNotFoundException(String aMsg)
	{
		super(aMsg);
	}
}
