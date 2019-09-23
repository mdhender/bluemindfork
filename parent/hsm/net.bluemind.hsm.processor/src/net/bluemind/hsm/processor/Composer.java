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
package net.bluemind.hsm.processor;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.james.mime4j.dom.Header;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.field.Fields;
import org.apache.james.mime4j.message.BasicBodyFactory;
import org.apache.james.mime4j.message.HeaderImpl;
import org.apache.james.mime4j.message.MessageImpl;
import org.apache.james.mime4j.stream.Field;
import org.apache.james.mime4j.stream.RawField;
import org.apache.james.mime4j.util.MimeUtil;

import com.google.common.collect.ImmutableMap;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import net.bluemind.mime4j.common.Mime4JHelper;
import net.bluemind.pool.impl.BmConfIni;

public class Composer {

	private Configuration cfg;

	public Composer() {
		this.cfg = new Configuration();
		cfg.setClassForTemplateLoading(Composer.class, "/templates");
	}

	public InputStream render(Message orig, String lang, String hsmId, String dateTime)
			throws IOException, TemplateException {
		Template tpl = null;
		try {
			tpl = cfg.getTemplate("demoted_" + lang + ".ftl");
		} catch (FileNotFoundException fnfe) {
			tpl = cfg.getTemplate("demoted_en.ftl");
		}
		Map<String, String> m = new HashMap<String, String>();
		m.put("archLink", getHSMLink(hsmId));
		StringWriter sw = new StringWriter(4096);
		tpl.process(m, sw);
		try (Message msg = new MessageImpl()) {
			Header heads = new HeaderImpl();
			heads.setField(Fields.contentType("text/html", ImmutableMap.of("charset", "utf-8")));
			heads.setField(Fields.contentTransferEncoding(MimeUtil.ENC_QUOTED_PRINTABLE));
			heads.setField(new RawField(HSMHeaders.HSM_ID, hsmId));
			heads.setField(new RawField(HSMHeaders.HSM_DATETIME, dateTime));

			Header oh = orig.getHeader();
			List<Field> origFields = oh.getFields();
			for (Field f : origFields) {
				if (!f.getName().toLowerCase().startsWith("content")) {
					heads.addField(f);
				}
			}

			BasicBodyFactory bbf = new BasicBodyFactory();
			String mime = sw.toString().replace("\r", "").replace("\n", "\r\n");
			TextBody tb = bbf.textBody(mime, Charset.forName("utf-8"));
			msg.setBody(tb);
			msg.setHeader(heads);

			return Mime4JHelper.asStream(msg);
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	private String getHSMLink(String hsmId) {
		StringBuilder sb = new StringBuilder();

		BmConfIni ini = new BmConfIni();
		sb.append("https://" + ini.get("external-url"));
		sb.append("/webmail/?_task=mail&_action=show&_uid=");
		sb.append(hsmId);

		return sb.toString();
	}
}
