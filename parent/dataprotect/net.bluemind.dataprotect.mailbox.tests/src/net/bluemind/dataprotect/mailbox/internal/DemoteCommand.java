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
package net.bluemind.dataprotect.mailbox.internal;

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

import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.stream.RawField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import freemarker.template.TemplateException;
import net.bluemind.backend.mail.replica.indexing.IMailIndexService;
import net.bluemind.backend.mail.replica.indexing.RecordIndexActivator;
import net.bluemind.hsm.api.TierChangeResult;
import net.bluemind.hsm.processor.Composer;
import net.bluemind.hsm.processor.HSMContext;
import net.bluemind.hsm.processor.HSMHeaders;
import net.bluemind.hsm.processor.HSMRunStats;
import net.bluemind.hsm.processor.commands.AbstractHSMCommand;
import net.bluemind.imap.Flag;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.IMAPByteSource;
import net.bluemind.imap.IMAPHeaders;
import net.bluemind.imap.InternalDate;
import net.bluemind.imap.StoreClient;
import net.bluemind.imap.Summary;
import net.bluemind.mime4j.common.Mime4JHelper;

public class DemoteCommand extends AbstractHSMCommand {

	private static final Logger logger = LoggerFactory.getLogger(DemoteCommand.class);

	private List<InternalDate> ids;
	private Composer composer;
	private String lang;
	private Optional<Integer> quota;
	private double quotaUsed;
	private HSMContext context;
	private List<Integer> mailUids;

	public DemoteCommand(String folderPath, StoreClient storeClient, HSMContext context, List<InternalDate> ids,
			Optional<Integer> userQuota) {
		super(folderPath, storeClient, context.getHSMStorage());
		this.composer = new Composer();
		this.lang = context.getLang();
		this.quota = userQuota;
		Optional<IMailIndexService> indexer = RecordIndexActivator.getIndexer();
		if (indexer.isPresent()) {
			this.quotaUsed = indexer.get().getArchivedMailSum(context.getLoginContext().uid);
		}

		this.context = context;
		this.ids = ids;
		mailUids = new ArrayList<Integer>(ids.size());

	}

	public List<TierChangeResult> run(HSMRunStats stats) throws IOException {

		List<TierChangeResult> ret = new ArrayList<TierChangeResult>(ids.size());

		Iterator<InternalDate> it = ids.iterator();
		while (it.hasNext()) {
			InternalDate id = it.next();
			ret.add(demote(stats, id));
		}

		return ret;
	}

	private TierChangeResult demote(HSMRunStats stats, InternalDate id) throws IOException {
		Collection<Summary> summaries = sc.uidFetchSummary("" + id.getUid());

		if (summaries.size() != 1) {
			throw new IOException("Failed to fetch summary on " + id.getUid());
		}
		Summary summary = summaries.iterator().next();

		IMAPHeaders ih = summary.getHeaders();
		// check to avoid archiving the archive inception
		String hsmId = ih.getRawHeader(HSMHeaders.HSM_ID);
		if (hsmId != null && !hsmId.isEmpty()) {

			if (logger.isDebugEnabled()) {
				logger.debug("IMAP ID {} is already archived with HSM ID {}", id.getUid(), hsmId);
			}

			return TierChangeResult.create(id.getUid(), hsmId);
		}

		final AtomicBoolean quotaOk = new AtomicBoolean(true);
		quota.ifPresent((userQuota) -> {
			long msgSize = Long.valueOf(summary.getSize());
			double calculatedTargetSize = quotaUsed + msgSize;
			long userQuotaInBytes = userQuota * 1024l * 1024l;
			if (calculatedTargetSize >= userQuotaInBytes) {
				logger.info(
						"HSM quota of uid {} exceeded: currentQuotaUsed : {}, msgSize: {}, calculatedTargetSize: {}, userQuotaInBytes: {}",
						context.getLoginContext().uid, quotaUsed, msgSize, calculatedTargetSize, userQuotaInBytes);
				quotaOk.set(false);
			}
			quotaUsed = calculatedTargetSize;
		});

		if (!quotaOk.get()) {
			throw new IOException("HSM quota exceeded");
		}

		logger.info("Demote {} to archive store {}", id.getUid(), storage);

		// the message is not archived yet, do the magic
		IMAPByteSource mailContent = sc.uidFetchMessage(id.getUid());
		try {
			hsmId = storage.store(context.getSecurityContext().getContainerUid(), context.getLoginContext().uid,
					mailContent.source().openStream());
		} catch (Exception e) {
			logger.error("Can't demote message" + id.getUid(), e);
			throw new IOException("Fail to demote");
		} finally {
			mailContent.close();
		}

		try (Message msg = Mime4JHelper.parse(mailContent.source().openStream())) {
			// Replace old mail
			FlagsList fl = summary.getFlags();
			fl.add(Flag.BMARCHIVED);
			String dateTime = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("EE, dd MMM YYYY HH:mm:ss"));
			int newUid = replace(msg, fl, hsmId, dateTime, id);
			if (newUid < 0) {
				throw new IOException("Failed to demote");
			}
			msg.getHeader().setField(new RawField(HSMHeaders.HSM_ID, hsmId));
			msg.getHeader().setField(new RawField(HSMHeaders.HSM_DATETIME, dateTime));

			// FIXME INDEX ME
			// ByteArrayOutputStream out = new ByteArrayOutputStream();
			// Mime4JHelper.serialize(msg, out);
			// try {
			// IndexedMessageBody indexData = IndexedMessageBody
			// .createIndexBody(<GUID>VertxStream.stream(new
			// Buffer(out.toByteArray())));
			// MailIndexActivator.getService().storeBody(indexData);
			// } catch (Exception e) {
			// logger.warn("Cannot index demoted mail", e);
			// }

			// Delete the old mail
			fl = summary.getFlags();
			fl.add(Flag.DELETED);
			ArrayList<Integer> oldList = Lists.newArrayList(id.getUid());
			sc.uidStore(oldList, fl, true);
			sc.uidExpunge(oldList);

			stats.mailMoved();
			mailUids.add(id.getUid());

			logger.info("Demoted: old uid {}, new uid {}, hsm id {}", id.getUid(), newUid, hsmId);

			return TierChangeResult.create(newUid, hsmId);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new IOException(e);
		}
	}

	private int replace(Message msg, FlagsList fl, String hsmId, String dateTime, InternalDate id)
			throws IOException, TemplateException {
		InputStream stream = composer.render(msg, lang, hsmId, dateTime);
		int delivered = sc.append(folderPath, stream, fl, id);
		return delivered;
	}
}