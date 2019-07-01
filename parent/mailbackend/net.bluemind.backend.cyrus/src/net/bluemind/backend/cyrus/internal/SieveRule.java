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

import net.bluemind.mailbox.api.MailFilter;

public class SieveRule extends MailFilter.Rule {

	public SieveRule(MailFilter.Rule f, String rule) {
		this.rule = rule;
		this.active = f.active;
		this.criteria = f.criteria;
		this.delete = f.delete;
		this.deliver = f.deliver;
		this.star = f.star;
		this.forward = f.forward;
		this.discard = f.discard;
		this.read = f.read;
	}

	public String rule;

}
