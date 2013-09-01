/*******************************************************************************
 * Copyright 2011
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
package de.tudarmstadt.ukp.dkpro.lab.groovy.task;

import groovy.lang.Closure;

class Constraint implements de.tudarmstadt.ukp.dkpro.lab.task.Constraint {
	private Closure closure;

	public Constraint(aClosure) {
		closure = aClosure;
	}

	public Constraint(Closure aClosure) {
		closure = aClosure;
	}

	public boolean isValid(java.util.Map aConfiguration) {
		closure.call(aConfiguration);
	}
}
