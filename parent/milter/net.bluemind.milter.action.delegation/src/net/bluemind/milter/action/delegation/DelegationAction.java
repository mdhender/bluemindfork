/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.milter.action.delegation;

import java.util.List;
import java.util.Map;

import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.domain.api.Domain;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailflow.rbe.IClientContext;
import net.bluemind.milter.IMilterListener;
import net.bluemind.milter.action.DomainAliasCache;
import net.bluemind.milter.action.MilterAction;
import net.bluemind.milter.action.MilterActionException;
import net.bluemind.milter.action.MilterActionsFactory;
import net.bluemind.milter.action.UpdatedMailMessage;

public class DelegationAction implements MilterAction {
	private static final Logger logger = LoggerFactory.getLogger(DelegationAction.class);

	public static class DelegationActionFactory implements MilterActionsFactory {

		@Override
		public MilterAction create() {
			return new DelegationAction();
		}
	}

	@Override
	public String identifier() {
		return "milter.delegation";
	}

	@Override
	public String description() {
		return "Milter delegation action";
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

		if (!modifier.getMessage().getFrom().isEmpty()) {
			Mailbox from = modifier.getMessage().getFrom().get(0);
			if (modifier.envelopSender.isPresent() && !from.getAddress().equals(modifier.envelopSender.get())) {
				checkSameFromAndSenderDomains(modifier.getMessage(), domainItem);

				String fromAddress = from.getLocalPart();
				String sendAddress = modifier.envelopSender.get().split("@")[0];
				if (!fromAddress.equalsIgnoreCase(sendAddress)) {
					List<AccessControlEntry> acls = getAcls(fromAddress, sendAddress, domainItem);
					if (acls.isEmpty()) {
						modifier.errorStatus = IMilterListener.Status.DELEGATION_ACL_FAIL;
					} else if (acls.stream().anyMatch(a -> Verb.SendAs == a.verb)) {
						modifier.addHeader("X-BM-Sender", modifier.envelopSender.get(), identifier());
					}
				}
			}

		}
	}

	private List<AccessControlEntry> getAcls(String fromAddress, String senderAddress, ItemValue<Domain> domainItem) {
		String fromEmailAddress = fromAddress + "@" + domainItem.value.defaultAlias;
		String senderEmailAddress = senderAddress + "@" + domainItem.value.defaultAlias;

		IMailboxes mailboxService = DomainAliasCache.provider().instance(IMailboxes.class, domainItem.uid);
		String fromMbUid = mailboxService.byEmail(fromEmailAddress).uid;
		String senderMbUid = mailboxService.byEmail(senderEmailAddress).uid;

		return mailboxService.getMailboxAccessControlList(fromMbUid).stream()
				.filter(a -> a.subject.equals(senderMbUid) && (Verb.SendOnBehalf == a.verb || Verb.SendAs == a.verb))
				.toList();
	}

	private void checkSameFromAndSenderDomains(Message message, ItemValue<Domain> domainItem) {
		if (message.getFrom().size() != 1) {
			return;
		}
		Mailbox fromMb = message.getFrom().get(0);
		if (!domainItem.value.aliases.contains(fromMb.getDomain())) {
			throw new MilterActionException(
					String.format("Domains do not match between from address '%s' and sender address '%s'",
							fromMb.getAddress(), message.getSender().getAddress()));
		}
	}

}
