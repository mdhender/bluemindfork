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
package net.bluemind.ui.common.client.forms.acl;

import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.directory.api.DirEntry;

public class AclEntity {

	private DirEntry entry;
	private Verb verb;

	public AclEntity(DirEntry entry, Verb verb) {
		this.entry = entry;
		this.verb = verb;
	}

	public DirEntry getEntry() {
		return entry;
	}

	public Verb getVerb() {
		return verb;
	}
}
