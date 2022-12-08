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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;

import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.replica.api.IDbMessageBodies;
import net.bluemind.backend.mail.replica.service.tests.compat.CyrusGUID;
import net.bluemind.core.api.Stream;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;

public class DbMessageBodiesServiceTests extends AbstractMessageBodiesServiceTests {

	@Test
	public void crudFromStream() {
		IDbMessageBodies mboxes = getService(SecurityContext.SYSTEM);
		assertNotNull(mboxes);
		Stream emlReadStream = openResource("data/with_inlines.eml");
		String uid = CyrusGUID.randomGuid();
		mboxes.create(uid, emlReadStream);

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

	@Test
	public void storeFromStreamBM14964Subject() {
		IDbMessageBodies mboxes = getService(SecurityContext.SYSTEM);
		assertNotNull(mboxes);
		Stream emlReadStream = openResource("data/mail.eml");
		String uid = CyrusGUID.randomGuid();
		mboxes.create(uid, emlReadStream);

		MessageBody loaded = mboxes.getComplete(uid);
		assertEquals("Engagements Budget DSI_ FONCTION détail", loaded.subject);
	}

	@Test
	public void repeatBM15193() {

		for (int i = 0; i < 10; i++) {
			try {
				storeFromStreamBM15193();
			} catch (Exception e) {
				fail("Test broken at loop " + i + ": " + e.getMessage());
			}
		}
	}

	@Test
	public void storeFromStreamBM15193() {
		IDbMessageBodies mboxes = getService(SecurityContext.SYSTEM);
		assertNotNull(mboxes);
		Stream emlReadStream = openResource("data/bm-15193.eml");
		String uid = "d2d5876ba2d00d2c1290e46323adefa48567f0a7";
		mboxes.create(uid, emlReadStream);

		MessageBody loaded = mboxes.getComplete(uid);
		assertNotNull(loaded);
		mboxes.delete(uid);
	}

	@Test
	public void storeFromStreamBM15245() {
		IDbMessageBodies mboxes = getService(SecurityContext.SYSTEM);
		assertNotNull(mboxes);
		Stream emlReadStream = openResource("data/BM-15245.eml");
		String uid = CyrusGUID.randomGuid();
		mboxes.create(uid, emlReadStream);

		MessageBody loaded = mboxes.getComplete(uid);
		assertNotNull(loaded);
		mboxes.delete(uid);
	}

	@Test
	public void storeFromStreamTLIB766() {
		IDbMessageBodies mboxes = getService(SecurityContext.SYSTEM);
		assertNotNull(mboxes);
		Stream emlReadStream = openResource("data/TLIB-766.eml");
		String uid = CyrusGUID.randomGuid();
		mboxes.create(uid, emlReadStream);

		MessageBody loaded = mboxes.getComplete(uid);
		assertNotNull(loaded);
		mboxes.delete(uid);
	}

	@Test
	public void storeFromStreamFactorFx327() {
		IDbMessageBodies mboxes = getService(SecurityContext.SYSTEM);
		assertNotNull(mboxes);
		Stream emlReadStream = openResource("data/nested.eml");
		String uid = CyrusGUID.randomGuid();
		mboxes.create(uid, emlReadStream);

		MessageBody loaded = mboxes.getComplete(uid);
		assertNotNull(loaded);
		System.err.println("body " + loaded);
		mboxes.delete(uid);
	}

	protected IDbMessageBodies getService(SecurityContext ctx) {
		return ServerSideServiceProvider.getProvider(ctx).instance(IDbMessageBodies.class, partition);
	}

}
