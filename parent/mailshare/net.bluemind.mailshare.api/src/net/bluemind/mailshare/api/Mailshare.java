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
package net.bluemind.mailshare.api;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.core.api.BMApi;
import net.bluemind.directory.api.DirBaseValue;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Type;

@BMApi(version = "3")
public final class Mailshare extends DirBaseValue {

	public String name;

	public Integer quota;

	public Mailbox.Routing routing;

	public VCard card;

	public static Mailshare fromMailbox(Mailbox mb) {
		Mailshare ms = new Mailshare();
		ms.archived = mb.archived;
		ms.dataLocation = mb.dataLocation;
		ms.emails = mb.emails;
		ms.name = mb.name;
		ms.quota = mb.quota;
		ms.routing = mb.routing;
		ms.hidden = mb.hidden;
		ms.system = mb.system;
		return ms;
	}

	public Mailbox toMailbox() {
		Mailbox mb = new Mailbox();
		mb.type = Type.mailshare;
		mb.routing = this.routing;
		mb.emails = this.emails;
		mb.name = this.name;
		mb.archived = this.archived;
		mb.dataLocation = this.dataLocation;
		mb.quota = this.quota;
		mb.hidden = this.hidden;
		mb.system = this.system;
		return mb;
	}
}
