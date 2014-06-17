/*******************************************************************************
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *   
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *   
 *   http://www.apache.org/licenses/LICENSE-2.0
 *   
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.dkpro.lab.task;

/**
 * Exception thrown when all sub-tasks of a batch task have been deferred because a prerequisite
 * could not be fulfilled.
 */
public class UnfulfillablePrerequisiteException
	extends IllegalStateException
{
	private static final long serialVersionUID = -5502439811384893289L;

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
