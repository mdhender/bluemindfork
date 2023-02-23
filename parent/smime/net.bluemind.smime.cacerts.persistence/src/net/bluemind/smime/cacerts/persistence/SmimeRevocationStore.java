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
package net.bluemind.smime.cacerts.persistence;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import javax.sql.DataSource;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.jdbc.JdbcAbstractStore;
import net.bluemind.smime.cacerts.api.SmimeCacert;
import net.bluemind.smime.cacerts.api.SmimeCertClient;
import net.bluemind.smime.cacerts.api.SmimeRevocation;

public class SmimeRevocationStore extends JdbcAbstractStore {

	private static final Creator<SmimeRevocation> SMIME_REVOCATION_CREATOR = con -> new SmimeRevocation();

	private Container container;

	public SmimeRevocationStore(DataSource pool, Container container) {
		super(pool);
		this.container = container;
	}

	private static String insertQuery() {
		StringBuilder query = new StringBuilder("INSERT INTO t_smime_revocations (");
		query.append(SmimeRevocationColumns.cols.names());
		query.append(") VALUES (");
		query.append(SmimeRevocationColumns.cols.values());
		query.append(")");
		return query.toString();
	}

	public void create(SmimeRevocation revocation, ItemValue<SmimeCacert> cacert) throws SQLException {
		insert(insertQuery(), revocation, SmimeRevocationColumns.values(cacert));
	}

	public void batchInsert(List<SmimeRevocation> revocations, ItemValue<SmimeCacert> cacert) throws SQLException {
		batchInsert(insertQuery(), revocations, SmimeRevocationColumns.values(cacert));
	}

	public List<SmimeRevocation> getByCacert(ItemValue<SmimeCacert> cacert) throws SQLException {
		StringBuilder query = new StringBuilder("SELECT ");
		SmimeRevocationColumns.cols.appendNames("rev", query);
		query.append(" FROM t_smime_revocations rev ");
		query.append(" WHERE rev.ca_item_id = ? ");
		return select(query.toString(), SMIME_REVOCATION_CREATOR, SmimeRevocationColumns.populator(cacert),
				new Object[] { cacert.item().id });
	}

	public List<SmimeRevocation> getBySnList(List<String> snList) throws SQLException {
		StringBuilder query = new StringBuilder("SELECT ");
		SmimeRevocationColumns.cols.appendNames("rev", query);
		query.append(", ci.uid FROM t_smime_revocations rev ");
		query.append(" JOIN t_container_item ci ON ci.id = rev.ca_item_id ");
		query.append(" WHERE rev.serial_number = ANY (?::text[]) ");
		query.append(" AND ci.container_id = ? ");

		final String[] array = snList.stream().toArray(String[]::new);
		return select(query.toString(), SMIME_REVOCATION_CREATOR, SmimeRevocationColumns.populator(),
				new Object[] { array, container.id });
	}

	public List<SmimeRevocation> getBySn(String sn) throws SQLException {
		StringBuilder query = new StringBuilder("SELECT ");
		SmimeRevocationColumns.cols.appendNames("rev", query);
		query.append(", ci.uid FROM t_smime_revocations rev ");
		query.append(" JOIN t_container_item ci ON ci.id = rev.ca_item_id ");
		query.append(" WHERE rev.serial_number like ? ");
		query.append(" AND ci.container_id = ? ");

		return select(query.toString(), SMIME_REVOCATION_CREATOR, SmimeRevocationColumns.populator(),
				new Object[] { sn, container.id });
	}

	public SmimeRevocation getByCertClient(SmimeCertClient client) throws SQLException {
		StringBuilder query = new StringBuilder("SELECT ");
		SmimeRevocationColumns.cols.appendNames("rev", query);
		query.append(", ci.uid FROM t_smime_revocations rev ");
		query.append(" JOIN t_container_item ci ON ci.id = rev.ca_item_id ");
		query.append(" WHERE rev.serial_number like ? ");
		query.append(" AND rev.issuer like ? ");
		query.append(" AND ci.container_id = ? ");

		return unique(query.toString(), SMIME_REVOCATION_CREATOR, SmimeRevocationColumns.populator(),
				new Object[] { client.serialNumber, client.issuer, container.id });
	}

	public SmimeRevocation getBySn(String sn, ItemValue<SmimeCacert> cacert) throws SQLException {
		StringBuilder query = new StringBuilder("SELECT ");
		SmimeRevocationColumns.cols.appendNames("rev", query);
		query.append(" FROM t_smime_revocations rev ");
		query.append(" WHERE rev.serial_number like ? ");
		query.append(" AND rev.ca_item_id = ? ");

		return unique(query.toString(), SMIME_REVOCATION_CREATOR, SmimeRevocationColumns.populator(cacert),
				new Object[] { sn, cacert.item().id });
	}

	public List<SmimeRevocation> get(ItemValue<SmimeCacert> cacert) throws SQLException {
		StringBuilder query = new StringBuilder("SELECT ");
		SmimeRevocationColumns.cols.appendNames("rev", query);
		query.append(" FROM t_smime_revocations rev ");
		query.append(" WHERE rev.ca_item_id = ? ");

		return select(query.toString(), SMIME_REVOCATION_CREATOR, SmimeRevocationColumns.populator(cacert),
				new Object[] { cacert.item().id });
	}

	public void delete(ItemValue<SmimeCacert> cacert) throws SQLException {
		delete("DELETE FROM t_smime_revocations rev WHERE rev.ca_item_id = ? ", new Object[] { cacert.item().id });
	}

	public List<SmimeRevocation> all() throws SQLException {
		StringBuilder query = new StringBuilder("SELECT ");
		SmimeRevocationColumns.cols.appendNames("rev", query);
		query.append(", ci.uid FROM t_smime_revocations rev ");
		query.append(" JOIN t_container_item ci ON ci.id = rev.ca_item_id ");
		query.append(" WHERE ci.container_id = ? ");

		return select(query.toString(), SMIME_REVOCATION_CREATOR, SmimeRevocationColumns.populator(),
				new Object[] { container.id });
	}

	public List<SmimeRevocation> getByNextUpdateDate(Timestamp update) throws SQLException {
		StringBuilder query = new StringBuilder("SELECT ");
		SmimeRevocationColumns.cols.appendNames("rev", query);
		query.append(", ci.uid FROM t_smime_revocations rev ");
		query.append(" JOIN t_container_item ci ON ci.id = rev.ca_item_id ");
		query.append(" WHERE rev.next_update <= ? ");
		query.append(" AND ci.container_id = ? ");

		return select(query.toString(), SMIME_REVOCATION_CREATOR, SmimeRevocationColumns.populator(),
				new Object[] { update, container.id });
	}

}
