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
package net.bluemind.ysnp.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.vertx.core.buffer.Buffer;
import jnr.unixsocket.UnixSocketAddress;
import jnr.unixsocket.UnixSocketChannel;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.network.topology.Topology;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.ysnp.YSNPConfiguration;

public class YsnpTests {

	private String socketPath;

	@Before
	public void setup() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		File tmpSock = File.createTempFile("ysnp", ".sock");
		socketPath = tmpSock.getAbsolutePath();
		Files.deleteIfExists(tmpSock.toPath());
		System.setProperty("ysnp.sock", socketPath);

		PopulateHelper.initGlobalVirt();
		PopulateHelper.addDomainAdmin("admin0", "global.virt");

		VertxPlatform.spawnBlocking(1, TimeUnit.MINUTES);
		Topology.get();

		YSNPConfiguration conf = YSNPConfiguration.INSTANCE;
		System.out.println("path " + conf.getSocketPath());

	}

	@After
	public void after() throws Exception {

	}

	@Test
	public void testLogin() throws IOException {
		assertFalse(tryLogin("fake@fakedomain", "fakePassword"));
		assertTrue(tryLogin("admin0@global.virt", "admin"));
		assertFalse(tryLogin("admin0@global.virt", "fakePassword"));
	}

	private boolean tryLogin(String login, String password) {
		String service = "smtp";
		String realm = "test";

		UnixSocketAddress address = new UnixSocketAddress(new File(socketPath));
		try (UnixSocketChannel channel = UnixSocketChannel.open(address)) {
			assertTrue(channel.isConnected());

			Buffer b = Buffer.buffer();
			b.appendShort((short) login.length());
			b.appendString(login);
			b.appendShort((short) password.length());
			b.appendString(password);

			b.appendShort((short) service.length());
			b.appendString(service);

			b.appendShort((short) realm.length());
			b.appendString(realm);

			channel.write(ByteBuffer.wrap(b.getBytes()));
			byte[] res = new byte[256];
			int readed = channel.read(ByteBuffer.wrap(res));
			assertTrue(readed >= 4);

			int messageSize = (((int) res[0]) << 8) + (int) res[1];

			String result = new String(res, 2, messageSize);

			if (result.startsWith("NO")) {
				return false;
			} else if (result.startsWith("OK")) {
				return true;
			} else {
				Assert.fail("unkonwn response " + result);
				return false;
			}
		} catch (IOException e) {
			fail(e.getMessage());
			return false;
		}

	}
}
