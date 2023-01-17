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

import net.bluemind.imap.sieve.SieveClient;
import net.bluemind.imap.sieve.SieveClient.SieveConnectionData;

public class SieveLoginTests extends SieveTestCase {

	public void testLoginLogout() {
		SieveConnectionData connectionData = new SieveConnectionData(testLogin, testPass, cyrusIp);
		try (SieveClient sc = new SieveClient(connectionData)) {
			boolean ret = sc.login();
			assertTrue(ret);
			sc.logout();
		} catch (Exception e) {
			e.printStackTrace();
			fail("error on testLoginLogout");
		}
	}

	public void testUnauthenticate() {
		SieveConnectionData connectionData = new SieveConnectionData(testLogin, testPass, cyrusIp);
		try (SieveClient sc = new SieveClient(connectionData)) {

			boolean ret = sc.login();
			assertTrue(ret);
			System.err.println("before unauth");
			sc.unauthenticate();
			System.err.println("after unauth");
			sc.logout();
		} catch (Exception e) {
			e.printStackTrace();
			fail("error on testUnauthenticate");
		}
	}

	public void testLoginLogoutPerf() throws InterruptedException {
		final int IT_COUNT = 300; // fails when > 200....
		SieveConnectionData connectionData = new SieveConnectionData(testLogin, testPass, cyrusIp);
		try (SieveClient sc = new SieveClient(connectionData)) {

			for (int i = 0; i < 3; i++) {
				assertTrue(sc.login());
				sc.logout();
			}

			long time = System.currentTimeMillis();
			for (int i = 0; i < IT_COUNT; i++) {
				System.err.println("iteration " + i);
				sc.login();
				sc.logout();
			}
			time = System.currentTimeMillis() - time;
			System.out.println(IT_COUNT + " sieve connections done in " + ((time + 0.1) / IT_COUNT) + " seconds.");
		} catch (Exception e) {
			e.printStackTrace();
			fail("error on testLoginLogoutPerf");
		}
	}

}
