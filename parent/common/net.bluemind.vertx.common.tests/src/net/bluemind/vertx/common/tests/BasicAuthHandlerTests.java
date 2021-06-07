/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.vertx.common.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.Test;

import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.vertx.common.http.BasicAuthHandler;
import net.bluemind.vertx.common.http.BasicAuthHandler.Creds;

public class BasicAuthHandlerTests {

	@Test
	public void testParseHeader() {

		checkEncodingCombo("nico@devenv.blue", "problème", StandardCharsets.UTF_8);
		checkEncodingCombo("nico@devenv.blue", "problème", StandardCharsets.ISO_8859_1);

		checkEncodingCombo("nico@devenv.blue", "la fille du bédouin ... banàààne", StandardCharsets.UTF_8);
		checkEncodingCombo("nico@devenv.blue", "la fille du bédouin ... banàààne", StandardCharsets.ISO_8859_1);

		checkEncodingCombo("nico@devenv.blue", "çaVàChi€r", StandardCharsets.UTF_8);
	}

	private void checkEncodingCombo(String login, String pass, Charset toCheck) {
		String withUtf8 = login + ":" + pass;
		String encoded = Base64.getEncoder().encodeToString(withUtf8.getBytes(toCheck));

		System.err.println("l: " + login + ", p: " + pass + " => 'Basic " + encoded + "'");
		BasicAuthHandler bh = new BasicAuthHandler(VertxPlatform.getVertx(), "junit", dummy -> {

		});

		Creds creds = bh.getCredentials("Basic " + encoded);
		assertNotNull(creds);
		assertEquals(login, creds.getLogin());
		assertEquals("checking " + pass + " given as " + toCheck, pass, creds.getPassword());
	}

}
