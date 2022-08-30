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
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.delivery.lmtp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;

import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.field.address.LenientAddressBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.smtp.helper.SimpleMessageListener;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;
import com.google.common.io.ByteStreams;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.vertx.core.buffer.Buffer;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.replica.api.AppendTx;
import net.bluemind.backend.mail.replica.api.IDbByContainerReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IDbMessageBodies;
import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.core.api.Stream;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.delivery.lmtp.LmtpStarter.ApiProv;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.mailbox.api.IMailboxes;

public class LmtpMessageHandler implements SimpleMessageListener {

	private static final Logger logger = LoggerFactory.getLogger(LmtpMessageHandler.class);
	private final ApiProv prov;

	public LmtpMessageHandler(ApiProv prov) {
		this.prov = prov;
	}

	@Override
	public boolean accept(String from, String recipient) {
		try {
			ResolvedBox found = lookupEmail(recipient);
			boolean ret = found != null;
			if (ret) {
				logger.info("Accept from {} to {}", from, found.mbox.value);
			} else {
				logger.warn("Reject from {} to {}", from, recipient);
			}
			return ret;
		} catch (Exception e) {
			logger.warn("Reject from {} to {} ({})", from, recipient, e.getMessage());
			return false;
		}
	}

	public static class ResolvedBox {
		public ResolvedBox(DirEntry entry, ItemValue<net.bluemind.mailbox.api.Mailbox> mbox, ItemValue<Domain> dom) {
			this.entry = entry;
			this.mbox = mbox;
			this.dom = dom;
		}

		DirEntry entry;
		ItemValue<net.bluemind.mailbox.api.Mailbox> mbox;
		ItemValue<Domain> dom;
	}

	@Override
	public void deliver(String from, String recipient, InputStream data) throws IOException {
		Path tmp = Files.createTempFile("lmtp-inc-", ".eml");
		try (@SuppressWarnings("deprecation")
		HashingInputStream hash = new HashingInputStream(Hashing.sha1(), data);
				OutputStream output = Files.newOutputStream(tmp)) {
			long copied = ByteStreams.copy(hash, output);
			ResolvedBox tgtBox = lookupEmail(recipient);
			deliverImpl(tgtBox, tmp, copied, hash.hash());
		} finally {
			Files.delete(tmp);
		}
	}

	private ResolvedBox lookupEmail(String recipient) {
		Mailbox m4jBox = LenientAddressBuilder.DEFAULT.parseMailbox(recipient);
		IDomains domApi = prov.system().instance(IDomains.class);
		ItemValue<Domain> dom = domApi.findByNameOrAliases(m4jBox.getDomain());
		if (dom == null) {
			return null;
		}
		IDirectory dirApi = prov.system().instance(IDirectory.class, m4jBox.getDomain());
		DirEntry entry = dirApi.getByEmail(recipient);
		if (entry == null) {
			return null;
		}
		IMailboxes mboxApi = prov.system().instance(IMailboxes.class, dom.uid);
		logger.info("Lookup {}@{} ({})", entry.entryUid, dom.uid, entry.email);
		ItemValue<net.bluemind.mailbox.api.Mailbox> mailbox = mboxApi.getComplete(entry.entryUid);
		if (mailbox == null) {
			return null;
		}
		return new ResolvedBox(entry, mailbox, dom);
	}

	private void deliverImpl(ResolvedBox tgtBox, Path tmp, long size, HashCode hash) throws IOException {
		String subtree = IMailReplicaUids.subtreeUid(tgtBox.dom.uid, tgtBox.mbox);
		IDbReplicatedMailboxes treeApi = prov.system().instance(IDbByContainerReplicatedMailboxes.class, subtree);
		ItemValue<MailboxReplica> rootFolder = treeApi
				.byReplicaName(tgtBox.mbox.value.type.sharedNs ? tgtBox.mbox.value.name : "INBOX");

		String partition = CyrusPartition.forServerAndDomain(tgtBox.entry.dataLocation, tgtBox.dom.uid).name;
		logger.info("Deliver {} ({}bytes) into {} - {} (partition {})", hash, size, subtree, rootFolder.value,
				partition);
		IDbMessageBodies bodiesUpload = prov.system().instance(IDbMessageBodies.class, partition);
		Stream stream = mmapStream(tmp, size);
		bodiesUpload.create(hash.toString(), stream);
		logger.info("Body {} uploaded.", hash);

		AppendTx appendTx = treeApi.prepareAppend(rootFolder.internalId, 1);
		MailboxRecord rec = new MailboxRecord();
		rec.messageBody = hash.toString();
		rec.imapUid = appendTx.imapUid;
		rec.modSeq = appendTx.modSeq;
		rec.flags = new ArrayList<>();
		rec.internalDate = new Date();
		rec.lastUpdated = rec.internalDate;
		rec.conversationId = rec.internalDate.getTime();
		IDbMailboxRecords recs = prov.system().instance(IDbMailboxRecords.class, rootFolder.uid);
		recs.create(rec.imapUid + ".", rec);
		logger.info("Record with imapUid {} created.", rec.imapUid);

	}

	private Stream mmapStream(Path tmp, long size) throws IOException {
		try (RandomAccessFile raf = new RandomAccessFile(tmp.toFile(), "r")) {
			MappedByteBuffer mmap = raf.getChannel().map(MapMode.READ_ONLY, 0, size);
			ByteBuf nettyMapping = Unpooled.wrappedBuffer(mmap);
			Buffer vxMapping = Buffer.buffer(nettyMapping);
			return VertxStream.stream(vxMapping);
		}
	}

}
