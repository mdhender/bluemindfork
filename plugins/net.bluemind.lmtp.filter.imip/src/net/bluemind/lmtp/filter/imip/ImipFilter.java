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
package net.bluemind.lmtp.filter.imip;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.james.mime4j.codec.DecodeMonitor;
import org.apache.james.mime4j.dom.Header;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.address.MailboxList;
import org.apache.james.mime4j.dom.field.UnstructuredField;
import org.apache.james.mime4j.field.UnstructuredFieldImpl;
import org.apache.james.mime4j.stream.Field;
import org.apache.james.mime4j.stream.RawField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.imip.parser.IIMIPParser;
import net.bluemind.imip.parser.IMIPInfos;
import net.bluemind.imip.parser.IMIPParserFactory;
import net.bluemind.imip.parser.PureICSRewriter;
import net.bluemind.lmtp.backend.FilterException;
import net.bluemind.lmtp.backend.IMessageFilter;
import net.bluemind.lmtp.backend.LmtpAddress;
import net.bluemind.lmtp.backend.LmtpEnvelope;
import net.bluemind.lmtp.backend.PermissionDeniedException;
import net.bluemind.lmtp.backend.PermissionDeniedException.CounterNotAllowedException;
import net.bluemind.lmtp.backend.PermissionDeniedException.MailboxInvitationDeniedException;
import net.bluemind.lmtp.filter.imip.cache.MailboxCache;
import net.bluemind.mailbox.api.Mailbox;

public class ImipFilter extends AbstractLmtpHandler implements IMessageFilter {
	private static final Logger logger = LoggerFactory.getLogger(ImipFilter.class);

	public ImipFilter() {
		super(null, null);
	}

	@Override
	public Message filter(LmtpEnvelope env, Message m, long messageSize) throws FilterException {

		Header header = m.getHeader();
		if (header != null) {
			Field spamFlag = header.getField("X-Spam-Flag");
			if (spamFlag != null) {
				if ("YES".equals(spamFlag.getBody())) {
					logger.info("Not attempting IMIP processing on Spam message");
					return null;
				}
			}
		}

		IIMIPParser parser = IMIPParserFactory.create();

		Message message = new PureICSRewriter().rewrite(m);

		IMIPInfos infos = parser.parse(message);

		if (infos != null) {
			try {
				return filter(env, message, infos);
			} finally {
				infos.release();
			}
		} else {
			return null;
		}

	}

	private Message filter(LmtpEnvelope env, Message message, IMIPInfos infos)
			throws FilterException, PermissionDeniedException {
		LmtpAddress sender = null;
		MailboxList fromHeader = message.getFrom();
		org.apache.james.mime4j.dom.address.Mailbox senderHeader = message.getSender();

		// see #3766, clever mail system override lmtp mail from with a
		// custom bounce address. So we prioritize From: and Sender: over
		// lmtp sender
		if (fromHeader != null && !fromHeader.isEmpty()) {
			String fromMail = fromHeader.iterator().next().getAddress();
			sender = new LmtpAddress("<" + fromMail.toLowerCase() + ">", null, null);
		} else if (senderHeader != null) {
			sender = new LmtpAddress("<" + senderHeader.getAddress().toLowerCase() + ">", null, null);
		} else if (env.hasSender()) {
			sender = env.getSender();
			logger.info("sender is: {}", sender);
		} else if (infos.organizerEmail != null) {
			String em = "<" + infos.organizerEmail.toLowerCase() + ">";
			logger.warn("Missing sender in envelope, using organizer email");
			sender = new LmtpAddress(em, null, null);
		} else {
			logger.error("No sender or organizer email, don't know how to process");
			return message;
		}

		// BM-7151 fix null organizer email
		if (infos.organizerEmail == null) {
			infos.organizerEmail = sender.getEmailAddress();
		}

		List<LmtpAddress> recipients = env.getRecipients();
		HeaderList headers = new HeaderList();
		if (sender != null && recipients != null) {
			List<String> deniedRecipients = new ArrayList<>(recipients.size());
			for (LmtpAddress recipient : recipients) {
				try {
					IMIPResponse resp = handleIMIPMessage(sender, recipient, infos.copy());
					headers.addAll(resp.headerFields);
				} catch (MailboxInvitationDeniedException e) {
					deniedRecipients.add(e.mboxUid);
				} catch (CounterNotAllowedException e) {
					deniedRecipients.add(e.targetMailbox);
				} catch (ServerFault e) {
					logger.error("[{}] Error while handling imip message: {}", infos.messageId, e.getCode(), e);
					throw new FilterException();
				}
			}
			// Mail modifications must be discarded only if all recipients
			// are denied.
			if (deniedRecipients.size() == recipients.size()) {
				throw new PermissionDeniedException(deniedRecipients);
			} else if (!deniedRecipients.isEmpty()) {
				RawField rf = new RawField("X-BM-Discard", deniedRecipients.stream().collect(Collectors.joining(",")));
				UnstructuredField discard = UnstructuredFieldImpl.PARSER.parse(rf, DecodeMonitor.SILENT);
				headers.add(discard);
			}
		}

		headers.stream().forEach(field -> {
			message.getHeader().addField(field);
		});

		return message;
	}

	/**
	 * @param sender
	 * @param recipient
	 * @param imip
	 * @return
	 * @throws ServerFault
	 */
	private IMIPResponse handleIMIPMessage(LmtpAddress sender, final LmtpAddress recipient, final IMIPInfos imip)
			throws ServerFault, MailboxInvitationDeniedException, CounterNotAllowedException {
		logger.info("[" + imip.messageId + "] IMIP message from: " + sender + " to " + recipient + ". Method: "
				+ imip.method + ". Organizer: " + imip.organizerEmail);

		ItemValue<Domain> domain = provider().instance(IDomains.class).findByNameOrAliases(recipient.getDomainPart());
		if (domain == null) {
			throw new ServerFault("domain not found " + recipient.getDomainPart(), ErrorCode.NOT_FOUND);
		}
		final ItemValue<Mailbox> recipientMailbox = getRecipientMailbox(recipient, domain);

		try {
			final IIMIPHandler handler = IMIPHandlerFactory.get(imip, recipient, sender);
			return handler.handle(imip, recipient, domain, recipientMailbox);
		} catch (ServerFault sf) {
			if (sf.getCode() == ErrorCode.PERMISSION_DENIED) {
				throw (MailboxInvitationDeniedException) sf.getCause();
			}
			if (sf.getCode() == ErrorCode.EVENT_ACCEPTS_NO_COUNTERS) {
				throw (CounterNotAllowedException) sf.getCause();
			}
			throw sf;
		} catch (Exception e) {
			logger.error("error during handling msg", e);
			throw new ServerFault(e.getMessage(), ErrorCode.UNKNOWN);
		}
	}

	private ItemValue<Mailbox> getRecipientMailbox(LmtpAddress recipient, ItemValue<Domain> domain)
			throws ServerFault, MailboxInvitationDeniedException {
		String box = null;
		if (recipient.getEmailAddress().startsWith("+")) {
			box = recipient.getNormalizedLocalPart().substring(1, recipient.getNormalizedLocalPart().length());
		} else {
			box = recipient.getNormalizedLocalPart();
		}

		Optional<ItemValue<Mailbox>> mailbox = MailboxCache.get(provider(), domain.uid, box);

		if (!mailbox.isPresent()) {
			throw new ServerFault("Unable to find mailbox entry for name: " + recipient.getNormalizedLocalPart());
		}

		if (mailbox.get().value.type != Mailbox.Type.user && mailbox.get().value.type != Mailbox.Type.resource) {
			logger.warn("Unsuported entry kind: {} for email {}", mailbox.get().value.type.toString(),
					recipient.getEmailAddress());
			throw new MailboxInvitationDeniedException(mailbox.get().uid);
		}

		return mailbox.get();
	}

	private static class HeaderList {
		private List<String> exclusiveKeys = Arrays.asList(new String[] { "X-BM-EVENT" });
		private List<Field> headers = new ArrayList<>();

		public void add(Field f) {
			if (exclusiveKeys.contains(f.getName().toUpperCase()) && isSet(f.getName(), f.getBody())) {
				return;
			}
			headers.add(f);
		}

		public void addAll(List<Field> headerFields) {
			headerFields.forEach(this::add);
		}

		private boolean isSet(String name, String body) {
			for (Field f : headers) {
				if (f.getName().equalsIgnoreCase(name) && f.getBody().equals(body)) {
					return true;
				}
			}
			return false;
		}

		public Stream<Field> stream() {
			return headers.stream();
		}

	}
}
