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
package de.tudarmstadt.ukp.dkpro.lab.engine.impl;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataAccessResourceFailureException;

import de.tudarmstadt.ukp.dkpro.lab.resteasy.UriInfoImpl;

public class ImportUtil
{
	private static final Log log = LogFactory.getLog(ImportUtil.class);

	public static boolean matchConstraints(Map<String, String> aDiscriminators,
			Map<String, String> aConstraints, boolean aStrict)
	{
		nextconstraint: for (Entry<String, String> e : aConstraints.entrySet()) {
			String keyPattern = e.getKey();
			String valuePattern = "^" + e.getValue() + "$";

			// If a property is not specified with a full class name, then we only use the
			// simple property name for matching
			if (!keyPattern.contains(Pattern.quote("|"))) {
				keyPattern = ".*" + Pattern.quote("|") + keyPattern;
			}
			keyPattern = "^"+keyPattern+"$";

			Set<String> keys = aDiscriminators.keySet();
			nextKey: for (String key : keys) {
				if (!(e.getKey().equals(key) || Pattern.matches(keyPattern, key))) {
					// key pattern does not match or equal the key. Try next key
					log.trace("No key match: ["+keyPattern+"] ["+key+"]");
					continue nextKey;
				}

				String val = aDiscriminators.get(key);
				if (!(e.getValue().equals(val) || Pattern.matches(valuePattern, val))) {
					// value pattern does not match or equal the property value, this is
					// not the context we look for
					log.debug("No value match: ["+key+"] ["+valuePattern+"] ["+val+"]");
					return false;
				}
				else {
					// Ok, this key matched, so we can continue with the next constraint
					log.trace("Match: ["+key+"] ["+val+"]");
					continue nextconstraint;
				}
			}

			// If we get here no key has matched the constraint, thus this is not the
			// context we look for
			if (aStrict) {
				log.debug("Missing key: ["+keyPattern+"]");
				return false;
			}
		}
		return true;
	}

	public static Map<String, String> extractConstraints(URI aUri)
	{
		@SuppressWarnings("unchecked")
		UriInfoImpl uriInfo = new UriInfoImpl(aUri, null, "", aUri.getRawQuery(),
				Collections.EMPTY_LIST);

		MultivaluedMap<String, String> parameters = uriInfo.getQueryParameters(true);
		Map<String, String> constraints = new HashMap<String, String>();
		for (String key : parameters.keySet()) {
			for (String value : parameters.get(key)) {
				constraints.put(key, value);
			}
		}

		return constraints;
	}

	public static DataAccessResourceFailureException createTaskNeverExecutedException(
			String aTaskType, Map<String, String> aConstraints)
	{
		if (aTaskType == null) {
			throw new IllegalArgumentException("Task type cannot be null");
		}

		StringBuilder sb = new StringBuilder();
		sb.append("Task [" + aTaskType + "] has never been executed.");
		if (aConstraints.size() > 0) {
			sb.append(" There are additional constraints:\n");
			for (Entry<String, String> e : aConstraints.entrySet()) {
				sb.append("[");
				sb.append(e.getKey());
				sb.append("] = [");
				sb.append(e.getValue());
				sb.append("]\n");
			}
		}
		return new DataAccessResourceFailureException(sb.toString());
	}
}
