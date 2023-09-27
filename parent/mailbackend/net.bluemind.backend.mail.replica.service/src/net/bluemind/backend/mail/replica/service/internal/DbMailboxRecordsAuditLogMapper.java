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

	public DbMailboxRecordsAuditLogMapper(BaseContainerDescriptor bcd, IMailIndexService mis) {
		mailIndexService = mis;
		container = bcd;
	}

	@Override
	public ContentElement createContentElement(MailboxRecord itemValue) {
		String mailboxUniqueId = container.uid.replace("mbox_records_", "");

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
		List<MailboxItemFlag> removedFlags = oldFlags.stream().filter(element -> !newFlags.contains(element))
				.collect(Collectors.toList());
		List<MailboxItemFlag> addedFlags = newFlags.stream().filter(element -> !oldFlags.contains(element))
				.collect(Collectors.toList());
		StringBuilder stringBuilder = new StringBuilder();
		if (!removedFlags.isEmpty()) {
			stringBuilder.append("Removed Flags:\n")
					.append(addedFlags.stream().map(MailboxItemFlag::toString).collect(Collectors.joining(",")))
					.append("\n");
		}
		if (!addedFlags.isEmpty()) {
			stringBuilder.append("Added Flags:\n")
					.append(addedFlags.stream().map(MailboxItemFlag::toString).collect(Collectors.joining(",")))
					.append("\n");
		}
		if (isMinorDifference(newFlags)) {
			return new AuditLogUpdateStatus(stringBuilder.toString(), MessageCriticity.MINOR);
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
			return Arrays.asList(toArray).stream().map(s -> s.trim()).toList();
		}
		return Collections.emptyList();
	}

	private boolean isMinorDifference(List<MailboxItemFlag> newFlags) {
		return newFlags.size() == 1 && newFlags.contains(MailboxItemFlag.System.Seen.value());
	}

}
