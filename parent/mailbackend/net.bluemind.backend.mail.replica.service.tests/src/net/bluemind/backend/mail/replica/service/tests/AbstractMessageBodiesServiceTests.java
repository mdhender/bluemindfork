/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
 *
 * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License)
 * or the CeCILL as published by CeCILL.info (version 2 of the License).
 *
 * There are special exceptions to the terms and conditions of the
 * licenses as they are applied to this program. See LICENSE.txt in
 * the directory of this program distribution.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.backend.mail.replica.service.tests;

import java.io.InputStream;
import java.util.Objects;

import org.junit.Before;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import net.bluemind.backend.mail.replica.api.IDbMessageBodies;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.utils.InputReadStream;
import net.bluemind.lib.vertx.VertxPlatform;

public abstract class AbstractMessageBodiesServiceTests {

	protected String partition;
	protected MailboxReplicaRootDescriptor mboxDescriptor;
	protected Vertx vertx;

	protected ReadStream<Buffer> openResource(String path) {
		InputStream inputStream = AbstractReplicatedMailboxesServiceTests.class.getClassLoader()
				.getResourceAsStream(path);
		Objects.requireNonNull(inputStream, "Failed to open resource @ " + path);
		return new InputReadStream(inputStream);
	}

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();
		vertx = VertxPlatform.getVertx();

		partition = "datalocation__vagrant" + System.currentTimeMillis() + "_vmw";
		JdbcActivator.getInstance().addMailboxDataSource("datalocation", JdbcActivator.getInstance().getDataSource());
	}

	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	protected abstract IDbMessageBodies getService(SecurityContext ctx);

}
