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
package net.bluemind.proxy.http.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Loads HPS configuration from the filesystem. This might be usefull if you
 * wan't to use HPS a standard reverse proxy and not only for Blue Mind apps.
 * 
 * @author tom
 * 
 */
public class FSConfigLoader extends AbstractXMLConfigLoader {

	public FSConfigLoader() {
	}

	@Override
	protected InputStream openBmSsoXml() throws IOException {
		return new FileInputStream("/etc/bm-hps/bm_sso.xml");
	}

	@Override
	protected Collection<IOpenable> openDefinitions(String defs) {
		File defDir = new File(defs);

		File[] files = defDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".xml");
			}
		});

		List<IOpenable> l = new LinkedList<IOpenable>();
		for (final File f : files) {
			l.add(new IOpenable() {

				@Override
				public InputStream open() throws IOException {
					return new FileInputStream(f);
				}
			});
		}
		return l;
	}

}
