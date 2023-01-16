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
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.delivery.lmtp.internal;

public enum RecipientAcceptance {

	ACCEPT(250, true, "Ok"),

	TEMPORARY_REJECT(452, false, null),

	PERMANENT_REJECT(553, false, null);

	private final int code;
	private final boolean deliver;
	private final RecipientDeliveryStatus constReason;

	private RecipientAcceptance(int code, boolean deliver, String s) {
		this.code = code;
		this.deliver = deliver;
		this.constReason = s != null ? reason(s) : null;
	}

	public boolean deliver() {
		return deliver;
	}

	public int code() {
		return code;
	}

	public RecipientDeliveryStatus reason(String s) {
		return constReason != null ? constReason : new RecipientDeliveryStatus(this, s);
	}

}
