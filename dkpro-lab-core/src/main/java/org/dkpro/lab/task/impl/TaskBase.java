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
package org.dkpro.lab.task.impl;

import static org.dkpro.lab.storage.StorageService.CONTEXT_ID_SCHEME;
import static org.dkpro.lab.storage.StorageService.LATEST_CONTEXT_SCHEME;
import static org.dkpro.lab.storage.filesystem.FileSystemStorageService.isStaticImport;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.core.UriBuilder;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dkpro.lab.Util;
import org.dkpro.lab.conversion.ConversionService;
import org.dkpro.lab.engine.TaskContext;
import org.dkpro.lab.reporting.Report;
import org.dkpro.lab.storage.StorageService;
import org.dkpro.lab.storage.impl.PropertiesAdapter;
import org.dkpro.lab.task.Discriminator;
import org.dkpro.lab.task.Property;
import org.dkpro.lab.task.Task;
import org.dkpro.lab.task.TaskContextMetadata;

public class TaskBase
	implements Task
{
	private final Log log = LogFactory.getLog(getClass());

	private String type;
	private Map<String, String> imports;
	private Map<String, String> attributes;
	private Map<String, String> discriminators;
	private Map<String, String> analyzedAttributes;
    private Map<String, String> analyzedDiscriminators;
	private List<Class<? extends Report>> reports;
	
	private boolean initialized = false;
	private boolean didRun = false;

    private TaskContext aContext;

	{
		attributes = new HashMap<String, String>();
		analyzedAttributes = new HashMap<String, String>();
		discriminators = new HashMap<String, String>();
		analyzedDiscriminators = new HashMap<String, String>();
		reports = new ArrayList<Class<? extends Report>>();
		imports = new HashMap<String, String>();
	}

	/**
	 * Create a new task with a default type name. The type is the simple name of the class. If 
	 * it is an inner class, only the name of the inner class is used. Be careful that type names
	 * must uniquely identify a task class.
	 */
	public TaskBase()
	{
		String className = getClass().getName();
		int innerClassSep = className.lastIndexOf('$');
		if (innerClassSep != 1) {
			String innerName = className.substring(innerClassSep+1);
			if (!StringUtils.isNumeric(innerName)) {
				className = innerName;
			}
		}
		setType(className);
	}

	public TaskBase(String aType)
	{
		setType(aType);
	}

	@Override
	public void initialize(TaskContext aContext)
	{
        this.aContext = aContext;
        initialized = true;
	}
	
	@Override
    public boolean isInitialized()
	{
	    return initialized;
	}
	
	@Override
	public final void analyze()
	{
	    analyzedAttributes = new HashMap<String, String>();
        analyzedDiscriminators = new HashMap<String, String>();
        analyze(getClass(), Property.class, analyzedAttributes);
        analyze(getClass(), Discriminator.class, analyzedDiscriminators);
	}
	
	@Override
	public void destroy(TaskContext aContext)
	{
	    initialized = false;
	}
	
	public void setType(String aType)
	{
		if (aType == null) {
			throw new IllegalArgumentException("Must specify a type");
		}
		type = aType;
	}

	@Override
	public String getType()
	{
		return type;
	}

	@Override
	public void setAttribute(String aKey, String aValue)
	{
	    if(didTaskRun()){
	        throw new IllegalStateException("[An already executed Task cannot be modified]");
	    }
	    
		if (aKey == null) {
			throw new IllegalArgumentException("Must specify a key");
		}
		if (aValue == null) {
			attributes.remove(aKey);
		}
		else {
			attributes.put(aKey, aValue);
		}
	}

	@Override
	public String getAttribute(String aKey)
	{
	    String value = analyzedAttributes.get(aKey);
	    if(value != null){
	        return value;
	    }
	    
		return attributes.get(aKey);
	}

	@Override
	public Map<String, String> getAttributes()
	{
	    Map<String, String> allAttributes = new HashMap<>();
	    allAttributes.putAll(attributes);
	    allAttributes.putAll(analyzedAttributes);
	    
		return allAttributes;
	}

	@Override
	public void setDescriminator(String aKey, String aValue)
	{
	    if(didTaskRun()){
            throw new IllegalStateException("[An already executed Task cannot be modified]");
        }
	    
		if (aKey == null) {
			throw new IllegalArgumentException("Must specify a key");
		}
		if (aValue == null) {
			discriminators.remove(aKey);
		}
		else {
			discriminators.put(aKey, aValue);
		}
	}

	@Override
	public String getDescriminator(String aKey)
	{
	    String value = analyzedDiscriminators.get(aKey);
	    if(value != null){
	        return value;
	    }
	    
		return discriminators.get(aKey);
	}

	@Override
	public Map<String, String> getDescriminators()
	{
	    Map<String,String> allDiscriminators = new HashMap<>();
        allDiscriminators.putAll(discriminators);
        allDiscriminators.putAll(analyzedDiscriminators);
		return allDiscriminators;
	}

	@Override
	public Map<String, String> getResolvedDescriminators(TaskContext aContext)
	{
		StorageService storageService = aContext.getStorageService();
		Map<String, String> descs = new HashMap<String, String>();
		descs.putAll(getDescriminators());

		// Load previous discriminators and check that the do not conflict with discriminators
		// defined in this task
		for (String rawUri : aContext.getMetadata().getImports().values()) {
			URI uri = URI.create(rawUri);

			if (isStaticImport(uri)) {
				continue;
			}

			final TaskContextMetadata meta = aContext.resolve(uri);

			Map<String, String> prerequisiteDiscriminators = storageService.retrieveBinary(
					meta.getId(), DISCRIMINATORS_KEY, new PropertiesAdapter()).getMap();

			for (Entry<String, String> e : prerequisiteDiscriminators.entrySet()) {
				if (descs.containsKey(e.getKey()) && !descs.get(e.getKey()).equals(e.getValue())) {
					throw new IllegalStateException("Discriminator [" + e.getKey()
							+ "] in task [" + getType() + "] conflicts with dependency ["
							+ meta.getType() + "]");
				}
				descs.put(e.getKey(), e.getValue());
			}
		}
		return descs;
	}

	@Deprecated
    @Override
	public void addImport(String aKey, String aUri)
	{
		if (aKey == null) {
			throw new IllegalArgumentException("Must specify a key");
		}
		if (aUri == null) {
			throw new IllegalArgumentException("Must specify a URI");
		}
		imports.put(aKey, aUri);
	}

	@Deprecated
    @Override
	public void addImportById(String aKey, String aUuid, String aSourceKey)
	{
		if (aKey == null) {
			throw new IllegalArgumentException("Must specify a key");
		}
		if (aSourceKey == null) {
			throw new IllegalArgumentException("Must specify a source key");
		}
		if (aUuid == null) {
			throw new IllegalArgumentException("Must specify a task id");
		}
		imports.put(aKey, CONTEXT_ID_SCHEME+"://"+aUuid+"/"+aSourceKey);
	}

	@Deprecated
    @Override
	public void addImportLatest(String aKey, String aSourceKey, String aType)
	{
		if (aKey == null) {
			throw new IllegalArgumentException("Must specify a key");
		}
		if (aSourceKey == null) {
			throw new IllegalArgumentException("Must specify a source key");
		}
		if (aType == null) {
			throw new IllegalArgumentException("Must specify a type");
		}
		imports.put(aKey, LATEST_CONTEXT_SCHEME+"://"+aType+"/"+aSourceKey);
	}

	@Deprecated
    @Override
	public void addImportLatest(String aKey, String aSourceKey, String aType, String... aConstraints)
	{
		if (aKey == null) {
			throw new IllegalArgumentException("Must specify a key");
		}
		if (aSourceKey == null) {
			throw new IllegalArgumentException("Must specify a source key");
		}
		if (aType == null) {
			throw new IllegalArgumentException("Must specify a type");
		}
		if ((aConstraints.length % 2) != 0) {
			throw new IllegalArgumentException("Restrictions must be key/value pairs and " +
					"therefore have be represented by an even number of parameters");
		}

		UriBuilder ub = UriBuilder.fromUri(LATEST_CONTEXT_SCHEME+"://"+aType+"/"+aSourceKey);

		for (int i = 0; i < aConstraints.length; i += 2) {
			String key = aConstraints[i];
			String value = aConstraints[i+1];

			ub.queryParam(key, value);
		}

		imports.put(aKey, ub.build().toString());
	}

	@Deprecated
    @Override
	public void addImportLatest(String aKey, String aSourceKey, String aType,
			Map<String, String> aRestrictions)
	{
		int i = 0;
		String[] constraints = new String[aRestrictions.size()*2];
		for (Entry<String, String> e : aRestrictions.entrySet()) {
			constraints[i++] = e.getKey();
			constraints[i++] = e.getValue();
		}

		addImportLatest(aKey, aSourceKey, aType, constraints);
	}

	@Override
	public void addImport(File aFile, String aKey)
	{
	    addImport(aFile.toURI(), aKey);
	}
	
	@Override
	public void addImport(URI aUri, String aKey)
	{
	    addImport(aKey, aUri.toString());
	}
	
	@Override
	public void addImport(Task aTask, String aKey)
	{
	    addImport(aTask, aKey, aKey);
	}
	
	@Override
	public void addImport(Task aTask, String aKey, String aAlias)
	{
	    addImportLatest(aAlias, aKey, aTask.getType());
	}
	
	@Override
	public void addImport(TaskContext aTaskContext, String aKey, String aAlias)
	{
	    addImportById(aAlias, aTaskContext.getId(), aKey);
	}
	
	@Override
	public Map<String, String> getImports()
	{
		return imports;
	}

	@Override
	public void addReport(Class<? extends Report> aReport)
	{
		if (aReport == null) {
			throw new IllegalArgumentException("Report class cannot be null.");
		}
		reports.add(aReport);
	}

	@Override
	public void removeReport(Class<? extends Report> aReport)
	{
		reports.remove(aReport);
	}

	public void setReports(List<Class<? extends Report>> aReports)
	{
		reports = new ArrayList<Class<? extends Report>>(aReports);
	}

	@Override
	public List<Class<? extends Report>> getReports()
	{
		return reports;
	}

	@Override
	public void persist(final TaskContext aContext)
		throws IOException
	{
        if (!initialized) {
            throw new IllegalStateException(
                    "Task not initialized. Maybe forgot to call super.initialize(ctx) in ["
                            + getClass().getName() + "]?");
        }
	    
		aContext.storeBinary(ATTRIBUTES_KEY, new PropertiesAdapter(getAttributes(), "Task properties"));

		aContext.storeBinary(DISCRIMINATORS_KEY, new PropertiesAdapter(getResolvedDescriminators(aContext)));
	}

	protected void analyze(Class<?> aClazz, Class<? extends Annotation> aAnnotation, Map<String, String> props)
	{
		if (aClazz.getSuperclass() != null) {
			analyze(aClazz.getSuperclass(), aAnnotation, props);
		}

		for (Field field : aClazz.getDeclaredFields()) {
			field.setAccessible(true);
			try {
				if (field.isAnnotationPresent(aAnnotation)) {
					String name;
					
					Annotation annotation = field.getAnnotation(aAnnotation);
					
                    if (StringUtils.isNotBlank(ParameterUtil.getName(annotation))) {
                        name = getClass().getName() + "|" + ParameterUtil.getName(annotation);
                    }
                    else {
                        name = getClass().getName() + "|" + field.getName();
                    }
					
					String value = Util.toString(field.get(this));
					String oldValue = props.put(name, value);
					if (oldValue != null) {
                        throw new IllegalStateException(
                                "Discriminator/property name must be unique and cannot be used "
                                + "on multiple fields in the same class [" + name + "]");
					}
					
					//Override with conversion service information if available
                    Object object = field.get(this);
                    ConversionService cs = aContext.getConversionService();
                    if(cs.isRegistered(object)){
                        props.put(name, cs.getDiscriminableValue(object));
                    }
					
					log.debug("Found "+aAnnotation.getSimpleName()+" ["+name+"]: "+value);
				}
			}
			catch (IllegalAccessException e) {
				throw new IllegalStateException(e);
			}
			finally {
				field.setAccessible(false);
			}
		}
	}

    @Override
    public void markExecuted()
    {
        didRun = true;
    }

    @Override
    public boolean didTaskRun()
    {
        return didRun;
    }
}
