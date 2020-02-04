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
package net.bluemind.webmodule.server;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Optional;
import java.util.TreeMap;

import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

public class WebResource {
	private static final Logger logger = LoggerFactory.getLogger(WebResource.class);

	private final TreeMap<String, File> resources = new TreeMap<>();
	private final boolean preload;
	private final Optional<File> rootPath;
	private final String path;

	private final Bundle bundle;

	public WebResource(Bundle bundle, boolean preload) {
		this.preload = preload;
		this.bundle = bundle;
		logger.info("loading resource for {}", bundle.getSymbolicName());
		this.path = bundle.getHeaders().get("Web-Resources");
		this.rootPath = getRootPath(path);
		if (path == null) {
			logger.debug("no resource found into {} ", getBundleName());
			return;
		}
		if (preload) {
			preloadFiles(bundle, path);
		}
	}

	private Optional<File> getRootPath(String path) {
		if (path != null) {
			try {
				URL resource = bundle.getResource(path);
				if (resource != null) {
					URL url = org.eclipse.core.runtime.FileLocator.toFileURL(resource);
					return Optional.of(new File(url.getFile()));

				}
			} catch (InvalidRegistryObjectException | IOException e1) {
			}
		}
		return Optional.empty();
	}

	private void preloadFiles(Bundle bundle, String path) {

		rootPath.ifPresent(rootPath -> {
			Iterable<File> fileTraversal = Files.fileTraverser().breadthFirst(rootPath);

			for (File file : fileTraversal) {

				if (file.isFile()) {
					String base = file.getAbsolutePath().substring(rootPath.getAbsolutePath().length() + 1);
					logger.debug("bundle {} : {}", getBundleName(), base);
					resources.put(base, file);
				}

			}

			Enumeration<URL> e = bundle.findEntries(path, "*", true);
			while (e.hasMoreElements()) {
				URL i = e.nextElement();
				try {
					URL url = org.eclipse.core.runtime.FileLocator.toFileURL(i);
					File file = new File(url.getFile());
					if (file.isFile()) {
						String p = url.getPath();
						String base = p.substring(p.lastIndexOf(path) + path.length() + 1);
						logger.debug("load bundle webresource {} : {} {}", getBundleName(), base,
								file.getAbsolutePath());
						resources.put(base, file);
					}
				} catch (IOException e1) {
					logger.warn("error during loading resource of {} ", getBundleName(), e1);
				}
			}

		});

		logger.info("statics for bundle {} : {}", getBundleName(), resources.keySet());
	}

	public Bundle getBundle() {
		return bundle;
	}

	public String getBundleName() {
		return bundle.getSymbolicName();
	}

	public File getResource(String path) {
		File f = resources.get(path);
		String isFound = (f != null) ? "found" : "not found";
		logger.debug("try to find {} into {} : {} ", path, getBundleName(), isFound);
		if (f == null && !preload) {
			f = load(path);
		}
		return f;
	}

	private File load(String path) {
		File file = new File(rootPath.get().getAbsolutePath(), path);
		if (!file.exists()) {
			return null;
		}
		resources.put(path, file);
		return file;
	}

	public Collection<String> getResources() {
		return resources.keySet();
	}
}
