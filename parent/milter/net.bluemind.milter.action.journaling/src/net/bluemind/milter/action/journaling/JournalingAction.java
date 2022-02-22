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
package net.bluemind.milter.action.journaling;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.dom.address.MailboxList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.mailflow.rbe.IClientContext;
import net.bluemind.milter.action.DomainAliasCache;
import net.bluemind.milter.action.MilterAction;
import net.bluemind.milter.action.MilterActionException;
import net.bluemind.milter.action.MilterActionsFactory;
import net.bluemind.milter.action.UpdatedMailMessage;

public class JournalingAction implements MilterAction {

	private static final Logger logger = LoggerFactory.getLogger(JournalingAction.class);

	public static final String identifier = "JournalingAction";
	public static final String EMAILS_SEPARATOR = ";";
	public static final String TARGET_EMAIL_KEY = "targetEmail";
	public static final String EMAILS_FILTERED_KEY = "emailsFiltered";
	public static final String NO_REPLY = "no-reply@";

	public static class JournalingActionFactory implements MilterActionsFactory {
		@Override
		public MilterAction create() {
			return new JournalingAction();
		}
	}

	@Override
	public String identifier() {
		return identifier;
	}

	@Override
	public void execute(UpdatedMailMessage modifier, Map<String, String> configuration,
			Map<String, String> evaluationData, IClientContext context) {
		ItemValue<Domain> contextDomain = context.getSenderDomain();
		String contextAliasDomain = contextDomain.value.defaultAlias;
		ItemValue<Domain> domainItem = DomainAliasCache.getDomain(contextAliasDomain);
		if (domainItem == null) {
			String msg = String.format("Cannot find domain/alias %s", contextDomain);
			logger.warn(msg);
			throw new MilterActionException(msg);
		}

		MailboxList recipients = modifier.getMessage().getTo().flatten();
		Mailbox sender = modifier.getMessage().getFrom().get(0);
		Set<String> newEnvelopSenders = getJournalisationEnvelopSender(configuration.get(EMAILS_FILTERED_KEY), sender,
				recipients, contextAliasDomain);
		if (newEnvelopSenders.isEmpty()) {
			String msg = String.format("this email not forwarded for domain %s with sender %s and rcpt %s",
					contextAliasDomain, sender,
					recipients.stream().map(r -> r.getAddress()).collect(Collectors.joining(" | ")));
			logger.warn(msg);
			throw new MilterActionException(msg);
		}

		if (newEnvelopSenders.size() > 1) {
			logger.warn("multiple sender in enveloppe because multiple recipients from email : {}",
					newEnvelopSenders.stream().collect(Collectors.joining("; ")));
		}

		modifier.envelopSender = Optional.of(newEnvelopSenders.iterator().next());
		modifier.addRcpt.add(configuration.get(TARGET_EMAIL_KEY));
		modifier.addHeader("X-BM-Journaling-Orig-From", sender.getAddress(), identifier());
		modifier.addHeader("X-BM-Journaling-Orig-To",
				recipients.stream().map(r -> r.getAddress()).collect(Collectors.joining(",")), identifier());

		logger.debug("journalisation data on execute : sender = {}, rcpt = {}", modifier.envelopSender.get(),
				modifier.addRcpt.stream().collect(Collectors.joining(" | ")));
	}

	private Set<String> getJournalisationEnvelopSender(String filteredEmails, Mailbox sender, MailboxList rcptList,
			String domain) {
		Set<String> senderEmails = new HashSet<>();
		if (filteredEmails == null || filteredEmails.isEmpty()) {
			if (senderOrRcptFromDomain(sender, rcptList, domain)) {
				senderEmails.add(NO_REPLY.concat(domain));
			}
		} else {
			List<String> emailsFilteredList = Arrays.asList(filteredEmails.split(EMAILS_SEPARATOR));
			if (emailsFilteredList.contains(sender.getAddress())) {
				senderEmails.add(NO_REPLY.concat(sender.getDomain()));
			} else {
				emailsFilteredList.forEach(e -> rcptList.stream().filter(r -> r.getAddress().equals(e)).findFirst()
						.ifPresent(r -> senderEmails.add(NO_REPLY.concat(r.getDomain()))));
			}
		}

		return senderEmails;
	}

	private boolean senderOrRcptFromDomain(Mailbox sender, MailboxList rcptList, String domain) {
		return sender.getDomain().equals(domain) || rcptList.stream().anyMatch(r -> r.getDomain().equals(domain));
	}

	@Override
	public String description() {
		return "Adds mail journaling";
	}

}
