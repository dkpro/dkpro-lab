/*******************************************************************************
 * Copyright 2011
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
package de.tudarmstadt.ukp.dkpro.lab.task.impl;

import static de.tudarmstadt.ukp.dkpro.lab.storage.StorageService.CONTEXT_ID_SCHEME;
import static de.tudarmstadt.ukp.dkpro.lab.storage.StorageService.LATEST_CONTEXT_SCHEME;
import static de.tudarmstadt.ukp.dkpro.lab.storage.filesystem.FileSystemStorageService.isStaticImport;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.ws.rs.core.UriBuilder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.tudarmstadt.ukp.dkpro.lab.Util;
import de.tudarmstadt.ukp.dkpro.lab.engine.TaskContext;
import de.tudarmstadt.ukp.dkpro.lab.reporting.Report;
import de.tudarmstadt.ukp.dkpro.lab.storage.StorageService;
import de.tudarmstadt.ukp.dkpro.lab.storage.impl.PropertiesAdapter;
import de.tudarmstadt.ukp.dkpro.lab.task.Discriminator;
import de.tudarmstadt.ukp.dkpro.lab.task.Property;
import de.tudarmstadt.ukp.dkpro.lab.task.Task;
import de.tudarmstadt.ukp.dkpro.lab.task.TaskContextMetadata;

public class TaskBase
	implements Task
{
	private final Log log = LogFactory.getLog(getClass());

	private String type;
	private Map<String, String> imports;
	private Map<String, String> properties;
	private Map<String, String> discriminators;
	private Set<Class<? extends Report>> reports;

	{
		properties = new HashMap<String, String>();
		discriminators = new HashMap<String, String>();
		reports = new HashSet<Class<? extends Report>>();
		imports = new HashMap<String, String>();
	}

	public TaskBase()
	{
		setType(getClass().getName());
	}

	public TaskBase(String aType)
	{
		setType(aType);
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
	public void setProperty(String aKey, String aValue)
	{
		if (aKey == null) {
			throw new IllegalArgumentException("Must specify a key");
		}
		if (aValue == null) {
			properties.remove(aKey);
		}
		else {
			properties.put(aKey, aValue);
		}
	}

	@Override
	public String getProperty(String aKey)
	{
		return properties.get(aKey);
	}

	@Override
	public Map<String, String> getProperties()
	{
		analyze(getClass(), Property.class, properties);
		return properties;
	}

	@Override
	public void setDescriminator(String aKey, String aValue)
	{
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
		return discriminators.get(aKey);
	}

	@Override
	public Map<String, String> getDescriminators()
	{
		analyze(getClass(), Discriminator.class, discriminators);
		return discriminators;
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
	public Map<String, String> getImports()
	{
		return imports;
	}

	@Override
	public void addReport(Class<? extends Report> aReport)
	{
		reports.add(aReport);
	}

	@Override
	public void removeReport(Class<? extends Report> aReport)
	{
		reports.remove(aReport);
	}

	public void setReports(Set<Class<? extends Report>> aReports)
	{
		reports = new HashSet<Class<? extends Report>>(aReports);
	}

	@Override
	public Set<Class<? extends Report>> getReports()
	{
		return reports;
	}

	@Override
	public void persist(final TaskContext aContext)
		throws IOException
	{
		aContext.storeBinary(PROPERTIES_KEY, new PropertiesAdapter(getProperties(), "Task properties"));

		aContext.storeBinary(DISCRIMINATORS_KEY, new PropertiesAdapter(getResolvedDescriminators(aContext)));
	}

	private void analyze(Class<?> aClazz, Class<? extends Annotation> aAnnotation, Map<String, String> props)
	{
		if (aClazz.getSuperclass() != null) {
			analyze(aClazz.getSuperclass(), aAnnotation, props);
		}

		for (Field field : aClazz.getDeclaredFields()) {
			field.setAccessible(true);
			try {
				if (field.isAnnotationPresent(aAnnotation)) {
					String name = getClass().getName()+"|"+field.getName();
					String value = Util.toString(field.get(this));
					props.put(name, value);
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
}
