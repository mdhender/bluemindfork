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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.james.mime4j.dom.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.smtp.helper.SimpleMessageListener;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;
import com.google.common.io.ByteStreams;
import com.netflix.spectator.api.Counter;
import com.netflix.spectator.api.Registry;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.vertx.core.buffer.Buffer;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.api.MessageBody;
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
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.delivery.lmtp.common.IDeliveryContext;
import net.bluemind.delivery.lmtp.common.IDeliveryHook;
import net.bluemind.delivery.lmtp.common.LmtpEnvelope;
import net.bluemind.delivery.lmtp.common.ResolvedBox;
import net.bluemind.delivery.lmtp.filters.IMessageFilter;
import net.bluemind.delivery.lmtp.filters.LmtpFilters;
import net.bluemind.delivery.lmtp.filters.PermissionDeniedException;
import net.bluemind.delivery.lmtp.hooks.LmtpHooks;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;
import net.bluemind.mime4j.common.Mime4JHelper;

public class LmtpMessageHandler implements SimpleMessageListener {

	private static final Logger logger = LoggerFactory.getLogger(LmtpMessageHandler.class);
	private final ApiProv prov;
	private final MailboxLookup lookup;
	private Counter internalCount;
	private Counter externalCount;

	public LmtpMessageHandler(ApiProv prov) {
		this.prov = prov;
		this.lookup = new MailboxLookup(prov);
		Registry reg = MetricsRegistry.get();
		IdFactory idf = new IdFactory("bm-lmtpd", reg, LmtpMessageHandler.class);
		internalCount = reg.counter(idf.name("deliveries", "source", "internal"));
		externalCount = reg.counter(idf.name("deliveries", "source", "external"));
	}

	@Override
	public boolean accept(String from, String recipient) {
		try {
			ResolvedBox found = lookup.lookupEmail(recipient);
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

	@Override
	public void deliver(String from, String recipient, InputStream data) throws IOException {
		Path tmp = Files.createTempFile("lmtp-inc-", ".eml");
		try (@SuppressWarnings("deprecation")
		HashingInputStream hash = new HashingInputStream(Hashing.sha1(), data);
				OutputStream output = Files.newOutputStream(tmp)) {
			long copied = ByteStreams.copy(hash, output);
			ResolvedBox tgtBox = lookup.lookupEmail(recipient);
			Optional<ResolvedBox> fromBox = Optional.ofNullable(lookup.lookupEmail(from));

			deliverImpl(from, tgtBox, tmp, copied, hash.hash());
			if (fromBox.isPresent()) {
				internalCount.increment();
			} else {
				externalCount.increment();
			}
		} finally {
			Files.delete(tmp);
		}
	}

	private void deliverImpl(String from, ResolvedBox tgtBox, Path tmp, long size, HashCode hash) throws IOException {
		String subtree = IMailReplicaUids.subtreeUid(tgtBox.dom.uid, tgtBox.mbox);
		IDbReplicatedMailboxes treeApi = prov.system().instance(IDbByContainerReplicatedMailboxes.class, subtree);

		ItemValue<MailboxReplica> rootFolder = treeApi
				.byReplicaName(tgtBox.mbox.value.type.sharedNs ? tgtBox.mbox.value.name : "INBOX");

		String partition = CyrusPartition.forServerAndDomain(tgtBox.entry.dataLocation, tgtBox.dom.uid).name;
		logger.info("Deliver {} ({}bytes) into {} - {} (partition {})", hash, size, subtree, rootFolder.value,
				partition);

		ByteBuf mmap = applyFilters(from, tgtBox, tmp, size);
		if (mmap == null) {
			return;
		}

		IDbMessageBodies bodiesUpload = prov.system().instance(IDbMessageBodies.class, partition);
		Stream stream = VertxStream.stream(Buffer.buffer(mmap));
		bodiesUpload.create(hash.toString(), stream);
		logger.debug("Body {} uploaded.", hash);

		MessageBody uploadedBody = bodiesUpload.getComplete(hash.toString());

		AppendTx appendTx = treeApi.prepareAppend(rootFolder.internalId, 1);
		MailboxRecord rec = new MailboxRecord();
		rec.conversationId = System.currentTimeMillis();

		rec.messageBody = hash.toString();
		rec.imapUid = appendTx.imapUid;
		rec.modSeq = appendTx.modSeq;
		rec.flags = new ArrayList<>();
		rec.internalDate = new Date();
		rec.lastUpdated = rec.internalDate;
		IDbMailboxRecords recs = prov.system().instance(IDbMailboxRecords.class, rootFolder.uid);
		applyHooks(tgtBox, rec, uploadedBody);
		recs.create(rec.imapUid + ".", rec);
		logger.info("Record with imapUid {} created.", rec.imapUid);

	}

	private MailboxRecord applyHooks(ResolvedBox tgtBox, MailboxRecord rec, MessageBody messageBody) {
		List<IDeliveryHook> hooks = LmtpHooks.get();

		IDeliveryContext delCtx = new IDeliveryContext() {

			@Override
			public IServiceProvider provider() {
				return prov.system();
			}

		};

		for (IDeliveryHook hook : hooks) {
			try {
				hook.preDelivery(delCtx, tgtBox, rec, messageBody);
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
		}

		return rec;
	}

	private ByteBuf applyFilters(String from, ResolvedBox tgtBox, Path tmp, long size) throws IOException {
		List<IMessageFilter> filters = LmtpFilters.get();
		ByteBuf mmap = mmapBuffer(tmp, size);
		if (logger.isDebugEnabled()) {
			logger.debug("Start filtering of {} with {} filter(s)", mmap, filters.size());
		}
		try (Message msg = Mime4JHelper.parse(new ByteBufInputStream(mmap.duplicate()))) {
			Message cur = msg;
			boolean modified = false;
			LmtpEnvelope le = new LmtpEnvelope(from, Collections.singletonList(tgtBox));
			for (IMessageFilter f : filters) {
				Message fresh = f.filter(le, cur, size);
				if (fresh != null) {
					modified = true;
					logger.info("Marking message {} as modified by {}", msg, f);
					if (fresh != cur) {
						logger.debug("Dispose {}, swap to {}", cur, fresh);
						cur.dispose();
						cur = fresh;
					}
				}
			}
			if (modified) {
				ByteBuf freshBuf = Unpooled.buffer();
				try (ByteBufOutputStream out = new ByteBufOutputStream(freshBuf)) {
					Mime4JHelper.serialize(cur, out);
				}
				mmap = freshBuf;
			}
		} catch (PermissionDeniedException pde) {
			// this used to set a X-Bm-Discard here & drop from sieve
			// we can just return
			logger.info("Discard because of PDE: {}", pde.getMessage());
			return null;
		} catch (Exception e) {
			// we have the original buffer to deliver
			logger.error("Filtering error, keeping the original one", e);
		}
		logger.debug("Return {} for delivery", mmap);
		return mmap;
	}

	private ByteBuf mmapBuffer(Path tmp, long size) throws IOException {
		try (RandomAccessFile raf = new RandomAccessFile(tmp.toFile(), "r")) {
			MappedByteBuffer mmap = raf.getChannel().map(MapMode.READ_ONLY, 0, size);
			return Unpooled.wrappedBuffer(mmap);
		}
	}

}
