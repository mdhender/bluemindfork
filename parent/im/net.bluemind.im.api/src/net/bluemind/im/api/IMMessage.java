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
package net.bluemind.im.api;

import java.util.Date;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class IMMessage {

	public Date timestamp;
	public String from;
	public String to;
	public String body;

	public IMMessage() {

	}

	@Override
	public String toString() {
		return "from: " + from + ", to: " + to + ", date: " + timestamp + ", body: " + body;
	}

}
