/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.keycloak.utils;

import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigUpdateHelper {
	private static final Logger logger = LoggerFactory.getLogger(ConfigUpdateHelper.class);
	private static final UpdatesStatus updStatus = new UpdatesStatus();

	public static void updateRealmFor(String domainUid) {
		if (updStatus.startUpdate(domainUid)) {
			new Worker(domainUid).start();
		}
	}

	static class UpdatesStatus {
		private HashSet<String> runningUpdates = new HashSet<String>();

		protected synchronized boolean startUpdate(String domainUid) {
			if (runningUpdates.contains(domainUid)) {
				return false;
			}
			runningUpdates.add(domainUid);
			return true;
		}

		protected synchronized void doneUpdate(String domainUid) {
			runningUpdates.remove(domainUid);
		}
	}

	static class Worker extends Thread {
		private String domainUid;
		private static final int secWait = 5;

		Worker(String domainUid) {
			this.domainUid = domainUid;
		}

		@Override
		public void run() {
			try {
				TimeUnit.SECONDS.sleep(secWait);
			} catch (InterruptedException e) {
			}
			try {
				KeycloakHelper.updateForDomain(domainUid);
			} catch (Throwable t) {
				logger.error("Error updating Keycloak conf for domain " + domainUid, t);
			}
			updStatus.doneUpdate(domainUid);
		}
	}
}
