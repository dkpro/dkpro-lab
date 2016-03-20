/*******************************************************************************
 * Copyright 2015
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
package org.dkpro.lab.task.impl;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import org.dkpro.lab.task.Discriminator;
import org.dkpro.lab.task.Property;
import org.springframework.util.ReflectionUtils;

public class ParameterUtil
{
    public static String getName(Annotation aAnnotation)
    {
        if (aAnnotation instanceof Discriminator) {
            String name = ((Discriminator) aAnnotation).name();
            return Discriminator.USE_FIELD_NAME.equals(name) ? null : name;
        }
        else if (aAnnotation instanceof Property) {
            String name = ((Property) aAnnotation).name();
            return Property.USE_FIELD_NAME.equals(name) ? null : name;
        }
        else {
            throw new IllegalArgumentException("Cannot get name from a [" + aAnnotation.getClass()
                    + "]");
       }
    }
    
    public static List<String> findBeanPropertiesWithName(Object aObject, String aName)
    {
        List<String> beanProperties = new ArrayList<>();
        
        ReflectionUtils.doWithFields(aObject.getClass(), 
                f -> { if (aName.equals(getName(f.getAnnotation(Discriminator.class)))) {
                    beanProperties.add(f.getName());
                }; }, 
                f -> { return f.isAnnotationPresent(Discriminator.class); });
        
        ReflectionUtils.doWithFields(aObject.getClass(), 
                f -> { if (aName.equals(getName(f.getAnnotation(Property.class)))) {
                    beanProperties.add(f.getName());
                }; }, 
                f -> { return f.isAnnotationPresent(Property.class); });
        
        return beanProperties;
    }
}
