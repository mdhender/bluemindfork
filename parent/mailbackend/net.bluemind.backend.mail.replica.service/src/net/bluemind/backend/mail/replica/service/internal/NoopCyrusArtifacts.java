/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.backend.mail.replica.service.internal;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.mail.replica.api.ICyrusReplicationArtifacts;
import net.bluemind.backend.mail.replica.api.MailboxSub;
import net.bluemind.backend.mail.replica.api.QuotaRoot;
import net.bluemind.backend.mail.replica.api.SeenOverlay;
import net.bluemind.backend.mail.replica.api.SieveScript;
import net.bluemind.core.api.fault.ServerFault;

public class NoopCyrusArtifacts implements ICyrusReplicationArtifacts {
	private static final Logger logger = LoggerFactory.getLogger(NoopCyrusArtifacts.class);
	private final String userId;

	public NoopCyrusArtifacts(String userId) {
		this.userId = userId;
	}

	@Override
	public void storeScript(SieveScript ss) {
		logger.info("NOOP #storeScript() for {}", userId);
	}

	@Override
	public void deleteScript(SieveScript ss) {
		logger.info("NOOP #deleteScript() for {}", userId);
	}

	@Override
	public List<SieveScript> sieves() {
		logger.info("NOOP #sieves() for {}", userId);
		return Collections.emptyList();
	}

	@Override
	public void storeSub(MailboxSub ss) {
		logger.info("NOOP #storeSub() for {}", userId);

	}

	@Override
	public void deleteSub(MailboxSub ss) {
		logger.info("NOOP #deleteSub() for {}", userId);
	}

	@Override
	public List<MailboxSub> subs() {
		logger.info("NOOP #subs() for {}", userId);
		return Collections.emptyList();
	}

	@Override
	public void storeQuota(QuotaRoot ss) {
		logger.info("NOOP #storeQuota() for {}", userId);

	}

	@Override
	public void deleteQuota(QuotaRoot ss) {
		logger.info("NOOP #deleteQuota() for {}", userId);

	}

	@Override
	public List<QuotaRoot> quotas() {
		logger.info("NOOP #quotas() for {}", userId);
		return Collections.emptyList();
	}

	@Override
	public void storeSeen(SeenOverlay ss) {
		logger.info("NOOP #storeSeen() for {}", userId);
	}

	@Override
	public List<SeenOverlay> seens() {
		logger.info("NOOP #seens() for {}", userId);
		return Collections.emptyList();
	}

	@Override
	public void xfer(String serverUid) throws ServerFault {
		// noop
	}
}
