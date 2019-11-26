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
package net.bluemind.addressbook.persistence;

import java.sql.Array;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.core.jdbc.Columns;

public class OrganizationalColumns {

	public static final Columns COLUMNS = Columns.create() //
			//
			.col("title") //
			.col("role") //
			.col("company") //
			.col("division") //
			.col("department") //
			.col("member_container_uid") //
			.col("member_item_uid") //
			.col("member_cn") //
			.col("member_mailto");

	public static VCardStore.StatementValues<VCard> values() {
		return (conn, statement, index, currentRow, value) -> {

			VCard.Organizational organizational = value.organizational;

			statement.setString(index++, organizational.title);
			statement.setString(index++, organizational.role);
			statement.setString(index++, organizational.org.company);
			statement.setString(index++, organizational.org.division);
			statement.setString(index++, organizational.org.department);

			String[] memberContainerUid = new String[organizational.member.size()];
			String[] memberItemUid = new String[organizational.member.size()];
			String[] memberCn = new String[organizational.member.size()];
			String[] memberMailto = new String[organizational.member.size()];
			for (int i = 0; i < organizational.member.size(); i++) {
				memberContainerUid[i] = organizational.member.get(i).containerUid;
				memberItemUid[i] = organizational.member.get(i).itemUid;
				memberCn[i] = organizational.member.get(i).commonName;
				memberMailto[i] = organizational.member.get(i).mailto;
			}

			statement.setArray(index++, conn.createArrayOf("text", memberContainerUid));
			statement.setArray(index++, conn.createArrayOf("text", memberItemUid));
			statement.setArray(index++, conn.createArrayOf("text", memberCn));
			statement.setArray(index++, conn.createArrayOf("text", memberMailto));

			return index;
		};

	}

	public static VCardStore.EntityPopulator<VCard> populator() {
		return (rs, index, value) -> {

			String title = rs.getString(index++);
			String role = rs.getString(index++);
			String company = rs.getString(index++);
			String div = rs.getString(index++);
			String dept = rs.getString(index++);

			String[] membersContainerUid = arrayOfString(rs.getArray(index++));
			String[] membersItemUid = arrayOfString(rs.getArray(index++));
			String[] membersCn = arrayOfString(rs.getArray(index++));
			String[] membersMailto = arrayOfString(rs.getArray(index++));

			List<VCard.Organizational.Member> members = new ArrayList<>(membersContainerUid.length);

			for (int i = 0; i < membersContainerUid.length; i++) {
				VCard.Organizational.Member member = VCard.Organizational.Member.create(membersContainerUid[i],
						membersItemUid[i], membersCn[i], membersMailto[i]);
				members.add(member);
			}
			value.organizational = VCard.Organizational.create(title, role,
					VCard.Organizational.Org.create(company, div, dept), members);
			return index;
		};

	}

	protected static String[] arrayOfString(Array array) throws SQLException {
		String[] ret = null;
		if (array != null) {
			ret = (String[]) array.getArray();
		} else {
			ret = new String[0];
		}
		return ret;
	}

}