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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.backend.mail.replica.service.internal.transfer;

import java.io.File;

import net.bluemind.backend.mail.replica.service.internal.ItemsTransferServiceFactory.BodyTransfer;
import net.bluemind.backend.mail.replica.service.sds.MessageBodyObjectStore;
import net.bluemind.core.rest.BmContext;

public class CrossMailboxCrossLocationTransferService extends CrossMailboxTransferService {

	private final String fromLocation;
	private final String toLocation;

	public CrossMailboxCrossLocationTransferService(BmContext context, TransferContext transferContext,
			String fromLocation, String toLocation) {
		super(context, transferContext);
		this.fromLocation = fromLocation;
		this.toLocation = toLocation;
	}

	@Override
	protected BodyTransfer bodyXfer() {
		MessageBodyObjectStore srcBodies = new MessageBodyObjectStore(context, fromLocation);
		if (!srcBodies.isSingleNamespaceBody()) {
			MessageBodyObjectStore tgtBodies = new MessageBodyObjectStore(context, toLocation);
			return (guid, date) -> {
				File toXfer = null;
				try {
					toXfer = srcBodies.open(guid).toFile();
					tgtBodies.store(guid, date, toXfer);
				} finally {
					if (toXfer != null) {
						toXfer.delete();// NOSONAR do not throw
					}
				}
			};
		} else {
			return BodyTransfer.NOOP;
		}
	}

}
