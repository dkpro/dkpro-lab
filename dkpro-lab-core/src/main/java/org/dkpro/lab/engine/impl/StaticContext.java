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
package org.dkpro.lab.engine.impl;

import java.util.HashMap;
import java.util.Map;

import org.dkpro.lab.engine.TaskContext;

/**
 * Helper-class to allow injecting {@link TaskContext} instances into UIMA components. This is
 * actually a bad hack that will not work if the components are deployed in another Java VM.
 */
public class StaticContext
{
	private static Map<String, Object> context = new HashMap<String, Object>();

	public static void bind(String key, Object object)
	{
		context.put(key, object);
	}

	public static void unbind(String key)
	{
		context.remove(key);
	}

	public static Object lookup(String key)
	{
		return context.get(key);
	}
}
