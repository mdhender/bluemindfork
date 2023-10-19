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
package net.bluemind.eas.backend.bm.mail.loader;

import net.bluemind.backend.mail.api.IMailboxItems;
import net.bluemind.backend.mail.api.MessageBody.Part;
import net.bluemind.core.rest.base.GenericStream;
import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.bm.impl.CoreConnect;

public class MailAttachmentProvider {

	private static final CoreConnect coreStub = new CoreConnect();
	private IMailboxItems service;
	private BackendSession bs;

	public MailAttachmentProvider(BackendSession bs) {
		this.bs = bs;
	}

	public byte[] fetchPart(Part part, long imapUid, String container) {
		service = coreStub.getMailboxItemsService(bs, container);
		return GenericStream.streamToBytes(service.fetch(imapUid, part.address, null, null, null, null));
	}

}
