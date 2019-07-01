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
package net.bluemind.ui.adminconsole.dataprotect;

import net.bluemind.dataprotect.api.Restorable;

public class ClientRestorable extends Restorable {

	public ClientRestorable(Restorable rest, boolean deleted) {
		super.displayName = rest.displayName;
		super.domainUid = rest.domainUid;
		super.entryUid = rest.entryUid;
		super.kind = rest.kind;
		this.deleted = deleted;
	}

	public boolean deleted;

}
