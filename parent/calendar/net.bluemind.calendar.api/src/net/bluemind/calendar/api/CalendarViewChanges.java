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
package net.bluemind.calendar.api;

import java.util.List;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class CalendarViewChanges {

	public List<ItemAdd> add;
	public List<ItemModify> modify;
	public List<ItemDelete> delete;

	public static CalendarViewChanges create(List<ItemAdd> add, List<ItemModify> update, List<ItemDelete> delete) {
		CalendarViewChanges ret = new CalendarViewChanges();
		ret.add = add;
		ret.modify = update;
		ret.delete = delete;
		return ret;
	}

	@BMApi(version = "3")
	public static class ItemAdd {
		public String uid;
		public CalendarView value;

		public static ItemAdd create(String uid, CalendarView value) {
			ItemAdd ret = new ItemAdd();
			ret.uid = uid;
			ret.value = value;
			return ret;
		}
	}

	@BMApi(version = "3")
	public static class ItemModify {
		public String uid;
		public CalendarView value;

		public static ItemModify create(String uid, CalendarView value) {
			ItemModify ret = new ItemModify();
			ret.uid = uid;
			ret.value = value;

			return ret;
		}
	}

	@BMApi(version = "3")
	public static class ItemDelete {
		public String uid;

		public static ItemDelete create(String uid) {
			ItemDelete ret = new ItemDelete();
			ret.uid = uid;

			return ret;
		}
	}

}
