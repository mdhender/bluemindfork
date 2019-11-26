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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.jdbc.JdbcAbstractStore;
import net.bluemind.document.api.DocumentMetadata;

public class DocumentMetadataStore extends JdbcAbstractStore {

	private static final Logger logger = LoggerFactory.getLogger(DocumentMetadataStore.class);

	public DocumentMetadataStore(DataSource dataSource) {
		super(dataSource);
	}

	public void create(Item item, DocumentMetadata value) throws ServerFault {
		logger.debug("create document metadata {} for item {} ", value.filename, item.id);

		String query = "INSERT INTO t_document ( " + DocumentMetadataColumns.COLUMNS.names() + " , item_id ) VALUES ( "
				+ DocumentMetadataColumns.COLUMNS.values() + " , ? )";
		try {
			insert(query, value, DocumentMetadataColumns.values(item));
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	public void update(Item item, DocumentMetadata value) throws ServerFault {
		logger.debug("update document metadata {} for item {} ", value.filename, item.id);

		StringBuilder query = new StringBuilder();
		query.append("UPDATE t_document SET (");
		DocumentMetadataColumns.COLUMNS.appendNames(null, query);
		query.append(") = (");
		DocumentMetadataColumns.COLUMNS.appendValues(query);
		query.append(") WHERE item_id = ? AND uid = ?");

		try {
			update(query.toString(), value, DocumentMetadataColumns.values(item), new Object[] { value.uid });
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	public DocumentMetadata get(String uid) throws ServerFault {
		logger.debug("get document metadata {} ", uid);

		DocumentMetadata ret = null;

		StringBuilder query = new StringBuilder();
		query.append("SELECT ");
		DocumentMetadataColumns.COLUMNS.appendNames(null, query);
		query.append(" FROM t_document WHERE uid = ?");
		try {
			List<DocumentMetadata> items = select(query.toString(), DOCUMENT_METADATA_CREATOR,
					DocumentMetadataColumns.populator(), new Object[] { uid });

			if (items.size() == 1) {
				ret = items.get(0);
			}
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
		return ret;
	}

	public void delete(String uid) throws ServerFault {
		logger.debug("update document metadata {}", uid);
		String query = "DELETE FROM t_document WHERE uid = ?";
		try {
			delete(query, new Object[] { uid });
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	public void deleteAll(Item item) throws ServerFault {
		logger.debug("delete all document metadata for item {}", item.id);
		String query = "DELETE FROM t_document WHERE item_id = ?";
		try {
			delete(query, new Object[] { item.id });
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	public List<DocumentMetadata> getAll(Item item) throws ServerFault {
		logger.debug("get all documents metadata {} for item ", item.id);

		List<DocumentMetadata> items = null;
		StringBuilder query = new StringBuilder();
		query.append("SELECT ");
		DocumentMetadataColumns.COLUMNS.appendNames(null, query);
		query.append(" FROM t_document WHERE item_id = ?");
		try {
			items = select(query.toString(), DOCUMENT_METADATA_CREATOR, DocumentMetadataColumns.populator(),
					new Object[] { item.id });
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

		return items;
	}

	private final Creator<DocumentMetadata> DOCUMENT_METADATA_CREATOR = new Creator<DocumentMetadata>() {
		@Override
		public DocumentMetadata create(ResultSet con) throws SQLException {
			return new DocumentMetadata();
		}
	};
}
