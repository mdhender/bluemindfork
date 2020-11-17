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
package net.bluemind.backend.mail.replica.service.tests;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.google.common.collect.ImmutableMap;

import freemarker.template.Configuration;
import freemarker.template.Template;

public class EmlTemplates {

	@SuppressWarnings("serial")
	private static class EmlException extends RuntimeException {
		EmlException(Throwable t) {
			super(t);
		}

	}

	private EmlTemplates() {
	}

	public static InputStream withRandomMessageId(String tplName) {
		Configuration fmCfg = new Configuration(Configuration.VERSION_2_3_30);
		fmCfg.setClassForTemplateLoading(EmlTemplates.class, "/data");
		fmCfg.setTagSyntax(Configuration.AUTO_DETECT_TAG_SYNTAX);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.US_ASCII);
		try {
			Template tpl = fmCfg.getTemplate(tplName, "US-ASCII");
			tpl.process(ImmutableMap.of("randomUUID", UUID.randomUUID().toString()), writer);
			writer.close();
			return new ByteArrayInputStream(out.toByteArray());
		} catch (Exception e) {
			throw new EmlException(e);
		}

	}

}
