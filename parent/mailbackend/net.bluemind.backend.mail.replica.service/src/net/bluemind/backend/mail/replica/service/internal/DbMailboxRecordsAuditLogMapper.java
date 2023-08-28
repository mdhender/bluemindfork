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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.bluemind.backend.mail.api.flags.MailboxItemFlag;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.indexing.IMailIndexService;
import net.bluemind.core.auditlogs.AuditLogEntry;
import net.bluemind.core.auditlogs.ContentElement;
import net.bluemind.core.auditlogs.ContentElement.ContentElementBuilder;
import net.bluemind.core.auditlogs.ILogMapperProvider;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.ChangeLogEntry.Type;
import net.bluemind.core.container.model.Item;

public class DbMailboxRecordsAuditLogMapper implements ILogMapperProvider<MailboxRecord> {

	private static final Logger logger = LoggerFactory.getLogger(DbMailboxRecordsAuditLogMapper.class);
	private static final ObjectMapper objectMapper = new ObjectMapper();
	private final IMailIndexService mailIndexService;
	private final BaseContainerDescriptor container;

	public DbMailboxRecordsAuditLogMapper(BaseContainerDescriptor bcd, IMailIndexService mis) {
		mailIndexService = mis;
		container = bcd;
	}

	@Override
	public AuditLogEntry enhanceAuditLogEntry(Item item, MailboxRecord oldValue, MailboxRecord newValue, Type action,
			AuditLogEntry auditLogEntry) {
		String mailboxUniqueId = container.uid.replace("mbox_records_", "");

		Map<String, Object> messageBodyMap = mailIndexService.fetchBody(mailboxUniqueId, newValue);
		if (messageBodyMap != null) {
			// Remove useless fields
			ContentElement filteredBody = filterMessageBody(messageBodyMap, newValue);
			auditLogEntry.content = filteredBody;
		}
		if (oldValue != null) {
			var tagDifference = new UpdateTagDifference(oldValue, newValue);
			if (!tagDifference.isTagDifference) {
				return null;
			}
			auditLogEntry.updatemessage = tagDifference.getUpdateMessage();
		}
		return auditLogEntry;
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

		try {
			String source = objectMapper.writeValueAsString(newValue);
			builder.newValue(source);
		} catch (JsonProcessingException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
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

	private class UpdateTagDifference {
		private static final MailboxItemFlag DELETED_TAG = MailboxItemFlag.System.Deleted.value();
		private List<MailboxItemFlag> oldFlags;
		private List<MailboxItemFlag> newFlags;
		public boolean isTagDifference = false;

		public UpdateTagDifference(MailboxRecord oldValue, MailboxRecord newValue) {
			oldFlags = oldValue.flags;
			newFlags = newValue.flags;
			isTagDifference = isTagDifference();
		}

		private boolean isTagDifference() {
			// The Deleted flags are in old and new values -> no need for update detection
			if (oldFlags.contains(DELETED_TAG) && newFlags.contains(DELETED_TAG)) {
				return false;
			}
			return oldFlags.contains(DELETED_TAG) || newFlags.contains(DELETED_TAG);
		}

		public String getUpdateMessage() {
			if (oldFlags.contains(DELETED_TAG) && !newFlags.contains(DELETED_TAG)) {
				return "Flag " + DELETED_TAG + " has been undeleted.";
			}
			if (!oldFlags.contains(DELETED_TAG) && newFlags.contains(DELETED_TAG)) {
				return "Flag " + DELETED_TAG + " has been added.";
			}
			return "";
		}
	}

}
