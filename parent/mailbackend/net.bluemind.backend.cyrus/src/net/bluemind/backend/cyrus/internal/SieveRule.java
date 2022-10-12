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

package net.bluemind.backend.cyrus.internal;

import net.bluemind.mailbox.api.MailFilter.Forwarding;
import net.bluemind.mailbox.api.rules.MailFilterRule;

public class SieveRule {
	public String rule;
	public boolean star;
	public boolean read;
	public boolean delete;
	public boolean discard;
	public Forwarding forward = new Forwarding();
	public String deliver;
	public boolean active = true;
	public boolean stop = true;

	public SieveRule(MailFilterRule f, String rule) {
		this.rule = rule;
		this.active = f.active;
		this.stop = f.stop;
		this.delete = f.markAsDeleted().isPresent();
		this.star = f.markAsImportant().isPresent();
		this.read = f.markAsRead().isPresent();
		this.deliver = f.move().map(move -> move.folder).orElse(null);
		this.forward = f.redirect().map(Forwarding::fromAction).orElse(new Forwarding());
		this.discard = f.discard().isPresent();
	}

}
