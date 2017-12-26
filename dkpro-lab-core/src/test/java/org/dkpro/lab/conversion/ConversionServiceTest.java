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
package org.dkpro.lab.conversion;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.dkpro.lab.Lab;
import org.dkpro.lab.engine.TaskContext;
import org.dkpro.lab.task.Dimension;
import org.dkpro.lab.task.Discriminator;
import org.dkpro.lab.task.ParameterSpace;
import org.dkpro.lab.task.Task;
import org.dkpro.lab.task.impl.DefaultBatchTask;
import org.dkpro.lab.task.impl.ExecutableTaskBase;
import org.junit.Before;
import org.junit.Test;

public class ConversionServiceTest
{
    final String KEY = "abcdef";
    static String discriminatorText = null;
    Task consumer;

    @Before
    public void setup()
    {
        File path = new File("target/repository/" + getClass().getSimpleName() + "/");
        System.setProperty("DKPRO_HOME", path.getAbsolutePath());
        FileUtils.deleteQuietly(path);
        initConsumer();
    }

    private void initConsumer()
    {
        consumer = new ExecutableTaskBase()
        {
            @Discriminator(name = KEY)
            protected Integer x;

            @Override
            public void execute(TaskContext aContext)
                throws Exception
            {
                Map<String, String> descriminators = getDescriminators();
                for (String k : descriminators.keySet()) {
                    if (k.contains(KEY)) {
                        discriminatorText = descriminators.get(k);
                        break;
                    }
                }
            }
        };        
    }

    @Test
    public void testDiscriminationWithoutConversionServiceOverride()
        throws Exception
    {
        Integer integer = new Integer(3);
        ParameterSpace ps = new ParameterSpace(Dimension.create(KEY, integer));
        DefaultBatchTask batch = new DefaultBatchTask();
        batch.setParameterSpace(ps);
        batch.addTask(consumer);

        Lab instance = Lab.newInstance(Lab.DEFAULT_CONTEXT);
        instance.run(batch);
        assertEquals("3", discriminatorText);
    }

    @Test
    public void testDiscriminationWithConversionServiceOverride()
        throws Exception
    {
        Integer integer = new Integer(3);
        ParameterSpace ps = new ParameterSpace(Dimension.create(KEY, integer));
        DefaultBatchTask batch = new DefaultBatchTask();
        batch.setParameterSpace(ps);
        batch.addTask(consumer);

        Lab instance = Lab.newInstance(Lab.DEFAULT_CONTEXT);
        // we register an alternative text for the integer value which should be used instead of the
        // default of converting the numerical value to string
        instance.getConversionService().registerDiscriminable(integer, "three");
        instance.run(batch);
        assertEquals("three", discriminatorText);
    }
    
    @Test
    public void testConversionService(){
        Lab instance = Lab.newInstance(Lab.DEFAULT_CONTEXT);
        ConversionService conversionService = instance.getConversionService();
        assertNotNull(conversionService);
        
        String key = "hello";
        conversionService.registerDiscriminable(key, "a Text");
        
        assertTrue(conversionService.isRegistered(key));
        assertEquals("a Text", conversionService.getDiscriminableValue(key));
        
        
    }
}
