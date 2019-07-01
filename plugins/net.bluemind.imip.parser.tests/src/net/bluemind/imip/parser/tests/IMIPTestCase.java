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
package net.bluemind.imip.parser.tests;

import java.io.InputStream;

import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.message.DefaultMessageBuilder;
import org.apache.james.mime4j.message.MessageServiceFactoryImpl;
import org.apache.james.mime4j.stream.MimeConfig;

import junit.framework.TestCase;

public abstract class IMIPTestCase extends TestCase {

	private InputStream open(String name) {
		return IMIPTestCase.class.getClassLoader().getResourceAsStream(name);
	}

	protected Message parseData(String name) throws Exception {
		DefaultMessageBuilder dmb = (DefaultMessageBuilder) MessageServiceFactoryImpl.newInstance().newMessageBuilder();
		MimeConfig cfg = new MimeConfig();
		cfg.setMaxHeaderLen(-1);
		cfg.setMaxHeaderCount(-1);
		cfg.setMalformedHeaderStartsBody(false);
		cfg.setMaxLineLen(-1);

		dmb.setMimeEntityConfig(cfg);

		Message m = dmb.parseMessage(open(name));
		System.out.println("" + name + " parsing done.");
		return m;
	}

}
