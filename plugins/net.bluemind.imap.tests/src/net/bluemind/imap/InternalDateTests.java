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
package net.bluemind.imap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collection;

import org.junit.Test;

public class InternalDateTests extends LoggedTestCase {

	@Test
	public void testInternalDate() throws IMAPException {
		try (StoreClient sc = newStore(false)) {
			;
			sc.select("INBOX");
			long time = System.currentTimeMillis();
			Collection<Integer> uids = sc.uidSearch(new SearchQuery());
			long search = System.currentTimeMillis();
			System.out.println("Search time for " + uids.size() + " mails: " + (search - time) + "ms.");
			InternalDate[] dates = sc.uidFetchInternalDate(uids);
			long idates = System.currentTimeMillis();
			System.out.println("internal dates time for " + uids.size() + " mails: " + (idates - search) + "ms.");
			assertNotNull(dates);
			assertEquals(uids.size(), dates.length);
		}
	}
}
