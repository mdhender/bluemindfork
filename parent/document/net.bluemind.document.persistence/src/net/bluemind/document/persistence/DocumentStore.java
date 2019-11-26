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
package net.bluemind.document.persistence;

import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Item;
import net.bluemind.document.api.DocumentMetadata;
import net.bluemind.document.storage.DocumentStorage;
import net.bluemind.document.storage.IDocumentStore;

public class DocumentStore {

	private static final Logger logger = LoggerFactory.getLogger(DocumentStore.class);

	private DocumentMetadataStore metadataStore;
	private IDocumentStore storage;

	public DocumentStore(DataSource dataSource) {
		metadataStore = new DocumentMetadataStore(dataSource);
		storage = DocumentStorage.store;
	}

	public void delete(Item item) {
		try {
			List<DocumentMetadata> docs = metadataStore.getAll(item);
			for (DocumentMetadata data : docs) {
				storage.delete(data.uid);
			}
			metadataStore.deleteAll(item);
		} catch (ServerFault sf) {
			logger.error("Fail to delete document {}", item.uid, sf);
		}
	}
}
