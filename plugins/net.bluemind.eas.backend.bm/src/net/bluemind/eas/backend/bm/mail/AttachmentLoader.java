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
package net.bluemind.eas.backend.bm.mail;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.vertx.java.core.buffer.Buffer;

import net.bluemind.backend.mail.api.FetchOptions;
import net.bluemind.backend.mail.api.IMailboxItems;
import net.bluemind.backend.mail.api.MailboxItem;
import net.bluemind.core.api.Stream;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.MailFolder;
import net.bluemind.eas.backend.bm.impl.CoreConnect;
import net.bluemind.eas.backend.bm.mail.loader.FastByteInputStream;
import net.bluemind.eas.backend.bm.mail.loader.SyncStreamDownload;

public class AttachmentLoader extends CoreConnect {

	private BackendSession bs;
	private MailFolder folder;

	public AttachmentLoader(BackendSession bs, MailFolder folder) {
		this.bs = bs;
		this.folder = folder;
	}

	public InputStream fetch(int id, String partAddr, String contentTransferEncoding)
			throws InterruptedException, ExecutionException, TimeoutException {

		IMailboxItems service = getMailboxItemsService(bs, folder.uid);
		ItemValue<MailboxItem> item = service.getCompleteById(id);
		if (item == null) {
			return null;
		}

		Stream partStream = service.fetch(item.value.imapUid, partAddr, FetchOptions.decoded(contentTransferEncoding));
		CompletableFuture<Buffer> partContent = SyncStreamDownload.read(partStream);
		Buffer part = partContent.get(10, TimeUnit.SECONDS);
		return new FastByteInputStream(part.getBytes());
	}

}
