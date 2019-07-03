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

import java.util.Map.Entry;

public class AnnotationCase extends CyradmTestCase {
	public void testSpecialUseAnnotation() {
		String sentFolder = mboxCyrusPrefix + mboxName + "/Sent@" + domainUid;
		sc.create(sentFolder, "Sent");

		AnnotationList annotations = sc.getAnnotation(sentFolder);
		for (Entry<String, Annotation> entry : annotations.entrySet()) {
			if (!"/specialuse".equals(entry.getKey())) {
				continue;
			}

			assertNull(entry.getValue().valuePriv);
			assertNull(entry.getValue().valueShared);
		}

		try (StoreClient usc = new StoreClient(cyrusIp, 1143, mboxLogin, "pass")) {
			assertTrue(usc.login());
			annotations = usc.getAnnotation("Sent");
			for (Entry<String, Annotation> entry : annotations.entrySet()) {
				System.out.println(entry.getKey() + ", priv: " + entry.getValue().valuePriv + ", shared: "
						+ entry.getValue().valueShared);
				if (!"/specialuse".equals(entry.getKey())) {
					continue;
				}

				assertEquals("\\Sent", entry.getValue().valuePriv);
				assertNull(entry.getValue().valueShared);
			}

		}
	}
}
