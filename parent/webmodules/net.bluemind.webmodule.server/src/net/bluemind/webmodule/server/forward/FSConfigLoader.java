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
package net.bluemind.webmodule.server.forward;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import net.bluemind.webmodule.server.WebserverConfiguration;

/**
 * Loads configuration from the filesystem. This might be usefull if you wan't
 * to use a standard reverse proxy and not only for Blue Mind apps.
 * 
 * @author tom
 * 
 */
public class FSConfigLoader extends AbstractXMLConfigLoader {

	@Override
	protected InputStream openBmSsoXml() throws IOException {
		return new FileInputStream(WebserverConfiguration.BM_SSO_XML);
	}

	@Override
	protected Collection<IOpenable> openDefinitions(String defs) {
		File defDir = new File(defs);

		File[] files = defDir.listFiles((directory, filename) -> filename.endsWith(".xml"));

		List<IOpenable> l = new LinkedList<>();
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
