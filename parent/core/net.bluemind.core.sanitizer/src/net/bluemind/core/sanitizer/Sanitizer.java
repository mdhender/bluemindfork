/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.core.sanitizer;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.rest.BmContext;
import net.bluemind.eclipse.common.RunnableExtensionLoader;

public class Sanitizer {
	private static final Logger logger = LoggerFactory.getLogger(Sanitizer.class);

	private static final List<ISanitizerFactory<Object>> sanitizers = loadSanitizer();

	private static List<ISanitizerFactory<Object>> loadSanitizer() {
		RunnableExtensionLoader<ISanitizerFactory<Object>> rel = new RunnableExtensionLoader<ISanitizerFactory<Object>>();
		List<ISanitizerFactory<Object>> stores = rel.loadExtensionsWithPriority("net.bluemind.core", "sanitizerfactory",
				"sanitizerfactory", "implementation");

		stores.stream().forEach(store -> logger.info("sanitizer factory class: {} for: {}", store.getClass().getName(),
				store.support()));

		return stores;
	}

	private final BmContext context;
	private final Container container;

	public Sanitizer(BmContext context) {
		this.context = context;
		this.container = null;
	}

	public Sanitizer(BmContext context, Container container) {
		this.context = context;
		this.container = container;
	}

	public void create(Object entity) throws ServerFault {
		create(entity, Collections.emptyMap());
	}

	public void update(Object current, Object entity) throws ServerFault {
		update(current, entity, Collections.emptyMap());
	}

	public void create(Object entity, Map<String, String> params) throws ServerFault {
		if (entity == null) {
			return;
		}

		for (ISanitizerFactory<Object> sanitizer : sanitizers) {
			if (entity.getClass() == sanitizer.support()) {
				sanitizer.create(context, container).create(entity, params);
			}
		}
	}

	public void update(Object current, Object entity, Map<String, String> params) throws ServerFault {
		if (current == null || entity == null) {
			return;
		}

		for (ISanitizerFactory<Object> sanitizer : sanitizers) {
			if (entity.getClass() == sanitizer.support()) {
				sanitizer.create(context, container).update(current, entity, params);
			}
		}
	}

}
