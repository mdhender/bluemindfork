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
package net.bluemind.backend.postfix.internal.cf;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.Configuration;
import freemarker.template.Template;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.server.api.IServer;

public abstract class AbstractConfFile {

	protected Logger logger = LoggerFactory.getLogger(getClass());
	private final Configuration cfg;
	protected final IServer service;
	protected final String serverUid;

	protected AbstractConfFile(IServer service, String serverUid) throws ServerFault {
		this.service = service;
		this.serverUid = serverUid;
		cfg = new Configuration();
		cfg.setClassForTemplateLoading(getClass(), "/templates");
	}

	public Template openTemplate(String name) throws ServerFault {
		Template t;
		try {
			t = cfg.getTemplate(name);
			return t;
		} catch (IOException e) {
			throw new ServerFault(e);
		}
	}

	public abstract void write() throws ServerFault;

	public byte[] render(Template t, Map<String, Object> data) throws ServerFault {
		StringWriter sw = processTemplate(t, data);
		if (!sw.toString().endsWith("\r\n")) {
			sw.append("\r\n");
		}

		return sw.toString().getBytes();
	}

	private StringWriter processTemplate(Template t, Map<String, Object> data) throws ServerFault {
		StringWriter sw = new StringWriter();
		try {
			t.process(data, sw);
		} catch (Exception e) {
			throw new ServerFault(e);
		}
		return sw;
	}
}
