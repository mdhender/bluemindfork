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
package net.bluemind.tag.service.internal;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.tag.api.Tag;

public class TagValidator {

	public void validate(Tag tag) throws ServerFault {
		if (tag == null) {
			throw new ServerFault("tag shouldnt be null");
		}

		if (tag.label == null || tag.label.length() == 0) {
			throw new ServerFault("tag label shouldnt be empty");
		}

		if (tag.color == null || tag.color.isEmpty()) {
			throw new ServerFault("tag color should not be empty");
		}

		// TODO check tag.color validity ?

	}

}
