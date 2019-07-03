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
package net.bluemind.core.jdbc;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchemaDescriptors {

	private static final Logger logger = LoggerFactory.getLogger(SchemaDescriptors.class);

	private Map<String, SchemaDescriptor> schemas = new HashMap<String, SchemaDescriptor>();
	private boolean loaded = false;

	public SchemaDescriptors() {
		loadSchemas();
	}

	public SchemaDescriptor getSchemaDescriptor(String schemaId) {
		return schemas.get(schemaId);
	}

	public void loadSchemas() {

		if (loaded) {
			return;
		}
		logger.debug("loading extensionpoint net.bluemind.core.jdbc");
		IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint("net.bluemind.core.jdbc", "schema");

		if (point == null) {
			logger.error("point net.bluemind.core.jdbc.schema name:schema not found");
			throw new RuntimeException("point net.bluemind.core.jdbc.schema name:schema not found");
		}
		IExtension[] extensions = point.getExtensions();
		for (IExtension ie : extensions) {
			Bundle bundle = Platform.getBundle(ie.getContributor().getName());
			logger.debug("loading schemas from bundle:{}", bundle.getSymbolicName());

			for (IConfigurationElement e : ie.getConfigurationElements()) {
				if (e.getName().equals("schema")) {

					String resource = e.getAttribute("resource");
					URL url = bundle.getResource(resource);
					if (url == null) {
						logger.error("bundle [{}] resource {} not found", bundle.getSymbolicName(), resource);
						continue;
					}

					IConfigurationElement[] required = e.getChildren();
					List<String> ids = new ArrayList<>();
					if (required != null) {
						for (IConfigurationElement req : required) {
							ids.add(req.getAttribute("id"));
						}
					}

					boolean ignoreErrors = false;
					if (e.getAttribute("ignoreErrors") != null) {
						ignoreErrors = Boolean.parseBoolean(e.getAttribute("ignoreErrors"));

					}
					SchemaDescriptor descriptor = new SchemaDescriptor(e.getAttribute("name"),
							bundle.getVersion().toString(), url, ids, ignoreErrors);

					logger.debug("registred schema {} version: {}", descriptor.getId(), descriptor.getVersion());
					schemas.put(descriptor.getId(), descriptor);
				}
			}

		}

		loaded = true;
	}

	public List<SchemaDescriptor> getDescriptors() {
		List<SchemaDescriptor> ret = new ArrayList<>();
		Set<String> done = new HashSet<>();

		for (SchemaDescriptor descr : schemas.values()) {
			buildDescriptors(ret, descr, done);
		}

		return ret;
	}

	private void buildDescriptors(List<SchemaDescriptor> ret, SchemaDescriptor descr, Set<String> done) {
		if (done.contains(descr.getId())) {
			return;
		}

		for (String schemaId : descr.getRequiredSchemas()) {
			SchemaDescriptor parent = schemas.get(schemaId);
			if (parent == null) {
				logger.error("schema with id {} not found ", schemaId);
				throw new RuntimeException("schema with id " + schemaId + " not found");
			}
			logger.debug("{} requires {}", descr.getId(), schemaId);
			buildDescriptors(ret, parent, done);
		}

		done.add(descr.getId());
		ret.add(descr);
	}

}
