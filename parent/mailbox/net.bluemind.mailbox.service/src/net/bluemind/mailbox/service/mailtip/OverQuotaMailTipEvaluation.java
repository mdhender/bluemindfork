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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.mailbox.service.mailtip;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.MailboxQuota;
import net.bluemind.mailflow.common.api.Recipient;
import net.bluemind.mailmessage.api.IMailTipEvaluation;
import net.bluemind.mailmessage.api.MessageContext;

public class OverQuotaMailTipEvaluation implements IMailTipEvaluation {

	private static final Logger logger = LoggerFactory.getLogger(OverQuotaMailTipEvaluation.class);

	@Override
	public List<EvaluationResult> evaluate(String domain, MessageContext messageContext) {
		return messageContext.recipients.stream().map(r -> process(domain, r))
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	private EvaluationResult process(String domain, Recipient r) {
		Boolean full = isMailboxFull(domain, r.email);
		if (full == null) {
			return null;
		}
		return EvaluationResult.matchesForRecipient(r, full.toString());
	}

	private Boolean isMailboxFull(String domain, String email) {
		IMailboxes mailboxes = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IMailboxes.class,
				domain);
		IDirectory directory = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDirectory.class,
				domain);
		DirEntry entry = directory.getByEmail(email);
		if (entry == null) {
			logger.info("DirEntry with email {} not found", email);
			return null;
		}
		ItemValue<Mailbox> mailbox = mailboxes.getComplete(entry.entryUid);
		if (mailbox == null) {
			logger.info("Mailbox not found for {}", email);
			return null;
		}
		MailboxQuota quota = mailboxes.getMailboxQuota(mailbox.uid);
		if (quota.quota == null) {
			logger.info("Mailbox {} has no quota defined", mailbox.value.name);
			return null;
		}
		return quota.used >= quota.quota;
	}

	@Override
	public String mailtipType() {
		return "Overquota";
	}

}
