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

package net.bluemind.backend.mail.replica.service.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import net.bluemind.backend.mail.api.flags.MailboxItemFlag;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.indexing.IMailIndexService;
import net.bluemind.core.auditlogs.AuditLogUpdateStatus;
import net.bluemind.core.auditlogs.AuditLogUpdateStatus.MessageCriticity;
import net.bluemind.core.auditlogs.ContentElement;
import net.bluemind.core.auditlogs.ContentElement.ContentElementBuilder;
import net.bluemind.core.auditlogs.ILogMapperProvider;
import net.bluemind.core.container.model.BaseContainerDescriptor;

public class DbMailboxRecordsAuditLogMapper implements ILogMapperProvider<MailboxRecord> {
	private final IMailIndexService mailIndexService;
	private final BaseContainerDescriptor container;
	private static final String VOICEMAIL = "voicemail";
	private static final String INVITATION = "invitation";
	private static final String ATTACHMENTS = "attachments";

	private static final AuditLogUpdateStatus SEEN_UPD = new AuditLogUpdateStatus("SeenChanged",
			MessageCriticity.MINOR);

	public DbMailboxRecordsAuditLogMapper(BaseContainerDescriptor bcd, IMailIndexService mis) {
		mailIndexService = mis;
		container = bcd;
	}

	@Override
	public ContentElement createContentElement(MailboxRecord itemValue) {
		String mailboxUniqueId = container.uid.replace("mbox_records_", "");
		if (itemValue == null) {
			return null;
		}
		Map<String, Object> messageBodyMap = mailIndexService.fetchBody(mailboxUniqueId, itemValue);
		if (messageBodyMap != null) {
			return filterMessageBody(messageBodyMap, itemValue);
		}
		return null;
	}

	@Override
	public AuditLogUpdateStatus createUpdateMessage(MailboxRecord oldValue, MailboxRecord newValue) {
		List<MailboxItemFlag> oldFlags = oldValue.flags;
		List<MailboxItemFlag> newFlags = newValue.flags;
		List<MailboxItemFlag> removedFlags = oldFlags.stream().filter(element -> !newFlags.contains(element)).toList();
		List<MailboxItemFlag> addedFlags = newFlags.stream().filter(element -> !oldFlags.contains(element)).toList();
		if (isMinorDifference(removedFlags, addedFlags)) {
			return SEEN_UPD;
		}

		StringBuilder stringBuilder = new StringBuilder();
		if (!removedFlags.isEmpty()) {
			stringBuilder.append("Removed Flags:\n")
					.append(removedFlags.stream().map(MailboxItemFlag::toString).collect(Collectors.joining(",")))
					.append("\n");
		}
		if (!addedFlags.isEmpty()) {
			stringBuilder.append("Added Flags:\n")
					.append(addedFlags.stream().map(MailboxItemFlag::toString).collect(Collectors.joining(",")))
					.append("\n");
		}
		return new AuditLogUpdateStatus(stringBuilder.toString());
	}

	private ContentElement filterMessageBody(Map<String, Object> body, MailboxRecord newValue) {
		ContentElementBuilder builder = new ContentElement.ContentElementBuilder();
		List<String> recipientsList = new ArrayList<>();
		for (Entry<String, Object> entry : body.entrySet()) {
			String key = entry.getKey();
			if (body.get(key) != null) {
				switch (key) {
				case "subject":
					builder.description(body.get(key).toString());
					break;
				case "messageId":
					builder.key(body.get(key).toString());
					break;
				case "from":
					List<String> fromList = createList(body.get(key).toString());
					if (!fromList.isEmpty()) {
						builder.author(fromList);
						recipientsList.addAll(fromList);
					}
					break;
				case "to", "cc":
					List<String> toList = createList(body.get(key).toString());
					if (!toList.isEmpty()) {
						recipientsList.addAll(toList);
					}
					break;
				default:
					break;
				}
			}
		}
		if (!recipientsList.isEmpty()) {
			builder.with(recipientsList);
		}

		if (body.containsKey("has")) {
			builder.has = new ArrayList<>();
			String hasString = body.get("has").toString();
			if (hasString.startsWith("[")) {
				List<String> hasList = Arrays.asList(hasString.replace("[", "").replace("]", "").split(", "));
				if (hasList.contains(VOICEMAIL)) {
					builder.has.add(VOICEMAIL);
				}
				if (hasList.contains(INVITATION)) {
					builder.has.add(INVITATION);
				}
				if (hasList.contains(ATTACHMENTS)) {
					builder.has.add(ATTACHMENTS);
				}
			}
		}

		return builder.build();
	}

	private List<String> createList(String recipients) {
		if (recipients.startsWith("[")) {
			recipients = recipients.substring(1);
		}
		if (recipients.endsWith("]")) {
			recipients = recipients.substring(0, recipients.length() - 1);
		}
		if (recipients.isBlank()) {
			return Collections.emptyList();
		}
		String[] toArray = recipients.split(",");
		if (toArray.length > 0) {
			return Arrays.asList(toArray).stream().map(String::trim).toList();
		}
		return Collections.emptyList();
	}

	private boolean isMinorDifference(List<MailboxItemFlag> removed, List<MailboxItemFlag> added) {
		return justSeenChanged(removed, added) || justSeenChanged(added, removed);
	}

	private boolean justSeenChanged(List<MailboxItemFlag> from, List<MailboxItemFlag> to) {
		return from.size() == 1 && to.isEmpty() && from.contains(MailboxItemFlag.System.Seen.value());
	}

}
