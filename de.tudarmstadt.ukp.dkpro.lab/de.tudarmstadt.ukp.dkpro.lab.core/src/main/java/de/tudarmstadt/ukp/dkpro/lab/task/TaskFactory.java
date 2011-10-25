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
package de.tudarmstadt.ukp.dkpro.lab.task;

import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;

public class TaskFactory
{
	public static void configureTask(Task aTask, Map<String, Object> aConfiguration)
	{
		PropertyAccessor paBean = PropertyAccessorFactory.forBeanPropertyAccess(aTask);
		PropertyAccessor paDirect = PropertyAccessorFactory.forDirectFieldAccess(aTask);
		for (Entry<String, Object> property : aConfiguration.entrySet()) {
			String key = property.getKey();
			Object value = property.getValue();

			// Try setter - there may be extra logic in the setter
			if (paBean.isWritableProperty(key)) {
				paBean.setPropertyValue(key, value);
			}
			// Otherwise try direct access
			else if (paDirect.isWritableProperty(key)) {
				paDirect.setPropertyValue(key, value);
			}
		}
	}

	public static <T extends Task> T createTask(Class<T> aTaskClass,
			Map<String, Object> aConfiguration)
		throws Exception
	{
		T t = aTaskClass.newInstance();
		configureTask(t, aConfiguration);
		return t;
	}
}
