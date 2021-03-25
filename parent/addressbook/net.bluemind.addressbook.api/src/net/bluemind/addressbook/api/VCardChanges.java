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
package net.bluemind.addressbook.api;

import java.util.Collections;
import java.util.List;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class VCardChanges {

	/**
	 * updated items
	 */
	public List<ItemAdd> add;
	/**
	 * modified items
	 */
	public List<ItemModify> modify;
	/**
	 * deleted items
	 */
	public List<ItemDelete> delete;

	public static VCardChanges create(List<ItemAdd> add, List<ItemModify> modify, List<ItemDelete> delete) {
		VCardChanges ret = new VCardChanges();
		ret.add = add;
		ret.modify = modify;
		ret.delete = delete;
		return ret;
	}

	public static VCardChanges empty() {
		VCardChanges ret = new VCardChanges();
		ret.add = Collections.emptyList();
		ret.modify = Collections.emptyList();
		ret.delete = Collections.emptyList();
		return ret;
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

	@BMApi(version = "3")
	public static class ItemModify {
		public String uid;
		public VCard value;

		/**
		 * <ul>
		 * <li>null => do nothing
		 * <li>byte[0] => delete photo
		 * <li>else upload photo
		 */
		public byte[] photo;

		public static ItemModify create(String uid, VCard value) {
			ItemModify ret = new ItemModify();
			ret.uid = uid;
			ret.value = value;
			return ret;
		}
	}

	@BMApi(version = "3")
	public static class ItemAdd {
		public String uid;
		public VCard value;

		/**
		 * <ul>
		 * <li>null => do nothing
		 * <li>byte[0] => delete photo
		 * <li>else upload photo
		 */
		public byte[] photo;

		public static ItemAdd create(String uid, VCard value) {
			ItemAdd ret = new ItemAdd();
			ret.uid = uid;
			ret.value = value;
			if (value.identification.photo) {
				ret.photo = value.identification.photoBinary;
			}
			return ret;
		}
	}

}
