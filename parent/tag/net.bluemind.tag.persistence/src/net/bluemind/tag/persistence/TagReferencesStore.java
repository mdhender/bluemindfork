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
package net.bluemind.tag.persistence;

import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import net.bluemind.core.container.model.ItemUri;
import net.bluemind.core.jdbc.JdbcAbstractStore;

public class TagReferencesStore extends JdbcAbstractStore {

	public TagReferencesStore(DataSource dataSource) {
		super(dataSource);
	}

	public List<ItemUri> referencedBy(String containerType, String tagContainerUid, String tagUid) throws SQLException {
		String select = "SELECT tagged_container_uid, tagged_item_uid FROM t_tagref"
				+ " WHERE ref_container_uid = ? AND ref_item_uid = ? AND tagged_container_type = ?"
				+ " ORDER BY ref_container_uid";
		return select(select, rs -> new ItemUri(), (rs, index, value) -> {
			value.containerUid = rs.getString(index++);
			value.itemUid = rs.getString(index++);
			return index;
		}, new Object[] { tagContainerUid, tagUid, containerType });
	}

	public void deleteReferences(String containerType, String tagContainerUid, String tagUid) throws SQLException {
		String select = "DELETE FROM t_tagref"
				+ " WHERE ref_container_uid = ? AND ref_item_uid = ? AND tagged_container_type = ?";
		delete(select, new Object[] { tagContainerUid, tagUid, containerType });
	}

}
