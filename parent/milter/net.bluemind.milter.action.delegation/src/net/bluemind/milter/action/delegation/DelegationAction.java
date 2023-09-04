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
import org.columba.ristretto.message.Address;
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

				String fromLocalPartAddress = from.getLocalPart();
				String senderLocalPartAddress = modifier.envelopSender.get().split("@")[0];
				if (!fromLocalPartAddress.equalsIgnoreCase(senderLocalPartAddress)) {
					String fromEmailAddress = fromLocalPartAddress + "@" + domainItem.value.defaultAlias;
					String senderEmailAddress = senderLocalPartAddress + "@" + domainItem.value.defaultAlias;

					IMailboxes mailboxService = DomainAliasCache.provider().instance(IMailboxes.class, domainItem.uid);
					ItemValue<net.bluemind.mailbox.api.Mailbox> fromMb = mailboxService.byEmail(fromEmailAddress);
					ItemValue<net.bluemind.mailbox.api.Mailbox> senderMb = mailboxService.byEmail(senderEmailAddress);

					List<AccessControlEntry> acls = mailboxService.getMailboxAccessControlList(fromMb.uid).stream()
							.filter(a -> a.subject.equals(senderMb.uid)
									&& (Verb.SendOnBehalf == a.verb || Verb.SendAs == a.verb))
							.toList();
					if (acls.isEmpty()) {
						modifier.errorStatus = IMilterListener.Status.DELEGATION_ACL_FAIL;
					} else if (acls.stream().anyMatch(a -> Verb.SendOnBehalf == a.verb)) {
						modifier.addHeader("Sender", new Address(senderMb.displayName, senderEmailAddress).toString(),
								identifier());
					}
				}
			}

		}
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
