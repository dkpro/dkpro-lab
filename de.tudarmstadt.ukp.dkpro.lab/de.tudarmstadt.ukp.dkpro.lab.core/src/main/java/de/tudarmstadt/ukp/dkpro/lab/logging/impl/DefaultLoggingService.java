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
package de.tudarmstadt.ukp.dkpro.lab.logging.impl;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.tudarmstadt.ukp.dkpro.lab.logging.LoggingService;

public class DefaultLoggingService
	implements LoggingService
{
	private final Log log = LogFactory.getLog(this.getClass());

	@Override
	public void message(String aUuid, String aMessage)
	{
		log.info("["+aUuid+"] "+aMessage);
	}
	
	@Override
	public void error(String aUuid, String aMessage, Throwable aCause)
	{
		if (aCause != null) {
			log.error("[" + aUuid + "] " + aMessage + "(caused by "
					+ ExceptionUtils.getRootCauseMessage(aCause) + ")");
			if (log.isDebugEnabled()) {
				log.debug("[" + aUuid + "] Problem stack trace:", aCause);
			}
		}
		else {
			log.error("["+aUuid+"] "+aMessage);
		}
	}
}
