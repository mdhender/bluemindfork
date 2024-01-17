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
package net.bluemind.eas.state;

import net.bluemind.eas.dto.base.ChangeType;

public class SentStatus {

	private String key;
	private ChangeType sentType;

	public SentStatus() {
		this.sentType = ChangeType.ADD;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public ChangeType getSentType() {
		return sentType;
	}

	public void setSentType(ChangeType sentType) {
		this.sentType = sentType;
	}

	@Override
	public String toString() {
		return sentType + " " + key;
	}

}
