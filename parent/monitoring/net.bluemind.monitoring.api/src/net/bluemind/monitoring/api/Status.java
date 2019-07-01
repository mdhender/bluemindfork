/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2016
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
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
package net.bluemind.monitoring.api;

import net.bluemind.core.api.BMApi;

/**
 * A status is an indicator of the result type of an information.
 */
@BMApi(version = "3")
public enum Status {

	UNKNOWN(-1, "Unknown"), OK(0, "Success"), WARNING(1, "Warning"), KO(2, "Error");

	/**
	 * Value (code) of the status. The higher the more important.
	 */
	private int value;

	/**
	 * Title of the status
	 */
	private String title;

	private Status(int value, String title) {
		this.value = value;
		this.title = title;
	}

	public int getValue() {
		return this.value;
	}

	public String getTitle() {
		return title;
	}

	@Override
	public String toString() {
		return this.title;
	}

}
