/*******************************************************************************
 * Copyright 2014
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
package org.dkpro.lab.task;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.dkpro.lab.Lab;
import org.dkpro.lab.engine.TaskContext;
import org.dkpro.lab.task.impl.DefaultBatchTask;
import org.dkpro.lab.task.impl.ExecutableTaskBase;
import org.dkpro.lab.task.impl.TaskBase;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public class TaskBaseTest {
    
    @Rule
    public TestName name = new TestName();
    
    @Before
    public void setup()
    {
        File path = new File("target/repository/" + getClass().getSimpleName() + "/"
                + name.getMethodName());
        System.setProperty("DKPRO_HOME", path.getAbsolutePath());
        FileUtils.deleteQuietly(path);
    }

	@Test(expected=IllegalArgumentException.class)
	public void nullReportTest() {
		TaskBase base = new TaskBase();
		base.addReport(null);
	}
	
	@Test(expected=IllegalStateException.class)
	public void settingAttributesNotAllowedAfterTaskRan() throws Exception{
	    
	    Task consumer = new ExecutableTaskBase()
        {
            @Override
            public void execute(TaskContext aContext)
                throws Exception
            {
               //do nothing
            }
        };
        
        //this should still work
        consumer.setAttribute("DUMMY_KEY", "123");

        DefaultBatchTask batch = new DefaultBatchTask();
        batch.addTask(consumer);
        Lab.getInstance().run(batch);
        
        //Task did run - no modification allowed
        consumer.setAttribute("DUMMY_KEY_2", "1234");
	}
	
	@Test(expected=IllegalStateException.class)
    public void settingDiscriminatorsNotAllowedAfterTaskRan() throws Exception{
        
        Task consumer = new ExecutableTaskBase()
        {
            @Override
            public void execute(TaskContext aContext)
                throws Exception
            {
               //do nothing
            }
        };
        
        //this should still work
        consumer.setDescriminator("DUMMY_KEY", "123");

        DefaultBatchTask batch = new DefaultBatchTask();
        batch.addTask(consumer);
        Lab.getInstance().run(batch);
        
        //Task did run - no modification allowed
        consumer.setDescriminator("DUMMY_KEY_2", "1234");
    }
}
