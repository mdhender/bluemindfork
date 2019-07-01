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
package net.bluemind.core.api;

import java.net.URL;
import java.util.jar.Manifest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BMVersion {
	static String version;
	static String versionName;

	private static final Logger logger = LoggerFactory.getLogger(BMVersion.class);

	public static String getVersionName() {
		return versionName;
	}

	public static String getVersion() {
		if (version == null) {
			version = readVersionFromManifest();
		}
		return version;
	}

	private static String readVersionFromManifest() {
		try {
			String className = BMVersion.class.getSimpleName() + ".class";
			String classPath = BMVersion.class.getResource(className).toString();
			String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF";
			Manifest manifest = new Manifest(new URL(manifestPath).openStream());
			String version = manifest.getMainAttributes().getValue("Bundle-Version");
			if (version != null) {
				return version;
			}
		} catch (Exception e) {
			logger.warn("Cannot read client version from manifest");
		}
		return "";
	}

}
