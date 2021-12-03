/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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

import java.sql.SQLException;
import java.util.List;

import net.bluemind.backend.mail.replica.api.ICyrusReplicationArtifacts;
import net.bluemind.backend.mail.replica.api.MailboxSub;
import net.bluemind.backend.mail.replica.api.QuotaRoot;
import net.bluemind.backend.mail.replica.api.SeenOverlay;
import net.bluemind.backend.mail.replica.api.SieveScript;
import net.bluemind.backend.mail.replica.persistence.MailboxSubStore;
import net.bluemind.backend.mail.replica.persistence.QuotaStore;
import net.bluemind.backend.mail.replica.persistence.SeenOverlayStore;
import net.bluemind.backend.mail.replica.persistence.SieveScriptStore;
import net.bluemind.core.api.fault.ServerFault;

public class CyrusArtifactsService implements ICyrusReplicationArtifacts {

	private final MailboxSubStore subStore;
	private final QuotaStore quotaStore;
	private final SeenOverlayStore seenStore;
	private final SieveScriptStore sieveStore;
	private final String userId;

	public CyrusArtifactsService(String userId, MailboxSubStore sub, QuotaStore quota, SeenOverlayStore seen,
			SieveScriptStore sieve) {
		this.userId = userId;
		this.subStore = sub;
		this.quotaStore = quota;
		this.seenStore = seen;
		this.sieveStore = sieve;
	}

	@Override
	public void storeScript(SieveScript ss) {
		try {
			ss.userId = userId;
			sieveStore.store(ss);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public void deleteScript(SieveScript ss) {
		try {
			ss.userId = userId;
			sieveStore.delete(ss);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public List<SieveScript> sieves() {
		try {
			return sieveStore.byUser(userId);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public void storeSub(MailboxSub ss) {
		try {
			subStore.store(ss);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public void deleteSub(MailboxSub ss) {
		try {
			subStore.delete(ss);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public List<MailboxSub> subs() {
		try {
			return subStore.byUser(userId);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public void storeQuota(QuotaRoot ss) {
		try {
			quotaStore.store(ss);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public void deleteQuota(QuotaRoot ss) {
		try {
			quotaStore.delete(ss);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public List<QuotaRoot> quotas() {
		try {
			return quotaStore.byUser(userId);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public void storeSeen(SeenOverlay ss) {
		try {
			seenStore.store(ss);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public List<SeenOverlay> seens() {
		try {
			return seenStore.byUser(userId);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}
}
