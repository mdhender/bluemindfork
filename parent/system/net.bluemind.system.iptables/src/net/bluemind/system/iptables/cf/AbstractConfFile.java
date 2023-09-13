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
package net.bluemind.system.iptables.cf;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import freemarker.template.Configuration;
import freemarker.template.Template;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.node.api.INodeClient;

public abstract class AbstractConfFile {
	protected INodeClient nc;
	private Configuration cfg;

	protected AbstractConfFile() throws ServerFault {
		cfg = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
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

	public abstract void clear();

	public String render(Template t, Map<String, Object> data) throws ServerFault {
		StringWriter sw = processTemplate(t, data);
		if (!sw.toString().endsWith("\n")) {
			sw.append("\n");
		}

		return sw.toString();
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

	public abstract void write(INodeClient nc) throws ServerFault;

}
