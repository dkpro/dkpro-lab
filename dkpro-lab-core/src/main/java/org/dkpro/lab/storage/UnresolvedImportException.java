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
package org.dkpro.lab.storage;

import org.dkpro.lab.engine.TaskContext;
import org.springframework.dao.DataAccessResourceFailureException;

/**
 * Exception thrown when an import cannot be resolved.
 */
public class UnresolvedImportException
    extends DataAccessResourceFailureException
{
    private static final long serialVersionUID = -6316793159383062743L;

    private TaskContext context;

    public UnresolvedImportException(TaskContext aContext, String aImport, String aReason)
    {
        super("Unable to resolve import of task [" + aContext.getMetadata().getType()
                + "] pointing to [" + aImport + "]: " + aReason);
        context = aContext;
    }

    public UnresolvedImportException(TaskContext aContext, String aImport, Throwable aCause)
    {
        super("Unable to resolve import of task [" + aContext.getMetadata().getType()
                + "] pointing to [" + aImport + "]", aCause);
        context = aContext;
    }

    public UnresolvedImportException(TaskContext aContext, String aKey, String aImport,
            String aReason)
    {
        super("Unable to resolve key [" + aKey + "] of task [" + aContext.getMetadata().getType()
                + "] pointing to [" + aImport + "]: " + aReason);
        context = aContext;
    }

    public UnresolvedImportException(TaskContext aContext, String aKey, String aImport,
            Throwable aCause)
    {
        super("Unable to resolve key [" + aKey + "] of task [" + aContext.getMetadata().getType()
                + "] pointing to [" + aImport + "]", aCause);
        context = aContext;
    }

    public UnresolvedImportException(UnresolvedImportException aChainedException, String aDetails)
    {
        super(aDetails, aChainedException);
    }

    public TaskContext getContext()
    {
        return context;
    }
}
