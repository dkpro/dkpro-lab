<h1>Task Lifecycle and Configuration</h1>



# Lifecycle basics #

In DKPro Lab, a task instantiated by the user. This allows it to pre-configure the task before it is handed over to the framework. While still under the control of the user, the task usually passes through the following stages:

  * **instantiation:** the task is instantiated by the user
  * **configuration:** the task is configured by the user
  * **run:** the user invokes `Lab.run()`. At this point, the control over the task is handed over to the framework.

## Primitive task lifecycle ##

A primitive task, i.e. one that is not running within a batch task, then passes through the following stages:

  * **engine lookup:** the framework locates the engine responsible for executing the task.
  * **context setup:** the execution engine creates a new task context. The framework tries to resolve data dependencies at this point - if the dependencies cannot be resolved, this fails.
  * **engine setup:** the engine performs preparative steps necessary before the task can run
  * **lifecycle event "initialize":** signal that the task has been fully initialized. The default lifecycle manager stores the values of the discriminators and task properties in the task context at this point.
  * **lifecycle event "begin":** signal that the task execution is about to begin. The default lifecycle manager enforces several JVM garbage collection runs to free up memory and records the start time.
  * **engine execution** the engine performs the actual task
  * **lifecycle event "complete":** if the task completed successfully. The default lifecycle manager records the end time of the task, runs all reports registered on the task and stores the task metadata in the context to record that the task has completed.
  * **lifecycle event "fail"**: if the task failed. The default lifecycle manager deletes the task context and logs a failure message.

## Batch task and subtask lifecycle ##

The framework was built for parameter sweeping experiments, so most setup involve one or more batch tasks. The batch task itself is a primitive task which passes through the stages outlined above. Subtasks running within the batch task, however, pass through additional stages which are executed for every location in the parameter space:

  * **configuration:** all tasks in the batch are configured with the parameter at the current location in the parameter space
  * **check for previous execution:** usually only a subset of the parameters in the parameter space apply to a particular task. Consequently, the output produced by a task for a particular parameter configuration is valid for every position in the parameter space in which the parameters applicable to the task are constant. If the task has already been executed for a particular parameter combination, it does not need to be executed again.
  * **context setup:** if the data-dependencies cannot be resolved, the task execution is deferred and the next task in the batch is tried. (cf. _context setup_ in primitive task lifecycle)
  * **loop detection:** if all task executions fail due to unresolvable data-depndencies, the batch task is aborted.
  * **subtask execution:** primitive task lifecycle for the subtask is executed (see above, except _context setup_)
  * **scope extension:** the task execution is added to the batch task scope.

# Static configuration #

Since a task is instantiated by the user, it is possible to pre-configure the task before it is handed over to the framework. In a typical scenario, data dependencies are pre-configured by the user while all other parameters are managed via the parameter space.
All invariant parameters and data-dependencies can be configured at this point. Some users prefer this over maintaining even invariant parameters in the parameter space.

# Dynamic data-dependencies #

The framework resolves data-dependencies only after the task has been configured. This allows the task to configure data-dependencies dynamically, depending on parameters. To configure a data-dependency based on a parameter X, implement a setter for X in the task and use the `addImportLatest()` method in the setter to configure the dependency, e.g.:

```
  @Discriminator
  String preprocessingTaskType;

  void setPreprocessingTask(String aPreprocessingTaskType) {
    preprocessingTaskType = aPreprocessingTaskType;
    addImportLatest("KEY", "KEY", preprocessingTaskType);
  }
```

If a certain data-dependency is not required, it is important that the dependency is explictly removed. This easiers way to do this in a setter is:

```
    getImports().remove("KEY");
```

# Dynamic workflows #

The framework allows completely dynamic workflows, because it creates no global execution plan. Not only is it possible to configure data-dependencies based on parameter configurations, but is is possible to dynamically create the whole set of tasks including additional parameters that should be executed for a particular parameter configuration. To archive this, create a custom batch task class and override the `execute()` method of the batch task. In your custom execute() method, you can use methods such as `setTasks()` and `setParameterSpace()` to configure the tasks that need to be executed and their parameters. Your custom batch task can have discriminator fields as any other task can have them. Be aware that the parameter configurations of nested batch tasks are additive. Assume an outer batch task O with a parameter space `{A: 1}` and an inner batch task I with a parameter space `{B: 1}`, a task T running within the inner batch task is configured with `{A: 1, B: 1}`.

An example for a dynamically created parameter space is given in the [cross-validation tutorial](CrossValidationTutorial.md).

## Data-dependencies with nested batch tasks ##

Assume an outer batch task O. Within O runs a primitive task T1 and an inner batch task I. Within I runs a second primitive task T2. It should be possible that T2 declares a data-dependency on data produced by T1 because the inner batch task I inherits the scope the outer task O.

However, it is not possible that a task T3 running within the outer batch task O declares a data-dependency on T2. While T3 lives in the parameter space of O, T2 lives in the combined parameter space of O and I. Thus, T2 is potentially executed more than once when T3 is executed only once. Since a data-dependency must be uniquely resolvable to a particular task context, and for one execution of T3 there can be several applicable executions of T2, a direct data-dependency is not possible. It is possible, though, to implement and register a report on I which aggregates the data of all executions of T2 and stores it in the task context of I. T3 can then declare a dependency on I to fetch the aggregated data.

# Dynamic reports #

Reports can be dynamically configured as necessary either by implementing a setter or as part of setting up a dynamic workflow. If set up using a setter, reports need to be explicitly removed from a task if they are not needed. The easiest way to do this in a setter is:

```
   getReports().removeReport(ReportClass.class)
```