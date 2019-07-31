/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.milter.action.signature;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.james.mime4j.dom.Body;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.TextBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.mailflow.rbe.IClientContext;
import net.bluemind.milter.action.DomainAliasCache;
import net.bluemind.milter.action.MilterAction;
import net.bluemind.milter.action.UpdatedMailMessage;
import net.bluemind.mime4j.common.AddressableEntity;
import net.bluemind.mime4j.common.Mime4JHelper;
import net.bluemind.signature.commons.action.AddDisclaimer;

public class AddSignatureAction implements MilterAction {

	private static final Logger logger = LoggerFactory.getLogger(AddSignatureAction.class);

	public static final String identifier = "AddSignatureAction";

	@Override
	public String identifier() {
		return identifier;
	}

	@Override
	public void execute(UpdatedMailMessage modifier, Map<String, String> configuration,
			Map<String, String> evaluationData, IClientContext context) {

		if (containsSignedOrEncryptedParts(modifier)) {
			logger.debug("Skipping email. Content is signed or encrypted");
			return;
		}

		String sender = modifier.getMessage().getFrom().get(0).getAddress();
		String domainPart = modifier.getMessage().getFrom().get(0).getDomain();

		ItemValue<Domain> domainItem = DomainAliasCache.getDomain(domainPart);
		if (null == domainItem) {
			logger.warn("Cannot find domain/alias of sender {}", sender);
			return;
		}

		if (addDisclaimer(modifier, context, configuration, sender, domainItem.uid)) {
			modifier.updateBody(modifier.getMessage(), identifier());
			modifier.addHeader("X-BM-Disclaimer", "Yes", identifier());
		}

	}

	protected Optional<VCard> getVCard(IClientContext context, String sender, String domain) {
		return DirectoryCache.getVCard(context, domain, sender);
	}

	public boolean addDisclaimer(UpdatedMailMessage modifier, IClientContext context, Map<String, String> configuration,
			String sender, String domain) {
		AddDisclaimer action = new AddDisclaimer(() -> getVCard(context, sender, domain));

		Message message = modifier.getMessage();

		Body body = message.getBody();

		if (body instanceof Multipart) {
			action.addToMultiPart(message, configuration);
			return true;
		}

		if (body instanceof TextBody) {
			try {
				modifier.removeHeader("Content-Transfer-Encoding");

				if ("text/html".equals(message.getMimeType())) {
					action.addToHtmlPart(message, configuration);
				} else {
					action.addToTextPart(message, configuration);
				}
				return true;
			} catch (Exception e) {
				modifier.removeHeaders.remove("Content-Transfer-Encoding");
				throw e;
			}
		}

		return false;
	}

	public boolean containsSignedOrEncryptedParts(UpdatedMailMessage message) {
		String disposition = message.getMessage().getDispositionType();
		if (disposition != null && disposition.equals("attachment")) {
			String filename = message.getMessage().getFilename();
			if (filename != null && filename.endsWith(".p7m")) {
				return true;
			}
		}
		String mime = message.getMessage().getMimeType();
		if (mime.contains(Mime4JHelper.M_SIGNED) || mime.contains(Mime4JHelper.M_ENCRYPTED)) {
			return true;
		}
		Body body = message.getBody();
		if (body instanceof Multipart) {
			Multipart mp = (Multipart) body;
			List<AddressableEntity> parts = Mime4JHelper.expandParts(mp.getBodyParts());
			for (AddressableEntity ae : parts) {
				mime = ae.getMimeType();
				if (mime.contains(Mime4JHelper.M_SIGNED) || mime.contains(Mime4JHelper.M_ENCRYPTED)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public String description() {
		return "Adds a disclaimer";
	}

}
