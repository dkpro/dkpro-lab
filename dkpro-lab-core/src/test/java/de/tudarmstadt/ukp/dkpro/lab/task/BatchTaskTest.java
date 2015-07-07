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

import java.io.File;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import de.tudarmstadt.ukp.dkpro.lab.Lab;
import de.tudarmstadt.ukp.dkpro.lab.engine.TaskContext;
import de.tudarmstadt.ukp.dkpro.lab.storage.impl.PropertiesAdapter;
import de.tudarmstadt.ukp.dkpro.lab.task.impl.DefaultBatchTask;
import de.tudarmstadt.ukp.dkpro.lab.task.impl.ExecutableTaskBase;

public class BatchTaskTest
{
    @Before
    public void setup()
    {
        File path = new File("target/repository/" + getClass().getSimpleName() + "/"
                + name.getMethodName());
        System.setProperty("DKPRO_HOME", path.getAbsolutePath());
        FileUtils.deleteQuietly(path);
    }

    @Test(expected = RuntimeException.class)
    public void importTest()
        throws Exception
    {
        Task producer = new ExecutableTaskBase()
        {
            @Override
            public void execute(TaskContext aContext)
                throws Exception
            {
                System.out.println("Running producer");

                Properties data = new Properties();
                data.setProperty("key", "value");

                aContext.storeBinary("DATA", new PropertiesAdapter(data));
            }
        };

        Task consumer = new ExecutableTaskBase()
        {
            @Override
            public void execute(TaskContext aContext)
                throws Exception
            {
                System.out.println("Running consumer");

                Properties data = new Properties();
                aContext.retrieveBinary("DATA", new PropertiesAdapter(data));
                Assert.assertEquals(data.getProperty("key"), "value");
            }
        };

        consumer.addImport(producer, "DATA1", "DATA");

        DefaultBatchTask batch = new DefaultBatchTask();
        batch.addTask(producer);
        batch.addTask(consumer);

        Lab.getInstance().run(batch);
    }

    @Test
    public void testNested()
        throws Exception
    {
        Dimension innerDim = Dimension.create("inner", "1", "2", "3");
        ParameterSpace innerPSpace = new ParameterSpace(innerDim);
        DefaultBatchTask innerTask = new DefaultBatchTask()
        {
            @Override
            public void setConfiguration(Map<String, Object> aConfig)
            {
                super.setConfiguration(aConfig);
                System.out.printf("A %10d %s %s%n", this.hashCode(), getType(), aConfig);
            }
        };
        innerTask.setParameterSpace(innerPSpace);
        innerTask.addTask(new ConfigDumperTask1());

        Dimension outerDim = Dimension.create("outer", "1", "2", "3");
        ParameterSpace outerPSpace = new ParameterSpace(outerDim);
        DefaultBatchTask outerTask = new DefaultBatchTask()
        {
            @Override
            public void setConfiguration(Map<String, Object> aConfig)
            {
                super.setConfiguration(aConfig);
                System.out.printf("B %10d %s %s%n", this.hashCode(), getType(), aConfig);
            }
        };
        outerTask.setParameterSpace(outerPSpace);
        outerTask.addTask(innerTask);
        outerTask.addTask(new ConfigDumperTask2());

        Lab.getInstance().run(outerTask);
    }

    @Test
    public void testNested2()
        throws Exception
    {
        DefaultBatchTask innerTask = new DefaultBatchTask()
        {
            @Discriminator
            private Integer outer;

            @Override
            public ParameterSpace getParameterSpace()
            {
                // Dynamically configure parameter space of nested batch task
                Integer[] values = new Integer[outer];
                for (int i = 0; i < outer; i++) {
                    values[i] = i;
                }
                Dimension<Integer> innerDim = Dimension.create("inner", values);
                ParameterSpace innerPSpace = new ParameterSpace(innerDim);
                return innerPSpace;
            }
            
            @Override
            public void setConfiguration(Map<String, Object> aConfig)
            {
                super.setConfiguration(aConfig);
                System.out.printf("A %10d %s %s%n", this.hashCode(), getType(), aConfig);
            }
        };
        innerTask.addTask(new ConfigDumperTask1());

        Dimension<Integer> outerDim = Dimension.create("outer", 1, 2, 3);
        ParameterSpace outerPSpace = new ParameterSpace(outerDim);
        DefaultBatchTask outerTask = new DefaultBatchTask()
        {
            @Override
            public void setConfiguration(Map<String, Object> aConfig)
            {
                super.setConfiguration(aConfig);
                System.out.printf("B %10d %s %s%n", this.hashCode(), getType(), aConfig);
            }
        };
        outerTask.setParameterSpace(outerPSpace);
        outerTask.addTask(innerTask);
        outerTask.addTask(new ConfigDumperTask2());

        Lab.getInstance().run(outerTask);
    }

    @Test(expected = RuntimeException.class)
    public void testUnresolvable()
        throws Exception
    {
        Dimension<String> dim = Dimension.create("param", "1", "2", "3");

        ParameterSpace pSpace = new ParameterSpace(dim);

        Task task1 = new ExecutableTaskBase()
        {
            @Discriminator
            private String param;

            @Override
            public void execute(TaskContext aContext)
                throws Exception
            {
                // Nothing to do
            }
        };

        Task task2 = new ExecutableTaskBase()
        {
            @Discriminator
            private String param;

            @Override
            public void execute(TaskContext aContext)
                throws Exception
            {
                // Nothing to do
            }
        };

        task2.addImport(task1, "DUMMY");
        task1.addImport(task2, "DUMMY");

        DefaultBatchTask batchTask = new DefaultBatchTask();
        batchTask.setParameterSpace(pSpace);
        batchTask.addTask(task1);
        batchTask.addTask(task2);

        Lab.getInstance().run(batchTask);
    }

    public static class ConfigDumperTask1
        extends ExecutableTaskBase
        implements ConfigurationAware
    {
        @Discriminator
        private String inner;

        private Map<String, Object> config;

        @Override
        public void execute(TaskContext aContext)
            throws Exception
        {
            System.out.printf("C %10d %s %s%n", this.hashCode(), getType(), config);
        }

        @Override
        public void setConfiguration(Map<String, Object> aConfig)
        {
            config = aConfig;
        }
    }

    public static class ConfigDumperTask2
        extends ExecutableTaskBase
        implements ConfigurationAware
    {
        @Discriminator
        private String outer;

        private Map<String, Object> config;

        @Override
        public void execute(TaskContext aContext)
            throws Exception
        {
            System.out.printf("D %10d %s %s%n", this.hashCode(), getType(), config);
        }

        @Override
        public void setConfiguration(Map<String, Object> aConfig)
        {
            config = aConfig;
        }
    }

    @Rule
    public TestName name = new TestName();

    @Before
    public void printSeparator()
    {
        System.out.println("\n=== " + name.getMethodName() + " =====================");
    }
}
