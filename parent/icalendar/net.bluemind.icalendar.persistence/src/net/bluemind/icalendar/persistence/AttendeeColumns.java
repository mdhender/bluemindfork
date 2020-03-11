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
package net.bluemind.icalendar.persistence;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import net.bluemind.core.jdbc.Columns;
import net.bluemind.core.jdbc.JdbcAbstractStore.EntityPopulator;
import net.bluemind.core.jdbc.JdbcAbstractStore.StatementValues;
import net.bluemind.icalendar.api.ICalendarElement;

public class AttendeeColumns {

	public static final Columns cols = Columns.create() //
			.col("cutype") // 3.2.3. Calendar User Type
			.col("member") // 3.2.11. Group or List Membership
			.col("att_role") // 3.2.16. Participation Role
			.col("partstat") // 3.2.12. Participation Status
			.col("rsvp") // 3.2.17. RSVP Expectation
			.col("delto") // 3.2.5. Delegatees
			.col("delfrom") // 3.2.4. Delegators
			.col("sentby") // 3.2.6. Directory Entry Reference
			.col("cn") // 3.2.2. Common Name
			.col("dir") // 3.2.6. Directory Entry Reference
			.col("language") // 3.2.10. Language
			.col("att_uid") // link to another entity uid
			.col("att_mailto") // mailto for external attendees
			.col("att_resp_comment"); // comment sended by attendee

	public static StatementValues<ICalendarElement> values() {
		return new StatementValues<ICalendarElement>() {

			@Override
			public int setValues(Connection conn, PreparedStatement statement, int index, int currentRow,
					ICalendarElement value) throws SQLException {

				List<ICalendarElement.Attendee> attendees = value.attendees;
				if (value.attendees != null) {
					int count = attendees.size();

					ICalendarElement.CUType[] cuType = new ICalendarElement.CUType[count];
					String[] member = new String[count];
					ICalendarElement.Role[] role = new ICalendarElement.Role[count];
					ICalendarElement.ParticipationStatus[] partStat = new ICalendarElement.ParticipationStatus[count];
					Boolean[] rsvp = new Boolean[count];
					String[] delTo = new String[count];
					String[] delFrom = new String[count];
					String[] sentBy = new String[count];
					String[] cn = new String[count];
					String[] dir = new String[count];
					String[] lang = new String[count];
					String[] mailto = new String[count];
					String[] uid = new String[count];
					String[] comment = new String[count];
					for (int i = 0; i < count; i++) {
						ICalendarElement.Attendee attendee = attendees.get(i);
						cuType[i] = attendee.cutype;
						member[i] = attendee.member;
						role[i] = attendee.role;
						partStat[i] = attendee.partStatus;
						rsvp[i] = attendee.rsvp;
						delTo[i] = attendee.delTo;
						delFrom[i] = attendee.delFrom;
						sentBy[i] = attendee.sentBy;
						cn[i] = attendee.commonName;
						dir[i] = attendee.dir;
						lang[i] = attendee.lang;
						mailto[i] = attendee.mailto;
						uid[i] = attendee.uri;
						comment[i] = attendee.responseComment;
					}

					statement.setArray(index++, conn.createArrayOf("t_icalendar_cutype", cuType));
					statement.setArray(index++, conn.createArrayOf("text", member));
					statement.setArray(index++, conn.createArrayOf("t_icalendar_role", role));
					statement.setArray(index++, conn.createArrayOf("t_icalendar_partstat", partStat));
					statement.setArray(index++, conn.createArrayOf("boolean", rsvp));
					statement.setArray(index++, conn.createArrayOf("text", delTo));
					statement.setArray(index++, conn.createArrayOf("text", delFrom));
					statement.setArray(index++, conn.createArrayOf("text", sentBy));
					statement.setArray(index++, conn.createArrayOf("text", cn));

					statement.setArray(index++, conn.createArrayOf("text", dir));

					statement.setArray(index++, conn.createArrayOf("text", lang));
					statement.setArray(index++, conn.createArrayOf("text", mailto));

					statement.setArray(index++, conn.createArrayOf("text", uid));
					statement.setArray(index++, conn.createArrayOf("text", comment));

				} else {
					statement.setNull(index++, Types.ARRAY);
					statement.setNull(index++, Types.ARRAY);
					statement.setNull(index++, Types.ARRAY);
					statement.setNull(index++, Types.ARRAY);
					statement.setNull(index++, Types.ARRAY);
					statement.setNull(index++, Types.ARRAY);
					statement.setNull(index++, Types.ARRAY);
					statement.setNull(index++, Types.ARRAY);
					statement.setNull(index++, Types.ARRAY);
					statement.setNull(index++, Types.ARRAY);
					statement.setNull(index++, Types.ARRAY);
					statement.setNull(index++, Types.ARRAY);
					statement.setNull(index++, Types.ARRAY);
					statement.setNull(index++, Types.ARRAY);
				}
				return index;
			}
		};
	}

	public static EntityPopulator<ICalendarElement> populator() {
		return new EntityPopulator<ICalendarElement>() {

			@Override
			public int populate(ResultSet rs, int index, ICalendarElement value) throws SQLException {

				List<ICalendarElement.CUType> cuType = arrayOfCUType(rs.getArray(index++));
				String[] member = arrayOfString(rs.getArray(index++));
				List<ICalendarElement.Role> role = arrayOfRole(rs.getArray(index++));
				List<ICalendarElement.ParticipationStatus> partStatus = arrayOfPartStatus(rs.getArray(index++));
				Boolean[] rsvp = arrayOfBoolean(rs.getArray(index++));
				String[] delTo = arrayOfString(rs.getArray(index++));
				String[] delFrom = arrayOfString(rs.getArray(index++));
				String[] sentBy = arrayOfString(rs.getArray(index++));
				String[] cn = arrayOfString(rs.getArray(index++));
				String[] dir = arrayOfString(rs.getArray(index++));
				String[] lang = arrayOfString(rs.getArray(index++));
				String[] mailto = arrayOfString(rs.getArray(index++));
				String[] uid = arrayOfString(rs.getArray(index++));
				String[] comment = arrayOfString(rs.getArray(index++));

				if (uid.length > 0) {
					List<ICalendarElement.Attendee> attendees = new ArrayList<>(uid.length);
					for (int i = 0; i < member.length; i++) {
						ICalendarElement.Attendee attendee = ICalendarElement.Attendee.create(cuType.get(i), member[i],
								role.get(i), partStatus.get(i), rsvp[i], delTo[i], delFrom[i], sentBy[i], cn[i], dir[i],
								lang[i], uid[i], mailto[i]);
						attendee.responseComment = stringFromArray(comment, i);
						if (null == attendee.dir) {
							attendee.internal = false;
						}
						attendees.add(attendee);
					}
					value.attendees = attendees;
				}

				return index;
			}
		};
	}

	protected static String stringFromArray(String[] array, int i) {
		if (i >= array.length) {
			return null;
		} else {
			return array[i];
		}
	}

	private static List<ICalendarElement.CUType> arrayOfCUType(Array array) throws SQLException {
		List<ICalendarElement.CUType> ret = null;

		if (array != null) {
			String[] values = (String[]) array.getArray();
			ret = new ArrayList<ICalendarElement.CUType>(values.length);
			for (String value : values) {
				if (value != null) {
					ret.add(ICalendarElement.CUType.valueOf(value));
				} else {
					ret.add(null);
				}
			}

		}
		return ret;
	}

	private static List<ICalendarElement.Role> arrayOfRole(Array array) throws SQLException {
		List<ICalendarElement.Role> ret = null;

		if (array != null) {
			String[] values = (String[]) array.getArray();
			ret = new ArrayList<ICalendarElement.Role>(values.length);
			for (String value : values) {
				if (value != null) {
					ret.add(ICalendarElement.Role.valueOf(value));
				} else {
					ret.add(null);
				}
			}

		}
		return ret;
	}

	private static List<ICalendarElement.ParticipationStatus> arrayOfPartStatus(Array array) throws SQLException {
		List<ICalendarElement.ParticipationStatus> ret = null;

		if (array != null) {
			String[] values = (String[]) array.getArray();
			ret = new ArrayList<ICalendarElement.ParticipationStatus>(values.length);
			for (String value : values) {
				if (value != null) {
					ret.add(ICalendarElement.ParticipationStatus.valueOf(value));
				} else {
					ret.add(ICalendarElement.ParticipationStatus.NeedsAction);
				}
			}

		}
		return ret;
	}

	private static Boolean[] arrayOfBoolean(Array array) throws SQLException {
		Boolean[] ret = null;
		if (array != null) {
			ret = (Boolean[]) array.getArray();
		} else {
			ret = new Boolean[0];// FIXME length
		}
		return ret;
	}

	private static String[] arrayOfString(Array array) throws SQLException {
		String[] ret = null;
		if (array != null) {
			ret = (String[]) array.getArray();
		} else {
			ret = new String[0];// FIXME length
		}
		return ret;
	}

}
