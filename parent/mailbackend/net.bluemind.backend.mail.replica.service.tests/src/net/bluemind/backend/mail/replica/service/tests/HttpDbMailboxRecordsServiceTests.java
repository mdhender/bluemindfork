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

import java.util.Date;

import org.junit.Test;

import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IDbMessageBodies;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.IdQuery;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.core.sessions.Sessions;

public class HttpDbMailboxRecordsServiceTests extends AbstractMailboxRecordsServiceTests<IDbMailboxRecords> {

	protected IDbMailboxRecords getService(SecurityContext ctx) {
		Sessions.get().put("test-sid", ctx);
		return ClientSideServiceProvider.getProvider("http://127.0.0.1:8090", "test-sid")
				.instance(IDbMailboxRecords.class, mboxUniqueId);
	}

	protected IDbMessageBodies getBodies(SecurityContext ctx) {
		Sessions.get().put("test-sid", ctx);
		return ClientSideServiceProvider.getProvider("http://127.0.0.1:8090", "test-sid")
				.instance(IDbMessageBodies.class, partition);
	}

	private ListResult<Long> recsAllIds(IDbMailboxRecords recs, IdQuery q) {
		return recs.allIds(ItemFlagFilter.toQueryString(q.filter), q.knownContainerVersion, q.limit, q.offset);
	}

	@Test
	public void testAllIds() {
		IDbMailboxRecords recs = getService(SecurityContext.SYSTEM);
		IdQuery q = new IdQuery();
		q.filter = ItemFlagFilter.create().mustNot(ItemFlag.Deleted);
		q.knownContainerVersion = recs.getVersion();
		ListResult<Long> result = recsAllIds(recs, q);
		assertNotNull(result);
		MailboxRecord mr = new MailboxRecord();
		mr.imapUid = 43;
		mr.internalDate = new Date();
		mr.lastUpdated = mr.internalDate;
		mr.messageBody = "deadbeef";
		recs.create(mr.imapUid + ".", mr);
		try {
			recsAllIds(recs, q);
			fail();// expect stale client version
		} catch (ServerFault sf) {
			// ok
		}
		q.knownContainerVersion = recs.getVersion();
		result = recsAllIds(recs, q);
		assertEquals(1, result.total);
		System.err.println("result: " + result.values);
		for (long imap = 44; imap < 144; imap++) {
			MailboxRecord r = new MailboxRecord();
			r.imapUid = imap;
			r.internalDate = new Date();
			r.lastUpdated = r.internalDate;
			r.messageBody = "deadbeef" + Long.toHexString(imap);
			recs.create(r.imapUid + ".", r);
		}
		q.knownContainerVersion = recs.getVersion();
		result = recsAllIds(recs, q);
		System.err.println("result: " + result.values + " " + result.total);
		assertEquals(q.limit, result.values.size());
		long fetched = 0;
		while (!result.values.isEmpty()) {
			q.offset += result.values.size();
			fetched += result.values.size();
			result = recsAllIds(recs, q);
			System.err.println("result: " + result.values + " " + result.total);
		}
		assertEquals(fetched, result.total);

	}

}
