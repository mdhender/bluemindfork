/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.backend.cyrus.internal;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import net.bluemind.core.api.fault.ServerFault;

public class HSMConfigTests {

	private Configuration cfg;

	@Before
	public void before() {
		cfg = new Configuration();
		cfg.setClassForTemplateLoading(CyrusMailboxesStorage.class, "/templates");
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

	@Test
	public void testGenConfig() throws TemplateException, IOException {
		Template cyrusConf = openTemplate("backend.hsm.conf");
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("cyrusConf", ImmutableMap.of("titi", "toto"));
		StringWriter out = new StringWriter();
		cyrusConf.process(data, out);
		String output = out.toString();
		System.err.println(output);
	}

}
