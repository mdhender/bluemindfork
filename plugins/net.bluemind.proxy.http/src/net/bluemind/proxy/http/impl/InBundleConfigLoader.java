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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import net.bluemind.proxy.http.Activator;

public class InBundleConfigLoader extends AbstractXMLConfigLoader {

	public InBundleConfigLoader() {
	}

	private InputStream open(String p) {
		return InBundleConfigLoader.class.getClassLoader().getResourceAsStream(p);
	}

	@Override
	protected InputStream openBmSsoXml() {
		return open("config/bm_sso.xml");
	}

	@Override
	protected Collection<IOpenable> openDefinitions(String defs) {
		Enumeration<URL> fwds = Activator.getContext().getBundle().findEntries(defs, "*.xml", true);
		List<IOpenable> l = new LinkedList<IOpenable>();
		while (fwds.hasMoreElements()) {
			final URL u = fwds.nextElement();
			l.add(new IOpenable() {

				@Override
				public InputStream open() throws IOException {
					return u.openStream();
				}
			});
		}
		return l;
	}

}
