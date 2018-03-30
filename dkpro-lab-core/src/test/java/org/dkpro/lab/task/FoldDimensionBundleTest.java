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
package org.dkpro.lab.task;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.dkpro.lab.Lab;
import org.dkpro.lab.engine.TaskContext;
import org.dkpro.lab.storage.filesystem.FileSystemStorageService;
import org.dkpro.lab.task.Dimension;
import org.dkpro.lab.task.Discriminator;
import org.dkpro.lab.task.ParameterSpace;
import org.dkpro.lab.task.Task;
import org.dkpro.lab.task.impl.DefaultBatchTask;
import org.dkpro.lab.task.impl.ExecutableTaskBase;
import org.dkpro.lab.task.impl.FoldDimensionBundle;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public class FoldDimensionBundleTest
{
    @Before
    public void setup()
    {
        String path = "target/repository/"+getClass().getSimpleName()+"/"+name.getMethodName();
        System.setProperty("DKPRO_HOME", new File(path).getAbsolutePath());
    }

    @Test
    public void testSimpleFold()
    {
        Dimension<String> baseData = Dimension.create("base", "1", "2", "3", "4", "5", "6", "7", 
                "8", "9", "10");
        
        FoldDimensionBundle<String> foldBundle = new FoldDimensionBundle<String>("fold", baseData, 3);
        
        String expected = 
                "0 - [1, 4, 7, 10] [2, 5, 8, 3, 6, 9]\n" +
                "1 - [2, 5, 8] [1, 4, 7, 10, 3, 6, 9]\n" +
                "2 - [3, 6, 9] [1, 4, 7, 10, 2, 5, 8]\n";

        StringBuilder actual = new StringBuilder();

        int n = 0;
        ParameterSpace pSpace = new ParameterSpace(foldBundle);
        for (Map<String, Object> config : pSpace) {
            actual.append(String.format("%d - %s %s\n", n, config.get("fold_validation"),
                    config.get("fold_training")));
            n++;
        }
        
        assertEquals(3 , n);
        assertEquals(3, pSpace.getStepCount());
        assertEquals(expected, actual.toString());
    }

    @Test
    public void testFileFold()
    {
        Dimension<String> baseData = Dimension.create("base", "1", "2", "3", "4", "5", "6", "7", 
                "8", "9", "10");
        
        FoldDimensionBundle<String> foldBundle = new FoldDimensionBundle<String>("fold", baseData, 3);
        
        String expected = 
                "0 - [1, 4, 7, 10] [2, 5, 8, 3, 6, 9]\n" +
                "1 - [2, 5, 8] [1, 4, 7, 10, 3, 6, 9]\n" +
                "2 - [3, 6, 9] [1, 4, 7, 10, 2, 5, 8]\n";

        StringBuilder actual = new StringBuilder();

        int n = 0;
        ParameterSpace pSpace = new ParameterSpace(foldBundle);
        for (Map<String, Object> config : pSpace) {
            actual.append(String.format("%d - %s %s\n", n, config.get("fold_validation"),
                    config.get("fold_training")));
            n++;
        }
        
        assertEquals(3 , n);
        assertEquals(3, pSpace.getStepCount());
        assertEquals(expected, actual.toString());
    }
    
    /**
     * Tests that instances in the same cluster (here, parent folder) go into a fold together.
     */
    @Test
    public void testComparator()
    {
            Dimension<String> baseData = Dimension.create("base", "aa/1.txt", "aa/2.txt", "aa/3.txt", 
                            "bb/4.txt", "bb/5.txt", "bb/6.txt", "cc/7.txt",
                            "cc/8.txt", "cc/9.txt", "cc/10.txt");
            
            Comparator<String> comp = new Comparator<String>(){

                @Override
                public int compare(String filename1, String filename2)
                {
                    File file1 = new File(filename1);
                    File file2 = new File(filename2);
                    String folder1 = file1.getParentFile().getName();
                    String folder2 = file2.getParentFile().getName();
                    
                    if(folder1.equals(folder2)){
                        return 0;
                    }
                    return 1;
                }
            };
           
            FoldDimensionBundle<String> foldBundle = new FoldDimensionBundle<String>("fold", baseData, 3, comp);
           
            String expected =
                            "0 - [aa/1.txt, aa/2.txt, aa/3.txt] [bb/4.txt, bb/5.txt, bb/6.txt, cc/7.txt, cc/8.txt, cc/9.txt, cc/10.txt]\n" + 
                            "1 - [bb/4.txt, bb/5.txt, bb/6.txt] [aa/1.txt, aa/2.txt, aa/3.txt, cc/7.txt, cc/8.txt, cc/9.txt, cc/10.txt]\n" + 
                            "2 - [cc/7.txt, cc/8.txt, cc/9.txt, cc/10.txt] [aa/1.txt, aa/2.txt, aa/3.txt, bb/4.txt, bb/5.txt, bb/6.txt]\n";
            

            StringBuilder actual = new StringBuilder();

            int n = 0;
            ParameterSpace pSpace = new ParameterSpace(foldBundle);
            for (Map<String, Object> config : pSpace) {
                    actual.append(String.format("%d - %s %s\n", n, config.get("fold_validation"),
                                    config.get("fold_training")));
                    n++;
            }
           
            assertEquals(3 , n);
            assertEquals(3, pSpace.getStepCount());
            assertEquals(expected, actual.toString());
    }
    
    /**
     * Tests to see that instances are evenly distributed across folds.
     */
    @Test
    public void testFoldDistribution()
    {
            Dimension<String> baseData = Dimension.create("base", "aa/1.txt", "aa/2.txt", "bb/3.txt", 
                            "cc/4.txt", "dd/5.txt", "dd/6.txt", "ee/7.txt",
                            "ff/8.txt", "gg/9.txt", "gg/10.txt");
            
            Comparator<String> comp = new Comparator<String>(){

                @Override
                public int compare(String filename1, String filename2)
                {
                    File file1 = new File(filename1);
                    File file2 = new File(filename2);
                    String folder1 = file1.getParentFile().getName();
                    String folder2 = file2.getParentFile().getName();
                    
                    if(folder1.equals(folder2)){
                        return 0;
                    }
                    return 1;
                }
            };
           
            FoldDimensionBundle<String> foldBundle = new FoldDimensionBundle<String>("fold", baseData, 3, comp);
           
            String expected =
                            "0 - [aa/1.txt, aa/2.txt, ff/8.txt] [bb/3.txt, dd/5.txt, dd/6.txt, cc/4.txt, ee/7.txt, gg/9.txt, gg/10.txt]\n" + 
                            "1 - [bb/3.txt, dd/5.txt, dd/6.txt] [aa/1.txt, aa/2.txt, ff/8.txt, cc/4.txt, ee/7.txt, gg/9.txt, gg/10.txt]\n" + 
                            "2 - [cc/4.txt, ee/7.txt, gg/9.txt, gg/10.txt] [aa/1.txt, aa/2.txt, ff/8.txt, bb/3.txt, dd/5.txt, dd/6.txt]\n";
            

            StringBuilder actual = new StringBuilder();

            int n = 0;
            ParameterSpace pSpace = new ParameterSpace(foldBundle);
            for (Map<String, Object> config : pSpace) {
                    actual.append(String.format("%d - %s %s\n", n, config.get("fold_validation"),
                                    config.get("fold_training")));
                    n++;
            }
//            System.out.println(actual.toString());
           
            assertEquals(3 , n);
            assertEquals(3, pSpace.getStepCount());
            assertEquals(expected, actual.toString());
    }
    
    @Test
    public void testFoldInjection() throws Exception
    {
        File repo = new File("target/repository/"+getClass().getSimpleName()+"/"+name.getMethodName());
        FileUtils.deleteDirectory(repo);
        repo.mkdirs();
        ((FileSystemStorageService) Lab.getInstance().getStorageService()).setStorageRoot(repo);
        
        Dimension<String> baseData = Dimension.create("base", "1", "2", "3", "4", "5", "6", "7", 
                "8", "9", "10");
        
        FoldDimensionBundle<String> foldBundle = new FoldDimensionBundle<String>("fold", baseData, 3);
        
        String expected = 
                "0 - [1, 4, 7, 10] [2, 5, 8, 3, 6, 9]\n" +
                "1 - [2, 5, 8] [1, 4, 7, 10, 3, 6, 9]\n" +
                "2 - [3, 6, 9] [1, 4, 7, 10, 2, 5, 8]\n";

        ParameterSpace pSpace = new ParameterSpace(foldBundle);

        final StringBuilder actual = new StringBuilder();

        Task testTask = new ExecutableTaskBase()
        {
            int n = 0;
            
            @Discriminator
            Collection<String> fold_validation;
            
            @Discriminator
            Collection<String> fold_training;
            
            @Override
            public void execute(TaskContext aContext)
                throws Exception
            {
                System.out.printf("%d training  : %s\n", n, fold_training);
                System.out.printf("%d validation: %s\n", n, fold_validation);
                actual.append(String.format("%d - %s %s\n", n, fold_validation, fold_training));
                n++;
            }
        };
        
        DefaultBatchTask batchTask = new DefaultBatchTask();
        batchTask.setParameterSpace(pSpace);
        batchTask.addTask(testTask);
            
        Lab.getInstance().run(batchTask);
        
        assertEquals(3, pSpace.getStepCount());
        assertEquals(expected, actual.toString());
    }

    @Rule
    public TestName name = new TestName();

    @Before
    public void printSeparator()
    {
        System.out.println("\n=== " + name.getMethodName() + " =====================");
    }
}
