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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import net.bluemind.backend.mail.api.IBaseMailboxFolders;
import net.bluemind.backend.mail.replica.api.IReplicatedMailboxesRootMgmt;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor.Namespace;
import net.bluemind.backend.mail.replica.api.utils.Subtree;
import net.bluemind.backend.mail.replica.utils.SubtreeContainer;
import net.bluemind.backend.mailapi.testhelper.MailApiTestsBase;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.tests.BmTestContext;

public abstract class AbstractReplicatedMailboxesServiceTests<T extends IBaseMailboxFolders> extends MailApiTestsBase {

	protected String partition;
	protected MailboxReplicaRootDescriptor mboxDescriptor;
	protected Subtree subtreeDescriptor;

	@BeforeEach
	@Override
	public void before(TestInfo testInfo) throws Exception {
		super.before(testInfo);

		partition = domUid.replace('.', '_');
		mboxDescriptor = MailboxReplicaRootDescriptor.create(Namespace.users, userUid);
		IReplicatedMailboxesRootMgmt rootMgmt = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IReplicatedMailboxesRootMgmt.class, partition);
		rootMgmt.create(mboxDescriptor);
		this.subtreeDescriptor = SubtreeContainer.mailSubtreeUid(domUid, Namespace.users, userUid);
	}

	@AfterEach
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	protected abstract T getService(SecurityContext ctx);

	@Test
	public void testGetApi() {
		BmTestContext testCtx = BmTestContext.contextWithSession("test-sid", subtreeDescriptor.ownerUid, domUid);
		assertNotNull(getService(testCtx.getSecurityContext()));
	}
}
