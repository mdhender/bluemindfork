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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.addressbook.service.internal;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import net.bluemind.addressbook.api.IAddressBooks;
import net.bluemind.addressbook.api.VCardQuery;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.mailflow.common.api.Recipient;
import net.bluemind.mailmessage.api.IMailTipEvaluation;
import net.bluemind.mailmessage.api.MessageContext;

public class PublicCertMailTipEvaluation implements IMailTipEvaluation {

	@Override
	public String mailtipType() {
		return "HasPublicKeyCertificate";
	}

	@Override
	public List<EvaluationResult> evaluate(String domainUid, MessageContext messageContext) {
		DirEntry sender = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDirectory.class, domainUid).getByEmail(messageContext.fromIdentity.sender);
		if (sender == null) {
			return Collections.emptyList();
		}
		String senderUid = sender.entryUid;
		try (Sudo sudo = new Sudo(senderUid, domainUid)) {
			var books = ServerSideServiceProvider.getProvider(sudo.context).instance(IAddressBooks.class);
			return messageContext.recipients.stream().map(r -> process(books, r)).filter(Objects::nonNull)
					.collect(Collectors.toList());
		}
	}

	private EvaluationResult process(IAddressBooks books, Recipient r) {
		var cards = books.search(VCardQuery.create("value.communications.emails.value:" + r.email));

		var canReceiveEncryptedMessage = cards.values.stream().anyMatch(card -> card.value.hasSecurityKey);

		return EvaluationResult.matchesForRecipient(r, Boolean.toString(canReceiveEncryptedMessage));
	}

}
