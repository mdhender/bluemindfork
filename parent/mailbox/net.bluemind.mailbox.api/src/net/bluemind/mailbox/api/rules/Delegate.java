/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2024
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
package net.bluemind.mailbox.api.rules;

import net.bluemind.core.api.BMApi;

/**
 * A delegate has access to its boss mailbox
 */
@BMApi(version = "3")
public class Delegate {

	public String uid;
	public boolean keepCopy;

	public Delegate() {

	}

	public Delegate(String uid, boolean keepCopy) {
		this.uid = uid;
		this.keepCopy = keepCopy;
	}
}
