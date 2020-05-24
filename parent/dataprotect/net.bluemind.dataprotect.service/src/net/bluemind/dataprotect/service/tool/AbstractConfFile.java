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
package net.bluemind.dataprotect.service.tool;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Map;

import freemarker.template.Configuration;
import freemarker.template.Template;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.node.api.INodeClient;

public abstract class AbstractConfFile {

	protected INodeClient nc;
	private Configuration cfg;

	protected AbstractConfFile(INodeClient nc) throws ServerFault {
		this.nc = nc;
		cfg = new Configuration();
	}

	private void setClassReference(Class<?> clazz) {
		cfg.setClassForTemplateLoading(clazz, "/templates");
	}

	public Template openTemplate(Class<?> clazz, String name) throws ServerFault {
		setClassReference(clazz);
		Template t;
		try {
			t = cfg.getTemplate(name);
			return t;
		} catch (IOException e) {
			throw new ServerFault(e);
		}
	}

	public abstract void clear();

	public InputStream render(Template t, Map<String, Object> data) throws ServerFault {
		StringWriter sw = processTemplate(t, data);
		if (!sw.toString().endsWith("\n")) {
			sw.append("\n");
		}

		return new ByteArrayInputStream(sw.toString().getBytes());
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

	public abstract void write() throws ServerFault;
}
