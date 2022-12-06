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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.james.mime4j.dom.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.smtp.helper.SimpleMessageListener;

import com.google.common.io.CountingInputStream;
import com.netflix.spectator.api.Counter;
import com.netflix.spectator.api.Registry;

import io.netty.buffer.ByteBuf;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
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
import net.bluemind.delivery.lmtp.common.DeliveryContent;
import net.bluemind.delivery.lmtp.common.FreezableDeliveryContent;
import net.bluemind.delivery.lmtp.common.IDeliveryHook;
import net.bluemind.delivery.lmtp.common.LmtpEnvelope;
import net.bluemind.delivery.lmtp.common.ResolvedBox;
import net.bluemind.delivery.lmtp.dedup.DuplicateDeliveryDb;
import net.bluemind.delivery.lmtp.filters.IMessageFilter;
import net.bluemind.delivery.lmtp.filters.LmtpFilters;
import net.bluemind.delivery.lmtp.filters.PermissionDeniedException;
import net.bluemind.delivery.lmtp.hooks.LmtpHooks;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;
import net.bluemind.mime4j.common.Mime4JHelper;

public class LmtpMessageHandler implements SimpleMessageListener {

	private static final Logger logger = LoggerFactory.getLogger(LmtpMessageHandler.class);
	private final ApiProv prov;
	private final MailboxLookup lookup;
	private final Counter internalCount;
	private final Counter externalCount;
	private final DuplicateDeliveryDb dedup;

	public LmtpMessageHandler(ApiProv prov, DuplicateDeliveryDb dedup) {
		this.prov = prov;
		this.lookup = new MailboxLookup(prov);
		this.dedup = dedup;
		Registry reg = MetricsRegistry.get();
		IdFactory idf = new IdFactory("bm-lmtpd", reg, LmtpMessageHandler.class);
		this.internalCount = reg.counter(idf.name("deliveries", "source", "internal"));
		this.externalCount = reg.counter(idf.name("deliveries", "source", "external"));
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
			logger.warn("Reject from {} to {} ({})", from, recipient, e);
			return false;
		}
	}

	@Override
	public void deliver(String from, String recipient, InputStream data) throws IOException {
		ResolvedBox tgtBox = lookup.lookupEmail(recipient);
		String subtree = IMailReplicaUids.subtreeUid(tgtBox.dom.uid, tgtBox.mbox);

		FreezableDeliveryContent freezedContent = preDelivery(from, tgtBox, subtree, data);
		if (freezedContent.isEmpty()) {
			return;
		}

		boolean delivered = dedup.runIfUnique(freezedContent, () -> doDeliver(freezedContent));
		if (delivered) {
			Optional.ofNullable(lookup.lookupEmail(from)) //
					.ifPresentOrElse(box -> internalCount.increment(), externalCount::increment);
		}
	}

	private FreezableDeliveryContent preDelivery(String from, ResolvedBox tgtBox, String subtree, InputStream data)
			throws IOException {
		IDbReplicatedMailboxes treeApi = prov.system().instance(IDbByContainerReplicatedMailboxes.class, subtree);
		ItemValue<MailboxReplica> rootFolder = treeApi
				.byReplicaName(tgtBox.mbox.value.type.sharedNs ? tgtBox.mbox.value.name : "INBOX");

		MailboxRecord rec = new MailboxRecord();
		rec.conversationId = System.currentTimeMillis();
		rec.flags = new ArrayList<>();
		rec.internalFlags = new ArrayList<>();
		rec.internalDate = new Date();
		rec.lastUpdated = rec.internalDate;

		DeliveryContent content = new DeliveryContent(from, tgtBox, rootFolder, null, rec);
		FreezableDeliveryContent freezableContent = applyFilters(from, content, data);
		return (!freezableContent.isFrozen() || freezableContent.isEmpty()) //
				? applyHooks(freezableContent.content()) //
				: freezableContent;
	}

	private void doDeliver(FreezableDeliveryContent freezableContent) {
		ResolvedBox tgtBox = freezableContent.content().box();
		String subtree = IMailReplicaUids.subtreeUid(tgtBox.dom.uid, tgtBox.mbox);
		ItemValue<MailboxReplica> folder = freezableContent.content().folderItem();
		MailboxRecord mailboxRecord = freezableContent.content().mailboxRecord();
		String guid = freezableContent.serializedMessage().guid();
		long size = freezableContent.serializedMessage().size();
		ByteBuf buffer = freezableContent.serializedMessage().buffer();

		String partition = CyrusPartition.forServerAndDomain(tgtBox.entry.dataLocation, tgtBox.dom.uid).name;
		logger.info("Deliver {} ({}bytes) into {} - {} (partition {})", guid, size, subtree, folder.value, partition);

		IDbMessageBodies bodiesUpload = prov.system().instance(IDbMessageBodies.class, partition);
		Stream stream = VertxStream.stream(Buffer.buffer(buffer));
		bodiesUpload.create(guid, stream);
		logger.debug("Body {} uploaded.", guid);

		IDbReplicatedMailboxes treeApi = prov.system().instance(IDbByContainerReplicatedMailboxes.class, subtree);
		AppendTx appendTx = treeApi.prepareAppend(folder.internalId, 1);
		mailboxRecord.imapUid = appendTx.imapUid;
		mailboxRecord.modSeq = appendTx.modSeq;

		IDbMailboxRecords recs = prov.system().instance(IDbMailboxRecords.class, folder.uid);
		long id = recs.create(mailboxRecord.imapUid + ".", mailboxRecord);
		logger.info("Record with imapUid {} created.", mailboxRecord.imapUid);

		if (!freezableContent.content().deferredActionMessages().isEmpty()) {
			JsonObject msg = new JsonObject();
			msg.put("owner", freezableContent.content().box().mbox.uid);
			msg.put("containerUid", freezableContent.content().folderItem().uid);
			msg.put("messageId", id);
			JsonArray deferredActions = new JsonArray();
			freezableContent.content().deferredActionMessages()
					.forEach(deferredAction -> deferredActions.add(deferredAction.toJson()));
			msg.put("deferredActions", deferredActions);
			VertxPlatform.eventBus().publish(Topic.MAPI_DEFERRED_ACTION_NOTIFICATIONS, msg);
			logger.info("[rules] dam published by lmtp for message id:{} in containerUid:{}", id,
					freezableContent.content().folderItem().uid);
		}

	}

	private FreezableDeliveryContent applyFilters(String from, DeliveryContent content, InputStream data)
			throws IOException {
		List<IMessageFilter> filters = LmtpFilters.get();
		CountingInputStream countedInput = new CountingInputStream(data);
		Message messageToFilter = Mime4JHelper.parse(countedInput);
		try {
			LmtpEnvelope le = new LmtpEnvelope(from, Collections.singletonList(content.box()));
			for (IMessageFilter f : filters) {
				Message updatedMessage = f.filter(le, messageToFilter);
				if (updatedMessage != null && updatedMessage != messageToFilter) {
					messageToFilter.close();
					messageToFilter = updatedMessage;
				}
			}
			return FreezableDeliveryContent.create(content.withMessage(messageToFilter), countedInput.getCount());
		} catch (PermissionDeniedException pde) {
			// this used to set a X-Bm-Discard here & drop from sieve
			// we can just return
			logger.info("Discard because of PDE: {}", pde.getMessage());
			close(messageToFilter);
			return FreezableDeliveryContent.discard(content);
		} catch (Exception e) {
			// we have the original buffer to deliver
			logger.error("Filtering error, keeping the original one", e);
			return FreezableDeliveryContent.freeze(content.withMessage(messageToFilter));
		}
	}

	private FreezableDeliveryContent applyHooks(DeliveryContent content) throws IOException {
		List<IDeliveryHook> hooks = LmtpHooks.get();
		DeliveryContext ctx = new DeliveryContext(prov.system(), lookup);
		for (IDeliveryHook hook : hooks) {
			try {
				content = hook.preDelivery(ctx, content);
			} catch (Exception e) {
				logger.error("[delivery] failed to apply delivery hook {} on {}", //
						hook.getClass().getCanonicalName(), content, e);
			}
		}
		return (content.isEmpty()) //
				? FreezableDeliveryContent.discard(content) //
				: FreezableDeliveryContent.freeze(content);
	}

	private void close(Message message) {
		try {
			message.close();
		} catch (Exception e) {
			// This should not happen
		}
	}

}
