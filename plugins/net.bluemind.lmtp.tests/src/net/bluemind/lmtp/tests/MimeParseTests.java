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
package net.bluemind.lmtp.tests;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;

import org.apache.james.mime4j.dom.Body;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.message.MessageServiceFactoryImpl;

import junit.framework.TestCase;

public class MimeParseTests extends TestCase {

	private InputStream open(String name) {
		return MimeParseTests.class.getClassLoader().getResourceAsStream("data/" + name);
	}

	public void testParse() throws IOException {
		Message m = null;
		try {
			m = MessageServiceFactoryImpl.newInstance().newMessageBuilder().parseMessage(open("email.2007.txt"));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		System.out.println("Message: " + m);
		System.out.println("multipart: " + m.isMultipart());
		Body body = m.getBody();
		System.out.println("Body: " + body.getClass());
		TextBody btb = (TextBody) body;
		System.out.println("charset: " + btb.getMimeCharset());
		BufferedReader reader = new BufferedReader(btb.getReader());
		String line = null;
		System.err.println("----- START -----");
		while ((line = reader.readLine()) != null) {
			System.err.println(line);
		}
		System.err.println("----- END -----");

	}

}
