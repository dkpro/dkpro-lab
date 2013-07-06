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
package de.tudarmstadt.ukp.dkpro.lab.uima.task.impl;

import static java.text.MessageFormat.format;
import static org.apache.uima.internal.util.I18nUtil.localizeMessage;

import java.io.OutputStream;
import java.io.PrintStream;

import org.apache.uima.resource.ResourceManager;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;

import de.tudarmstadt.ukp.dkpro.lab.engine.TaskContext;

public class UimaLoggingAdapter
	implements Logger
{
	private TaskContext taskContext;
	private ResourceManager resMgr;

	public UimaLoggingAdapter(TaskContext aTaskContext)
	{
		setTaskContext(aTaskContext);
	}

	public void setTaskContext(TaskContext aTaskContext)
	{
		taskContext = aTaskContext;
	}

	public TaskContext getTaskContext()
	{
		return taskContext;
	}

	@Override
	public void log(String aMessage)
	{
		taskContext.message(aMessage);
	}

	@Override
	public void log(String aBundleName, String aMsgKey, Object[] aArguments)
	{
        taskContext.message(localizeMessage(aBundleName, aMsgKey, aArguments,
                getExtensionClassLoader()));
	}

	@Override
	public void logException(Exception aException)
	{
		taskContext.message(aException.getMessage());
	}

	@Override
	public void setOutputStream(PrintStream aStream)
	{
		// Do nothing
	}

	@Override
	public void setOutputStream(OutputStream aStream)
	{
		// Do nothing
	}

	@Override
	public void log(Level aLevel, String aMessage)
	{
		taskContext.message(aMessage);
	}

	@Override
	public void log(Level aLevel, String aMessage, Object aParam1)
	{
		taskContext.message(format(aMessage, new Object[] { aParam1 }));
	}

	@Override
	public void log(Level aLevel, String aMessage, Object[] aParams)
	{
		taskContext.message(format(aMessage, aParams));
	}

	@Override
	public void log(Level aLevel, String aMessage, Throwable aThrown)
	{
		taskContext.message(aMessage + ": " + aThrown.getMessage());
	}

	@Override
	public void logrb(Level aLevel, String aSourceClass, String aSourceMethod, String aBundleName,
			String aMsgKey)
	{
		taskContext.message(localizeMessage(aBundleName, aMsgKey, null, getExtensionClassLoader()));
	}

	@Override
	public void logrb(Level aLevel, String aSourceClass, String aSourceMethod, String aBundleName,
			String aMsgKey, Object aParam1)
	{
		taskContext.message(localizeMessage(aBundleName, aMsgKey, new Object[] { aParam1 },
				getExtensionClassLoader()));
	}

	@Override
	public void logrb(Level aLevel, String aSourceClass, String aSourceMethod, String aBundleName,
			String aMsgKey, Object[] aParams)
	{
		taskContext.message(localizeMessage(aBundleName, aMsgKey, aParams,
				getExtensionClassLoader()));
	}

	@Override
	public void logrb(Level aLevel, String aSourceClass, String aSourceMethod, String aBundleName,
			String aMsgKey, Throwable aThrown)
	{
		taskContext.message(localizeMessage(aBundleName, aMsgKey, null, getExtensionClassLoader())
				+ ": " + aThrown);
	}

	@Override
	public boolean isLoggable(Level aLevel)
	{
		return true;
	}

	@Override
	public void setLevel(Level aLevel)
	{
		// Ignore
	}

	@Override
	public void setResourceManager(ResourceManager aResourceManager)
	{
		resMgr = aResourceManager;
	}

	private ClassLoader getExtensionClassLoader()
	{
		if (resMgr == null) {
			return null;
		}
		else {
			return resMgr.getExtensionClassLoader();
		}
	}

    @Override
    public void log(String aWrapperFQCN, Level aLevel, String aMessage, Throwable aThrown)
    {
        taskContext.message(aMessage + ": " + aThrown.getMessage());
    }
}
