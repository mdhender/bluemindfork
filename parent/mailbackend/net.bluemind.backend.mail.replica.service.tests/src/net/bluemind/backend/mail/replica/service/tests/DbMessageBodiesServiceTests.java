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

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.vertx.java.core.streams.ReadStream;

import net.bluemind.backend.cyrus.replication.testhelper.CyrusGUID;
import net.bluemind.backend.cyrus.replication.testhelper.InputStreamWrapper;
import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.replica.api.IDbMessageBodies;
import net.bluemind.core.api.Stream;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.vertx.VertxStream;

public class DbMessageBodiesServiceTests extends AbstractMessageBodiesServiceTests {

	@Test
	public void crudFromStream() {
		IDbMessageBodies mboxes = getService(SecurityContext.SYSTEM);
		assertNotNull(mboxes);
		ReadStream<InputStreamWrapper> emlReadStream = openResource("data/with_inlines.eml");
		Stream bmStream = VertxStream.stream(emlReadStream);
		String uid = CyrusGUID.randomGuid();
		mboxes.create(uid, bmStream);

		MessageBody loaded = mboxes.getComplete(uid);
		System.out.println("Found " + loaded.subject);
		MessageBody body = loaded;
		System.out.println("[" + body.size + " byte(s)] S: '" + body.subject + "', headers: " + body.headers.size()
				+ ", smartAttach: " + body.smartAttach);
		body.recipients.forEach(recip -> {
			if (recip.dn != null) {
				System.out.println(recip.kind + " " + recip.dn + " <" + recip.address + ">");
			} else {
				System.out.println(recip.kind + " <" + recip.address + ">");
			}
		});
	}

	protected IDbMessageBodies getService(SecurityContext ctx) {
		return ServerSideServiceProvider.getProvider(ctx).instance(IDbMessageBodies.class, partition);
	}

}
