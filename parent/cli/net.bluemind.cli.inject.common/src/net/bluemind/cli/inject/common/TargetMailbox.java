/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.cli.inject.common;

import net.bluemind.mailbox.api.Mailbox;

public abstract class TargetMailbox {

	public static record Auth(String email, String sid, Mailbox box) {
	}

	public TargetMailbox.Auth auth;

	public TargetMailbox(TargetMailbox.Auth auth) {
		this.auth = auth;
	}

	public abstract boolean prepare();

	public abstract void exchange(TargetMailbox from, byte[] emlContent, long cycle);

}