/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.backend.cyrus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.cyrus.internal.MailboxOps;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.config.Token;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.imap.CreateMailboxResult;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.StoreClient;
import net.bluemind.server.api.Server;

public class CyrusServiceForTests extends CyrusService {

	private static final Logger logger = LoggerFactory.getLogger(CyrusServiceForTests.class);

	public CyrusServiceForTests(String backendAddress) throws ServerFault {
		super(backendAddress);
	}

	public CyrusServiceForTests(ItemValue<Server> srv) {
		super(srv);
	}

	/**
	 * Using this will set an incorrect/random UNIQUEID for the mailbox in cyrus,
	 * which is not what we want.
	 * 
	 * 
	 * @param boxName
	 * @param domainUid
	 * 
	 * @deprecated only used by tests right now, prefer
	 *             {@link CyrusService#createRoot(String, ItemValue)}
	 * 
	 * @throws ServerFault
	 */
	@Deprecated
	public void createBox(String boxName, String domainUid) throws ServerFault {
		CyrusPartition realPartition = CyrusPartition.forServerAndDomain(backend, domainUid);
		try (StoreClient sc = new StoreClient(backendAddress, 1143, "admin0", Token.admin0())) {
			if (!sc.login()) {
				throw new ServerFault(
						"createMbox failed for '" + boxName + "'. Login as admin0 failed, server " + backendAddress);
			}
			CreateMailboxResult result = sc.createMailbox(boxName, realPartition.name);
			logger.info("MAILBOX create: {} for '{}' on partition '{}'", result.isOk() ? "OK" : result.getMessage(),
					boxName, realPartition.name);

			if (!result.isOk()) {
				if (!result.getMessage().contains("NO Mailbox already exists")) {
					logger.error(
							"createMailbox failed for mbox '" + boxName + "', server said: " + result.getMessage());
					throw new ServerFault(
							"createMbox failed for '" + boxName + "'. server msg: " + result.getMessage());
				} else {
					logger.info("mbox " + boxName + " already exists, that's fine.");
				}
			}

			MailboxOps.addSharedSeenAnnotation(sc, boxName);
		} catch (IMAPException e) {
			logger.error(e.getMessage(), e);
			throw new ServerFault(
					"error during mailbox [" + boxName + ":" + realPartition.name + "] creation " + e.getMessage());
		}
	}
}
