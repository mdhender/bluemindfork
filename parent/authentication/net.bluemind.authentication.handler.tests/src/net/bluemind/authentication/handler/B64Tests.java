/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.authentication.handler;

import static org.junit.Assert.assertNotNull;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class B64Tests {

	int COUNT = 1000000;

	@Test
	public void testSpeed() {
		long start = System.nanoTime();
		for (int i = 0; i < COUNT; i++) {
			String decoded2 = new String(java.util.Base64.getDecoder().decode(("dGVzdEBkb21haW4ubmV0")));
			assertNotNull(decoded2);

		}
		start = System.nanoTime();
		for (int i = 0; i < COUNT; i++) {
			String decoded2 = new String(java.util.Base64.getDecoder().decode(("dGVzdEBkb21haW4ubmV0")));
			assertNotNull(decoded2);
		}
		// Assert.assertEquals("test@domain.net", decoded);
		long elapsedTime = System.nanoTime() - start;
		System.out.println("jdk b64: " + TimeUnit.NANOSECONDS.toMillis(elapsedTime) + "ms.");

		start = System.nanoTime();
		for (int i = 0; i < COUNT; i++) {
			String decoded = Nginx.decode("dGVzdEBkb21haW4ubmV0");
			if (i == 0) {
				System.out.println("decoded: " + decoded);
			}
			assertNotNull(decoded);
		}
		start = System.nanoTime();
		for (int i = 0; i < COUNT; i++) {
			String decoded = Nginx.decode("dGVzdEBkb21haW4ubmV0");
			assertNotNull(decoded);
		}
		elapsedTime = System.nanoTime() - start;
		System.out.println("netty b64: " + TimeUnit.NANOSECONDS.toMillis(elapsedTime) + "ms.");
	}

}
