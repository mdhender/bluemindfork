/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2024
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
package net.bluemind.eas.http.tests.builders;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class DocumentRequestBuilder {

	public static Document getDocumentRequestUpdate(String template, Map<TemplateKey, String> values) throws Exception {
		try (InputStream in = DocumentRequestBuilder.class.getResourceAsStream("/templates/" + template)) {
			String xml = readTemplate(in, values);
			DocumentBuilder builder = DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder();
			return builder.parse(new InputSource(new StringReader(xml)));
		}
	}

	private static String readTemplate(InputStream in, Map<TemplateKey, String> values) throws IOException {
		StringBuilder sb = new StringBuilder();
		int i;
		while ((i = in.read()) != -1) {
			sb.append((char) i);
		}

		String xml = sb.toString();
		for (Entry<TemplateKey, String> element : values.entrySet()) {
			xml = xml.replace("#" + element.getKey().name() + "#", element.getValue());
		}
		return xml;
	}

	public enum TemplateKey {
		Subject, //
		To, //
		Html, //
		Read, //
		SrcMsgId, //
		SrcFldId, //
		DstFldId;
	}
}
