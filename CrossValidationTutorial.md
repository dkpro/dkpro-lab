# Cross validation tutorial #

This is a brief introduction to running an experiment that has a cross validation part. The most important things to learn in this example is, how to set a parameter space dimension which is created dynamically via a nested `BatchTask` and how to use a `FoldDimensionBundle`.

## Basic tasks ##

Assume you have these tasks:

  * `preprocessingTask`: this task does some basic preprocessing of your data. The data produced by this task is the data that you want to run your cross validation experiment on. So in several iterations, some part of this data is used to train a classifier (_trainingSet_), while the rest of the data is used to evaluate the quality of the classifier (_testSet_).
  * `featureExtractionTask`: this task extracts the features from the  to train the classifier from the preprocessed data of the _trainingSet_. Separating this task from the `preprocessingTask` allows you to have run your experiment with different feature sets but while only doing the preprocessing once.
  * `trainingTask`: this task trains a classifier from the extracted features.
  * `validationTask`: this task finally evaluates the classifier on _testSet_.
  * `batchTask`: this tasks runs all the four task above.

## Adding a dimension for n-fold cross-validation ##

With such a setup, the data which is the input for the cross validation is unknown, until the `preprocessingTask` has completed. Only when it is done, the n-fold cross validation can be set up during which part of the data is assigned to the _trainingSet_ and _testSet_.

In the conceptualization of the Lab, a the values assigned to the _trainingSet_ and _evaluationSet_ in each fold correspond to a parameter space dimension. The Lab provides a special `FoldDimensionBundle` to create a n-fold assignment to these sets. An illustration

```
Dimension<String> data = Dimension.create("data", "1", "2", "3", "4", "5", "6");
FoldDimensionBundle<String> foldBundle = new FoldDimensionBundle<String>("fold", data, 3);
```

The _foldBundle_ dimension produces three assignments for two parameters: `fold_training` (the _trainingSet_) and `fold_validation` (the _testSet_ - sorry, this should probably be called `fold_test` ...):

| **Assignment** | **_fold\_training_** | **_fold\_validation_** |
|:---------------|:---------------------|:------------------------|
| 1 | 2, 3, 5, 6 | 1, 4 |
| 2 | 1, 3, 4, 6 | 2, 5 |
| 3 | 1, 2, 4, 5 | 3, 6 |

The problem in this illustration is, that the `foldBundle` dimension is created statically from the `data` dimension, but for our cross validation experiment, we need to create it from the data that was created by the `preprocessingTask`. So how can we create the `foldBundle` dynamically?

To solve this problem, we introduce another batch task and change the batch task we already have:
  * `crossValidationTask`: this task runs the `featureExtractionTask`, the `trainingTask` and the `validationTask`
  * `batchTask` (modified): this tasks now runs the `preprocessingTask` and the `crossValidationTask`.

## Adding a cross-validation task ##

Now, instead of statically setting up the parameter space for the `crossValidationTask`, we set it up dynamically by overriding the `execute()` method. In the following example, we assume that the `preprocessingTask` has produced some `XMI` data, which is the data on which we want to perform the cross-validation. The `featureExtractionTask` requires two parameters, `filesRoot` (the base directory of the data) and `files_training` (the data in the _trainingSet_). The `validationTask`also requires `filesRoot`and it needs `files_validation` (the _testSet_ ) to evaluate the classifier.

```
   | BatchTask crossValidationTask = new BatchTask() {
   |   public void execute(TaskContext aContext) throws Exception {
1  |     File xmiPathRoot = aContext.getStorageLocation("XMI", AccessMode.READONLY);
2  |     Collection<File> files = FileUtils.listFiles(xmiPathRoot, new String[] { "xmi" }, false);
3  |     String[] fileNames = new String[files.size()];
4  |     int i = 0;
5  |     for (File f : files) { fileNames[i] = f.getName(); i++; }
6  |     Arrays.sort(fileNames);
   |
7  |     FoldDimensionBundle<String> foldDim = new FoldDimensionBundle<String>("files", 
8  |       Dimension.create("", fileNames), 10);
9  |     Dimension<File> filesRootDim = Dimension.create("filesRoot", xmiPathRoot);
   |				
10 |     ParameterSpace pSpace = new ParameterSpace(foldDim, filesRootDim);
11 |     setParameterSpace(pSpace);
   |				
12 |     super.execute(aContext);
   |   }
   | };
13 | crossValidationTask.addImportLatest("XMI", "XMI", preprocessTask.getType());
14 | crossValidationTask.addTask(featureExtractionTask);
15 | crossValidationTask.addTask(trainingTask);
16 | crossValidationTask.addTask(validationTask);
   | 
17 | BatchTask batchTask = new BatchTask();
18 | batchTask.setParameterSpace(pSpace);
19 | batchTask.addTask(preprocessTask);
20 | batchTask.addTask(crossValidationTask);
```

  * **line 1-6**: create a sorted list of the file names which the `preprocessingTask` has stored under the key `XMI`.
  * **line 7-8**: create a 10-fold `FoldDimensionBundle` called `files` which will set the parameters `files_training` and `files_validation`.
  * **line 9**: create a constant dimension which sets the parameter `files_root`.
  * **line 10-11**: create the parameter space for the `crossValidationTask` for the parameters `files_training`, `files_validation` and _files\_root_. Since we later add the `crossValidationTask` to the `BatchTask`, the `crossValidationTask` also inherits all parameters from the parameter space of the `BatchTask`.
  * **line 12**: finally, run the cross validation
  * **line 13**: import the `XMI` key from the `PreprocessingTask` into the `CrossValidationTask`.
  * **line 14-16**: add the sub-tasks that perform the cross-validation.
  * **line 17-20**: configure the overall batchTask that runs the preprocessing and the cross-validation.

## Using `XmiReader` in a cross-validation experiment ##

The following snipped illustrates how to use the _trainingSet_ parameter `files_training_` and `filesRoot` to configure the DKPro Core ASL `XmiReader` component.

```
   | Task featureExtractionTask = new UimaTaskBase() {
   | @Discriminator private File filesRoot;
   | @Discriminator private Collection<String> files_training;
   | 			
   | public CollectionReaderDescription getCollectionReaderDescription(TaskContext aContext)
   |   throws ResourceInitializationException, IOException {
   |   Collection<String> patterns = new ArrayList<String>();
   |   for (String f : files_training) {
   |     patterns.add(XmiReader.INCLUDE_PREFIX+f);
   |   }
   | 
   |   return createReader(XmiReader.class,
   |     XmiReader.PARAM_PATH, filesRoot,
   |     XmiReader.PARAM_PATTERNS, patterns);
   |   }
   | /** ... getAnalysisEngineDescription() omitted ... */
   | };
```

## Caveat ##

Mind, that it is currently not tested to import data across batch task boundaries. That is, in the example above, the `featureExtractionTask` does not directly import data from the `preprocessingTask`. Instead, the `crossvalidationTask` imports the data from the `preprocessingTask` and forwards it to the `featureExtractionTask` via the file names in the fold dimension.

## Summary ##

If a cross-validation task depends on the output of a preprocessing task, it is impossible to set up a static parameter dimension for the _trainingSet_ and _testSet_, because it depends on the data created by the preprocessing task. The tutorial has illustrated how to create a nested batch task which dynamically creates its own parameter space using a `FoldDimensionBundle` based on the output of the preprocessing task.