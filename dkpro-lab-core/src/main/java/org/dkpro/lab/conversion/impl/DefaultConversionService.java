/*******************************************************************************
 * Copyright 2016
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
package org.dkpro.lab.conversion.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.dkpro.lab.conversion.ConversionService;

public class DefaultConversionService
	implements ConversionService
{
	private Map<Object,String> services = new HashMap<>();

    @Override
    public void registerDiscriminable(Object aObject, String aDescription)
    {
        services.put(aObject, aDescription);
    }
    
    @Override
    public String getDiscriminableValue(Object aObject){
        return services.get(aObject);
    }

    @Override
    public boolean isRegistered(Object aObject)
    {
        return services.containsKey(aObject);
    }

    @Override
    public Set<Object> getRegisteredKeys()
    {
        return services.keySet();
    }

}
