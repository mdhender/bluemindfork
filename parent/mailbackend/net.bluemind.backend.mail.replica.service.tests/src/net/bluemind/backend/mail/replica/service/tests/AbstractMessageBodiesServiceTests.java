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

import com.google.common.io.ByteStreams;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import net.bluemind.backend.mail.replica.api.IDbMessageBodies;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.lib.vertx.VertxPlatform;

public abstract class AbstractMessageBodiesServiceTests {

	protected String partition;
	protected MailboxReplicaRootDescriptor mboxDescriptor;
	protected Vertx vertx;

	protected Stream openResource(String path) {
		try (InputStream inputStream = AbstractReplicatedMailboxesServiceTests.class.getClassLoader()
				.getResourceAsStream(path)) {
			Objects.requireNonNull(inputStream, "Failed to open resource @ " + path);
			return VertxStream.stream(Buffer.buffer(ByteStreams.toByteArray(inputStream)));
		} catch (Exception e) {
			throw new ServerFault(e);
		}
	}

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();
		vertx = VertxPlatform.getVertx();

		partition = "datalocation__vagrant" + System.currentTimeMillis() + "_vmw";
		JdbcActivator.getInstance().addMailboxDataSource("datalocation", JdbcActivator.getInstance().getDataSource());
		ElasticsearchTestHelper.getInstance().beforeTest();
	}

	public void after() throws Exception {
		ElasticsearchTestHelper.getInstance().afterTest();
		JdbcTestHelper.getInstance().afterTest();
	}

	protected abstract IDbMessageBodies getService(SecurityContext ctx);

}
