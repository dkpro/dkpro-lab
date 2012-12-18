package de.tudarmstadt.ukp.dkpro.lab.task;

/**
 * Exception thrown when all sub-tasks of a batch task have been deferred because a prerequisite
 * could not be fulfilled.
 */
public class UnfulfillablePrerequisiteException
	extends IllegalStateException
{

	public UnfulfillablePrerequisiteException()
	{
		// Nothing to do
	}

	public UnfulfillablePrerequisiteException(String aArg0)
	{
		super(aArg0);
	}

	public UnfulfillablePrerequisiteException(Throwable aArg0)
	{
		super(aArg0);
	}

	public UnfulfillablePrerequisiteException(String aArg0, Throwable aArg1)
	{
		super(aArg0, aArg1);
	}
}
