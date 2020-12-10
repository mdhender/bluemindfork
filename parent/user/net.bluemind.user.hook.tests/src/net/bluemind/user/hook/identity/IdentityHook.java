/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.user.hook.identity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.CountDownLatch;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.user.api.UserMailIdentity;

public class IdentityHook implements IUserMailIdentityHook {

	public static final CountDownLatch latch = new CountDownLatch(5);

	public IdentityHook() {
		System.out.println("Test hook created");
	}

	@Override
	public void beforeCreate(BmContext context, String domainUid, String uid, UserMailIdentity identity) {
		assertNotNull(context);
		assertNotNull(domainUid);
		assertNotNull(identity);
		assertEquals(identity.name, "John Doe");
		latch.countDown();
	}

	@Override
	public void beforeUpdate(BmContext context, String domainUid, String uid, UserMailIdentity update,
			UserMailIdentity previous) {
		assertNotNull(domainUid);
		assertNotNull(previous);
		assertEquals(previous.name, "John Doe");
		assertNotNull(update);
		assertEquals(update.name, "John Doe Updated");
		latch.countDown();

	}

	@Override
	public void beforeDelete(BmContext context, String domainUid, String uid, UserMailIdentity previous) {
		assertNotNull(domainUid);
		assertNotNull(previous);
		assertEquals(previous.name, "John Doe Updated");
		latch.countDown();
	}

	@Override
	public void onIdentityUpdated(BmContext context, String domainUid, String userUid, UserMailIdentity current,
			UserMailIdentity previous) throws ServerFault {
		// Nothing to do on update
		assertNotNull(domainUid);
		assertNotNull(previous);
		assertEquals(previous.name, "John Doe");
		assertNotNull(current);
		assertEquals(current.name, "John Doe Updated");
		latch.countDown();
	}

	@Override
	public void onIdentityDefault(BmContext context, String domainUid, String userUid, String id) {
		assertNotNull(domainUid);
		latch.countDown();
	}

}
