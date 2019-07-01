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
package net.bluemind.cti.api;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class Status {

	@BMApi(version = "3")
	public enum Type {
		DoNotDisturb("BUSY"), Available("AVAILABLE"), Busy("BUSY"), Offline("UNEXISTING");

		private final String code;

		Type(String code) {
			this.code = code;
		}

		public String code() {
			return code;
		}
	}

	@BMApi(version = "3")
	public enum PhoneState {
		Unknown, Available, DoNotDisturb, Ringing, Busy, OnHold
	}

	/**
	 * Status type
	 */
	public Type type;

	/**
	 * Status message
	 */
	public String message;
	/**
	 * Phone state
	 */
	public PhoneState phoneState;

	public static Status create(Type type, String message) {
		Status st = new Status();
		st.type = type;
		st.phoneState = PhoneState.Unknown;
		st.message = message;
		return st;
	}

	public static Status create(Type type, PhoneState phoneState, String message) {
		Status st = new Status();
		st.type = type;
		st.phoneState = phoneState;
		st.message = message;
		return st;
	}

	public static Status unexisting() {
		Status st = new Status();
		st.type = Type.Offline;
		st.phoneState = PhoneState.Unknown;
		return st;
	}
}
