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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.FactoryConfigurationError;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import net.bluemind.utils.DOMUtils;
import net.bluemind.webmodule.server.WebserverConfiguration;

public abstract class AbstractXMLConfigLoader implements IConfigLoader {

	private static final Logger logger = LoggerFactory.getLogger(AbstractXMLConfigLoader.class);

	protected AbstractXMLConfigLoader() {
	}

	public void load(WebserverConfiguration conf) throws FactoryConfigurationError {
		try {
			InputStream in = openBmSsoXml();
			Document doc = DOMUtils.parse(in);
			Element r = doc.getDocumentElement();

			Element forwards = DOMUtils.getUniqueElement(r, "forwards");
			String fd = forwards.getAttribute("definitions");
			logger.info("forward definitions: {}", fd);
			Collection<IOpenable> fwds = openDefinitions(fd);
			List<ForwardedLocation> fwCol = new LinkedList<>();
			conf.setForwardedLocations(fwCol);
			for (IOpenable o : fwds) {
				InputStream fin = o.open();
				addForwards(fwCol, fin);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	protected abstract InputStream openBmSsoXml() throws IOException;

	protected abstract Collection<IOpenable> openDefinitions(String defs);

	private void addForwards(Collection<ForwardedLocation> fwCol, InputStream fin) {
		try {
			Document doc = DOMUtils.parse(fin);
			Element r = doc.getDocumentElement();
			NodeList nl = r.getElementsByTagName("forward");
			for (int i = 0; i < nl.getLength(); i++) {
				Element f = (Element) nl.item(i);
				ForwardedLocation fl = new ForwardedLocation(f.getAttribute("path"), f.getAttribute("target"),
						f.getAttribute("auth"), f.getAttribute("role"));
				fwCol.add(fl);
				String cspDisabled = f.getAttribute("csp_disabled");
				if (cspDisabled != null && !cspDisabled.isEmpty()) {
					fl.cspEnabled(!Boolean.valueOf(cspDisabled));
				}
				NodeList wl = f.getElementsByTagName("wl");
				for (int j = 0; j < wl.getLength(); j++) {
					Element wle = (Element) wl.item(j);
					String whiteListUri = wle.getAttribute("uri");
					String whiteListRegex = wle.getAttribute("regex");
					if (whiteListUri != null && !whiteListUri.isEmpty()) {
						fl.whiteList(whiteListUri);
					}
					if (whiteListRegex != null && !whiteListRegex.isEmpty()) {
						fl.whiteListRegex(whiteListRegex);
					}
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

}
