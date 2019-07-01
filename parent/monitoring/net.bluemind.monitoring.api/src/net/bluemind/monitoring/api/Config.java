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

import java.util.ArrayList;
import java.util.List;

import net.bluemind.core.api.BMApi;

/**
 * Holds monitoring data like CPU or memory info.
 */
@BMApi(version = "3")
public class Config {

	/** Monitoring info list. */
	public List<String> part;

	public Config() {

	}

	/**
	 * Add monitoring info.
	 * 
	 * @param part the monitoring data to add
	 */
	public void addPart(String part) {
		if (this.part == null) {
			this.part = new ArrayList<String>();
		}

		this.part.add(part);
	}

}
