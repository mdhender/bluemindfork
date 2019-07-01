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
package net.bluemind.device.api;

import java.util.Date;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class Device {

	public String identifier;
	public String owner;
	public String type;

	public Date wipeDate;
	public String wipeBy;

	public Date unwipeDate;
	public String unwipeBy;

	public boolean isWipe = false;

	public boolean hasPartnership = false;
	public Integer policy = 0;

	public Date lastSync;
}
