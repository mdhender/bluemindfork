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
package net.bluemind.system.service.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.system.api.DomainTemplate;
import net.bluemind.system.api.DomainTemplate.Kind;
import net.bluemind.system.api.IDomainTemplate;
import net.bluemind.system.domaintemplate.DomainTemplateLoader;

public class DomainTemplateService implements IDomainTemplate {

	private static final Logger logger = LoggerFactory.getLogger(DomainTemplateService.class);

	@Override
	public DomainTemplate getTemplate() throws ServerFault {

		DomainTemplate mainTemplate = loadMain();

		List<DomainTemplate> templates = loadExtensions();
		return merge(mainTemplate, templates);
	}

	private DomainTemplate merge(DomainTemplate mainTemplate, List<DomainTemplate> templates) {

		for (DomainTemplate t : templates) {
			for (Kind kind : t.kinds) {
				mergeKind(mainTemplate, kind);
			}
		}

		return mainTemplate;
	}

	private void mergeKind(DomainTemplate mainTemplate, Kind kind) {

		Kind main = null;
		for (Kind k : mainTemplate.kinds) {
			if (k.id.equals(kind.id)) {
				main = k;
				break;
			}
		}

		if (main == null) {
			main = kind;
			mainTemplate.kinds.add(main);
		} else {
			main.tags.addAll(kind.tags);
		}
	}

	private List<DomainTemplate> loadExtensions() throws ServerFault {
		IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint("net.bluemind.system.domaintemplate");
		if (point == null) {
			logger.error("point net.bluemind.system.domaintemplate not found.");
			throw new ServerFault("error loading domain templates");
		}
		IExtension[] extensions = point.getExtensions();

		List<DomainTemplate> templates = new LinkedList<>();
		for (IExtension ie : extensions) {
			for (IConfigurationElement e : ie.getConfigurationElements()) {
				Bundle bundle = Platform.getBundle(ie.getContributor().getName());
				URL resource = bundle.getResource(e.getAttribute("document"));
				if (resource == null) {
					logger.warn("no found resource {} in bundle ", e.getAttribute("document"),
							ie.getContributor().getName());
					continue;
				}
				try (InputStream in = resource.openStream()) {
					templates.add(DomainTemplateLoader.load(in));
				} catch (Exception ex) {
					logger.error("error during load of domain template extension {}", ie.getUniqueIdentifier(), ex);
				}
			}
		}

		return templates;
	}

	private DomainTemplate loadMain() throws ServerFault {
		try (InputStream in = DomainTemplateService.class.getResourceAsStream("/templates/domain.xml")) {

			return DomainTemplateLoader.load(in);
		} catch (IOException e) {
			throw new ServerFault("error loading domain templates");
		}
	}

}
