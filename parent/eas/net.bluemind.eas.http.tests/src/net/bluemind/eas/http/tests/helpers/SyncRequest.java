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
package net.bluemind.eas.http.tests.helpers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;

import net.bluemind.eas.http.tests.builders.DocumentRequestBuilder;
import net.bluemind.eas.http.tests.builders.DocumentRequestBuilder.TemplateKey;

public record SyncRequest(boolean getChanges, List<Document> clientChangesAdd,
		List<ClientChangesModify> clientChangesModify, List<ClientChangesDelete> clientChangesDelete) {

	public static class SyncRequestBuilder {
		boolean getChanges = false;
		List<Document> clientChangesAdd = new ArrayList<>();
		List<ClientChangesModify> clientChangesModify = new ArrayList<>();
		List<ClientChangesDelete> clientChangesDelete = new ArrayList<>();

		public SyncRequestBuilder withChanges() {
			getChanges = true;
			return this;
		}

		public SyncRequestBuilder withClientChangesAdd(Document clientChange) {
			this.clientChangesAdd.add(clientChange);
			return this;
		}

		public SyncRequestBuilder withClientChangesModify(String serverId, Document clientChange) {
			this.clientChangesModify.add(new ClientChangesModify(serverId, clientChange));
			return this;
		}

		public SyncRequestBuilder withClientChangesDelete(String serverId) {
			this.clientChangesDelete.add(new ClientChangesDelete(serverId, null));
			return this;
		}

		public SyncRequestBuilder withClientChangesDeleteOccurrence(String serverId, String recurId) {
			this.clientChangesDelete.add(new ClientChangesDelete(serverId, recurId));
			return this;
		}

		public SyncRequestBuilder withClientChangesBodyHtmlModify(String serverId, String to, String subject,
				String html) throws Exception {
			Map<TemplateKey, String> values = new HashMap<>();
			values.put(TemplateKey.Subject, subject);
			values.put(TemplateKey.To, to);
			values.put(TemplateKey.Html, html);
			this.clientChangesModify.add(new ClientChangesModify(serverId,
					DocumentRequestBuilder.getDocumentRequestUpdate("SyncMailbodyUpdate.xml", values)));
			return this;
		}

		public SyncRequestBuilder withClientChangesBodyHtmlReadModify(String serverId, boolean read) throws Exception {
			Map<TemplateKey, String> values = new HashMap<>();
			values.put(TemplateKey.Read, read ? "1" : "0");
			this.clientChangesModify.add(new ClientChangesModify(serverId,
					DocumentRequestBuilder.getDocumentRequestUpdate("SyncMailReadUpdate.xml", values)));
			return this;
		}

		public SyncRequest build() {
			return new SyncRequest(getChanges, clientChangesAdd, clientChangesModify, clientChangesDelete);
		}

	}

	public SyncRequestBuilder copy() {
		SyncRequestBuilder syncRequestBuilder = new SyncRequestBuilder();
		if (getChanges) {
			syncRequestBuilder.withChanges();
		}
		syncRequestBuilder.clientChangesAdd = clientChangesAdd;
		syncRequestBuilder.clientChangesModify = clientChangesModify;
		syncRequestBuilder.clientChangesDelete = clientChangesDelete;
		return syncRequestBuilder;
	}

	@Override
	public String toString() {
		return getClass().getName();
	}

	public record ClientChangesModify(String serverId, Document data) {
	}

	public record ClientChangesDelete(String serverId, String recurId) {
	}
}
