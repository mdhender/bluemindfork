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
package net.bluemind.signature.commons.action;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import io.vertx.core.json.Json;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.mailflow.api.IMailflowRules;
import net.bluemind.mailflow.api.MailRuleActionAssignment;
import net.bluemind.mailflow.common.api.Message;
import net.bluemind.mailflow.common.api.Recipient.RecipientType;
import net.bluemind.mailmessage.api.IMailTipEvaluation;
import net.bluemind.mailmessage.api.MessageContext;

public class SignatureMailTipEvaluation implements IMailTipEvaluation {

	@Override
	public List<EvaluationResult> evaluate(String domain, MessageContext messageContext) {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IMailflowRules.class, domain)
				.evaluate(toMessage(messageContext))
				.stream()
				.filter(match -> match.actionIdentifier.equals("AddSignatureAction"))
				.map(rule -> asResult(rule, domain, messageContext))
				.collect(Collectors.toList());
	}

	private EvaluationResult asResult(MailRuleActionAssignment assignement, String domain, MessageContext messageContext) {
		String sender = messageContext.fromIdentity.from;
		DisclaimerVariables variables = new DisclaimerVariables(() -> getVCard(sender, domain));
		Map<String, String> configuration = assignement.actionConfiguration;
		String html = variables.replace(configuration.get("html"), VariableDecorators.newLineToBr());
		String text = variables.replace(configuration.get("plain"));
		Boolean isDisclaimer = Boolean.valueOf(configuration.get("isDisclaimer"));
		Boolean usePlaceholder = Boolean.valueOf(configuration.get("usePlaceholder"));
		String json = Json.encode(new SignatureMailTip(html, text, assignement.uid, isDisclaimer, usePlaceholder));
		return EvaluationResult.matchesForMessage(json);
	}

	private Message toMessage(MessageContext messageContext) {
		Message message = new Message();
		message.subject = messageContext.subject;
		message.sendingAs = messageContext.fromIdentity;
		message.to = messageContext.recipients.stream().filter(r -> r.recipientType == RecipientType.TO)
				.map(r -> r.email).collect(Collectors.toList());
		message.cc = messageContext.recipients.stream().filter(r -> r.recipientType == RecipientType.CC)
				.map(r -> r.email).collect(Collectors.toList());
		message.recipients = messageContext.recipients.stream().map(r -> r.email).collect(Collectors.toList());

		return message;
	}

	private static Optional<VCard> getVCard(String sender, String domain) {
		VCard vcard = null;
		IDirectory directory = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDirectory.class,
				domain);
		DirEntry entry = directory.getByEmail(sender);
		if (entry != null) {
			vcard = directory.getVCard(entry.entryUid).value;
		}
		return Optional.ofNullable(vcard);
	}

	@Override
	public String mailtipType() {
		return "Signature";
	}

}
