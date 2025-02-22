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

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import net.bluemind.backend.mail.replica.api.IReplicatedMailboxesRootMgmt;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor.Namespace;
import net.bluemind.backend.mailapi.testhelper.MailApiTestsBase;
import net.bluemind.core.context.SecurityContext;

public abstract class AbstractReplicatedMailboxesRootMgmtServiceTests extends MailApiTestsBase {

	protected String partition;

	@BeforeEach
	public void before(TestInfo testInfo) throws Exception {
		super.before(testInfo);
		partition = domUid;
	}

	@Test
	public void testGetService() {
		IReplicatedMailboxesRootMgmt service = getService(SecurityContext.SYSTEM);
		assertNotNull(service);
	}

	@Test
	public void testCrud() {
		IReplicatedMailboxesRootMgmt service = getService(SecurityContext.SYSTEM);
		MailboxReplicaRootDescriptor root = MailboxReplicaRootDescriptor.create(Namespace.users, userUid);
		service.create(root);
		service.delete(root.ns.name(), root.name);
	}

	protected abstract IReplicatedMailboxesRootMgmt getService(SecurityContext ctx);

}
