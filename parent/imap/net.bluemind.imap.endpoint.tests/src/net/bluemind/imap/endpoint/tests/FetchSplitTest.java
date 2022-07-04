/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.imap.endpoint.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import net.bluemind.imap.endpoint.cmd.AnalyzedCommand;
import net.bluemind.imap.endpoint.cmd.UidFetchCommand;

public class FetchSplitTest {

	private UidFetchCommand uidFetch(String s) {
		AnalyzedCommand uidFetch = RawCommand.analyzed(s);
		assertNotNull(uidFetch);
		UidFetchCommand f = (UidFetchCommand) uidFetch;
		System.err.println(f.fetchSpec());
		return f;
	}

	@Test
	public void testSplit() {
		UidFetchCommand f = uidFetch("a1 uid fetch 1:* (flags)");
		assertNotNull(f.fetchSpec());
		assertEquals(1, f.fetchSpec().size());
	}

	@Test
	public void testPartialPeek() {
		UidFetchCommand f = uidFetch("a1 uid fetch 1:* (body.peek[TEXT]<0.2028>)");
		assertNotNull(f.fetchSpec());
		assertEquals(1, f.fetchSpec().size());
		assertEquals("BODY.PEEK[TEXT]<0.2028>", f.fetchSpec().get(0).toString());
	}

	@Test
	public void testSplitComplex() {
		UidFetchCommand f = uidFetch(
				"a2 uid fetch 1:* (UID RFC822.SIZE FLAGS BODY.PEEK[HEADER.FIELDS (From To Cc Bcc Subject Date Message-ID Priority X-Priority References Newsgroups In-Reply-To Content-Type Reply-To)])");
		assertNotNull(f.fetchSpec());
		assertEquals(4, f.fetchSpec().size());
	}

}
