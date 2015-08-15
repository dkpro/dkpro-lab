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
package de.tudarmstadt.ukp.dkpro.lab.storage;

import org.springframework.dao.DataAccessResourceFailureException;

/**
 * Exception thrown when a task context could not be found, meaning that a task (possibly with a
 * certain configuration of discriminators) has never been executed or is outside of the scope.
 */
public class TaskContextNotFoundException
	extends DataAccessResourceFailureException
{
	private static final long serialVersionUID = -4904156192329856494L;

	public TaskContextNotFoundException(String aMsg)
	{
		super(aMsg);
	}
}
