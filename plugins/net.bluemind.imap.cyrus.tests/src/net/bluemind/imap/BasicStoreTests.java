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

import java.util.List;

import net.bluemind.imap.impl.MailThread;

public class BasicStoreTests extends LoggedTestCase {

	public void testUidThreads() throws Exception {
		sc.select("INBOX");

		IMAPByteSource msg1 = getUtf8Rfc822Message();
		IMAPByteSource msg2 = getUtf8Rfc822Message();
		sc.append("INBOX", msg1.source().openStream(), new FlagsList());
		sc.append("INBOX", msg2.source().openStream(), new FlagsList());
		msg1.close();
		msg2.close();

		List<MailThread> threads = sc.uidThreads();
		assertNotNull(threads);
		assertTrue(threads.size() > 0);
	}

}
