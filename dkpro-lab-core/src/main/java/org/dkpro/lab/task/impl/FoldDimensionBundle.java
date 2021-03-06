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
package org.dkpro.lab.task.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dkpro.lab.task.Dimension;

public class FoldDimensionBundle<T> extends DimensionBundle<Collection<T>> implements DynamicDimension
{
	private Dimension<T> foldedDimension;
	private List<T>[] buckets;
	private int validationBucket = -1;
	private int folds;
	private Comparator<T> comparator;
	
    public FoldDimensionBundle(String aName, Dimension<T> aFoldedDimension, int aFolds, Comparator<T> aComparator)
    {
        this(aName, aFoldedDimension, aFolds);
        comparator = aComparator;
    }
	public FoldDimensionBundle(String aName, Dimension<T> aFoldedDimension, int aFolds)
	{
		super(aName, new Object[0] );
		foldedDimension = aFoldedDimension;
		folds = aFolds;
		comparator = null;
	}
	
	private void init()
	{
		buckets = new List[folds];
		for(int bucket=0;bucket<buckets.length;bucket++){
			buckets[bucket] = new ArrayList<T>();
		}
		
		// Capture all data from the dimension into buckets, one per fold
		foldedDimension.rewind();
		
		//User controls instances across folds
		if(comparator != null){
		
	        while (foldedDimension.hasNext()) {
	            T newItem = foldedDimension.next();
	            
	            // Check every bucket if the current object belongs there
	            boolean found = false;
	            for(int bucket=0;bucket<buckets.length;bucket++){
	                for (int j=0;j<buckets[bucket].size();j++) {
	                	T item = buckets[bucket].get(j);
	                    if (comparator.compare(item, newItem) == 0) {
	                        // has to go into this bucket!
	                        found = true;
	                        addToBucket(newItem, bucket);
	                        break;
	                    }
	                }
	                if(found == true){
	                	break;
	                }
	            	
	            }
	            
	            // There is no bucket where the current item has to go into, just use the next one.
	            if (!found) {
	            	//put it in the smallest bucket
	            	int smallestBucket = 0;
	                int smallestBucketSize = buckets[smallestBucket].size();
	                for(int bucket=0;bucket<buckets.length;bucket++){
	                	if(buckets[bucket].size() < smallestBucketSize){
	                		smallestBucket = bucket;
	                		smallestBucketSize = buckets[smallestBucket].size();
	                	}
	                }
	                addToBucket(newItem, smallestBucket);
	            }
	        }

		//Default instance division across folds
		}else{
		
			int i = 0;
			while (foldedDimension.hasNext()) {
				int bucket = i % folds;
				
				if (buckets[bucket] == null) {
					buckets[bucket] = new ArrayList<T>();
				}
				
				buckets[bucket].add(foldedDimension.next());
				i++;
			}
		
		
			if (i < folds) {
				throw new IllegalStateException("Requested [" + folds + "] folds, but only got [" + i
						+ "] values. There must be at least as many values as folds.");
			}
		}
		String foldsAndSizes = "";
		for(int bucket=0;bucket<buckets.length;bucket++){
			foldsAndSizes = foldsAndSizes + " fold " + bucket + ": size " + buckets[bucket].size() + ".  ";
			if(buckets[bucket].size() == 0){
				throw new IllegalStateException("Detected an empty fold: " + bucket + ". " + 
			"Maybe your fold control is causing all of your instances to be put in very few buckets?  " + 
						"Previous folds and buckets: " + foldsAndSizes);
			}
		}
	}
    private void addToBucket(T newItem, int bucket){
		if (buckets[bucket] == null) {
			buckets[bucket] = new ArrayList<T>();
		}
		
		buckets[bucket].add(newItem);
    }

	@Override
	public boolean hasNext()
	{
		return validationBucket < buckets.length-1;
	}

	@Override
	public void rewind()
	{
		init();
		validationBucket = -1;
	}

	@Override
	public Map<String, Collection<T>> next()
	{
		validationBucket++;
		return current();
	}

	@Override
	public Map<String, Collection<T>> current()
	{
		List<T> trainingData = new ArrayList<T>();
		for (int i = 0; i < buckets.length; i++) {
			if (i != validationBucket) {
				trainingData.addAll(buckets[i]);
			}
		}
		
		Map<String, Collection<T>> data = new HashMap<String, Collection<T>>();
		data.put(getName()+"_training", trainingData);
		data.put(getName()+"_validation", buckets[validationBucket]);
		
		return data;
	}

	@Override
	public void setConfiguration(Map<String, Object> aConfig)
	{
		if (foldedDimension instanceof DynamicDimension) {
			((DynamicDimension) foldedDimension).setConfiguration(aConfig);
		}
	}
}
