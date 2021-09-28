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
package net.bluemind.eas.backend.bm.mail;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.james.mime4j.dom.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;

import io.vertx.core.buffer.Buffer;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.api.IItemsTransfer;
import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.IMailboxItems;
import net.bluemind.backend.mail.api.MailboxItem;
import net.bluemind.backend.mail.api.MessageBody.Part;
import net.bluemind.backend.mail.api.flags.MailboxItemFlag;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerSubscriptionModel;
import net.bluemind.core.container.api.IOwnerSubscriptions;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemIdentifier;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.core.sendmail.Sendmail;
import net.bluemind.core.sendmail.SendmailCredentials;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.HierarchyNode;
import net.bluemind.eas.backend.MailFolder;
import net.bluemind.eas.backend.bm.impl.CoreConnect;
import net.bluemind.eas.dto.base.AirSyncBaseResponse;
import net.bluemind.eas.dto.base.BodyOptions;
import net.bluemind.eas.dto.email.EmailResponse;
import net.bluemind.eas.dto.moveitems.MoveItemsResponse;
import net.bluemind.eas.dto.moveitems.MoveItemsResponse.Response.Status;
import net.bluemind.eas.dto.sync.CollectionId;
import net.bluemind.eas.dto.user.MSUser;
import net.bluemind.eas.impl.Backends;
import net.bluemind.mime4j.common.IRenderableMessage;

public class EmailManager extends CoreConnect {

	private static EmailManager instance;

	static {
		instance = new EmailManager();
	}

	protected static final Logger logger = LoggerFactory.getLogger(EmailManager.class);

	private EmailManager() {
	}

	public static EmailManager getInstance() {
		return instance;
	}

	/**
	 * @param bs
	 * @param folder
	 * @param id
	 * @return
	 */
	public EmailResponse loadStructure(BackendSession bs, MailFolder folder, long id) {
		StructureMailLoader ml = new StructureMailLoader(bs, folder);
		return ml.fetch(id);
	}

	public AirSyncBaseResponse loadBody(BackendSession bs, MailFolder folder, long id, BodyOptions options) {
		BodyMailLoader ml = new BodyMailLoader(bs, folder);
		return ml.fetch(id, options);

	}

	public InputStream fetchMimeStream(BackendSession bs, MailFolder folder, long id) {
		BodyMailLoader ml = new BodyMailLoader(bs, folder);
		return ml.fetchMimeInputStream(id);
	}

	public InputStream fetchAttachment(BackendSession bs, MailFolder folder, int id, String mimePartAddress,
			String contentTransferEncoding) throws InterruptedException, ExecutionException, TimeoutException {
		AttachmentLoader al = new AttachmentLoader(bs, folder);
		return al.fetch(id, mimePartAddress, contentTransferEncoding);
	}

	public List<MoveItemsResponse.Response> moveItems(BackendSession bs, HierarchyNode srcFolder,
			HierarchyNode dstFolder, List<Long> items) {

		logger.info("[{}] move to collection {} mail {}", bs.getUser().getUid(), dstFolder.containerUid, items);

		IItemsTransfer transferService = getService(bs, IItemsTransfer.class,
				IMailReplicaUids.uniqueId(srcFolder.containerUid), IMailReplicaUids.uniqueId(dstFolder.containerUid));

		List<MoveItemsResponse.Response> ret = new ArrayList<>(items.size());

		try {
			List<ItemIdentifier> result = transferService.move(items);
			boolean isCoherent = items.size() == result.size();
			for (int i = 0; i < items.size(); i++) {
				Long item = items.get(i);
				MoveItemsResponse.Response r = new MoveItemsResponse.Response();
				r.srcMsgId = srcFolder.collectionId.getValue() + ":" + item;
				if (isCoherent) {
					r.dstMsgId = dstFolder.collectionId.getValue() + ":" + result.get(i).id;
				} else {
					r.dstMsgId = r.srcMsgId;
				}
				r.status = Status.Success;
				ret.add(r);
			}
			return ret;
		} catch (ServerFault sf) {
			logger.error(sf.getMessage(), sf);
			items.forEach(id -> {
				MoveItemsResponse.Response r = new MoveItemsResponse.Response();
				r.srcMsgId = srcFolder.collectionId.getValue() + ":" + id;
				r.dstMsgId = r.srcMsgId;
				r.status = Status.SourceOrDestinationLocked;
				ret.add(r);
			});
			return ret;
		}
	}

	public void sendEmail(BackendSession bs, IRenderableMessage email, Boolean saveInSent) throws Exception {
		try {
			logger.info("Sending mail...");

			Message m = email.renderAs(Message.class);
			if (m.getDate() == null) {
				m.setDate(new Date());
			}

			if (Boolean.TRUE.equals(saveInSent)) {

				try (InputStream is = email.renderAsMimeStream()) {
					MailFolder sent = Backends.internalStorage().getMailFolderByName(bs, "Sent");
					IMailboxItems service = getMailboxItemsService(bs, sent.uid);
					byte[] data = ByteStreams.toByteArray(is);
					String partAddr = service.uploadPart(VertxStream.stream(Buffer.buffer(data)));
					try {
						MailboxItem mi = MailboxItem.of(m.getSubject(), Part.create(null, "message/rfc822", partAddr));
						mi.body.date = new Date();
						mi.flags = Arrays.asList(MailboxItemFlag.System.Seen.value());
						service.create(mi);
					} catch (ServerFault serverFault) {
						if (serverFault.getCode() != ErrorCode.TIMEOUT) {
							throw serverFault;
						}
					} finally {
						service.removePart(partAddr);
					}
				}
			}

			Sendmail sm = new Sendmail();
			MSUser u = bs.getUser();
			sm.send(SendmailCredentials.as(u.getLoginAtDomain(), u.getSid()), u.getDefaultEmail(), u.getDomain(), m);

		} catch (Exception e) {
			// TODO rm sent item
			logger.error(e.getMessage(), e);
			throw e;
		}

	}

	public void purgeFolder(BackendSession bs, MailFolder folder, CollectionId collectionId, boolean deleteSubFolder) {
		if (deleteSubFolder) {
			String mailboxRoot = "user." + bs.getUser().getUid().replace('.', '^');
			CyrusPartition part = CyrusPartition.forServerAndDomain(bs.getUser().getDataLocation(),
					bs.getUser().getDomain());

			if (collectionId.getSubscriptionId().isPresent()) {
				IOwnerSubscriptions subscriptionsService = getService(bs, IOwnerSubscriptions.class,
						bs.getUser().getDomain(), bs.getUser().getUid());
				ItemValue<ContainerSubscriptionModel> sub = subscriptionsService
						.getCompleteById(collectionId.getSubscriptionId().get());

				IDirectory directoryService = getAdmin0Service(bs, IDirectory.class, bs.getUser().getDomain());
				DirEntry dirEntry = directoryService.findByEntryUid(sub.value.owner);
				if (dirEntry.kind == Kind.USER) {
					mailboxRoot = "user." + dirEntry.entryUid.replace('.', '^');
				} else {
					mailboxRoot = dirEntry.entryUid.replace('.', '^');
				}
				part = CyrusPartition.forServerAndDomain(dirEntry.dataLocation, bs.getUser().getDomain());
			}

			IMailboxFolders service = getService(bs, IMailboxFolders.class, part.name, mailboxRoot);
			service.emptyFolder(folder.collectionId.getFolderId());
		} else {
			IMailboxItems service = getMailboxItemsService(bs, folder.uid);
			ContainerChangeset<ItemVersion> changeset = service.filteredChangesetById(0L,
					ItemFlagFilter.create().mustNot(ItemFlag.Deleted));
			changeset.created.forEach(iv -> {
				try {
					service.deleteById(iv.id);
				} catch (ServerFault serverFault) {
					if (serverFault.getCode() != ErrorCode.TIMEOUT) {
						throw serverFault;
					}
				}
			});
		}

	}

}
