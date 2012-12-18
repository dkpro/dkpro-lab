package de.tudarmstadt.ukp.dkpro.lab.storage;

import org.springframework.dao.DataAccessResourceFailureException;

/**
 * Exception thrown when an import cannot be resolved.
 * 
 * @author Richard Eckart de Castilho
 */
public class UnresolvedImportException
	extends DataAccessResourceFailureException
{

	public UnresolvedImportException(String aMsg)
	{
		super(aMsg);
	}

	public UnresolvedImportException(String aMsg, Throwable aCause)
	{
		super(aMsg, aCause);
	}
}
