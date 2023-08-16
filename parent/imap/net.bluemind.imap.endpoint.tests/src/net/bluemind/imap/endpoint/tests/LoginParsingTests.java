/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.imap.endpoint.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import io.netty.buffer.ByteBuf;
import io.vertx.core.buffer.Buffer;
import net.bluemind.imap.endpoint.cmd.AnalyzedCommand;
import net.bluemind.imap.endpoint.cmd.LoginCommand;
import net.bluemind.imap.endpoint.cmd.RawCommandAnalyzer;
import net.bluemind.imap.endpoint.cmd.RawImapCommand;
import net.bluemind.imap.endpoint.parsing.Part;

public class LoginParsingTests {

	@Test
	public void parseLoginPasswords() throws IOException {
		checkParsing("11 LOGIN test.dev@devenv.blue AZERTY23", "test.dev@devenv.blue", "AZERTY23");
		checkParsing("11 LOGIN \"test.dev@devenv.blue\" AZERTY23", "test.dev@devenv.blue", "AZERTY23");
		checkParsing("11 LOGIN \"test.dev@devenv.blue\" \"AZERTY23\"", "test.dev@devenv.blue", "AZERTY23");
		checkParsing("11 LOGIN test.dev@devenv.blue \"AZERTY23\"", "test.dev@devenv.blue", "AZERTY23");
		checkParsing("11 login test.dev@devenv.blue AZERTY23", "test.dev@devenv.blue", "AZERTY23");
		checkParsing("11 LOGIN test.dev@devenv.blue azerty23", "test.dev@devenv.blue", "azerty23");
		checkParsing("11 LOGIN test.dev@devenv.blue azer\"ty23", "test.dev@devenv.blue", "azer\"ty23");
		checkParsing("11 LOGIN test.dev@devenv.blue \"azer\\\"ty23\"", "test.dev@devenv.blue", "azer\"ty23");
	}

	private void checkParsing(String command, String login, String password) {

		Buffer chunk = Buffer.buffer(command);
		ByteBuf buf = chunk.getByteBuf();
		Part part = Part.endOfCommand(buf);
		List<Part> parts = Arrays.asList(part);
		RawImapCommand raw = new RawImapCommand(parts);

		AnalyzedCommand parsed = new RawCommandAnalyzer().analyze(null, raw);

		assertNotNull(parsed);
		assertTrue(parsed instanceof LoginCommand);
		LoginCommand res = (LoginCommand) parsed;
		assertEquals(login, res.login());
		assertEquals(password, res.password());
		System.err.println("login: " + res.login() + " 'password: '" + res.password() + "'");

	}

}
