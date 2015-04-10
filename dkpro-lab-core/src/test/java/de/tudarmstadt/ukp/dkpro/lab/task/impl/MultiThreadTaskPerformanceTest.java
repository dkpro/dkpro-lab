package de.tudarmstadt.ukp.dkpro.lab.task.impl;

import de.tudarmstadt.ukp.dkpro.lab.Lab;
import de.tudarmstadt.ukp.dkpro.lab.engine.TaskContext;
import de.tudarmstadt.ukp.dkpro.lab.storage.impl.PropertiesAdapter;
import de.tudarmstadt.ukp.dkpro.lab.task.Dimension;
import de.tudarmstadt.ukp.dkpro.lab.task.ParameterSpace;
import de.tudarmstadt.ukp.dkpro.lab.task.Task;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.io.File;
import java.util.*;

/**
 * @author Ivan Habernal
 */
public class MultiThreadTaskPerformanceTest
{
    private BatchTask batchTask;

    @Rule
    public TestName name = new TestName();

    @Before
    public void setup()
    {
        File path = new File("target/repository/" + getClass().getSimpleName() + "/"
                + name.getMethodName());
        System.setProperty("DKPRO_HOME", path.getAbsolutePath());
        FileUtils.deleteQuietly(path);

        //        batchTask = new BatchTask();
        batchTask = new MultiThreadBatchTask();
    }

    static class DummyTask
            extends ExecutableTaskBase
    {
        @Override
        public void execute(TaskContext aContext)
                throws Exception
        {
            Properties data = new Properties();
            data.setProperty("key", "value");

            aContext.storeBinary("DATA", new PropertiesAdapter(data));
        }
    }

    @Test
    public void testRandomWiring()
            throws Exception
    {
        Random random = new Random(0);

        List<List<Task>> layersOfTasks = new ArrayList<>();

        int layerSize = 100;
        int layersNumber = 4;
        int importsInEachLayer = 100;

        // create three layers with tasks
        for (int j = 0; j < layersNumber; j++) {
            // add a new layer if needed
            if (layersOfTasks.size() <= j) {
                layersOfTasks.add(new ArrayList<Task>());
            }

            for (int i = 0; i < layerSize; i++) {
                Task t = new DummyTask();

                ((ExecutableTaskBase) t).setType(String.format("%d-%d", j, i));
                layersOfTasks.get(j).add(t);
            }

        }

        // wire tasks in layers
        for (int l = 1; l < layersNumber; l++) {
            for (int j = l - 1; j >= 0; j--) {
                for (int i = 0; i < importsInEachLayer; i++) {
                    Task t1 = layersOfTasks.get(l).get(random.nextInt(layerSize));
                    Task t2 = layersOfTasks.get(j).get(random.nextInt(layerSize));

                    t1.addImport(t2, "DATA");
                }
            }
        }

        // shuffle all tasks
        List<Task> allTasksShuffled = new ArrayList<>();
        for (List<Task> tasks : layersOfTasks) {
            allTasksShuffled.addAll(tasks);
        }
        Collections.shuffle(allTasksShuffled);

        for (Task t : allTasksShuffled) {
            batchTask.addTask(t);
        }

        Lab.getInstance().run(batchTask);
    }
}
